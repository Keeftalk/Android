package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.CalendarSyncQueueEntity

@Dao
interface CalendarSyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CalendarSyncQueueEntity)

    @Query("SELECT * FROM calendar_sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<CalendarSyncQueueEntity>

    @Update
    suspend fun update(item: CalendarSyncQueueEntity)

    @Delete
    suspend fun delete(item: CalendarSyncQueueEntity)

    @Query("DELETE FROM calendar_sync_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
