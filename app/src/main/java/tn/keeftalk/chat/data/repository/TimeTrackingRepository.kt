package tn.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tn.keeftalk.chat.data.local.dao.TimeTrackingDao
import tn.keeftalk.chat.data.local.entities.TimeTrackingEntity
import tn.keeftalk.chat.di.AppModule
import java.time.LocalDate

@Serializable
private data class SupabaseTimeTrackingDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("epoch_day") val epochDay: Long,
    @SerialName("feature_key") val featureKey: String,
    @SerialName("duration_seconds") val durationSeconds: Long
)

@Serializable
private data class SyncTimeRpcParams(
    @SerialName("p_epoch_day") val epochDay: Long,
    @SerialName("p_feature_key") val featureKey: String,
    @SerialName("p_add_seconds") val addSeconds: Long
)

class TimeTrackingRepository(
    private val context: Context,
    private val dao: TimeTrackingDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        syncPendingLogsToCloud()
        fetchRemoteLogsAndMerge()
    }

    fun observeAllLogs(): Flow<List<TimeTrackingEntity>> {
        return dao.getAllLogsFlow()
    }

    fun observeLogsForRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TimeTrackingEntity>> {
        return dao.getLogsForRangeFlow(startEpochDay, endEpochDay)
    }

    suspend fun addActiveTimeSeconds(featureKey: String, addedSeconds: Long, date: LocalDate = LocalDate.now()) {
        if (addedSeconds <= 0) return
        val epochDay = date.toEpochDay()
        val existing = dao.getLog(epochDay, featureKey)
        val newDuration = (existing?.durationSeconds ?: 0L) + addedSeconds
        val newPending = (existing?.pendingSyncSeconds ?: 0L) + addedSeconds

        val updatedEntity = TimeTrackingEntity(
            epochDay = epochDay,
            featureKey = featureKey,
            durationSeconds = newDuration,
            pendingSyncSeconds = newPending,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertLog(updatedEntity)
        syncPendingLogsToCloud()
    }

    fun syncPendingLogsToCloud() {
        scope.launch {
            runCatching {
                val pendingLogs = dao.getPendingSyncLogs()
                if (pendingLogs.isEmpty()) return@launch

                val supabase: SupabaseClient = AppModule.provideSupabaseClientAsync(context)
                if (supabase.auth.currentUserOrNull() == null) return@launch

                for (log in pendingLogs) {
                    val addSecs = log.pendingSyncSeconds
                    if (addSecs <= 0) continue

                    val params = SyncTimeRpcParams(
                        epochDay = log.epochDay,
                        featureKey = log.featureKey,
                        addSeconds = addSecs
                    )

                    runCatching {
                        supabase.postgrest.rpc("sync_time_tracking", params)
                        dao.clearPendingSync(log.epochDay, log.featureKey)
                    }.onFailure { e ->
                        // Fallback: direct upsert if RPC not yet created in Supabase
                        runCatching {
                            val dto = SupabaseTimeTrackingDto(
                                userId = supabase.auth.currentUserOrNull()?.id ?: return@runCatching,
                                epochDay = log.epochDay,
                                featureKey = log.featureKey,
                                durationSeconds = log.durationSeconds
                            )
                            supabase.from("user_time_tracking").upsert(dto)
                            dao.clearPendingSync(log.epochDay, log.featureKey)
                        }.onFailure { err ->
                            Log.d("TimeTrackingRepo", "Sync fallback notice: ${err.message}")
                        }
                    }
                }
            }
        }
    }

    fun fetchRemoteLogsAndMerge() {
        scope.launch {
            runCatching {
                val supabase: SupabaseClient = AppModule.provideSupabaseClientAsync(context)
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch

                val remoteRows = supabase.from("user_time_tracking")
                    .select()
                    .decodeList<SupabaseTimeTrackingDto>()

                for (remote in remoteRows) {
                    val local = dao.getLog(remote.epochDay, remote.featureKey)
                    val pending = local?.pendingSyncSeconds ?: 0L
                    val mergedDuration = maxOf(local?.durationSeconds ?: 0L, remote.durationSeconds + pending)

                    dao.upsertLog(
                        TimeTrackingEntity(
                            epochDay = remote.epochDay,
                            featureKey = remote.featureKey,
                            durationSeconds = mergedDuration,
                            pendingSyncSeconds = pending,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }.onFailure { e ->
                Log.d("TimeTrackingRepo", "Remote fetch notice: ${e.message}")
            }
        }
    }
}
