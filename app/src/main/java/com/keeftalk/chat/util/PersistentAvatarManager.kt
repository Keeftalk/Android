package com.keeftalk.chat.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream

object PersistentAvatarManager {
    private const val TAG = "AvatarManager"
    private const val AVATAR_DIR = "avatars"
    private const val PREF_NAME = "avatar_cache_prefs"

    private fun getAvatarDir(context: Context): File {
        val dir = File(context.filesDir, AVATAR_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getLocalAvatarFile(context: Context, identifier: String): File? {
        val file = File(getAvatarDir(context), "${identifier}.jpg")
        return if (file.exists()) file else null
    }

    fun saveAvatar(context: Context, identifier: String, bytes: ByteArray, url: String? = null) {
        try {
            val file = File(getAvatarDir(context), "${identifier}.jpg")
            FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            if (url != null) {
                getPrefs(context).edit { putString(identifier, url) }
            }
            Log.d(TAG, "Avatar saved for $identifier")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save avatar for $identifier", e)
        }
    }

    fun saveAvatar(context: Context, identifier: String, sourceFile: File, url: String? = null) {
        try {
            val destFile = File(getAvatarDir(context), "${identifier}.jpg")
            sourceFile.copyTo(destFile, overwrite = true)
            if (url != null) {
                getPrefs(context).edit { putString(identifier, url) }
            }
            Log.d(TAG, "Avatar saved (copied) for $identifier")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy avatar for $identifier", e)
        }
    }

    fun clearAvatar(context: Context, identifier: String) {
        val file = File(getAvatarDir(context), "${identifier}.jpg")
        if (file.exists()) {
            file.delete()
            getPrefs(context).edit { remove(identifier) }
            Log.d(TAG, "Avatar cleared for $identifier")
        }
    }

    suspend fun downloadAndCacheAvatar(context: Context, identifier: String, url: String, force: Boolean = false) {
        val cachedUrl = getPrefs(context).getString(identifier, null)
        if (!force && cachedUrl == url && getLocalAvatarFile(context, identifier) != null) return
        
        try {
            val connection = java.net.URL(url).openConnection()
            connection.connect()
            val bytes = connection.getInputStream().readBytes()
            saveAvatar(context, identifier, bytes, url)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to proactively cache avatar for $identifier from $url", e)
        }
    }
}
