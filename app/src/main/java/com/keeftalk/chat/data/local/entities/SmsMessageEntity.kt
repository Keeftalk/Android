package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_messages",
    indices = [Index(value = ["threadId", "timestamp"])]
)
data class SmsMessageEntity(
    @PrimaryKey val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val read: Int,
    val type: Int, // 1 for inbox, 2 for sent, etc.
    val status: Int, // -1 for none, 0 for complete, 32 for pending, 64 for failed
    val isMms: Boolean = false,
    val attachmentsJson: String? = null, // JSON list of attachment URIs and types
    val deliveryStatus: Int = -1 // For delivery reports
)
