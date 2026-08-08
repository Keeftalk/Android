package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["messageId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"])]
)
data class MessageReactionEntity(
    val messageId: String,
    val userId: String,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)
