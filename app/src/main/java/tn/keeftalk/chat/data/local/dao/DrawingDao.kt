package tn.keeftalk.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.DrawingEntity
import tn.keeftalk.chat.data.local.entities.DrawingSyncQueueEntity

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings WHERE is_deleted = 0 ORDER BY updated_at DESC")
    fun getAllDrawingsFlow(): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM drawings WHERE is_deleted = 0 ORDER BY updated_at DESC")
    suspend fun getAllDrawings(): List<DrawingEntity>

    @Query("SELECT * FROM drawings WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getDrawingById(id: String): DrawingEntity?

    @Query("SELECT * FROM drawings WHERE id = :id AND is_deleted = 0 LIMIT 1")
    fun getDrawingByIdFlow(id: String): Flow<DrawingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDrawing(drawing: DrawingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDrawings(drawings: List<DrawingEntity>)

    @Query("UPDATE drawings SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markDrawingDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE drawings SET is_deleted = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun restoreDrawing(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM drawings WHERE id = :id")
    suspend fun hardDeleteDrawing(id: String)

    // Sync Queue
    @Query("SELECT * FROM drawing_sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingSyncItems(): List<DrawingSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: DrawingSyncQueueEntity)

    @Query("DELETE FROM drawing_sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: String)
}
