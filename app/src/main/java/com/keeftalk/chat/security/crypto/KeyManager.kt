package com.keeftalk.chat.security.crypto

import android.content.Context
import java.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Manages the Account Encryption Key (AEK) and its lifecycle.
 * This key is the root of trust for all synced user data.
 */
object KeyManager {
    private const val TAG = "KeyManager"
    private const val PREFS_NAME = "secure_key_prefs"
    private const val KEY_WRAPPED_AEK = "wrapped_aek"
    
    // Domain Separation Contexts
    private const val INFO_NOTES = "Keeftalk/Notes/v1"
    private const val INFO_AGENDA = "Keeftalk/Agenda/v1"
    private const val INFO_VAULT = "Keeftalk/Vault/v1"
    private const val INFO_CONV_WRAP = "Keeftalk/ConversationWrap/v1"
    private const val INFO_FILE_WRAP = "Keeftalk/FileWrap/v1"

    // In-memory cache for the Account Encryption Key.
    @Volatile
    private var accountEncryptionKey: SecretKey? = null

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

    /**
     * Persists the AEK locally on this device, wrapped by a Keystore-bound MasterKey.
     */
    fun persistAEK(context: Context, aek: SecretKey) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        prefs.edit().putString(KEY_WRAPPED_AEK, Base64.getEncoder().encodeToString(aek.encoded)).apply()
    }

    /**
     * Attempts to restore the AEK from local persistent storage.
     */
    fun restoreAEK(context: Context): Boolean {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val encoded = prefs.getString(KEY_WRAPPED_AEK, null) ?: return false
            val aekBytes = Base64.getDecoder().decode(encoded)
            accountEncryptionKey = SecretKeySpec(aekBytes, "AES")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore AEK from local storage", e)
            return false
        }
    }

    /**
     * Clears the Account Encryption Key from memory and local storage.
     */
    fun clearAEK(context: Context) {
        accountEncryptionKey = null
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.edit().remove(KEY_WRAPPED_AEK).apply()
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
     * @throws IllegalStateException if the user is not logged in or AEK is missing.
     */
    fun getMasterKey(): SecretKey {
        val key = accountEncryptionKey
        if (key == null) {
            val msg = "[FATAL] Account Encryption Key (AEK) is MISSING from memory. " +
                      "Please perform a fresh login to restore your encryption context."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
        return key
    }

    /**
     * Returns true if the Account Encryption Key is loaded in memory.
     */
    fun isInitialized(): Boolean = accountEncryptionKey != null

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
