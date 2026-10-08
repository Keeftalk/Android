package tn.keeftalk.chat.domain.model.habit

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val category: String,
    val baseDays: Int, // e.g. 18 for effTarget baseline
    val createdDate: LocalDate,
    val reminderTime: LocalTime?,
    val slot: String,
    val habitType: String,
    val amount: Int? = null,
    val unit: String? = null,
    val colorHex: String = "#00CCCC",
    val isReadingHabit: Boolean = false,
    val autoCountReading: Boolean = true,
    val linkedBookIds: List<String> = emptyList(),
    val updatedAt: Long,
    val deleted: Boolean = false
)

data class ScheduleVersion(
    val habitId: String,
    val effectiveFrom: LocalDate,
    val daysMask: Set<DayOfWeek> // e.g. set of days when habit is due
)

data class CheckIn(
    val habitId: String,
    val date: LocalDate,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

data class GraceUse(
    val habitId: String,
    val forgivenDate: LocalDate,
    val usedDate: LocalDate,
    val usedMonthKey: String, // "YYYY-MM"
    val updatedAt: Long = System.currentTimeMillis()
)

data class MilestoneUnlock(
    val key: String,
    val unlockedAt: LocalDate
)

data class HabitCalculatedStatus(
    val habitId: String,
    val streak: Int,
    val longestStreak: Int,
    val doneCount: Int,
    val effMissed: Int,
    val effTarget: Int,
    val automaticity: Float, // 0.0 to 1.0
    val isMastered: Boolean,
    val isDueToday: Boolean,
    val isCheckedInToday: Boolean,
    val isForgivenToday: Boolean,
    val hasUngracedMiss: Boolean = false,
    val recoveryActive: Boolean,
    val recoveryLevel: Int, // 0 to 4
    val recoveryProgress: Int,
    val recoveryRequired: Int
)
