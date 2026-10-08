package tn.keeftalk.chat.data.remote

import kotlinx.serialization.Serializable
import tn.keeftalk.chat.domain.model.UserHabitSettings

@Serializable
data class UserHabitSettingsDto(
    val defaultReminderTime: String = "08:00",
    val defaultCategory: String = "drink",
    val weekStartDay: String = "MONDAY",
    val gracePoolAlertsEnabled: Boolean = true,
    val autoArchiveMastered: Boolean = false,
    val hideCompletedToday: Boolean = false,
    val syncHabitsAcrossDevices: Boolean = true,
    val introShown: Boolean = false,
    val lastCelebratedEpochDay: Long = -1L
) {
    fun toDomain() = UserHabitSettings(
        defaultReminderTime = defaultReminderTime,
        defaultCategory = defaultCategory,
        weekStartDay = weekStartDay,
        gracePoolAlertsEnabled = gracePoolAlertsEnabled,
        autoArchiveMastered = autoArchiveMastered,
        hideCompletedToday = hideCompletedToday,
        syncHabitsAcrossDevices = syncHabitsAcrossDevices,
        introShown = introShown,
        lastCelebratedEpochDay = lastCelebratedEpochDay
    )

    companion object {
        fun fromDomain(d: UserHabitSettings) = UserHabitSettingsDto(
            defaultReminderTime = d.defaultReminderTime,
            defaultCategory = d.defaultCategory,
            weekStartDay = d.weekStartDay,
            gracePoolAlertsEnabled = d.gracePoolAlertsEnabled,
            autoArchiveMastered = d.autoArchiveMastered,
            hideCompletedToday = d.hideCompletedToday,
            syncHabitsAcrossDevices = d.syncHabitsAcrossDevices,
            introShown = d.introShown,
            lastCelebratedEpochDay = d.lastCelebratedEpochDay
        )
    }
}
