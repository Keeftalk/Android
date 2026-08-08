package com.keeftalk.chat.security.crypto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_keys",
    indices = [Index(value = ["conversationId", "user_id"], unique = true)]
)
data class ConversationKeyEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val encryptedKey: String, // PCK encrypted with CPK
    val nonce: String,
    val version: Int = 1,
    val epoch: Int = 1, // Incremented on membership change
    val createdAt: Long = System.currentTimeMillis()
)
