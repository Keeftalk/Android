package tn.keeftalk.chat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.dao.NativeHabitDao
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.data.remote.HabitsRemoteDataSource
import tn.keeftalk.chat.domain.engine.HabitEngine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HabitsRepositoryTest {

    private class FakeDao : NativeHabitDao {
        val checkIns = mutableListOf<CheckInEntity>()
        override fun getAllHabits(): Flow<List<HabitEntity>> = flowOf(emptyList())
        override fun getAllSchedules(): Flow<List<ScheduleVersionEntity>> = flowOf(emptyList())
        override fun getAllCheckIns(): Flow<List<CheckInEntity>> = flowOf(checkIns)
        override fun getAllGraceUses(): Flow<List<GraceUseEntity>> = flowOf(emptyList())
        override fun getAllMilestones(): Flow<List<MilestoneUnlockEntity>> = flowOf(emptyList())
        override suspend fun upsertHabit(habit: HabitEntity) {}
        override suspend fun upsertSchedule(schedule: ScheduleVersionEntity) {}
        override suspend fun upsertCheckIn(checkIn: CheckInEntity) {
            checkIns.removeIf { it.id == checkIn.id }
            checkIns.add(checkIn)
        }
        override suspend fun upsertGraceUse(graceUse: GraceUseEntity) {}
        override suspend fun upsertMilestone(milestone: MilestoneUnlockEntity) {}
        override suspend fun softDeleteHabit(habitId: String, timestamp: Long) {}
        override suspend fun deleteCheckIn(habitId: String, epochDay: Long) {
            checkIns.removeIf { it.habitId == habitId && it.epochDay == epochDay }
        }
    }

    private val dao = FakeDao()
    private val remoteDataSource = object : HabitsRemoteDataSource {
        override suspend fun syncPush(habits: List<tn.keeftalk.chat.domain.model.habit.Habit>, schedules: List<tn.keeftalk.chat.domain.model.habit.ScheduleVersion>, checkIns: List<tn.keeftalk.chat.domain.model.habit.CheckIn>, graceUses: List<tn.keeftalk.chat.domain.model.habit.GraceUse>) {}
        override suspend fun syncPull() = Result.success(Unit)
        override suspend fun checkInAuthoritative(habitId: String, completed: Boolean) = Result.success(Unit)
    }

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-03-10T10:00:00Z"),
        ZoneId.of("UTC")
    )
    private val engine = HabitEngine(fixedClock)
    private val repository = HabitsRepositoryImpl(null, dao, remoteDataSource, engine)

    @Test
    fun testS9_RepositoryRejectsNonTodayCheckIn() = runBlocking {
        val pastDate = LocalDate.of(2026, 3, 8) // Not today (2026-03-10)
        val result = repository.setCheckIn("habit_1", pastDate, true)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testS9_RepositoryAcceptsTodayCheckIn() = runBlocking {
        val todayDate = LocalDate.of(2026, 3, 10) // Today
        val result = repository.setCheckIn("habit_1", todayDate, true)

        assertTrue(result.isSuccess)
        assertEquals(1, dao.checkIns.size)
    }
}
