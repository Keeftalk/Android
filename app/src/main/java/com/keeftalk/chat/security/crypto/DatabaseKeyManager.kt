package com.keeftalk.chat.security.crypto

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the database encryption key, protecting it with Android Keystore.
 */
object DatabaseKeyManager {
    private const val TAG = "DatabaseKeyManager"
    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    /**
     * Returns the database encryption passphrase.
     * Generates and stores it securely if it doesn't exist.
     */
    fun getDatabasePassphrase(context: Context): String {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            var passphrase = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
            if (passphrase == null) {
                Log.i(TAG, "Generating new database passphrase...")
                val random = ByteArray(32)
                SecureRandom().nextBytes(random)
                passphrase = Base64.encodeToString(random, Base64.NO_WRAP)
                sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
            } else {
                Log.d(TAG, "Retrieved existing database passphrase.")
            }

            return passphrase
        } catch (e: Exception) {
            Log.e(TAG, "Failed to manage database passphrase securely. Check Keystore status.", e)
            // Fallback for extreme cases (e.g., Keystore wiped during upgrade)
            // In a production app, we might want to prompt the user or force a logout.
            return "emergency_fallback_key_v2" 
        }
    }
}
