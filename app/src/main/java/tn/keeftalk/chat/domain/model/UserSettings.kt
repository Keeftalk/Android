package tn.keeftalk.chat.domain.model

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
    val browserSettings: UserBrowserSettings = UserBrowserSettings(),
    val habitSettings: UserHabitSettings = UserHabitSettings(),
    val feedSettings: UserFeedSettings = UserFeedSettings(),
    val bookSettings: BookSettings = BookSettings(),
    val marketSettings: UserMarketSettings = UserMarketSettings(),
    val smsSettings: UserSmsSettings = UserSmsSettings(),
    val todoSettings: UserTodoSettings = UserTodoSettings(),
    val mapSettings: UserMapSettings = UserMapSettings(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserNotificationSettings(
    // Global & Delivery Layer Controls
    val allEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val systemEnabled: Boolean = true,
    val fcmEnabled: Boolean = true,
    val sound: String = "DEFAULT",
    val vibration: String = "NORMAL",
    val showPreview: Boolean = true,
    val hideSensitiveContent: Boolean = true,

    // Communication Sub-controls
    val messagesEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val callsEnabled: Boolean = true,
    val missedCallsEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val repliesEnabled: Boolean = true,

    // App & System Activity
    val profileViewsEnabled: Boolean = true,
    val activityLogsEnabled: Boolean = true,
    val systemNudgesEnabled: Boolean = true,

    // All 15 Feature Master Toggles
    val chatsEnabled: Boolean = true,
    val smsMmsEnabled: Boolean = true,
    val notesEnabled: Boolean = true,
    val vaultEnabled: Boolean = true,
    val secretsEnabled: Boolean = true,
    val calendarEnabled: Boolean = true,
    val walletEnabled: Boolean = true,
    val financesEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val feedEnabled: Boolean = true,
    val booksEnabled: Boolean = true,
    val marketEnabled: Boolean = true,
    val habitsEnabled: Boolean = true,
    val browserEnabled: Boolean = true
)

