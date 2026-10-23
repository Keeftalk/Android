package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val timestamp: Long,
    val cloudSyncStatus: Int = 0 // 0: Unsynced, 1: Synced
)
