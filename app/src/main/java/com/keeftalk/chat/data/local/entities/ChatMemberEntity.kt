package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chat_members",
    primaryKeys = ["chatId", "userId"],
    indices = [Index(value = ["userId"])]
)
data class ChatMemberEntity(
    val chatId: String,
    val userId: String,
    val lastReadMessageId: String? = null,
    val lastDeliveredMessageId: String? = null,
    val lastReadAt: Long? = null,
    val lastDeliveredAt: Long? = null
)
