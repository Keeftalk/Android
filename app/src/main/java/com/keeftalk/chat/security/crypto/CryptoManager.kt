package com.keeftalk.chat.security.crypto

import javax.crypto.SecretKey
import android.util.Log
import android.util.Base64

/**
 * Unified entry point for all cryptographic operations in Keeftalk.
 * Exclusively uses Cloud-E2EE (PCK/AEK) architecture with random keys.
 */
class CryptoManager(
    private val conversationKeyManager: ConversationKeyManager,
    val userId: String
) {
    private val TAG = "CryptoManager"
    private val B64_FLAGS = Base64.NO_WRAP

    // --- Messaging (PCK-based) ---

    /**
     * Encrypts a message for a conversation using its shared Per-Conversation Key (PCK).
     * Returns the envelope and the crypto version used.
     */
    suspend fun encryptMessage(
        chatId: String, 
        messageId: String,
        senderId: String,
        plaintext: String
    ): Pair<EncryptedMessageEnvelope, Int> {
        // Resolve the PCK. This will derive a deterministic shared key if one hasn't been shared yet.
        val key = conversationKeyManager.getOrLoadKey(chatId) ?: throw Exception("Failed to resolve encryption key for chat $chatId")
        
        Log.d(TAG, "[CRYPTO_DIAG] encryptMessage | chatId=$chatId | msgId=$messageId")

        // 1. Build AAD for Protocol Version 4 (Normalized and String-based)
        val version = 4
        val (aad, aadStr) = MessageCryptoContext.buildMessageAad(version, chatId, messageId)

        // 2. Encrypt with AAD
        val encryptedObj = StorageCryptoService.encrypt(plaintext.toByteArray(Charsets.UTF_8), key, aad, aadStr)
        
        val envelope = EncryptedMessageEnvelope(
            type = 100, // Cloud-E2EE PCK Type
            ciphertext = Base64.decode(encryptedObj.ciphertext, B64_FLAGS),
            nonce = encryptedObj.iv
        )
        return envelope to version
    }

    /**
     * Decrypts a message using the conversation's shared Per-Conversation Key (PCK).
     */
    suspend fun decryptMessage(
        chatId: String, 
        messageId: String,
        senderId: String,
        envelope: EncryptedMessageEnvelope, 
        cryptoVersion: Int = 1
    ): String {
        // Treat type 0 (Legacy/Unspecified) as 100 (Cloud-E2EE) for robustness
        if (envelope.type != 100 && envelope.type != 0) {
            throw Exception("Unsupported message type: ${envelope.type}. Only Cloud-E2EE (100) is supported.")
        }

        val nonce = envelope.nonce ?: throw Exception("Nonce missing for PCK message")
        
        Log.d(TAG, "[CRYPTO_DIAG] decryptMessage | chatId=$chatId | msgId=$messageId | cryptoVer=$cryptoVersion")

        // 1. Resolve PCK (Synchronized with encryption derivation)
        val key = conversationKeyManager.getOrLoadKey(chatId) ?: run {
            throw Exception("PCK not found for chat $chatId. Decryption aborted.")
        }

        // 2. Reconstruct AAD for Version 4+ ONLY
        // Version 3 and below are treated as legacy (No AAD) to restore compatibility
        val (aad, aadStr) = if (cryptoVersion >= 4) {
            MessageCryptoContext.buildMessageAad(cryptoVersion, chatId, messageId)
        } else null to null

        // 3. Decrypt
        val encryptedObj = EncryptedObject(
            version = 1,
            keyId = "pck",
            iv = nonce,
            ciphertext = Base64.encodeToString(envelope.ciphertext, B64_FLAGS)
        )
        
        return try {
            val decryptedBytes = StorageCryptoService.decrypt(encryptedObj, key, aad, aadStr)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES-GCM Decryption failed for chatId=$chatId version=$cryptoVersion. Error=${e.message}")
            throw e
        }
    }

    // --- Storage (Notes, Agenda, Vault) ---

    /**
     * Encrypts data for storage using domain-separated root keys.
     */
    fun encryptForStorage(data: ByteArray, purpose: StoragePurpose): EncryptedObject {
        val key = getKeyForPurpose(purpose)
        return StorageCryptoService.encrypt(data, key)
    }

    /**
     * Decrypts data from storage using domain-separated root keys.
     */
    fun decryptFromStorage(encrypted: EncryptedObject, purpose: StoragePurpose): ByteArray {
        val key = getKeyForPurpose(purpose)
        return StorageCryptoService.decrypt(encrypted, key)
    }

    // --- Media ---

    fun encryptMedia(data: ByteArray): Pair<EncryptedObject, SecretKey> {
        val mediaKey = StorageCryptoService.generateRandomKey()
        val encrypted = StorageCryptoService.encrypt(data, mediaKey)
        return Pair(encrypted, mediaKey)
    }

    fun decryptMedia(encrypted: EncryptedObject, mediaKey: SecretKey): ByteArray {
        return try {
            StorageCryptoService.decrypt(encrypted, mediaKey)
        } catch (e: Exception) {
            Log.e("FILE_PIPELINE", "DECRYPT_MEDIA_FAILED | keyId=${encrypted.keyId} | error=${e.message}")
            throw e
        }
    }

    private fun getKeyForPurpose(purpose: StoragePurpose): SecretKey {
        return when (purpose) {
            StoragePurpose.NOTES -> KeyManager.getNotesKey()
            StoragePurpose.AGENDA -> KeyManager.getAgendaKey()
            StoragePurpose.VAULT -> KeyManager.getVaultKey()
        }
    }

    enum class StoragePurpose {
        NOTES, AGENDA, VAULT
    }
}
