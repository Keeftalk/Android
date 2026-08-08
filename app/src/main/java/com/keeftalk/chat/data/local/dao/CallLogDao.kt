package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: String)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Query("SELECT * FROM call_logs WHERE cloudSyncStatus != 1")
    suspend fun getUnsyncedCallLogs(): List<CallLogEntity>

    @Query("UPDATE call_logs SET cloudSyncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: Int)
}
