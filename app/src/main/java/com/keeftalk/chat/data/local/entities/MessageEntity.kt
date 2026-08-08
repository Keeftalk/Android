package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId", "timestamp"]),
        Index(value = ["chatId", "decryptionState", "timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: String,
    val type: String,
    val replyToId: String? = null,
    val translatedContent: String? = null,
    val sourceLanguage: String? = null,
    val mediaLocked: Boolean = false,
    val mediaLockUpdatedAt: Long? = null,
    val mediaLockUpdatedBy: String? = null,
    val ciphertext: String? = null,
    val cryptoVersion: Int = 0,
    val envelopeType: Int = 0,
    val nonce: String? = null,
    val decryptionState: com.keeftalk.chat.domain.model.DecryptionState = com.keeftalk.chat.domain.model.DecryptionState.SUCCESS,
    val retryCount: Int = 0
)
