package tn.keeftalk.chat.security.crypto

import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoEngine {
    private const val TAG = "BackupCryptoEngine"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16

    /**
     * Encrypts source file into destination .ktb encrypted file using password.
     * Output format: [salt (16 bytes)][iv (12 bytes)][ciphertext with GCM auth tag]
     */
    fun encryptFile(sourceFile: File, outputFile: File, password: String) {
        val salt = Argon2idManager.generateSalt()
        val key = Argon2idManager.deriveKey(password, salt)

        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val plaintext = sourceFile.readBytes()
        val ciphertext = cipher.doFinal(plaintext)

        outputFile.outputStream().use { fos ->
            fos.write(salt)
            fos.write(iv)
            fos.write(ciphertext)
        }
    }

    /**
     * Decrypts encrypted .ktb file into destination decrypted file using password.
     * Throws SecurityException or DecryptionException if password is incorrect or file is corrupted.
     */
    fun decryptFile(encryptedFile: File, outputFile: File, password: String) {
        val bytes = encryptedFile.readBytes()
        if (bytes.size < SALT_LENGTH + IV_LENGTH + 16) {
            throw IllegalArgumentException("Invalid backup file format or corrupted file.")
        }

        val salt = bytes.copyOfRange(0, SALT_LENGTH)
        val iv = bytes.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val ciphertext = bytes.copyOfRange(SALT_LENGTH + IV_LENGTH, bytes.size)

        val key = Argon2idManager.deriveKey(password, salt)

        val cipher = Cipher.getInstance(AES_GCM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        try {
            val plaintext = cipher.doFinal(ciphertext)
            outputFile.writeBytes(plaintext)
        } catch (e: Exception) {
            throw SecurityException("Incorrect backup password or corrupted backup file.", e)
        }
    }

    /**
     * Decrypts in memory and returns plaintext bytes.
     */
    fun decryptBytes(encryptedBytes: ByteArray, password: String): ByteArray {
        if (encryptedBytes.size < SALT_LENGTH + IV_LENGTH + 16) {
            throw IllegalArgumentException("Invalid backup file format or corrupted file.")
        }

        val salt = encryptedBytes.copyOfRange(0, SALT_LENGTH)
        val iv = encryptedBytes.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val ciphertext = encryptedBytes.copyOfRange(SALT_LENGTH + IV_LENGTH, encryptedBytes.size)

        val key = Argon2idManager.deriveKey(password, salt)

        val cipher = Cipher.getInstance(AES_GCM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        try {
            return cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw SecurityException("Incorrect backup password or corrupted backup file.", e)
        }
    }
}
