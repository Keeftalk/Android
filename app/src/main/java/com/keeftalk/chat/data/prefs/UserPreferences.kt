package com.keeftalk.chat.data.prefs

import com.keeftalk.chat.domain.model.AppCustomization
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    val allEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val callsEnabled: Boolean = true,
    val missedCallsEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val repliesEnabled: Boolean = true,
    val contactJoinedEnabled: Boolean = true,
    val securityAlertsEnabled: Boolean = true,
    val appUpdatesEnabled: Boolean = true,
    val sound: String = "DEFAULT",
    val customSoundUri: String? = null,
    val vibration: String = "NORMAL",
    val ledEnabled: Boolean = true,
    val ledColor: Int = 0xFF6C63FF.toInt(),
    val showPreviewContent: Boolean = true,
    val lockScreenVisibility: String = "SHOW_ALL",
    val badgeEnabled: Boolean = true,
    val popupEnabled: Boolean = true,
    val headsUpEnabled: Boolean = true,
    val inAppNotificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)

@Serializable
data class UserPreferences(
    val userId: String = "",
    val userName: String = "",
    val themeMode: String = "SYSTEM",
    val isDynamicColorEnabled: Boolean = true,
    val isLogged: Boolean = false,
    val sessionData: String? = null,
    
    // Chat Settings
    val chatWallpaper: String? = null,
    val fontSize: Float = 16f,
    val fontScale: Float = 1.0f,
    val uiScale: Float = 1.0f,
    val isBold: Boolean = false,
    val extraNightModeEnabled: Boolean = false,
    val nightModeOpacity: Float = 0.4f,
    val chatBubblesStyle: String = "DEFAULT",
    val messageSpacing: Int = 8,
    val autoDownloadMedia: Boolean = true,
    val mediaQuality: String = "AUTO",
    val archiveBehavior: String = "HIDE",
    val swipeActions: String = "REPLY",

    // Notification Settings
    val notifications: NotificationPreferences = NotificationPreferences(),

    // Privacy Settings
    val profilePhotoVisibility: String = "EVERYONE",
    val aboutVisibility: String = "EVERYONE",
    val usernameVisibility: String = "EVERYONE",
    val phoneVisibility: String = "EVERYONE",
    val lastSeenVisibility: String = "EVERYONE",
    val onlineStatusVisibility: String = "EVERYONE",
    val readReceiptsEnabled: Boolean = true,
    val typingIndicatorsEnabled: Boolean = true,
    val profileViewHistoryEnabled: Boolean = false,
    val whoCanCallMe: String = "EVERYONE",
    val whoCanAddToGroups: String = "EVERYONE",
    val whoCanMentionMe: String = "EVERYONE",
    val screenshotProtectionEnabled: Boolean = false,
    val hideSensitivePreviews: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val biometricTimeout: Int = 0,
    val hideFromRecentApps: Boolean = false,

    // Country & Phone Settings
    val detectedCountryIso: String? = null,
    val accountCountryIso: String? = null,

    // Notes Settings
    val customNoteContainers: List<String> = emptyList(),
    val lastNotesSyncTimestamp: Long = 0L,
    val lastChatSyncTimestamp: Long = 0L,

    // App Customization
    val appCustomization: AppCustomization = AppCustomization()
)
