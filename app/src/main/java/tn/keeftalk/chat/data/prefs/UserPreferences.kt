package tn.keeftalk.chat.data.prefs

import tn.keeftalk.chat.domain.model.AppCustomization
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    val allEnabled: Boolean = true,
    val inAppNotificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val fcmNotificationsEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val callsEnabled: Boolean = true,
    val missedCallsEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val repliesEnabled: Boolean = true,
    val contactJoinedEnabled: Boolean = true,
    val securityAlertsEnabled: Boolean = true,
    val appUpdatesEnabled: Boolean = true,
    val profileViewsEnabled: Boolean = true,
    val activityLogsEnabled: Boolean = true,
    val systemNudgesEnabled: Boolean = true,
    val sound: String = "DEFAULT",
    val customSoundUri: String? = null,
    val vibration: String = "NORMAL",
    val ledEnabled: Boolean = true,
    val ledColor: Int = 0xFF6C63FF.toInt(),
    val showPreviewContent: Boolean = true,
    val hideSensitiveContent: Boolean = true,
    val lockScreenVisibility: String = "SHOW_ALL",
    val badgeEnabled: Boolean = true,
    val popupEnabled: Boolean = true,
    val headsUpEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",

    // 15 Feature Toggles
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
    val connectionsVisibility: String = "EVERYONE",
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
    val appCustomization: AppCustomization = AppCustomization(),

    // Weather Settings
    val weatherLocationMode: String = "AUTO", // "AUTO" or "MANUAL"
    val weatherManualLocation: String? = null,
    val prayerLocationMode: String = "AUTO", // "AUTO" or "MANUAL"
    val prayerManualLocation: String? = null,

    // Keef Theme & Blur Settings
    val keefWeatherMode: String = "REAL", // "REAL" or "SIMULATED"
    val keefUpdateIntervalMinutes: Int = 15, // 5 to 1440
    val keefSimCloudSmoothness: Float = 0.8f, // 0.0 to 1.0
    val keefSimWindSpeed: Float = 12f, // 0.0 to 300.0 km/h
    val keefSimCloudDensity: Int = 75, // 0 to 400%
    val keefSimRainDensity: Int = 25, // 0 to 100%
    val keefSimThunderFrequency: Int = 0, // 0 to 100%
    val keefSimTimeOfDay: Float = 12f, // 0.0 to 24.0 hours
    val keefSimDayOfMonth: Int = 15, // 1 to 31 (for moon phase simulation)
    val keefSimStarsDensity: Int = 60, // 0 to 100%
    val keefCardBlurPercent: Int = 20, // 0 to 100%
    val keefButtonBlurPercent: Int = 20, // 0 to 100%
    val keefSurfaceBlurPercent: Int = 20, // 0 to 100%
    val keefCardOpacityPercent: Int = 60, // 0 to 100%
    val keefButtonOpacityPercent: Int = 50, // 0 to 100%
    val keefSurfaceOpacityPercent: Int = 70, // 0 to 100%
    val keefBorderOpacityPercent: Int = 50, // 0 to 100%
    val keefCornerRadiusDp: Int = 22, // 8 to 36 dp
    val keefGroundType: String = "AUTO", // "AUTO", "FOREST", "CITY", "SAHARA", "GREEN"
    val keefLightPollutionPercent: Int = 30, // 0 to 100%
    val keefDialogBlurPercent: Int = 20, // 0 to 100%
    val keefDialogOpacityPercent: Int = 80, // 0 to 100%
    val keefFabBlurPercent: Int = 20, // 0 to 100%
    val keefFabOpacityPercent: Int = 60, // 0 to 100%
    val keefContainerBlurPercent: Int = 20, // 0 to 100%
    val keefContainerOpacityPercent: Int = 60, // 0 to 100%

    // Feed & Prayer Settings
    val feedCardOrder: List<String> = listOf("weather", "verse", "prayer", "markets"),
    val feedHiddenCards: List<String> = emptyList(),
    val feedLayoutMode: String = "STACKED", // "STACKED" or "SWIPE"
    val prayerCalculationMethod: String = "MWL",
    val prayerAsrMadhhab: String = "SHAFI",
    val prayerRemindersJson: String = "{\"Fajr\":true,\"Dhuhr\":true,\"Asr\":true,\"Maghrib\":true,\"Isha\":true}",
    val savedArticleLinksJson: String = "[]",
    val likedArticleLinksJson: String = "[]",
    val likedSourcesMapJson: String = "{}",

    // Module Integrations
    val emailIntegrationEnabled: Boolean = true,
    val smsBridgeEnabled: Boolean = false,
    val voipEnabled: Boolean = true,
    val walletEnabled: Boolean = false,
    val calendarSyncEnabled: Boolean = true,
    val notesSyncEnabled: Boolean = true,
    val cloudBackupEnabled: Boolean = true,

    // Data Saver System
    val isDataSaverEnabled: Boolean = false,

    // Browser Settings
    val browserSettings: tn.keeftalk.chat.domain.model.UserBrowserSettings = tn.keeftalk.chat.domain.model.UserBrowserSettings()
)
