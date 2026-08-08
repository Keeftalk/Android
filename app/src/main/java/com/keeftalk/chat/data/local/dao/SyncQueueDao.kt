package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("SELECT * FROM notes_sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Delete
    suspend fun deleteItems(items: List<SyncQueueEntity>)

    @Query("DELETE FROM notes_sync_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
