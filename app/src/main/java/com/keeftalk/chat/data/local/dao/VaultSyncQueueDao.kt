package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.VaultSyncQueueEntity

@Dao
interface VaultSyncQueueDao {
    @Query("SELECT * FROM vault_sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<VaultSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultSyncQueueEntity)

    @Update
    suspend fun update(item: VaultSyncQueueEntity)

    @Delete
    suspend fun delete(item: VaultSyncQueueEntity)
}
