package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val userId: String,
    val chatSettings: UserChatSettings = UserChatSettings(userId = ""),
    val callSettings: UserCallSettings = UserCallSettings(),
    val noteSettings: UserNoteSettings = UserNoteSettings(),
    val vaultSettings: UserVaultSettings = UserVaultSettings(),
    val calendarSettings: UserCalendarSettings = UserCalendarSettings(),
    val emailSettings: UserEmailSettings = UserEmailSettings(),
    val notificationSettings: UserNotificationSettings = UserNotificationSettings(),
    val privacySettings: UserPrivacySettings = UserPrivacySettings(userId = ""),
    val parentalControls: UserParentalControls = UserParentalControls(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserNotificationSettings(
    val allEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val callsEnabled: Boolean = true,
    val missedCallsEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val repliesEnabled: Boolean = true,
    val sound: String = "DEFAULT",
    val vibration: String = "NORMAL",
    val showPreview: Boolean = true
)
