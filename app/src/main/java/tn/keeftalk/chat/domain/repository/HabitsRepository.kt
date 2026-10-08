package tn.keeftalk.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.habit.*
import java.time.LocalDate

interface HabitsRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeSchedules(): Flow<List<ScheduleVersion>>
    fun observeCheckIns(): Flow<List<CheckIn>>
    fun observeGraceUses(): Flow<List<GraceUse>>
    fun observeMilestones(): Flow<List<MilestoneUnlock>>

    suspend fun saveHabit(habit: Habit, schedule: ScheduleVersion)
    suspend fun updateHabit(habit: Habit, newSchedule: ScheduleVersion?)
    suspend fun deleteHabit(habitId: String)
    suspend fun setCheckIn(habitId: String, date: LocalDate, completed: Boolean): Result<Unit>
    suspend fun useGrace(habitId: String, forgivenDate: LocalDate, usageDate: LocalDate): Result<Unit>
    suspend fun unlockMilestone(key: String, date: LocalDate)
    suspend fun seedDefaultDataIfEmpty() {}
}
