package com.keeftalk.chat.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.keeftalk.chat.domain.model.ChatListItemUiModel
import com.keeftalk.chat.domain.model.ChatType
import com.keeftalk.chat.domain.model.MessageStatus
import com.keeftalk.chat.util.AvatarUtils
import com.keeftalk.chat.ui.screens.formatTimestamp
import javax.inject.Inject
import javax.inject.Singleton

class RawChatListDataSource(
    private val db: SupportSQLiteDatabase,
    private val currentUserId: String? = null
) {

    fun getChatList(limit: Int, offset: Int, filter: String = "all"): List<ChatListItemUiModel> {
        val baseQuery = "SELECT * FROM chats INDEXED BY idx_chats_visibility_prio_time "
        val whereClause = when (filter) {
            "unread" -> "WHERE isArchived = 0 AND unreadCount > 0 "
            "groups" -> "WHERE isArchived = 0 AND type = 'GROUP' "
            "archived" -> "WHERE isArchived = 1 "
            "favorites" -> "WHERE isArchived = 0 AND isPinned = 1 "
            else -> "WHERE isArchived = 0 "
        }
        val orderClause = "ORDER BY isPinned DESC, lastTimestamp DESC LIMIT ? OFFSET ?"
        
        val query = baseQuery + whereClause + orderClause

        val cursor = db.query(query, arrayOf(limit, offset))
        val list = mutableListOf<ChatListItemUiModel>()
        
        cursor.use { c ->
            val idIdx = c.getColumnIndex("id")
            val nameIdx = c.getColumnIndex("name")
            val avatarIdx = c.getColumnIndex("avatarUrl")
            val typeIdx = c.getColumnIndex("type")
            val lastMsgIdx = c.getColumnIndex("lastMessage")
            val lastTsIdx = c.getColumnIndex("lastTimestamp")
            val unreadIdx = c.getColumnIndex("unreadCount")
            val isPinnedIdx = c.getColumnIndex("isPinned")
            val isMutedIdx = c.getColumnIndex("isMuted")
            val statusIdx = c.getColumnIndex("lastMessageStatus")
            val senderIdIdx = c.getColumnIndex("lastMessageSenderId")
            val peerIdIdx = c.getColumnIndex("peerId")
            val isArchivedIdx = c.getColumnIndex("isArchived")
            val snippetTypeIdx = c.getColumnIndex("snippetType")
            val snippetUriIdx = c.getColumnIndex("snippetUri")
            val metadataIdx = c.getColumnIndex("metadata")

            while (c.moveToNext()) {
                val rawName = c.getString(nameIdx)
                val snippetType = c.getString(snippetTypeIdx)
                val snippetUri = c.getString(snippetUriIdx)
                val metadataBlob = c.getBlob(metadataIdx)
                val threadMetadata = com.keeftalk.chat.util.ThreadMetadata.fromBlob(metadataBlob)
                
                val name = if (rawName == null || rawName == "Unknown" || rawName.equals("Direct Chat", ignoreCase = true)) {
                    threadMetadata.latestAuthorName ?: "Unknown"
                } else {
                    rawName
                }
                
                var lastMessage = c.getString(lastMsgIdx)
                val senderId = c.getString(senderIdIdx)
                val isFromMe = (senderId != null) && (senderId == currentUserId)
                val isGroup = c.getString(typeIdx) == "GROUP"
                
                // Fix for old generic placeholders or missing descriptive text
                val isPlaceholder = lastMessage == null || (lastMessage.startsWith("[") && lastMessage.endsWith("]")) || 
                                   lastMessage == "Sent a picture" || lastMessage == "Sent a video"
                
                if (isPlaceholder) {
                    val effectiveSnippetType = snippetType ?: lastMessage?.trim('[', ']') ?: ""
                    lastMessage = when (effectiveSnippetType) {
                        "IMAGE" -> if (isFromMe) "You sent a picture" else "You received a picture"
                        "VIDEO" -> if (isFromMe) "You sent a video" else "You received a video"
                        "VOICE" -> if (isFromMe) "You sent a voice message" else "You received a voice message"
                        "FILE" -> if (isFromMe) "You sent a document" else "You received a document"
                        else -> lastMessage ?: "Sent a message"
                    }
                }
                
                if (isGroup && !isFromMe && threadMetadata.latestAuthorName != null && !lastMessage.contains(":")) {
                    lastMessage = "${threadMetadata.latestAuthorName}: $lastMessage"
                }

                list.add(
                    ChatListItemUiModel(
                        id = c.getString(idIdx),
                        name = name,
                        avatarUrl = c.getString(avatarIdx),
                        initials = AvatarUtils.getInitials(name),
                        lastMessage = lastMessage,
                        lastTimestamp = c.getLong(lastTsIdx),
                        formattedTimestamp = formatTimestamp(c.getLong(lastTsIdx)),
                        unreadCount = c.getInt(unreadIdx),
                        isPinned = c.getInt(isPinnedIdx) == 1,
                        isMuted = c.getInt(isMutedIdx) == 1,
                        lastMessageStatus = try { c.getString(statusIdx)?.let { MessageStatus.valueOf(it) } } catch (_: Exception) { null },
                        lastMessageSenderId = senderId,
                        isArchived = c.getInt(isArchivedIdx) == 1,
                        peerId = c.getString(peerIdIdx),
                        type = if (isGroup) ChatType.GROUP else ChatType.ONE_TO_ONE,
                        isTyping = false,
                        snippetType = snippetType,
                        snippetUri = snippetUri,
                        latestAuthorName = threadMetadata.latestAuthorName,
                    )
                )
            }
        }
        return list
    }
}
