package com.keeftalk.chat.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CloudSecureStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "cloud_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(provider: String, token: String, expiresAt: Long) {
        prefs.edit {
            putString("${provider}_access_token", token)
            putLong("${provider}_expires_at", expiresAt)
        }
    }

    fun getAccessToken(provider: String): String? {
        val expiresAt = prefs.getLong("${provider}_expires_at", 0)
        if (System.currentTimeMillis() > expiresAt) return null
        return prefs.getString("${provider}_access_token", null)
    }

    fun clearToken(provider: String) {
        prefs.edit {
            remove("${provider}_access_token")
            remove("${provider}_expires_at")
        }
    }
}
