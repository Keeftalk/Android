package com.keeftalk.chat.security.crypto

import android.util.Log
import android.util.Base64
import com.keeftalk.chat.data.local.dao.MessageDao
import com.keeftalk.chat.data.local.entities.MessageEntity
import com.keeftalk.chat.domain.model.DecryptionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

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
    private val B64_FLAGS = Base64.NO_WRAP
    
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
                ciphertext = Base64.decode(entity.ciphertext, B64_FLAGS),
                nonce = entity.nonce
            )
            
            val decrypted = try {
                cryptoManager.decryptMessage(
                    chatId = entity.chatId, 
                    messageId = entity.id, 
                    senderId = entity.senderId,
                    envelope = envelope, 
                    cryptoVersion = entity.cryptoVersion
                )
            } catch (e: Exception) {
                if (e.message?.contains("BAD_DECRYPT") == true) {
                    Log.e(TAG, "[DECRYPT_PIPELINE] BAD_DECRYPT | chatId=${entity.chatId} | msgId=${entity.id} | version=${entity.cryptoVersion}")
                }
                throw e
            }
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
                Log.d(TAG, "[DECRYPT_PIPELINE] SUCCESS | updating local state for id=${entity.id}")
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
            
            // Differentiate between retryable and permanent failures
            val isBadDecrypt = errorMsg.contains("BAD_DECRYPT") || errorMsg.contains("Tag mismatch")
            val isKeyMissing = errorMsg.contains("PCK not found")
            
            val permanent = isBadDecrypt || entity.retryCount >= 10
            val nextState = when {
                permanent -> DecryptionState.PERMANENT_FAILURE
                isKeyMissing -> DecryptionState.PENDING // Wait for key sync
                else -> DecryptionState.RETRY_REQUIRED
            }

            if (updateDb && entity.decryptionState != nextState) {
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
                    // Only repair messages that aren't marked as permanent failures
                    val pendingCount = messageDao.countMessagesNeedingDecryption(maxRetries = 11)
                    if (pendingCount > 0) {
                        Log.d(TAG, "[BACKGROUND_REPAIR] Retrying $pendingCount messages...")
                        val messages = messageDao.getMessagesForRepair(limit = 20, maxRetries = 11)
                        messages.forEach { decrypt(it, updateDb = true, source = "REPAIR") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[BACKGROUND_REPAIR] Cycle failed", e)
                }
                delay(60000) // Run every 60 seconds to save battery
            }
        }
    }
}
