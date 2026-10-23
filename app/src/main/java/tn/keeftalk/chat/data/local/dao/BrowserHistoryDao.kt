package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.BrowserHistoryEntity

@Dao
interface BrowserHistoryDao {
    @Query("SELECT * FROM browser_history ORDER BY timestamp DESC")
    fun getBrowserHistory(): Flow<List<BrowserHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: String)

    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM browser_history WHERE cloudSyncStatus = 0")
    suspend fun getUnsyncedHistory(): List<BrowserHistoryEntity>

    @Query("UPDATE browser_history SET cloudSyncStatus = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
