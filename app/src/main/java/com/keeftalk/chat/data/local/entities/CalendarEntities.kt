package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.model.calendar.*
import com.keeftalk.chat.data.remote.*

@Entity(tableName = "calendar_items")
data class CalendarItemEntity(
    @PrimaryKey val id: String,
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
    val progress: Int = 0,
    val pomodoroCount: Int = 0,
    val deadline: Long? = null,
    val parentItemId: String? = null,
    val pendingSync: Boolean = false,
    val encryptedDescription: String? = null,
    val iv: String? = null,
    val cryptoVersion: Int = 0
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val color: String?,
    val icon: String?,
    val recurrenceRule: String,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true,
    val pendingSync: Boolean = false
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: Long,
    val status: HabitStatus,
    val note: String?,
    val pendingSync: Boolean = false
)

@Entity(tableName = "calendar_item_attendees")
data class CalendarItemAttendeeEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val userId: String,
    val role: AttendeeRole,
    val status: AttendeeStatus
)

@Entity(tableName = "calendar_reminders")
data class CalendarReminderEntity(
    @PrimaryKey val id: String,
    val itemId: String?,
    val habitId: String?,
    val type: ReminderType,
    val minutesBefore: Int,
    val isPersistent: Boolean = false
)

@Entity(tableName = "calendar_categories")
data class CalendarCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val icon: String?,
    val ownerId: String
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val memberId: String,
    val included: Boolean = true
)

@Entity(tableName = "family_permissions")
data class FamilyPermissionEntity(
    @PrimaryKey val id: String,
    val familyMemberId: String,
    val type: CalendarItemType,
    val level: PermissionLevel
)

@Entity(tableName = "calendar_invitations")
data class CalendarInvitationEntity(
    @PrimaryKey val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: InviteStatus,
    val createdAt: Long,
    val kind: String = "Family"
)

data class CalendarItemWithDetails(
    @Embedded val item: CalendarItemEntity,
    @Relation(
        entity = CalendarItemAttendeeEntity::class,
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val attendees: List<CalendarAttendeeWithUser>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            AgendaAttachmentEntity::class,
            parentColumn = "agenda_event_id",
            entityColumn = "file_id"
        )
    )
    val attachments: List<FileEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val reminders: List<CalendarReminderEntity>
)

data class CalendarAttendeeWithUser(
    @Embedded val attendee: CalendarItemAttendeeEntity,
    @Relation(
        entity = UserEntity::class,
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: UserEntity?
)

data class HabitWithLogs(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val logs: List<HabitLogEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val reminders: List<CalendarReminderEntity>
)

data class FamilyMemberWithDetails(
    @Embedded val familyMember: FamilyMemberEntity,
    @Relation(
        entity = UserEntity::class,
        parentColumn = "memberId",
        entityColumn = "id"
    )
    val member: UserEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "familyMemberId"
    )
    val permissions: List<FamilyPermissionEntity>
)

fun CalendarItemWithDetails.toDomain() = item.toDomain(
    attachments = attachments.map { it.toDomain() },
    attendees = attendees.map { it.toDomain() },
    reminders = reminders.map { it.toDomain() }
)

fun CalendarItemEntity.toDomain(
    attachments: List<com.keeftalk.chat.domain.model.File> = emptyList(),
    attendees: List<CalendarAttendee> = emptyList(),
    reminders: List<CalendarReminder> = emptyList()
) = CalendarItem(
    id = id,
    type = type,
    title = title,
    description = description,
    color = color,
    icon = icon,
    location = location,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    timezone = timezone,
    recurrenceRule = recurrenceRule,
    priority = priority,
    status = status,
    isPrivate = isPrivate,
    ownerId = ownerId,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    meetingType = meetingType,
    meetingLink = meetingLink,
    progress = progress,
    pomodoroCount = pomodoroCount,
    deadline = deadline,
    parentItemId = parentItemId,
    attachments = attachments,
    attendees = attendees,
    reminders = reminders
)

fun CalendarItem.toEntity() = CalendarItemEntity(
    id = id,
    type = type,
    title = title,
    description = description,
    color = color,
    icon = icon,
    location = location,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    timezone = timezone,
    recurrenceRule = recurrenceRule,
    priority = priority,
    status = status,
    isPrivate = isPrivate,
    ownerId = ownerId,
    categoryId = categoryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    meetingType = meetingType,
    meetingLink = meetingLink,
    progress = progress,
    pomodoroCount = pomodoroCount,
    deadline = deadline,
    parentItemId = parentItemId
)

fun CalendarAttendeeWithUser.toDomain() = CalendarAttendee(
    id = attendee.id,
    itemId = attendee.itemId,
    userId = attendee.userId,
    role = attendee.role,
    status = attendee.status,
    user = user?.toDomain()
)

fun CalendarAttendee.toEntity() = CalendarItemAttendeeEntity(
    id = id,
    itemId = itemId,
    userId = userId,
    role = role,
    status = status
)

fun CalendarReminderEntity.toDomain() = CalendarReminder(
    id = id,
    itemId = itemId,
    habitId = habitId,
    type = type,
    minutesBefore = minutesBefore,
    isPersistent = isPersistent
)

fun CalendarReminder.toEntity() = CalendarReminderEntity(
    id = id,
    itemId = itemId,
    habitId = habitId,
    type = type,
    minutesBefore = minutesBefore,
    isPersistent = isPersistent
)

fun CalendarCategoryEntity.toDomain() = CalendarCategory(
    id = id,
    name = name,
    color = color,
    icon = icon,
    ownerId = ownerId
)

fun CalendarCategory.toEntity() = CalendarCategoryEntity(
    id = id,
    name = name,
    color = color,
    icon = icon,
    ownerId = ownerId
)

fun FamilyMemberWithDetails.toDomain() = FamilyMember(
    id = familyMember.id,
    userId = familyMember.userId,
    memberId = familyMember.memberId,
    included = familyMember.included,
    member = member?.toDomain(),
    permissions = permissions.map { it.toDomain() }
)

fun FamilyMember.toEntity() = FamilyMemberEntity(
    id = id,
    userId = userId,
    memberId = memberId,
    included = included
)

fun FamilyPermissionEntity.toDomain() = FamilyPermission(
    id = id,
    familyMemberId = familyMemberId,
    type = type,
    level = level
)

fun FamilyPermission.toEntity() = FamilyPermissionEntity(
    id = id,
    familyMemberId = familyMemberId,
    type = type,
    level = level
)

fun FamilyMemberDto.toEntity() = FamilyMemberEntity(
    id = id,
    userId = user_id,
    memberId = member_id,
    included = included
)

fun FamilyPermissionDto.toEntity() = FamilyPermissionEntity(
    id = id,
    familyMemberId = family_member_id,
    type = try { CalendarItemType.valueOf(type) } catch(e: Exception) { CalendarItemType.EVENT },
    level = try { PermissionLevel.valueOf(level) } catch(e: Exception) { PermissionLevel.VIEW }
)

fun CalendarInvitationEntity.toDomain(fromUser: User? = null) = CalendarInvitation(
    id = id,
    fromUserId = fromUserId,
    toUserId = toUserId,
    status = status,
    createdAt = createdAt,
    kind = kind,
    fromUser = fromUser
)

fun HabitWithLogs.toDomain() = habit.toDomain(
    logs = logs.map { it.toDomain() },
    reminders = reminders.map { it.toDomain() }
)

fun HabitEntity.toDomain(
    logs: List<HabitLog> = emptyList(),
    reminders: List<CalendarReminder> = emptyList()
) = Habit(
    id = id,
    title = title,
    description = description,
    color = color,
    icon = icon,
    recurrenceRule = recurrenceRule,
    ownerId = ownerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive,
    logs = logs,
    reminders = reminders
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    description = description,
    color = color,
    icon = icon,
    recurrenceRule = recurrenceRule,
    ownerId = ownerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun HabitLogEntity.toDomain() = HabitLog(
    id = id,
    habitId = habitId,
    date = date,
    status = status,
    note = note
)

fun HabitLog.toEntity() = HabitLogEntity(
    id = id,
    habitId = habitId,
    date = date,
    status = status,
    note = note
)
