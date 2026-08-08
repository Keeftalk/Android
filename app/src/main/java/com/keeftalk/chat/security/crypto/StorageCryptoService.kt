package com.keeftalk.chat.security.crypto

import java.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides authenticated encryption (AES-GCM) for local storage and files.
 */
object StorageCryptoService {
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    private const val CRYPTO_VERSION = 1

    fun encrypt(data: ByteArray, key: SecretKey): EncryptedObject {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
        val ciphertext = cipher.doFinal(data)
        
        return EncryptedObject(
            version = CRYPTO_VERSION,
            keyId = "root",
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext)
        )
    }

    fun decrypt(encrypted: EncryptedObject, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = Base64.getDecoder().decode(encrypted.iv)
        val ciphertext = Base64.getDecoder().decode(encrypted.ciphertext)
        
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
        return cipher.doFinal(ciphertext)
    }
    
    fun generateRandomKey(): SecretKey {
        val keyBytes = ByteArray(32) // 256-bit
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Wraps a SecretKey using AES-GCM.
     */
    fun wrapKey(keyToWrap: SecretKey, wrappingKey: SecretKey): EncryptedObject {
        return encrypt(keyToWrap.encoded, wrappingKey)
    }

    /**
     * Unwraps a SecretKey using AES-GCM.
     */
    fun unwrapKey(wrappedKey: EncryptedObject, wrappingKey: SecretKey): SecretKey {
        val keyBytes = decrypt(wrappedKey, wrappingKey)
        return SecretKeySpec(keyBytes, "AES")
    }
}
