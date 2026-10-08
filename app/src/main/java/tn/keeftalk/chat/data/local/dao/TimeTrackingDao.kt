package tn.keeftalk.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.TimeTrackingEntity

@Dao
interface TimeTrackingDao {

    @Query("SELECT * FROM time_tracking_logs ORDER BY epochDay DESC")
    fun getAllLogsFlow(): Flow<List<TimeTrackingEntity>>

    @Query("SELECT * FROM time_tracking_logs WHERE epochDay = :epochDay")
    suspend fun getLogsForDay(epochDay: Long): List<TimeTrackingEntity>

    @Query("SELECT * FROM time_tracking_logs WHERE epochDay >= :startEpochDay AND epochDay <= :endEpochDay")
    fun getLogsForRangeFlow(startEpochDay: Long, endEpochDay: Long): Flow<List<TimeTrackingEntity>>

    @Query("SELECT * FROM time_tracking_logs WHERE epochDay >= :startEpochDay AND epochDay <= :endEpochDay")
    suspend fun getLogsForRange(startEpochDay: Long, endEpochDay: Long): List<TimeTrackingEntity>

    @Query("SELECT * FROM time_tracking_logs WHERE epochDay = :epochDay AND featureKey = :featureKey")
    suspend fun getLog(epochDay: Long, featureKey: String): TimeTrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: TimeTrackingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(logs: List<TimeTrackingEntity>)

    @Query("SELECT * FROM time_tracking_logs WHERE pendingSyncSeconds > 0")
    suspend fun getPendingSyncLogs(): List<TimeTrackingEntity>

    @Query("UPDATE time_tracking_logs SET pendingSyncSeconds = 0 WHERE epochDay = :epochDay AND featureKey = :featureKey")
    suspend fun clearPendingSync(epochDay: Long, featureKey: String)

    @Query("DELETE FROM time_tracking_logs")
    suspend fun deleteAll()
}
