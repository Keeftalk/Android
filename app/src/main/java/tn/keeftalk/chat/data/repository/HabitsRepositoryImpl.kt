package tn.keeftalk.chat.data.repository

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import tn.keeftalk.chat.data.local.dao.NativeHabitDao
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.data.remote.HabitsRemoteDataSource
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.engine.HabitEngine
import tn.keeftalk.chat.domain.model.habit.*
import tn.keeftalk.chat.domain.repository.HabitsRepository
import tn.keeftalk.chat.util.reminders.HabitReminderManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitsRepositoryImpl(
    private val context: Context? = null,
    private val nativeHabitDao: NativeHabitDao,
    private val remoteDataSource: HabitsRemoteDataSource,
    private val engine: HabitEngine = HabitEngine()
) : HabitsRepository {

    private suspend fun getUserId(): String {
        val uid = runCatching {
            val ctx = context ?: return@runCatching "local_user"
            val supabase: SupabaseClient = AppModule.provideSupabaseClientAsync(ctx)
            supabase.auth.currentUserOrNull()?.id ?: "local_user"
        }.getOrDefault("local_user")

        if (uid != "local_user") {
            try {
                nativeHabitDao.associateLocalHabits(uid)
            } catch (_: Exception) {}
        }
        return uid
    }

    override fun observeHabits(): Flow<List<Habit>> {
        return nativeHabitDao.getAllHabits().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeSchedules(): Flow<List<ScheduleVersion>> {
        return nativeHabitDao.getAllSchedules().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeCheckIns(): Flow<List<CheckIn>> {
        return nativeHabitDao.getAllCheckIns().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeGraceUses(): Flow<List<GraceUse>> {
        return nativeHabitDao.getAllGraceUses().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeMilestones(): Flow<List<MilestoneUnlock>> {
        return nativeHabitDao.getAllMilestones().map { entities ->
            entities.map { MilestoneUnlock(key = it.key, unlockedAt = LocalDate.ofEpochDay(it.unlockedAtEpochDay)) }
        }
    }

    override suspend fun saveHabit(habit: Habit, schedule: ScheduleVersion) {
        val userId = getUserId()
        nativeHabitDao.upsertHabit(habit.toEntity(userId))
        nativeHabitDao.upsertSchedule(schedule.toEntity(habit.id))
        context?.let { ctx ->
            HabitReminderManager.scheduleAllHabitAlarms(ctx, habit)
        }
    }

    override suspend fun updateHabit(habit: Habit, newSchedule: ScheduleVersion?) {
        val userId = getUserId()
        nativeHabitDao.upsertHabit(habit.toEntity(userId))
        if (newSchedule != null) {
            nativeHabitDao.upsertSchedule(newSchedule.toEntity(habit.id))
        }
        context?.let { ctx ->
            HabitReminderManager.scheduleAllHabitAlarms(ctx, habit)
        }
    }

    override suspend fun deleteHabit(habitId: String) {
        nativeHabitDao.softDeleteHabit(habitId, System.currentTimeMillis())
        context?.let { ctx ->
            HabitReminderManager.cancelReminder(ctx, habitId)
        }
    }

    override suspend fun setCheckIn(habitId: String, date: LocalDate, completed: Boolean): Result<Unit> {
        val today = engine.today()
        val now = java.time.LocalDateTime.now()
        val isToday = date == today
        val isYesterdayLateNight = date == today.minusDays(1) && now.hour < 5
        if (!isToday && !isYesterdayLateNight) {
            return Result.failure(IllegalArgumentException("Past or future check-ins are read-only and cannot be modified."))
        }
        val checkIn = CheckIn(
            habitId = habitId,
            date = date,
            completed = completed,
            updatedAt = System.currentTimeMillis()
        )
        if (completed) {
            nativeHabitDao.upsertCheckIn(checkIn.toEntity(syncState = "PENDING"))
        } else {
            nativeHabitDao.deleteCheckIn(habitId, date.toEpochDay())
        }

        context?.let { ctx ->
            val remote = tn.keeftalk.chat.data.remote.HabitsRemoteDataSourceImpl(ctx)
            runCatching {
                remote.checkInAuthoritative(habitId, completed)
            }.onSuccess { res ->
                if (res.isSuccess && completed) {
                    nativeHabitDao.upsertCheckIn(checkIn.toEntity(syncState = "SYNCED"))
                }
            }
        }

        return Result.success(Unit)
    }

    override suspend fun useGrace(habitId: String, forgivenDate: LocalDate, usageDate: LocalDate): Result<Unit> {
        val monthKey = usageDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val graceUse = GraceUse(
            habitId = habitId,
            forgivenDate = forgivenDate,
            usedDate = usageDate,
            usedMonthKey = monthKey,
            updatedAt = System.currentTimeMillis()
        )
        nativeHabitDao.upsertGraceUse(graceUse.toEntity())
        return Result.success(Unit)
    }

    override suspend fun unlockMilestone(key: String, date: LocalDate) {
        nativeHabitDao.upsertMilestone(
            MilestoneUnlockEntity(
                key = key,
                unlockedAtEpochDay = date.toEpochDay(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun seedDefaultDataIfEmpty() {
        // Remove all mockup/demo data if previously seeded
        val existing = nativeHabitDao.getAllHabits().firstOrNull() ?: emptyList()
        val seedHabits = existing.filter { it.id.startsWith("seed_habit_") }
        seedHabits.forEach { habit ->
            nativeHabitDao.softDeleteHabit(habit.id, System.currentTimeMillis())
        }
    }
}
