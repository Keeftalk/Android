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

    @Volatile
    private var cachedPassphrase: String? = null

    /**
     * Returns the database encryption passphrase.
     * Generates and stores it securely if it doesn't exist.
     */
    fun getDatabasePassphrase(context: Context): String {
        cachedPassphrase?.let { return it }
        
        return synchronized(this) {
            cachedPassphrase?.let { return@synchronized it }
            
            try {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val sharedPreferences = EncryptedSharedPreferences.create(
                    context.applicationContext,
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
                    sharedPreferences.edit().putString(KEY_DB_PASSPHRASE, passphrase).commit()
                } else {
                    Log.d(TAG, "Retrieved existing database passphrase.")
                }

                cachedPassphrase = passphrase
                passphrase
            } catch (e: Exception) {
                Log.e(TAG, "Failed to manage database passphrase securely. Check Keystore status.", e)
                // Fallback for extreme cases (e.g., Keystore wiped during upgrade)
                "emergency_fallback_key_v2" 
            }
        }
    }
}
