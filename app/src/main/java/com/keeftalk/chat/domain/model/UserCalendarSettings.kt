package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCalendarSettings(
    // Calendar
    val defaultCalendarId: String = "PRIMARY",
    val defaultEventDurationMinutes: Int = 60,
    val defaultReminderMinutes: Int = 15,
    val weekStartDay: Int = 1, // 1 = Sunday, 2 = Monday...
    val defaultView: String = "MONTH", // DAY, WEEK, MONTH, AGENDA
    val eventNotificationsEnabled: Boolean = true,
    val reminderNotificationsEnabled: Boolean = true,
    val allDayEventReminderTime: String = "09:00",
    
    // Sync
    val syncFrequencyMinutes: Int = 15,
    val syncDeviceCalendar: Boolean = false,
    val syncGoogleCalendar: Boolean = false,
    val offlineModeEnabled: Boolean = true,
    
    // Privacy
    val defaultEventPrivacy: String = "PRIVATE", // PRIVATE, PUBLIC, BUSY_ONLY
    val showBirthdays: Boolean = true,
    val showHolidays: Boolean = true
)
