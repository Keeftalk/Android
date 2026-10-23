package tn.keeftalk.chat.extension.persistence

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

class ExtensionStorageManager(private val context: Context) {
    private val TAG = "ExtensionStorageManager"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val storageBaseDir: File
        get() = File(context.filesDir, "extensions/storage").also { if (!it.exists()) it.mkdirs() }

    private val memoryCache = mutableMapOf<String, MutableMap<String, String>>()
    private val _storageStateFlows = mutableMapOf<String, MutableStateFlow<Map<String, String>>>()

    private fun getStorageFile(extensionId: String): File {
        val sanitizedId = extensionId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val extDir = File(storageBaseDir, sanitizedId)
        if (!extDir.exists()) extDir.mkdirs()
        return File(extDir, "prefs.json")
    }

    @Synchronized
    fun getPreferencesFlow(extensionId: String): StateFlow<Map<String, String>> {
        return _storageStateFlows.getOrPut(extensionId) {
            MutableStateFlow(loadPreferences(extensionId))
        }.asStateFlow()
    }

    @Synchronized
    fun getPreference(extensionId: String, key: String): String? {
        val map = memoryCache.getOrPut(extensionId) { loadPreferences(extensionId).toMutableMap() }
        return map[key]
    }

    @Synchronized
    fun setPreference(extensionId: String, key: String, value: String) {
        val map = memoryCache.getOrPut(extensionId) { loadPreferences(extensionId).toMutableMap() }
        map[key] = value
        savePreferences(extensionId, map)

        _storageStateFlows[extensionId]?.value = map.toMap()
    }

    @Synchronized
    fun removePreference(extensionId: String, key: String) {
        val map = memoryCache.getOrPut(extensionId) { loadPreferences(extensionId).toMutableMap() }
        if (map.containsKey(key)) {
            map.remove(key)
            savePreferences(extensionId, map)
            _storageStateFlows[extensionId]?.value = map.toMap()
        }
    }

    @Synchronized
    fun clearPreferences(extensionId: String) {
        val map = memoryCache.getOrPut(extensionId) { mutableMapOf() }
        map.clear()
        val file = getStorageFile(extensionId)
        if (file.exists()) {
            file.delete()
        }
        _storageStateFlows[extensionId]?.value = emptyMap()
    }

    @Synchronized
    fun getStorageSizeBytes(extensionId: String): Long {
        val file = getStorageFile(extensionId)
        return if (file.exists()) file.length() else 0L
    }

    private fun loadPreferences(extensionId: String): Map<String, String> {
        val file = getStorageFile(extensionId)
        if (!file.exists()) return emptyMap()

        return try {
            val text = file.readText()
            if (text.isBlank()) emptyMap() else json.decodeFromString<Map<String, String>>(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read isolated storage for extension '$extensionId': ${e.message}")
            emptyMap()
        }
    }

    private fun savePreferences(extensionId: String, data: Map<String, String>) {
        try {
            val file = getStorageFile(extensionId)
            val text = json.encodeToString(data)
            file.writeText(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save isolated storage for extension '$extensionId': ${e.message}")
        }
    }
}
