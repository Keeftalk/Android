package com.keeftalk.chat.domain.model.calendar

import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.model.File
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CalendarItemType : Parcelable {
    EVENT, TASK, REMINDER, BIRTHDAY, MEETING, GOAL
}

enum class CalendarPriority {
    LOW, MEDIUM, HIGH
}

enum class CalendarStatus {
    PENDING, COMPLETED, CANCELLED
}

enum class MeetingType {
    ONLINE, OFFLINE, GOOGLE_MEET, ZOOM, TEAMS, KEEFTALK_VOICE, KEEFTALK_VIDEO
}

data class CalendarItem(
    val id: String,
    val type: CalendarItemType,
    val title: String,
    val description: String,
    val color: String?,
    val icon: String?,
    val location: String?,
    val startTime: Long?,
    val endTime: Long?,
    val isAllDay: Boolean,
    val timezone: String?,
    val recurrenceRule: String?,
    val priority: CalendarPriority,
    val status: CalendarStatus,
    val isPrivate: Boolean,
    val ownerId: String,
    val categoryId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val meetingType: MeetingType?,
    val meetingLink: String?,
    val progress: Int,
    val pomodoroCount: Int,
    val deadline: Long?,
    val parentItemId: String?,
    val attachments: List<File> = emptyList(),
    val attendees: List<CalendarAttendee> = emptyList(),
    val reminders: List<CalendarReminder> = emptyList()
)

data class CalendarAttendee(
    val id: String,
    val itemId: String,
    val userId: String,
    val role: AttendeeRole,
    val status: AttendeeStatus,
    val user: User? = null
)

enum class AttendeeRole {
    OWNER, EDITOR, COMMENTER, VIEWER
}

enum class AttendeeStatus {
    PENDING, ACCEPTED, DECLINED, TENTATIVE
}

enum class PermissionLevel {
    NONE, VIEW, VIEW_EDIT, FULL
}

data class CalendarInvitation(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: InviteStatus,
    val createdAt: Long,
    val kind: String = "Family",
    val fromUser: User? = null,
    val initialPermissions: List<FamilyPermission> = emptyList()
)

enum class InviteStatus {
    PENDING, ACCEPTED, REJECTED
}

data class CalendarReminder(
    val id: String,
    val itemId: String?,
    val habitId: String?,
    val type: ReminderType,
    val minutesBefore: Int,
    val isPersistent: Boolean
)

enum class ReminderType {
    NOTIFICATION, POPUP, SOUND, VIBRATION, EMAIL, CHAT
}

data class CalendarCategory(
    val id: String,
    val name: String,
    val color: String,
    val icon: String?,
    val ownerId: String
)

@Parcelize
data class FamilyMember(
    val id: String,
    val userId: String,
    val memberId: String,
    val included: Boolean,
    val member: User? = null,
    val permissions: List<FamilyPermission> = emptyList()
) : Parcelable

@Parcelize
data class FamilyPermission(
    val id: String,
    val familyMemberId: String,
    val type: CalendarItemType,
    val level: PermissionLevel
) : Parcelable

data class Habit(
    val id: String,
    val title: String,
    val description: String?,
    val color: String?,
    val icon: String?,
    val recurrenceRule: String,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val logs: List<HabitLog> = emptyList(),
    val reminders: List<CalendarReminder> = emptyList()
)

data class HabitLog(
    val id: String,
    val habitId: String,
    val date: Long,
    val status: HabitStatus,
    val note: String?
)

enum class HabitStatus {
    COMPLETED, SKIPPED
}
