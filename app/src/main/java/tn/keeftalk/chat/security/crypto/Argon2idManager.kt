package tn.keeftalk.chat.security.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKeyFactory

/**
 * Manages KEK derivation using Argon2id.
 */
object Argon2idManager {
    private val argon2Kt: Argon2Kt? = try {
        Argon2Kt()
    } catch (e: Throwable) {
        null
    }
    
    // Argon2id Parameters
    private const val ITERATIONS = 3
    private const val MEMORY = 65536 // 64MB in KB
    private const val PARALLELISM = 4
    private const val SALT_LENGTH = 16
    private const val HASH_LENGTH = 32 // 256-bit key

    /**
     * Derives a Key Encryption Key (KEK) from a password and salt.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val instance = argon2Kt
        if (instance != null) {
            try {
                val result = instance.hash(
                    mode = Argon2Mode.ARGON2_ID,
                    password = password.toByteArray(Charsets.UTF_8),
                    salt = salt,
                    tCostInIterations = ITERATIONS,
                    mCostInKibibyte = MEMORY,
                    parallelism = PARALLELISM,
                    hashLengthInBytes = HASH_LENGTH
                )
                return SecretKeySpec(result.rawHashAsByteArray(), "AES")
            } catch (e: Throwable) {
                // Fallback to PBKDF2 if native Argon2 call fails
            }
        }
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /**
     * Generates a random 16-byte salt for Argon2id.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
