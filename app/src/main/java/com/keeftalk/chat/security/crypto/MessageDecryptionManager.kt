package com.keeftalk.chat.security.crypto

import android.util.Log
import com.keeftalk.chat.data.local.dao.MessageDao
import com.keeftalk.chat.data.local.entities.MessageEntity
import com.keeftalk.chat.domain.model.DecryptionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import java.util.Base64

/**
 * Centralized service for message decryption across all app components.
 * Exclusively uses Cloud-E2EE (PCK) architecture.
 */
class MessageDecryptionManager(
    private val messageDao: MessageDao,
    private val cryptoManager: CryptoManager
) {
    private val TAG = "DecryptionManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _decryptionEvents = MutableStateFlow<String?>(null)
    /** Emits messageId when a message decryption state changes. */
    val decryptionEvents: StateFlow<String?> = _decryptionEvents.asStateFlow()

    init {
        Log.i(TAG, "Initializing MessageDecryptionManager for user: ${cryptoManager.userId}")
        startBackgroundRepair()
    }

    /**
     * Shuts down the manager, cancelling background tasks and releasing resources.
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down MessageDecryptionManager for user: ${cryptoManager.userId}")
        scope.cancel("Manager shutdown")
    }

    sealed class DecryptionResult {
        data class Success(val plaintext: String, val wrappedFek: String? = null, val mediaIv: String? = null) : DecryptionResult()
        data class Pending(val reason: String) : DecryptionResult()
        data class Failed(val error: String, val permanent: Boolean = false) : DecryptionResult()
    }

    /**
     * Attempts to decrypt a message entity using the PCK architecture.
     */
    suspend fun decrypt(entity: MessageEntity, updateDb: Boolean = true, source: String = "MANUAL"): DecryptionResult {
        if (entity.ciphertext.isNullOrEmpty()) {
            return DecryptionResult.Success(entity.content)
        }

        val startTime = System.currentTimeMillis()
        com.keeftalk.chat.util.PerformanceProfiler.startStage("Message Decryption: ${entity.id}")

        try {
            val envelope = EncryptedMessageEnvelope(
                type = entity.envelopeType,
                ciphertext = Base64.getDecoder().decode(entity.ciphertext),
                nonce = entity.nonce
            )
            
            val decrypted = cryptoManager.decryptMessage(entity.chatId, envelope, entity.cryptoVersion)
            var decryptedContent = decrypted
            var wrappedFek: String? = null
            var mediaIv: String? = null
            
            try {
                val json = Json.parseToJsonElement(decrypted).jsonObject
                decryptedContent = json["text"]?.jsonPrimitive?.content ?: decrypted
                wrappedFek = json["wrappedFek"]?.jsonPrimitive?.content ?: json["mediaKey"]?.jsonPrimitive?.content
                mediaIv = json["mediaIv"]?.jsonPrimitive?.content
            } catch (_: Exception) {}
            
            if (updateDb) {
                messageDao.updateMessageContent(entity.id, decryptedContent)
                messageDao.updateDecryptionState(entity.id, DecryptionState.SUCCESS, entity.retryCount)
                _decryptionEvents.value = entity.id
            }

            com.keeftalk.chat.util.PerformanceProfiler.endStage("Message Decryption: ${entity.id}", category = com.keeftalk.chat.util.PerformanceProfiler.Category.ENCRYPTION)
            
            Log.i(TAG, "[DECRYPT_PIPELINE] SUCCESS | id=${entity.id} | source=$source | timeMs=${System.currentTimeMillis() - startTime}")
            return DecryptionResult.Success(decryptedContent, wrappedFek, mediaIv)

        } catch (e: Exception) {
            com.keeftalk.chat.util.PerformanceProfiler.endStage("Message Decryption: ${entity.id}", isError = true, category = com.keeftalk.chat.util.PerformanceProfiler.Category.ENCRYPTION)
            val errorMsg = e.message ?: "Unknown error"
            Log.w(TAG, "[DECRYPT_PIPELINE] FAILED | id=${entity.id} | source=$source | error=$errorMsg | timeMs=${System.currentTimeMillis() - startTime}")
            
            val permanent = entity.retryCount >= 5
            val nextState = if (permanent) DecryptionState.PERMANENT_FAILURE else DecryptionState.RETRY_REQUIRED

            if (updateDb) {
                messageDao.updateDecryptionState(entity.id, nextState, entity.retryCount + 1)
                _decryptionEvents.value = entity.id
            }

            return if (permanent) DecryptionResult.Failed(errorMsg, true) else DecryptionResult.Pending(errorMsg)
        }
    }

    private fun startBackgroundRepair() {
        scope.launch {
            while (isActive) {
                try {
                    val pendingCount = messageDao.countMessagesNeedingDecryption(maxRetries = 6)
                    if (pendingCount > 0) {
                        Log.d(TAG, "[BACKGROUND_REPAIR] Retrying $pendingCount messages...")
                        val messages = messageDao.getMessagesForRepair(limit = 10, maxRetries = 6)
                        messages.forEach { decrypt(it, updateDb = true, source = "REPAIR") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[BACKGROUND_REPAIR] Cycle failed", e)
                }
                delay(30000) // Run every 30 seconds
            }
        }
    }
}
