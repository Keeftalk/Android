package com.keeftalk.chat.security.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Manages encryption and decryption of media files.
 */
class MediaEncryptionManager(
    private val cryptoManager: CryptoManager
) {
    /**
     * Encrypts a file's bytes with a new random media key.
     * Returns the encrypted data, the media key, and the IV.
     */
    fun encryptMedia(fileBytes: ByteArray): MediaEncryptionResult {
        val (encryptedObj, mediaKey) = cryptoManager.encryptMedia(fileBytes)
        
        return MediaEncryptionResult(
            encryptedBytes = Base64.decode(encryptedObj.ciphertext, Base64.NO_WRAP),
            mediaKeyBase64 = Base64.encodeToString(mediaKey.encoded, Base64.NO_WRAP),
            mediaIvBase64 = encryptedObj.iv
        )
    }

    /**
     * Decrypts media bytes using the provided media key and IV.
     */
    fun decryptMedia(encryptedBytes: ByteArray, mediaKeyBase64: String, mediaIvBase64: String): ByteArray {
        val key = SecretKeySpec(Base64.decode(mediaKeyBase64, Base64.NO_WRAP), "AES")
        val encryptedObj = EncryptedObject(
            version = 1,
            keyId = "media",
            iv = mediaIvBase64,
            ciphertext = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        )
        return cryptoManager.decryptMedia(encryptedObj, key)
    }

    data class MediaEncryptionResult(
        val encryptedBytes: ByteArray,
        val mediaKeyBase64: String,
        val mediaIvBase64: String
    )
}
