package com.keeftalk.chat.security.crypto

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Exception thrown when an operation requires the Account Encryption Key (AEK),
 * but it is currently missing from memory and storage.
 */
class SecurityRecoveryRequiredException : Exception("Account Encryption Key (AEK) is missing. Security recovery is required.")

/**
 * Manages the Account Encryption Key (AEK) and its lifecycle.
 * This key is the root of trust for all synced user data.
 */
object KeyManager {
    private const val TAG = "KeyManager"
    private const val LEGACY_PREFS_NAME = "secure_key_prefs"
    private const val USER_PREFS_PREFIX = "secure_key_prefs_"
    private const val KEY_WRAPPED_AEK = "wrapped_aek"
    private const val KEY_RECOVERY_TOKEN = "recovery_token"
    
    // Domain Separation Contexts
    private const val INFO_NOTES = "Keeftalk/Notes/v1"
    private const val INFO_AGENDA = "Keeftalk/Agenda/v1"
    private const val INFO_VAULT = "Keeftalk/Vault/v1"
    private const val INFO_CONV_WRAP = "Keeftalk/ConversationWrap/v1"
    private const val INFO_FILE_WRAP = "Keeftalk/FileWrap/v1"

    private const val B64_FLAGS = Base64.NO_WRAP

    // In-memory cache for the Account Encryption Key.
    @Volatile
    private var accountEncryptionKey: SecretKey? = null

    @Volatile
    private var currentUserId: String? = null

    @Volatile
    private var appContext: Context? = null

    private val mutex = Mutex()

    /**
     * Initializes the KeyManager with the application context.
     */
    fun init(context: Context) {
        this.appContext = context.applicationContext
    }

    /**
     * Retrieves the current Account Encryption Key if available.
     */
    fun getAEK(): SecretKey? = accountEncryptionKey

    /**
     * Sets the Account Encryption Key in memory.
     */
    fun setAEK(key: SecretKey) {
        accountEncryptionKey = key
    }

    private fun getPrefs(context: Context, userId: String): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context.applicationContext,
            USER_PREFS_PREFIX + userId,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getLegacyPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context.applicationContext,
            LEGACY_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Persists the AEK locally on this device, wrapped by a Keystore-bound MasterKey.
     * Scoped to the specific userId.
     */
    suspend fun persistAEK(context: Context, userId: String, aek: SecretKey) = mutex.withLock {
        try {
            val prefs = getPrefs(context, userId)
            val success = prefs.edit()
                .putString(KEY_WRAPPED_AEK, Base64.encodeToString(aek.encoded, B64_FLAGS))
                .commit()
            
            if (success) {
                Log.i(TAG, "AEK successfully persisted for user $userId.")
            } else {
                Log.e(TAG, "Failed to persist AEK for user $userId (commit returned false).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during AEK persistence for user $userId", e)
        }
    }

    /**
     * Attempts to restore the AEK from local persistent storage for a specific user.
     * Includes logic to migrate from legacy global storage if applicable.
     */
    suspend fun restoreAEK(context: Context, userId: String): Boolean = mutex.withLock {
        try {
            val prefs = getPrefs(context, userId)
            var encoded = prefs.getString(KEY_WRAPPED_AEK, null)

            if (encoded == null) {
                Log.i(TAG, "AEK not found in user-scoped storage. Checking legacy storage...")
                // Attempt migration from legacy storage
                val legacyPrefs = getLegacyPrefs(context)
                val legacyEncoded = legacyPrefs.getString(KEY_WRAPPED_AEK, null)
                
                if (legacyEncoded != null) {
                    Log.i(TAG, "Legacy AEK found. Migrating to user-scoped storage for $userId.")
                    // Verify if this key is actually for this user (or if we should just trust it for now)
                    // In a production migration, we might want more checks, but for now we'll migrate it.
                    val success = prefs.edit().putString(KEY_WRAPPED_AEK, legacyEncoded).commit()
                    if (success) {
                        encoded = legacyEncoded
                        legacyPrefs.edit().remove(KEY_WRAPPED_AEK).commit()
                        Log.i(TAG, "Legacy AEK migrated successfully.")
                    }
                }
            }

            if (encoded == null) {
                Log.w(TAG, "AEK not found in any local persistent storage for user $userId.")
                return false
            }
            
            val aekBytes = Base64.decode(encoded, B64_FLAGS)
            accountEncryptionKey = SecretKeySpec(aekBytes, "AES")
            currentUserId = userId
            Log.i(TAG, "AEK successfully restored for user $userId.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore AEK for user $userId", e)
            return false
        }
    }

    /**
     * Clears the Account Encryption Key from memory and local storage.
     */
    suspend fun clearAEK(context: Context, userId: String?) = mutex.withLock {
        accountEncryptionKey = null
        currentUserId = null
        try {
            userId?.let {
                val prefs = getPrefs(context, it)
                prefs.edit()
                    .remove(KEY_WRAPPED_AEK)
                    .remove(KEY_RECOVERY_TOKEN)
                    .commit()
                Log.i(TAG, "AEK and recovery token cleared from local storage for user $it.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear AEK from local storage", e)
        }
    }

    /**
     * Persists a recovery token (derived KEK) locally to enable automatic background recovery.
     */
    suspend fun persistRecoveryToken(context: Context, userId: String, kek: SecretKey) = mutex.withLock {
        try {
            val prefs = getPrefs(context, userId)
            prefs.edit()
                .putString(KEY_RECOVERY_TOKEN, Base64.encodeToString(kek.encoded, B64_FLAGS))
                .commit()
            Log.i(TAG, "Recovery token persisted for user $userId.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist recovery token", e)
        }
    }

    /**
     * Retrieves the persisted recovery token for a user.
     */
    fun getRecoveryToken(context: Context, userId: String): SecretKey? {
        return try {
            val prefs = getPrefs(context, userId)
            val encoded = prefs.getString(KEY_RECOVERY_TOKEN, null) ?: return null
            val bytes = Base64.decode(encoded, B64_FLAGS)
            SecretKeySpec(bytes, "AES")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a new random 256-bit Account Encryption Key.
     */
    fun generateAEK(): SecretKey {
        return StorageCryptoService.generateRandomKey()
    }

    /**
     * Encrypts the AEK using a Key Encryption Key (KEK) for cloud synchronization.
     */
    fun encryptAEK(aek: SecretKey, kek: SecretKey): EncryptedObject {
        return StorageCryptoService.encrypt(aek.encoded, kek)
    }

    /**
     * Decrypts the AEK using a Key Encryption Key (KEK) during recovery.
     */
    fun decryptAEK(encryptedAEK: EncryptedObject, kek: SecretKey): SecretKey {
        val decrypted = StorageCryptoService.decrypt(encryptedAEK, kek)
        return SecretKeySpec(decrypted, "AES")
    }

    /**
     * Generates a verification tag for the AEK.
     * This is the string "VERIFIED" encrypted with the AEK itself.
     */
    fun generateVerificationTag(aek: SecretKey): EncryptedObject {
        return StorageCryptoService.encrypt("VERIFIED".toByteArray(Charsets.UTF_8), aek)
    }

    /**
     * Validates the AEK using a verification tag.
     */
    fun validateAEK(aek: SecretKey, tag: EncryptedObject): Boolean {
        return try {
            val decrypted = StorageCryptoService.decrypt(tag, aek)
            String(decrypted, Charsets.UTF_8) == "VERIFIED"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the master key for storage encryption.
     * Must be initialized via AEK.
     * @throws SecurityRecoveryRequiredException if the user is not logged in or AEK is missing.
     */
    fun getMasterKey(): SecretKey {
        val key = accountEncryptionKey
        if (key == null) {
            val msg = "Account Encryption Key (AEK) is MISSING from memory. Recovery is required."
            Log.w(TAG, msg)
            throw SecurityRecoveryRequiredException()
        }
        return key
    }

    /**
     * Returns true if the Account Encryption Key is loaded in memory.
     */
    fun isInitialized(): Boolean = accountEncryptionKey != null

    /**
     * Returns true if the Account Encryption Key is persisted in local storage for a user.
     */
    fun isPersisted(context: Context, userId: String): Boolean {
        return try {
            val prefs = getPrefs(context, userId)
            prefs.contains(KEY_WRAPPED_AEK)
        } catch (e: Exception) {
            false
        }
    }

    private fun deriveKey(info: String): SecretKey {
        val rootKey = getMasterKey()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(rootKey)
        val derivedBytes = mac.doFinal(info.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(derivedBytes, "AES")
    }

    // Purpose-specific keys derived from AEK with domain separation
    fun getNotesKey(): SecretKey = deriveKey(INFO_NOTES)
    fun getAgendaKey(): SecretKey = deriveKey(INFO_AGENDA)
    fun getVaultKey(): SecretKey = deriveKey(INFO_VAULT)
    fun getConversationProtectionKey(): SecretKey = deriveKey(INFO_CONV_WRAP)
    fun getFileProtectionKey(): SecretKey = deriveKey(INFO_FILE_WRAP)
}
