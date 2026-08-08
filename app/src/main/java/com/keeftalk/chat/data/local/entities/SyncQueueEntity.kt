package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: String,
    val operation: String, // 'SAVE', 'DELETE', 'PIN', 'ARCHIVE', 'SHARE'
    val payload: String?, // JSON representation of the action data
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
