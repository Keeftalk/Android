package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_threads")
data class SmsThreadEntity(
    @PrimaryKey val threadId: Long,
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unreadCount: Int,
    val recipientId: String? = null, // Keeftalk profile ID if matched
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val draft: String? = null
)
