package tn.keeftalk.chat.data.remote

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.habit.*

interface HabitsRemoteDataSource {
    suspend fun syncPush(habits: List<Habit>, schedules: List<ScheduleVersion>, checkIns: List<CheckIn>, graceUses: List<GraceUse>)
    suspend fun syncPull(): Result<Unit>
    suspend fun checkInAuthoritative(habitId: String, completed: Boolean): Result<Unit>
}

class HabitsRemoteDataSourceImpl(
    private val context: Context
) : HabitsRemoteDataSource {

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    override suspend fun syncPush(
        habits: List<Habit>,
        schedules: List<ScheduleVersion>,
        checkIns: List<CheckIn>,
        graceUses: List<GraceUse>
    ) {
        try {
            val supabase = getSupabase()
            for (ci in checkIns) {
                supabase.functions.invoke(
                    "habit-checkin",
                    body = mapOf(
                        "habitId" to ci.habitId,
                        "completed" to ci.completed
                    )
                )
            }
        } catch (_: Exception) {
            // Handled offline / outbox
        }
    }

    override suspend fun syncPull(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun checkInAuthoritative(habitId: String, completed: Boolean): Result<Unit> {
        return runCatching {
            val supabase = getSupabase()
            supabase.functions.invoke(
                "habit-checkin",
                body = mapOf(
                    "habitId" to habitId,
                    "completed" to completed
                )
            )
            Unit
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }
}

class HabitsRemoteDataSourceNoOp : HabitsRemoteDataSource {
    override suspend fun syncPush(
        habits: List<Habit>,
        schedules: List<ScheduleVersion>,
        checkIns: List<CheckIn>,
        graceUses: List<GraceUse>
    ) {}

    override suspend fun syncPull(): Result<Unit> = Result.success(Unit)

    override suspend fun checkInAuthoritative(habitId: String, completed: Boolean): Result<Unit> = Result.success(Unit)
}
