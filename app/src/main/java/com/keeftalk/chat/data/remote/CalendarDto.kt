package com.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarItemDto(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val color: String?,
    val icon: String?,
    val location: String?,
    val start_time: String?,
    val end_time: String?,
    val is_all_day: Boolean,
    val timezone: String?,
    val recurrence_rule: String?,
    val priority: String,
    val status: String,
    val is_private: Boolean,
    val owner_id: String,
    val category_id: String?,
    val created_at: String,
    val updated_at: String,
    val meeting_type: String?,
    val meeting_link: String?,
    val progress: Int,
    val pomodoro_count: Int,
    val deadline: String?,
    val parent_item_id: String?,
    val ciphertext: String? = null,
    val iv: String? = null,
    val crypto_version: Int = 1
)

@Serializable
data class CalendarAttendeeDto(
    val id: String,
    val item_id: String,
    val user_id: String,
    val role: String,
    val status: String
)

@Serializable
data class CalendarReminderDto(
    val id: String,
    val item_id: String?,
    val habit_id: String?,
    val type: String,
    val minutes_before: Int,
    val is_persistent: Boolean
)

@Serializable
data class HabitDto(
    val id: String,
    val title: String,
    val description: String?,
    val color: String?,
    val icon: String?,
    val recurrence_rule: String,
    val owner_id: String,
    val created_at: String,
    val updated_at: String,
    val is_active: Boolean
)

@Serializable
data class HabitLogDto(
    val id: String,
    val habit_id: String,
    val date: String,
    val status: String,
    val note: String?
)

@Serializable
data class CalendarCategoryDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String?,
    val owner_id: String
)

@Serializable
data class CalendarInvitationDto(
    val id: String,
    val from_user_id: String,
    val to_user_id: String,
    val status: String,
    val created_at: String,
    val kind: String = "Family"
)

@Serializable
data class FamilyMemberDto(
    val id: String,
    val user_id: String,
    val member_id: String,
    val included: Boolean
)

@Serializable
data class FamilyPermissionDto(
    val id: String,
    val family_member_id: String,
    val type: String,
    val level: String
)

@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    @SerialName("full_name")
    val full_name: String? = null,
    @SerialName("avatar_url")
    val avatar_url: String? = null
) {
    fun toDomain() = com.keeftalk.chat.domain.model.User(
        id = id,
        name = full_name ?: username,
        username = username,
        avatarUrl = avatar_url,
        isActive = false,
        lastSeen = 0L,
        isContact = false
    )
}
