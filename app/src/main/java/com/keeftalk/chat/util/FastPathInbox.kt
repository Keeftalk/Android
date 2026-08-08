package com.keeftalk.chat.util

import com.keeftalk.chat.domain.model.ChatListItemUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Singleton providing instant access to the top 10 chats.
 * Populated synchronously from SharedPreferences in Tier 1.
 */
object FastPathInbox {
    private val _chats = MutableStateFlow<List<ChatListItemUiModel>>(emptyList())
    val chats: StateFlow<List<ChatListItemUiModel>> = _chats.asStateFlow()

    fun loadFromKV() {
        try {
            val store = KeeftalkStore.getInstance()
            val json = store.getString("instant_chat_cache", null)
            if (!json.isNullOrEmpty()) {
                val items = Json.decodeFromString<List<ChatListItemUiModel>>(json)
                _chats.value = items
                PerformanceProfiler.logEvent("FastPathInbox: Loaded ${items.size} chats from KV cache", category = PerformanceProfiler.Category.STORAGE)
            }
        } catch (e: Exception) {
            Log.e("FastPathInbox", "KV load failed", e)
        }
    }

    suspend fun loadFromSQL(context: android.content.Context) = withContext(Dispatchers.IO) {
        try {
            val store = KeeftalkStore.getInstance()
            val currentUserId = store.getString("user_id", null)
            
            // Raw SQLite query for fallback/refresh
            val db = com.keeftalk.chat.di.AppModule.provideDatabase(context)
            
            // Safety check: is the DB open and valid?
            try {
                db.openHelper.readableDatabase.query("SELECT 1").use { it.moveToFirst() }
            } catch (e: Exception) {
                Log.w("FastPathInbox", "Database not ready for SQL load: ${e.message}")
                return@withContext
            }

            val cursor = db.openHelper.readableDatabase.query(
                "SELECT id, name, avatarUrl, lastMessage, lastTimestamp, unreadCount, peerId, lastMessageStatus, lastMessageSenderId, isPinned, isMuted, snippetType, snippetUri, type FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastTimestamp DESC LIMIT 15"
            )
            val items = mutableListOf<ChatListItemUiModel>()
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: "Unknown"
                val snippetType = cursor.getString(11)
                val snippetUri = cursor.getString(12)
                val senderId = cursor.getString(8)
                val chatTypeStr = cursor.getString(13)
                var lastMessage = cursor.getString(3) ?: ""

                val isFromMe = (senderId != null) && (senderId == currentUserId)
                val isGroup = chatTypeStr == "GROUP"

                if (lastMessage.startsWith("[") && lastMessage.endsWith("]") || lastMessage == "Sent a picture") {
                    lastMessage = when (snippetType) {
                        "IMAGE" -> if (isFromMe) "You sent a picture" else "You received a picture"
                        "VIDEO" -> if (isFromMe) "You sent a video" else "You received a video"
                        "VOICE" -> if (isFromMe) "You sent a voice message" else "You received a voice message"
                        "FILE" -> if (isFromMe) "You sent a document" else "You received a document"
                        else -> lastMessage
                    }
                }

                items.add(ChatListItemUiModel(
                    id = cursor.getString(0),
                    name = name,
                    avatarUrl = cursor.getString(2),
                    initials = AvatarUtils.getInitials(name),
                    lastMessage = lastMessage,
                    lastTimestamp = cursor.getLong(4),
                    formattedTimestamp = com.keeftalk.chat.ui.screens.formatTimestamp(cursor.getLong(4)),
                    unreadCount = cursor.getInt(5),
                    isPinned = cursor.getInt(9) == 1,
                    isMuted = cursor.getInt(10) == 1,
                    lastMessageStatus = try { cursor.getString(7)?.let { com.keeftalk.chat.domain.model.MessageStatus.valueOf(it) } } catch(e: Exception) { null },
                    lastMessageSenderId = senderId,
                    isArchived = false,
                    peerId = cursor.getString(6),
                    type = if (isGroup) com.keeftalk.chat.domain.model.ChatType.GROUP else com.keeftalk.chat.domain.model.ChatType.ONE_TO_ONE,
                    isTyping = false,
                    snippetType = snippetType,
                    snippetUri = snippetUri
                ))
            }
            cursor.close()
            
            // Only update if memory cache is empty or data changed
            if (_chats.value.isEmpty() || items != _chats.value) {
                _chats.value = items
                PerformanceProfiler.logEvent("FastPathInbox: Refreshed from SQL", category = PerformanceProfiler.Category.DATABASE)
            }
        } catch (e: Exception) {
            Log.e("FastPathInbox", "SQL load failed", e)
        }
    }

    fun update(newChats: List<ChatListItemUiModel>) {
        _chats.value = newChats
    }
}
