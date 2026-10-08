package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserHabitSettings(
    val defaultReminderTime: String = "08:00",
    val defaultCategory: String = "drink",
    val weekStartDay: String = "MONDAY",
    val gracePoolAlertsEnabled: Boolean = true,
    val autoArchiveMastered: Boolean = false,
    val hideCompletedToday: Boolean = false,
    val syncHabitsAcrossDevices: Boolean = true,
    val introShown: Boolean = false,
    val lastCelebratedEpochDay: Long = -1L
)
