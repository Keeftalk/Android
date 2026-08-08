package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.ReceiptSyncQueueEntity

@Dao
interface ReceiptSyncQueueDao {
    @Query("SELECT * FROM receipt_sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingReceipts(): List<ReceiptSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueReceipt(receipt: ReceiptSyncQueueEntity)

    @Query("UPDATE receipt_sync_queue SET status = 'COMPLETED' WHERE id = :id")
    suspend fun markAsCompleted(id: Int)

    @Query("DELETE FROM receipt_sync_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    @Query("DELETE FROM receipt_sync_queue WHERE chatId = :chatId AND type = :type AND timestamp < :timestamp")
    suspend fun removeObsoleteReceipts(chatId: String, type: String, timestamp: Long)
}
