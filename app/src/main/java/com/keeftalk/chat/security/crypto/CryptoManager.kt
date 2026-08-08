package com.keeftalk.chat.security.crypto

import javax.crypto.SecretKey
import android.util.Log
import java.util.Base64

/**
 * Unified entry point for all cryptographic operations in Keeftalk.
 * Exclusively uses Cloud-E2EE (PCK/AEK) architecture with random keys.
 */
class CryptoManager(
    private val conversationKeyManager: ConversationKeyManager,
    val userId: String
) {
    private val TAG = "CryptoManager"

    // --- Messaging (PCK-based) ---

    /**
     * Encrypts a message for a conversation using its random Per-Conversation Key (PCK).
     * Returns the envelope and the crypto version used.
     */
    suspend fun encryptMessage(chatId: String, plaintext: String): Pair<EncryptedMessageEnvelope, Int> {
        // Use a random key from the manager (creates if missing)
        val key = conversationKeyManager.getOrLoadKey(chatId) ?: conversationKeyManager.createKey(chatId)
        
        val encryptedObj = StorageCryptoService.encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        val envelope = EncryptedMessageEnvelope(
            type = 100, // Cloud-E2EE PCK Type
            ciphertext = Base64.getDecoder().decode(encryptedObj.ciphertext),
            nonce = encryptedObj.iv
        )
        return envelope to 3 // Version 3: Random PCK protected by CPK
    }

    /**
     * Decrypts a message using the conversation's Per-Conversation Key (PCK).
     */
    suspend fun decryptMessage(chatId: String, envelope: EncryptedMessageEnvelope, cryptoVersion: Int = 1): String {
        // Treat type 0 (Legacy/Unspecified) as 100 (Cloud-E2EE) for robustness
        if (envelope.type != 100 && envelope.type != 0) {
            throw Exception("Unsupported message type: ${envelope.type}. Only Cloud-E2EE (100) is supported.")
        }

        val nonce = envelope.nonce ?: throw Exception("Nonce missing for PCK message")
        val encryptedObj = EncryptedObject(
            version = 1,
            keyId = "pck",
            iv = nonce,
            ciphertext = Base64.getEncoder().encodeToString(envelope.ciphertext)
        )

        // Load key from manager (handles both legacy and new wrapped versions)
        val key = conversationKeyManager.getOrLoadKey(chatId) ?: run {
            throw Exception("PCK not found for chat $chatId. Decryption aborted.")
        }
        
        return try {
            String(StorageCryptoService.decrypt(encryptedObj, key), Charsets.UTF_8)
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
        return StorageCryptoService.decrypt(encrypted, mediaKey)
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
