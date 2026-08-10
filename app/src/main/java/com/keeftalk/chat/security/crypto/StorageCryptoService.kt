package com.keeftalk.chat.security.crypto

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.MessageDigest
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides authenticated encryption (AES-GCM) for local storage and files.
 * Exclusively uses android.util.Base64 with NO_WRAP for cross-component consistency.
 */
object StorageCryptoService {
    private const val TAG = "StorageCryptoService"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val BASE_IV_SIZE = 8
    private const val TAG_SIZE = 128
    private const val CRYPTO_VERSION = 1
    
    // Base64 flags for consistency: Standard with padding, no wrapping.
    private const val B64_FLAGS = Base64.NO_WRAP

    private fun getFingerprint(data: ByteArray?): String {
        if (data == null) return "null"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            hash.take(6).joinToString("") { "%02x".format(it) } + "..."
        } catch (e: Exception) { "err" }
    }

    fun encrypt(data: ByteArray, key: SecretKey, aad: ByteArray? = null, aadStr: String? = null): EncryptedObject {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
        aad?.let { cipher.updateAAD(it) }
        
        val ciphertext = cipher.doFinal(data)
        
        Log.d(TAG, "[CRYPTO_DIAG] ENCRYPT | keyFp=${getFingerprint(key.encoded)} | ivFp=${getFingerprint(iv)} | aadStr=${aadStr ?: "none"} | cipherLen=${ciphertext.size}")

        return EncryptedObject(
            version = CRYPTO_VERSION,
            keyId = "root",
            iv = Base64.encodeToString(iv, B64_FLAGS),
            ciphertext = Base64.encodeToString(ciphertext, B64_FLAGS)
        )
    }

    fun decrypt(encrypted: EncryptedObject, key: SecretKey, aad: ByteArray? = null, aadStr: String? = null): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = Base64.decode(encrypted.iv, B64_FLAGS)
        val ciphertext = Base64.decode(encrypted.ciphertext, B64_FLAGS)
        
        Log.d(TAG, "[CRYPTO_DIAG] DECRYPT_START | keyFp=${getFingerprint(key.encoded)} | ivFp=${getFingerprint(iv)} | aadStr=${aadStr ?: "none"} | cipherLen=${ciphertext.size}")

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
            aad?.let { cipher.updateAAD(it) }
            return cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.e(TAG, "[CRYPTO_DIAG] DECRYPT_FAILED | keyFp=${getFingerprint(key.encoded)} | ivFp=${getFingerprint(iv)} | aadStr=${aadStr ?: "none"} | error=${e.message}")
            throw e
        }
    }

    /**
     * Encrypts a single chunk of data for streaming (v2).
     * Nonce = BaseIV (8 bytes) || ChunkIndex (4 bytes BE).
     * AAD = FileID (String bytes) || ChunkIndex (4 bytes BE).
     */
    fun encryptChunk(
        data: ByteArray,
        key: SecretKey,
        baseIv: ByteArray,
        chunkIndex: Int,
        fileId: String
    ): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val nonce = ByteBuffer.allocate(IV_SIZE)
            .put(baseIv)
            .putInt(chunkIndex)
            .array()
            
        val aad = ByteBuffer.allocate(fileId.toByteArray().size + 4)
            .put(fileId.toByteArray())
            .putInt(chunkIndex)
            .array()

        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, nonce))
        cipher.updateAAD(aad)
        
        return cipher.doFinal(data)
    }

    /**
     * Decrypts a single chunk of data for streaming (v2).
     */
    fun decryptChunk(
        encryptedData: ByteArray,
        key: SecretKey,
        baseIv: ByteArray,
        chunkIndex: Int,
        fileId: String
    ): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val nonce = ByteBuffer.allocate(IV_SIZE)
            .put(baseIv)
            .putInt(chunkIndex)
            .array()
            
        val aad = ByteBuffer.allocate(fileId.toByteArray().size + 4)
            .put(fileId.toByteArray())
            .putInt(chunkIndex)
            .array()

        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, nonce))
        cipher.updateAAD(aad)
        
        return cipher.doFinal(encryptedData)
    }
    
    fun generateRandomKey(): SecretKey {
        val keyBytes = ByteArray(32) // 256-bit
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun generateBaseIv(): ByteArray {
        val iv = ByteArray(BASE_IV_SIZE)
        SecureRandom().nextBytes(iv)
        return iv
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
