package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_sync_queue")
data class VaultSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val operation: String, // 'UPLOAD', 'DELETE', 'RENAME', 'MOVE', 'FAVORITE', 'LOCK'
    val payload: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
