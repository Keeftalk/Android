package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_sync_queue")
data class ReceiptSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String,
    val messageId: String,
    val type: String, // "READ" or "DELIVERED"
    val timestamp: Long,
    val status: String = "PENDING"
)
