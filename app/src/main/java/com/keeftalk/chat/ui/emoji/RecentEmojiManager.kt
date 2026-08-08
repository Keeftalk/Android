package com.keeftalk.chat.ui.emoji

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "recent_emojis")

class RecentEmojiManager(private val context: Context) {
    private val RECENT_EMOJIS_KEY = stringPreferencesKey("recent_emojis_list")
    private val FREQUENCY_KEY = stringPreferencesKey("emoji_frequency")
    private val MAX_RECENT = 50

    val recentEmojis: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val serialized = preferences[RECENT_EMOJIS_KEY] ?: ""
            if (serialized.isEmpty()) emptyList() else serialized.split(",")
        }

    val frequentEmojis: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val serialized = preferences[FREQUENCY_KEY] ?: ""
            if (serialized.isEmpty()) {
                emptyList()
            } else {
                serialized.split(",")
                    .map { it.split(":") }
                    .filter { it.size == 2 }
                    .map { it[0] to (it[1].toIntOrNull() ?: 0) }
                    .sortedByDescending { it.second }
                    .take(MAX_RECENT)
                    .map { it.first }
            }
        }

    suspend fun addEmoji(emojiUnicode: String) {
        context.dataStore.edit { preferences ->
            // Update Recent
            val currentRecent = preferences[RECENT_EMOJIS_KEY]?.split(",")?.toMutableList() ?: mutableListOf()
            currentRecent.remove(emojiUnicode)
            currentRecent.add(0, emojiUnicode)
            if (currentRecent.size > MAX_RECENT) {
                currentRecent.removeAt(currentRecent.size - 1)
            }
            preferences[RECENT_EMOJIS_KEY] = currentRecent.joinToString(",")

            // Update Frequency
            val freqSerialized = preferences[FREQUENCY_KEY] ?: ""
            val freqMap = if (freqSerialized.isEmpty()) {
                mutableMapOf<String, Int>()
            } else {
                freqSerialized.split(",")
                    .map { it.split(":") }
                    .filter { it.size == 2 }
                    .associate { it[0] to (it[1].toIntOrNull() ?: 0) }
                    .toMutableMap()
            }
            freqMap[emojiUnicode] = (freqMap[emojiUnicode] ?: 0) + 1
            preferences[FREQUENCY_KEY] = freqMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }
}
