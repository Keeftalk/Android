package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_list_cache",
    indices = [androidx.room.Index(value = ["sortOrder"])]
)
data class ChatListCacheEntity(
    @PrimaryKey val chatId: String,
    val userId: String?,
    val username: String?,
    val avatarPath: String?,
    val lastMessage: String?,
    val lastMessageTimestamp: Long,
    val messageStatus: String?,
    val lastMessageSenderId: String?,
    val unreadCount: Int,
    val sortOrder: Int,
    val snippetType: String? = null,
    val snippetUri: String? = null
)
