package com.keeftalk.chat.data.local.converters

import androidx.room.TypeConverter
import com.keeftalk.chat.domain.model.calendar.*
import kotlinx.serialization.json.Json

class KeeftalkConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toMap(value: String): Map<String, String> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromCalendarItemType(value: CalendarItemType): String = value.name

    @TypeConverter
    fun toCalendarItemType(value: String): CalendarItemType = CalendarItemType.valueOf(value)

    @TypeConverter
    fun fromCalendarPriority(value: CalendarPriority): String = value.name

    @TypeConverter
    fun toCalendarPriority(value: String): CalendarPriority = CalendarPriority.valueOf(value)

    @TypeConverter
    fun fromCalendarStatus(value: CalendarStatus): String = value.name

    @TypeConverter
    fun toCalendarStatus(value: String): CalendarStatus = CalendarStatus.valueOf(value)

    @TypeConverter
    fun fromMeetingType(value: MeetingType?): String? = value?.name

    @TypeConverter
    fun toMeetingType(value: String?): MeetingType? = value?.let { MeetingType.valueOf(it) }

    @TypeConverter
    fun fromHabitStatus(value: HabitStatus): String = value.name

    @TypeConverter
    fun toHabitStatus(value: String): HabitStatus = HabitStatus.valueOf(value)

    @TypeConverter
    fun fromAttendeeRole(value: AttendeeRole): String = value.name

    @TypeConverter
    fun toAttendeeRole(value: String): AttendeeRole = AttendeeRole.valueOf(value)

    @TypeConverter
    fun fromAttendeeStatus(value: AttendeeStatus): String = value.name

    @TypeConverter
    fun toAttendeeStatus(value: String): AttendeeStatus = AttendeeStatus.valueOf(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromDecryptionState(value: com.keeftalk.chat.domain.model.DecryptionState): String = value.name

    @TypeConverter
    fun toDecryptionState(value: String): com.keeftalk.chat.domain.model.DecryptionState = com.keeftalk.chat.domain.model.DecryptionState.valueOf(value)
}
