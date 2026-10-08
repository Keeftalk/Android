package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.keeftalk.chat.domain.model.habit.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val icon: String,
    val category: String,
    val baseDays: Int,
    val createdDateEpochDay: Long,
    val reminderTimeMinutes: Int?,
    val slot: String,
    val habitType: String,
    val amount: Int? = null,
    val unit: String? = null,
    val colorHex: String = "#00CCCC",
    val isReadingHabit: Boolean = false,
    val autoCountReading: Boolean = true,
    val linkedBookIdsString: String = "",
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)

@Entity(tableName = "habit_schedules")
data class ScheduleVersionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val effectiveFromEpochDay: Long,
    val daysMaskString: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)

@Entity(tableName = "habit_checkins")
data class CheckInEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val completed: Boolean,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)

@Entity(tableName = "habit_grace_uses")
data class GraceUseEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val forgivenEpochDay: Long,
    val usedEpochDay: Long,
    val usedMonthKey: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)

@Entity(tableName = "habit_milestones")
data class MilestoneUnlockEntity(
    @PrimaryKey val key: String,
    val unlockedAtEpochDay: Long,
    val updatedAt: Long
)

// Mappers
fun HabitEntity.toDomain(): Habit {
    val reminder = if (reminderTimeMinutes != null && reminderTimeMinutes >= 0) {
        LocalTime.of(reminderTimeMinutes / 60, reminderTimeMinutes % 60)
    } else null
    return Habit(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        category = category,
        baseDays = baseDays,
        createdDate = LocalDate.ofEpochDay(createdDateEpochDay),
        reminderTime = reminder,
        slot = slot,
        habitType = habitType,
        amount = amount,
        unit = unit,
        colorHex = colorHex,
        updatedAt = updatedAt,
        deleted = deleted
    )
}

fun Habit.toEntity(userId: String, syncState: String = "PENDING"): HabitEntity {
    val mins = reminderTime?.let { it.hour * 60 + it.minute } ?: -1
    return HabitEntity(
        id = id,
        userId = userId,
        name = name,
        icon = icon,
        category = category,
        baseDays = baseDays,
        createdDateEpochDay = createdDate.toEpochDay(),
        reminderTimeMinutes = mins,
        slot = slot,
        habitType = habitType,
        amount = amount,
        unit = unit,
        colorHex = colorHex,
        updatedAt = updatedAt,
        deleted = deleted,
        syncState = syncState
    )
}

fun ScheduleVersionEntity.toDomain(): ScheduleVersion {
    val days = if (daysMaskString.isBlank()) emptySet() else {
        daysMaskString.split(",").map { DayOfWeek.valueOf(it.trim()) }.toSet()
    }
    return ScheduleVersion(
        habitId = habitId,
        effectiveFrom = LocalDate.ofEpochDay(effectiveFromEpochDay),
        daysMask = days
    )
}

fun ScheduleVersion.toEntity(habitId: String, syncState: String = "PENDING"): ScheduleVersionEntity {
    val maskStr = daysMask.joinToString(",") { it.name }
    return ScheduleVersionEntity(
        id = "${habitId}_${effectiveFrom.toEpochDay()}",
        habitId = habitId,
        effectiveFromEpochDay = effectiveFrom.toEpochDay(),
        daysMaskString = maskStr,
        updatedAt = System.currentTimeMillis(),
        deleted = false,
        syncState = syncState
    )
}

fun CheckInEntity.toDomain(): CheckIn {
    return CheckIn(
        habitId = habitId,
        date = LocalDate.ofEpochDay(epochDay),
        completed = completed,
        updatedAt = updatedAt
    )
}

fun CheckIn.toEntity(syncState: String = "PENDING"): CheckInEntity {
    return CheckInEntity(
        id = "${habitId}_${date.toEpochDay()}",
        habitId = habitId,
        epochDay = date.toEpochDay(),
        completed = completed,
        updatedAt = updatedAt,
        deleted = false,
        syncState = syncState
    )
}

fun GraceUseEntity.toDomain(): GraceUse {
    return GraceUse(
        habitId = habitId,
        forgivenDate = LocalDate.ofEpochDay(forgivenEpochDay),
        usedDate = LocalDate.ofEpochDay(usedEpochDay),
        usedMonthKey = usedMonthKey,
        updatedAt = updatedAt
    )
}

fun GraceUse.toEntity(syncState: String = "PENDING"): GraceUseEntity {
    return GraceUseEntity(
        id = "${habitId}_${forgivenDate.toEpochDay()}",
        habitId = habitId,
        forgivenEpochDay = forgivenDate.toEpochDay(),
        usedEpochDay = usedDate.toEpochDay(),
        usedMonthKey = usedMonthKey,
        updatedAt = updatedAt,
        deleted = false,
        syncState = syncState
    )
}
