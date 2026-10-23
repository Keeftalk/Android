package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.*

@Dao
interface NativeHabitDao {
    @Query("SELECT * FROM habits WHERE deleted = 0")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_schedules")
    fun getAllSchedules(): Flow<List<ScheduleVersionEntity>>

    @Query("SELECT * FROM habit_checkins WHERE deleted = 0")
    fun getAllCheckIns(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM habit_grace_uses WHERE deleted = 0")
    fun getAllGraceUses(): Flow<List<GraceUseEntity>>

    @Query("SELECT * FROM habit_milestones")
    fun getAllMilestones(): Flow<List<MilestoneUnlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(schedule: ScheduleVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckIn(checkIn: CheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGraceUse(graceUse: GraceUseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestone(milestone: MilestoneUnlockEntity)

    @Query("UPDATE habits SET deleted = 1, updatedAt = :timestamp WHERE id = :habitId")
    suspend fun softDeleteHabit(habitId: String, timestamp: Long)

    @Query("DELETE FROM habit_checkins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCheckIn(habitId: String, epochDay: Long)

    @Query("UPDATE habits SET userId = :newUserId WHERE userId = 'local_user'")
    suspend fun associateLocalHabits(newUserId: String)
}
