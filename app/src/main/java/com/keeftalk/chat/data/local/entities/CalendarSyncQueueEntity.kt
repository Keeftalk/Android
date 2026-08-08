package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_sync_queue")
data class CalendarSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val operation: String,
    val payload: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
