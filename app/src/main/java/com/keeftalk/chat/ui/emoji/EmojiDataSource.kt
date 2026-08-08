package com.keeftalk.chat.ui.emoji

import android.content.Context
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object EmojiDataSource {
    private var allEmojis = listOf<Emoji>()
    private val categoryMap = mutableMapOf<EmojiCategory, MutableList<Emoji>>()
    private val searchIndex = mutableMapOf<String, MutableSet<Emoji>>()
    private var isInitialized = false

    private class EmojiCategoryAdapter {
        @FromJson
        fun fromJson(json: String): EmojiCategory {
            return when (json) {
                "OTHER" -> EmojiCategory.SMILEYS
                else -> try {
                    EmojiCategory.valueOf(json)
                } catch (e: Exception) {
                    EmojiCategory.SMILEYS
                }
            }
        }

        @ToJson
        fun toJson(category: EmojiCategory): String {
            return category.name
        }
    }

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        
        try {
            val jsonString = context.assets.open("emojis.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder()
                .add(EmojiCategoryAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()
            val listType = Types.newParameterizedType(List::class.java, Emoji::class.java)
            val adapter = moshi.adapter<List<Emoji>>(listType)
            
            allEmojis = adapter.fromJson(jsonString) ?: emptyList()
            
            allEmojis.forEach { emoji ->
                categoryMap.getOrPut(emoji.category) { mutableListOf() }.add(emoji)
            }
            
            buildSearchIndex()
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            allEmojis = emptyList()
        }
    }

    private fun buildSearchIndex() {
        allEmojis.forEach { emoji ->
            // Index by name
            emoji.name.split(" ").forEach { word ->
                indexWord(word.lowercase(), emoji)
            }
            // Index by keywords
            emoji.keywords.forEach { keyword ->
                indexWord(keyword.lowercase(), emoji)
            }
        }
    }

    private fun indexWord(word: String, emoji: Emoji) {
        if (word.length < 2) return
        searchIndex.getOrPut(word) { mutableSetOf() }.add(emoji)
    }

    fun getEmojisByCategory(category: EmojiCategory): List<Emoji> {
        return categoryMap[category] ?: emptyList()
    }

    suspend fun search(query: String): List<Emoji> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        val lowerQuery = query.lowercase().removePrefix(":")
        
        val results = mutableSetOf<Emoji>()
        
        // Prefix match on keywords/name parts
        searchIndex.keys.forEach { key ->
            if (key.startsWith(lowerQuery)) {
                searchIndex[key]?.let { results.addAll(it) }
            }
        }
        
        results.toList().sortedBy { it.name.length }
    }
    
    fun getAllCategories(): List<EmojiCategory> {
        return EmojiCategory.entries.filter { it != EmojiCategory.RECENT }
    }
}
