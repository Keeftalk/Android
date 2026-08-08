package com.keeftalk.chat.feature.email.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EmailSecureStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "email_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accountId: String, refreshToken: String?, accessToken: String?, expiresAt: Long) {
        prefs.edit {
            putString("${accountId}_refresh", refreshToken)
            putString("${accountId}_access", accessToken)
            putLong("${accountId}_expires", expiresAt)
        }
    }

    fun getRefreshToken(accountId: String): String? = prefs.getString("${accountId}_refresh", null)
    fun getAccessToken(accountId: String): String? = prefs.getString("${accountId}_access", null)
    fun getTokenExpiration(accountId: String): Long = prefs.getLong("${accountId}_expires", 0)

    fun savePassword(accountId: String, password: String) {
        prefs.edit {
            putString("${accountId}_password", password)
        }
    }

    fun getPassword(accountId: String): String? = prefs.getString("${accountId}_password", null)

    fun removeCredentials(accountId: String) {
        prefs.edit {
            remove("${accountId}_refresh")
            remove("${accountId}_access")
            remove("${accountId}_expires")
            remove("${accountId}_password")
        }
    }
}
