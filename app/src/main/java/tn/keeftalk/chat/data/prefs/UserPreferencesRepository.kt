package tn.keeftalk.chat.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import tn.keeftalk.chat.domain.model.AppCustomization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val store: tn.keeftalk.chat.util.KeeftalkStore by lazy {
        tn.keeftalk.chat.util.KeeftalkStore.getInstance()
    }

    fun isLoggedFast(): Boolean {
        return !getUserIdFast().isNullOrEmpty()
    }
    fun getUserIdFast(): String? = store.getString("user_id", null)
    fun getThemeFast(): String = store.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    fun getFontSizeFast(): Float = store.getFloat("font_size", 16f)
    fun getFontScaleFast(): Float = store.getFloat("font_scale", 1.0f)
    fun getUiScaleFast(): Float = store.getFloat("ui_scale", 1.0f)
    fun isBoldFast(): Boolean = store.getBoolean("is_bold", false)
    fun isExtraNightModeEnabledFast(): Boolean = store.getBoolean("extra_night_mode_enabled", false)
    fun getNightModeOpacityFast(): Float = store.getFloat("night_mode_opacity", 0.4f)
    fun isDynamicColorFast(): Boolean = store.getBoolean("dynamic_color", true)
    fun isAppLockEnabledFast(): Boolean = store.getBoolean("biometric_lock_enabled", false)
    fun getAppLockTimeoutFast(): Int = store.getInt("biometric_timeout", 0)
    fun isDataSaverEnabledFast(): Boolean = store.getBoolean("data_saver_enabled", false)

    fun updateDataSaverEnabledFast(enabled: Boolean) {
        store.putBoolean("data_saver_enabled", enabled)
    }

    fun getInstantChatCache(): String? = store.getString("instant_chat_cache", null)
    fun updateInstantChatCache(json: String) {
        store.putString("instant_chat_cache", json)
    }

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val IS_LOGGED = booleanPreferencesKey("is_logged")
        val SESSION_DATA = stringPreferencesKey("session_data")
        
        // Chat Settings Keys
        val CHAT_WALLPAPER = stringPreferencesKey("chat_wallpaper")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val EXTRA_NIGHT_MODE_ENABLED = booleanPreferencesKey("extra_night_mode_enabled")
        val NIGHT_MODE_OPACITY = floatPreferencesKey("night_mode_opacity")
        val CHAT_BUBBLES_STYLE = stringPreferencesKey("chat_bubbles_style")
        val MESSAGE_SPACING = intPreferencesKey("message_spacing")
        val AUTO_DOWNLOAD_MEDIA = booleanPreferencesKey("auto_download_media")
        val MEDIA_QUALITY = stringPreferencesKey("media_quality")
        val ARCHIVE_BEHAVIOR = stringPreferencesKey("archive_behavior")
        val SWIPE_ACTIONS = stringPreferencesKey("swipe_actions")

        // Country & Phone Settings
        val DETECTED_COUNTRY_ISO = stringPreferencesKey("detected_country_iso")
        val ACCOUNT_COUNTRY_ISO = stringPreferencesKey("account_country_iso")
        val NOTIFICATIONS_JSON = stringPreferencesKey("notifications_json")

        // Privacy Settings Keys
        val PROFILE_PHOTO_VISIBILITY = stringPreferencesKey("profile_photo_visibility")
        val ABOUT_VISIBILITY = stringPreferencesKey("about_visibility")
        val LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
        val READ_RECEIPTS_ENABLED = booleanPreferencesKey("read_receipts_enabled")
        val TYPING_INDICATORS_ENABLED = booleanPreferencesKey("typing_indicators_enabled")
        val PROFILE_VIEW_HISTORY_ENABLED = booleanPreferencesKey("profile_view_history_enabled")
        val CALL_PERMISSION = stringPreferencesKey("call_permission")
        val GROUP_PERMISSION = stringPreferencesKey("group_permission")
        val SCREENSHOT_PROTECTION_ENABLED = booleanPreferencesKey("screenshot_protection_enabled")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val BIOMETRIC_TIMEOUT = intPreferencesKey("biometric_timeout")
        val CUSTOM_NOTE_CONTAINERS = stringPreferencesKey("custom_note_containers")
        val LAST_NOTES_SYNC_TIMESTAMP = longPreferencesKey("last_notes_sync_timestamp")
        val LAST_CHAT_SYNC_TIMESTAMP = longPreferencesKey("last_chat_sync_timestamp")
        val APP_CUSTOMIZATION_JSON = stringPreferencesKey("app_customization_json")
        val WEATHER_LOCATION_MODE = stringPreferencesKey("weather_location_mode")
        val WEATHER_MANUAL_LOCATION = stringPreferencesKey("weather_manual_location")
        val PRAYER_LOCATION_MODE = stringPreferencesKey("prayer_location_mode")
        val PRAYER_MANUAL_LOCATION = stringPreferencesKey("prayer_manual_location")

        // Keef Theme & Blur Keys
        val KEEF_WEATHER_MODE = stringPreferencesKey("keef_weather_mode")
        val KEEF_UPDATE_INTERVAL_MINUTES = intPreferencesKey("keef_update_interval_minutes")
        val KEEF_SIM_CLOUD_SMOOTHNESS = floatPreferencesKey("keef_sim_cloud_smoothness")
        val KEEF_SIM_WIND_SPEED = floatPreferencesKey("keef_sim_wind_speed")
        val KEEF_SIM_CLOUD_DENSITY = intPreferencesKey("keef_sim_cloud_density")
        val KEEF_SIM_RAIN_DENSITY = intPreferencesKey("keef_sim_rain_density")
        val KEEF_SIM_THUNDER_FREQUENCY = intPreferencesKey("keef_sim_thunder_frequency")
        val KEEF_SIM_TIME_OF_DAY = floatPreferencesKey("keef_sim_time_of_day")
        val KEEF_SIM_DAY_OF_MONTH = intPreferencesKey("keef_sim_day_of_month")
        val KEEF_SIM_STARS_DENSITY = intPreferencesKey("keef_sim_stars_density")
        val KEEF_CARD_BLUR_PERCENT = intPreferencesKey("keef_card_blur_percent")
        val KEEF_BUTTON_BLUR_PERCENT = intPreferencesKey("keef_button_blur_percent")
        val KEEF_SURFACE_BLUR_PERCENT = intPreferencesKey("keef_surface_blur_percent")
        val KEEF_CARD_OPACITY_PERCENT = intPreferencesKey("keef_card_opacity_percent")
        val KEEF_BUTTON_OPACITY_PERCENT = intPreferencesKey("keef_button_opacity_percent")
        val KEEF_SURFACE_OPACITY_PERCENT = intPreferencesKey("keef_surface_opacity_percent")
        val KEEF_BORDER_OPACITY_PERCENT = intPreferencesKey("keef_border_opacity_percent")
        val KEEF_CORNER_RADIUS_DP = intPreferencesKey("keef_corner_radius_dp")
        val KEEF_GROUND_TYPE = stringPreferencesKey("keef_ground_type")
        val KEEF_LIGHT_POLLUTION_PERCENT = intPreferencesKey("keef_light_pollution_percent")
        val KEEF_DIALOG_BLUR_PERCENT = intPreferencesKey("keef_dialog_blur_percent")
        val KEEF_DIALOG_OPACITY_PERCENT = intPreferencesKey("keef_dialog_opacity_percent")
        val KEEF_FAB_BLUR_PERCENT = intPreferencesKey("keef_fab_blur_percent")
        val KEEF_FAB_OPACITY_PERCENT = intPreferencesKey("keef_fab_opacity_percent")
        val KEEF_CONTAINER_BLUR_PERCENT = intPreferencesKey("keef_container_blur_percent")
        val KEEF_CONTAINER_OPACITY_PERCENT = intPreferencesKey("keef_container_opacity_percent")

        // Feed & Prayer Keys
        val FEED_CARD_ORDER = stringPreferencesKey("feed_card_order")
        val FEED_HIDDEN_CARDS = stringPreferencesKey("feed_hidden_cards")
        val FEED_LAYOUT_MODE = stringPreferencesKey("feed_layout_mode")
        val PRAYER_CALCULATION_METHOD = stringPreferencesKey("prayer_calculation_method")
        val PRAYER_ASR_MADHHAB = stringPreferencesKey("prayer_asr_madhhab")
        val PRAYER_REMINDERS_JSON = stringPreferencesKey("prayer_reminders_json")
        val SAVED_ARTICLES_JSON = stringPreferencesKey("saved_articles_json")
        val LIKED_ARTICLES_JSON = stringPreferencesKey("liked_articles_json")
        val LIKED_SOURCES_JSON = stringPreferencesKey("liked_sources_json")

        // Module Integrations
        val EMAIL_INTEGRATION_ENABLED = booleanPreferencesKey("email_integration_enabled")
        val SMS_BRIDGE_ENABLED = booleanPreferencesKey("sms_bridge_enabled")
        val VOIP_ENABLED = booleanPreferencesKey("voip_enabled")
        val WALLET_ENABLED = booleanPreferencesKey("wallet_enabled")
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val NOTES_SYNC_ENABLED = booleanPreferencesKey("notes_sync_enabled")
        val CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")

        // New Feature Settings Keys
        val CHAT_SETTINGS_JSON = stringPreferencesKey("chat_settings_json")
        val CALL_SETTINGS_JSON = stringPreferencesKey("call_settings_json")
        val NOTE_SETTINGS_JSON = stringPreferencesKey("note_settings_json")
        val VAULT_SETTINGS_JSON = stringPreferencesKey("vault_settings_json")
        val CALENDAR_SETTINGS_JSON = stringPreferencesKey("calendar_settings_json")
        val EMAIL_SETTINGS_JSON = stringPreferencesKey("email_settings_json")
        val PARENTAL_CONTROLS_JSON = stringPreferencesKey("parental_controls_json")
        val BROWSER_SETTINGS_JSON = stringPreferencesKey("browser_settings_json")
        val HABIT_SETTINGS_JSON = stringPreferencesKey("habit_settings_json")
        // Data Saver Key
        val DATA_SAVER_ENABLED = booleanPreferencesKey("data_saver_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                userId = preferences[PreferencesKeys.USER_ID] ?: "",
                userName = preferences[PreferencesKeys.USER_NAME] ?: "",
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "KEEF",
                isDynamicColorEnabled = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true,
                isLogged = preferences[PreferencesKeys.IS_LOGGED] ?: false,
                sessionData = preferences[PreferencesKeys.SESSION_DATA],
                chatWallpaper = preferences[PreferencesKeys.CHAT_WALLPAPER],
                fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: 16f,
                uiScale = preferences[PreferencesKeys.UI_SCALE] ?: 1.0f,
                extraNightModeEnabled = preferences[PreferencesKeys.EXTRA_NIGHT_MODE_ENABLED] ?: false,
                nightModeOpacity = preferences[PreferencesKeys.NIGHT_MODE_OPACITY] ?: 0.4f,
                chatBubblesStyle = preferences[PreferencesKeys.CHAT_BUBBLES_STYLE] ?: "DEFAULT",
                messageSpacing = preferences[PreferencesKeys.MESSAGE_SPACING] ?: 8,
                autoDownloadMedia = preferences[PreferencesKeys.AUTO_DOWNLOAD_MEDIA] ?: true,
                mediaQuality = preferences[PreferencesKeys.MEDIA_QUALITY] ?: "AUTO",
                archiveBehavior = preferences[PreferencesKeys.ARCHIVE_BEHAVIOR] ?: "HIDE",
                swipeActions = preferences[PreferencesKeys.SWIPE_ACTIONS] ?: "REPLY",
                detectedCountryIso = preferences[PreferencesKeys.DETECTED_COUNTRY_ISO],
                accountCountryIso = preferences[PreferencesKeys.ACCOUNT_COUNTRY_ISO],
                notifications = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                    try { Json.decodeFromString<NotificationPreferences>(it) } catch (_: Exception) { NotificationPreferences() }
                } ?: NotificationPreferences(),
                profilePhotoVisibility = preferences[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] ?: "EVERYONE",
                aboutVisibility = preferences[PreferencesKeys.ABOUT_VISIBILITY] ?: "EVERYONE",
                lastSeenVisibility = preferences[PreferencesKeys.LAST_SEEN_VISIBILITY] ?: "EVERYONE",
                readReceiptsEnabled = preferences[PreferencesKeys.READ_RECEIPTS_ENABLED] ?: true,
                typingIndicatorsEnabled = preferences[PreferencesKeys.TYPING_INDICATORS_ENABLED] ?: true,
                profileViewHistoryEnabled = preferences[PreferencesKeys.PROFILE_VIEW_HISTORY_ENABLED] ?: false,
                whoCanCallMe = preferences[PreferencesKeys.CALL_PERMISSION] ?: "EVERYONE",
                whoCanAddToGroups = preferences[PreferencesKeys.GROUP_PERMISSION] ?: "EVERYONE",
                screenshotProtectionEnabled = preferences[PreferencesKeys.SCREENSHOT_PROTECTION_ENABLED] ?: false,
                biometricLockEnabled = preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] ?: false,
                biometricTimeout = preferences[PreferencesKeys.BIOMETRIC_TIMEOUT] ?: 0,
                customNoteContainers = preferences[PreferencesKeys.CUSTOM_NOTE_CONTAINERS]?.let {
                    try { Json.decodeFromString<List<String>>(it) } catch (_: Exception) { emptyList() }
                } ?: emptyList(),
                lastNotesSyncTimestamp = preferences[PreferencesKeys.LAST_NOTES_SYNC_TIMESTAMP] ?: 0L,
                lastChatSyncTimestamp = preferences[PreferencesKeys.LAST_CHAT_SYNC_TIMESTAMP] ?: 0L,
                appCustomization = preferences[PreferencesKeys.APP_CUSTOMIZATION_JSON]?.let {
                    try { Json.decodeFromString<AppCustomization>(it) } catch (_: Exception) { AppCustomization() }
                } ?: AppCustomization(),
                weatherLocationMode = preferences[PreferencesKeys.WEATHER_LOCATION_MODE] ?: "AUTO",
                weatherManualLocation = preferences[PreferencesKeys.WEATHER_MANUAL_LOCATION],
                prayerLocationMode = preferences[PreferencesKeys.PRAYER_LOCATION_MODE] ?: "AUTO",
                prayerManualLocation = preferences[PreferencesKeys.PRAYER_MANUAL_LOCATION],
                keefWeatherMode = preferences[PreferencesKeys.KEEF_WEATHER_MODE] ?: "REAL",
                keefUpdateIntervalMinutes = preferences[PreferencesKeys.KEEF_UPDATE_INTERVAL_MINUTES] ?: 15,
                keefSimCloudSmoothness = preferences[PreferencesKeys.KEEF_SIM_CLOUD_SMOOTHNESS] ?: 0.8f,
                keefSimWindSpeed = preferences[PreferencesKeys.KEEF_SIM_WIND_SPEED] ?: 12f,
                keefSimCloudDensity = preferences[PreferencesKeys.KEEF_SIM_CLOUD_DENSITY] ?: 75,
                keefSimRainDensity = preferences[PreferencesKeys.KEEF_SIM_RAIN_DENSITY] ?: 25,
                keefSimThunderFrequency = preferences[PreferencesKeys.KEEF_SIM_THUNDER_FREQUENCY] ?: 0,
                keefSimTimeOfDay = preferences[PreferencesKeys.KEEF_SIM_TIME_OF_DAY] ?: 12f,
                keefSimDayOfMonth = preferences[PreferencesKeys.KEEF_SIM_DAY_OF_MONTH] ?: 15,
                keefSimStarsDensity = preferences[PreferencesKeys.KEEF_SIM_STARS_DENSITY] ?: 60,
                keefCardBlurPercent = preferences[PreferencesKeys.KEEF_CARD_BLUR_PERCENT] ?: 20,
                keefButtonBlurPercent = preferences[PreferencesKeys.KEEF_BUTTON_BLUR_PERCENT] ?: 20,
                keefSurfaceBlurPercent = preferences[PreferencesKeys.KEEF_SURFACE_BLUR_PERCENT] ?: 20,
                keefCardOpacityPercent = preferences[PreferencesKeys.KEEF_CARD_OPACITY_PERCENT] ?: 60,
                keefButtonOpacityPercent = preferences[PreferencesKeys.KEEF_BUTTON_OPACITY_PERCENT] ?: 50,
                keefSurfaceOpacityPercent = preferences[PreferencesKeys.KEEF_SURFACE_OPACITY_PERCENT] ?: 70,
                keefBorderOpacityPercent = preferences[PreferencesKeys.KEEF_BORDER_OPACITY_PERCENT] ?: 50,
                keefCornerRadiusDp = preferences[PreferencesKeys.KEEF_CORNER_RADIUS_DP] ?: 22,
                keefGroundType = preferences[PreferencesKeys.KEEF_GROUND_TYPE] ?: "AUTO",
                keefLightPollutionPercent = preferences[PreferencesKeys.KEEF_LIGHT_POLLUTION_PERCENT] ?: 30,
                keefDialogBlurPercent = preferences[PreferencesKeys.KEEF_DIALOG_BLUR_PERCENT] ?: 20,
                keefDialogOpacityPercent = preferences[PreferencesKeys.KEEF_DIALOG_OPACITY_PERCENT] ?: 80,
                keefFabBlurPercent = preferences[PreferencesKeys.KEEF_FAB_BLUR_PERCENT] ?: 20,
                keefFabOpacityPercent = preferences[PreferencesKeys.KEEF_FAB_OPACITY_PERCENT] ?: 60,
                keefContainerBlurPercent = preferences[PreferencesKeys.KEEF_CONTAINER_BLUR_PERCENT] ?: 20,
                keefContainerOpacityPercent = preferences[PreferencesKeys.KEEF_CONTAINER_OPACITY_PERCENT] ?: 60,
                feedCardOrder = preferences[PreferencesKeys.FEED_CARD_ORDER]?.let {
                    try { Json.decodeFromString<List<String>>(it) } catch (_: Exception) { listOf("weather", "verse", "prayer", "markets") }
                } ?: listOf("weather", "verse", "prayer", "markets"),
                feedHiddenCards = preferences[PreferencesKeys.FEED_HIDDEN_CARDS]?.let {
                    try { Json.decodeFromString<List<String>>(it) } catch (_: Exception) { emptyList() }
                } ?: emptyList(),
                feedLayoutMode = preferences[PreferencesKeys.FEED_LAYOUT_MODE] ?: "STACKED",
                prayerCalculationMethod = preferences[PreferencesKeys.PRAYER_CALCULATION_METHOD] ?: "MWL",
                prayerAsrMadhhab = preferences[PreferencesKeys.PRAYER_ASR_MADHHAB] ?: "SHAFI",
                prayerRemindersJson = preferences[PreferencesKeys.PRAYER_REMINDERS_JSON] ?: "{\"Fajr\":true,\"Dhuhr\":true,\"Asr\":true,\"Maghrib\":true,\"Isha\":true}",
                savedArticleLinksJson = preferences[PreferencesKeys.SAVED_ARTICLES_JSON] ?: "[]",
                likedArticleLinksJson = preferences[PreferencesKeys.LIKED_ARTICLES_JSON] ?: "[]",
                likedSourcesMapJson = preferences[PreferencesKeys.LIKED_SOURCES_JSON] ?: "{}",
                emailIntegrationEnabled = preferences[PreferencesKeys.EMAIL_INTEGRATION_ENABLED] ?: true,
                smsBridgeEnabled = preferences[PreferencesKeys.SMS_BRIDGE_ENABLED] ?: false,
                voipEnabled = preferences[PreferencesKeys.VOIP_ENABLED] ?: true,
                walletEnabled = preferences[PreferencesKeys.WALLET_ENABLED] ?: false,
                calendarSyncEnabled = preferences[PreferencesKeys.CALENDAR_SYNC_ENABLED] ?: true,
                notesSyncEnabled = preferences[PreferencesKeys.NOTES_SYNC_ENABLED] ?: true,
                cloudBackupEnabled = preferences[PreferencesKeys.CLOUD_BACKUP_ENABLED] ?: true,
                isDataSaverEnabled = preferences[PreferencesKeys.DATA_SAVER_ENABLED] ?: false,
                browserSettings = preferences[PreferencesKeys.BROWSER_SETTINGS_JSON]?.let {
                    try { Json.decodeFromString<tn.keeftalk.chat.domain.model.UserBrowserSettings>(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserBrowserSettings() }
                } ?: tn.keeftalk.chat.domain.model.UserBrowserSettings()
            )
        }

    suspend fun updateUserId(userId: String) {
        store.putString("user_id", userId)
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USER_ID] = userId }
    }

    suspend fun updateUserName(userName: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USER_NAME] = userName }
    }

    suspend fun updateThemeMode(themeMode: String) {
        store.putString("theme_mode", themeMode)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
            val userId = preferences[PreferencesKeys.USER_ID] ?: ""
            val currentSettings = preferences[PreferencesKeys.CHAT_SETTINGS_JSON]?.let {
                try { Json.decodeFromString<tn.keeftalk.chat.domain.model.UserChatSettings>(it) } catch (_: Exception) { null }
            } ?: tn.keeftalk.chat.domain.model.UserChatSettings(userId = userId)
            preferences[PreferencesKeys.CHAT_SETTINGS_JSON] = Json.encodeToString(currentSettings.copy(theme = themeMode))
        }
    }

    fun getSessionDataFast(): String? = store.getString("session_data", null)

    suspend fun updateDynamicColor(enabled: Boolean) {
        store.putBoolean("dynamic_color", enabled)
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateIsLogged(isLogged: Boolean) {
        store.putBoolean("is_logged", isLogged)
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_LOGGED] = isLogged }
    }

    suspend fun updateSessionData(sessionData: String?) {
        val isLogged = sessionData != null
        store.putBoolean("is_logged", isLogged)
        store.putString("session_data", sessionData)
        
        context.dataStore.edit { preferences ->
            if (sessionData == null) {
                preferences.remove(PreferencesKeys.SESSION_DATA)
                preferences[PreferencesKeys.IS_LOGGED] = false
            } else {
                preferences[PreferencesKeys.SESSION_DATA] = sessionData
                preferences[PreferencesKeys.IS_LOGGED] = true
                
                // Try to extract userId from session JSON to update fast cache
                try {
                    val json = Json.parseToJsonElement(sessionData) as? JsonObject
                    val user = json?.get("user") as? JsonObject
                    val id = (user?.get("id") as? JsonPrimitive)?.content
                    if (id != null) {
                        store.putString("user_id", id)
                        preferences[PreferencesKeys.USER_ID] = id
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun updateChatSettings(
        wallpaper: String? = null,
        fontSize: Float? = null,
        uiScale: Float? = null,
        extraNightMode: Boolean? = null,
        nightModeOpacity: Float? = null,
        bubblesStyle: String? = null,
        spacing: Int? = null,
        autoDownload: Boolean? = null,
        quality: String? = null,
        archive: String? = null,
        swipe: String? = null
    ) {
        context.dataStore.edit { preferences ->
            wallpaper?.let { preferences[PreferencesKeys.CHAT_WALLPAPER] = it }
            fontSize?.let { 
                store.putFloat("font_size", it)
                preferences[PreferencesKeys.FONT_SIZE] = it 
            }
            uiScale?.let {
                store.putFloat("ui_scale", it)
                preferences[PreferencesKeys.UI_SCALE] = it
            }
            extraNightMode?.let {
                store.putBoolean("extra_night_mode_enabled", it)
                preferences[PreferencesKeys.EXTRA_NIGHT_MODE_ENABLED] = it
            }
            nightModeOpacity?.let {
                store.putFloat("night_mode_opacity", it)
                preferences[PreferencesKeys.NIGHT_MODE_OPACITY] = it
            }
            bubblesStyle?.let { preferences[PreferencesKeys.CHAT_BUBBLES_STYLE] = it }
            spacing?.let { preferences[PreferencesKeys.MESSAGE_SPACING] = it }
            autoDownload?.let { preferences[PreferencesKeys.AUTO_DOWNLOAD_MEDIA] = it }
            quality?.let { preferences[PreferencesKeys.MEDIA_QUALITY] = it }
            archive?.let { preferences[PreferencesKeys.ARCHIVE_BEHAVIOR] = it }
            swipe?.let { preferences[PreferencesKeys.SWIPE_ACTIONS] = it }
        }
    }

    suspend fun updateDetectedCountryIso(iso: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DETECTED_COUNTRY_ISO] = iso }
    }

    suspend fun updateAccountCountryIso(iso: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ACCOUNT_COUNTRY_ISO] = iso }
    }

    suspend fun updateNotificationPreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                try { Json.decodeFromString<NotificationPreferences>(it) } catch (e: Exception) { NotificationPreferences() }
            } ?: NotificationPreferences()
            val new = update(current)
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = Json.encodeToString(new)
        }
    }

    suspend fun updatePrivacySettings(settings: UserPreferences) {
        store.putBoolean("biometric_lock_enabled", settings.biometricLockEnabled)
        store.putInt("biometric_timeout", settings.biometricTimeout)

        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] = settings.profilePhotoVisibility
            preferences[PreferencesKeys.ABOUT_VISIBILITY] = settings.aboutVisibility
            preferences[PreferencesKeys.LAST_SEEN_VISIBILITY] = settings.lastSeenVisibility
            preferences[PreferencesKeys.READ_RECEIPTS_ENABLED] = settings.readReceiptsEnabled
            preferences[PreferencesKeys.TYPING_INDICATORS_ENABLED] = settings.typingIndicatorsEnabled
            preferences[PreferencesKeys.PROFILE_VIEW_HISTORY_ENABLED] = settings.profileViewHistoryEnabled
            preferences[PreferencesKeys.CALL_PERMISSION] = settings.whoCanCallMe
            preferences[PreferencesKeys.GROUP_PERMISSION] = settings.whoCanAddToGroups
            preferences[PreferencesKeys.SCREENSHOT_PROTECTION_ENABLED] = settings.screenshotProtectionEnabled
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] = settings.biometricLockEnabled
            preferences[PreferencesKeys.BIOMETRIC_TIMEOUT] = settings.biometricTimeout
        }
    }

    suspend fun updatePrivacySetting(key: String, value: Any) {
        when (key) {
            "biometricLockEnabled" -> store.putBoolean("biometric_lock_enabled", value as Boolean)
            "biometricTimeout" -> store.putInt("biometric_timeout", value as Int)
        }
        context.dataStore.edit { preferences ->
            when (key) {
                "profilePhotoVisibility" -> preferences[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] = value as String
                "aboutVisibility" -> preferences[PreferencesKeys.ABOUT_VISIBILITY] = value as String
                "lastSeenVisibility" -> preferences[PreferencesKeys.LAST_SEEN_VISIBILITY] = value as String
                "readReceiptsEnabled" -> preferences[PreferencesKeys.READ_RECEIPTS_ENABLED] = value as Boolean
                "typingIndicatorsEnabled" -> preferences[PreferencesKeys.TYPING_INDICATORS_ENABLED] = value as Boolean
                "profileViewHistoryEnabled" -> preferences[PreferencesKeys.PROFILE_VIEW_HISTORY_ENABLED] = value as Boolean
                "whoCanCallMe" -> preferences[PreferencesKeys.CALL_PERMISSION] = value as String
                "whoCanAddToGroups" -> preferences[PreferencesKeys.GROUP_PERMISSION] = value as String
                "screenshotProtectionEnabled" -> preferences[PreferencesKeys.SCREENSHOT_PROTECTION_ENABLED] = value as Boolean
                "biometricLockEnabled" -> preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] = value as Boolean
                "biometricTimeout" -> preferences[PreferencesKeys.BIOMETRIC_TIMEOUT] = value as Int
            }
        }
    }

    suspend fun updateCustomNoteContainers(containers: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_NOTE_CONTAINERS] = Json.encodeToString(containers)
        }
    }

    suspend fun updateLastNotesSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_NOTES_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateLastChatSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CHAT_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateAppCustomization(customization: AppCustomization) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_CUSTOMIZATION_JSON] = Json.encodeToString(customization)
        }
    }

    suspend fun updateWeatherSettings(mode: String? = null, location: String? = null) {
        context.dataStore.edit { preferences ->
            mode?.let { preferences[PreferencesKeys.WEATHER_LOCATION_MODE] = it }
            location?.let { preferences[PreferencesKeys.WEATHER_MANUAL_LOCATION] = it }
        }
    }

    suspend fun updateKeefWeatherSettings(
        mode: String? = null,
        updateIntervalMinutes: Int? = null,
        cloudSmoothness: Float? = null,
        windSpeed: Float? = null,
        cloudDensity: Int? = null,
        rainDensity: Int? = null,
        thunderFrequency: Int? = null,
        timeOfDay: Float? = null,
        dayOfMonth: Int? = null,
        starsDensity: Int? = null,
        cardBlur: Int? = null,
        buttonBlur: Int? = null,
        surfaceBlur: Int? = null,
        cardOpacity: Int? = null,
        buttonOpacity: Int? = null,
        surfaceOpacity: Int? = null,
        borderOpacity: Int? = null,
        cornerRadiusDp: Int? = null,
        groundType: String? = null,
        lightPollution: Int? = null,
        dialogBlur: Int? = null,
        dialogOpacity: Int? = null,
        fabBlur: Int? = null,
        fabOpacity: Int? = null,
        containerBlur: Int? = null,
        containerOpacity: Int? = null
    ) {
        context.dataStore.edit { preferences ->
            mode?.let { preferences[PreferencesKeys.KEEF_WEATHER_MODE] = it }
            updateIntervalMinutes?.let { preferences[PreferencesKeys.KEEF_UPDATE_INTERVAL_MINUTES] = it }
            cloudSmoothness?.let { preferences[PreferencesKeys.KEEF_SIM_CLOUD_SMOOTHNESS] = it }
            windSpeed?.let { preferences[PreferencesKeys.KEEF_SIM_WIND_SPEED] = it }
            cloudDensity?.let { preferences[PreferencesKeys.KEEF_SIM_CLOUD_DENSITY] = it }
            rainDensity?.let { preferences[PreferencesKeys.KEEF_SIM_RAIN_DENSITY] = it }
            thunderFrequency?.let { preferences[PreferencesKeys.KEEF_SIM_THUNDER_FREQUENCY] = it }
            timeOfDay?.let { preferences[PreferencesKeys.KEEF_SIM_TIME_OF_DAY] = it }
            dayOfMonth?.let { preferences[PreferencesKeys.KEEF_SIM_DAY_OF_MONTH] = it }
            starsDensity?.let { preferences[PreferencesKeys.KEEF_SIM_STARS_DENSITY] = it }
            cardBlur?.let { preferences[PreferencesKeys.KEEF_CARD_BLUR_PERCENT] = it }
            buttonBlur?.let { preferences[PreferencesKeys.KEEF_BUTTON_BLUR_PERCENT] = it }
            surfaceBlur?.let { preferences[PreferencesKeys.KEEF_SURFACE_BLUR_PERCENT] = it }
            cardOpacity?.let { preferences[PreferencesKeys.KEEF_CARD_OPACITY_PERCENT] = it }
            buttonOpacity?.let { preferences[PreferencesKeys.KEEF_BUTTON_OPACITY_PERCENT] = it }
            surfaceOpacity?.let { preferences[PreferencesKeys.KEEF_SURFACE_OPACITY_PERCENT] = it }
            borderOpacity?.let { preferences[PreferencesKeys.KEEF_BORDER_OPACITY_PERCENT] = it }
            cornerRadiusDp?.let { preferences[PreferencesKeys.KEEF_CORNER_RADIUS_DP] = it }
            groundType?.let { preferences[PreferencesKeys.KEEF_GROUND_TYPE] = it }
            lightPollution?.let { preferences[PreferencesKeys.KEEF_LIGHT_POLLUTION_PERCENT] = it }
            dialogBlur?.let { preferences[PreferencesKeys.KEEF_DIALOG_BLUR_PERCENT] = it }
            dialogOpacity?.let { preferences[PreferencesKeys.KEEF_DIALOG_OPACITY_PERCENT] = it }
            fabBlur?.let { preferences[PreferencesKeys.KEEF_FAB_BLUR_PERCENT] = it }
            fabOpacity?.let { preferences[PreferencesKeys.KEEF_FAB_OPACITY_PERCENT] = it }
            containerBlur?.let { preferences[PreferencesKeys.KEEF_CONTAINER_BLUR_PERCENT] = it }
            containerOpacity?.let { preferences[PreferencesKeys.KEEF_CONTAINER_OPACITY_PERCENT] = it }
        }
    }

    suspend fun updatePrayerLocationSettings(mode: String? = null, location: String? = null) {
        context.dataStore.edit { preferences ->
            mode?.let { preferences[PreferencesKeys.PRAYER_LOCATION_MODE] = it }
            location?.let { preferences[PreferencesKeys.PRAYER_MANUAL_LOCATION] = it }
        }
    }

    suspend fun updateFeedPreferences(
        order: List<String>? = null,
        hidden: List<String>? = null,
        layoutMode: String? = null,
        method: String? = null,
        madhhab: String? = null,
        remindersJson: String? = null,
        savedArticlesJson: String? = null,
        likedArticlesJson: String? = null,
        likedSourcesJson: String? = null
    ) {
        context.dataStore.edit { preferences ->
            order?.let { preferences[PreferencesKeys.FEED_CARD_ORDER] = Json.encodeToString(it) }
            hidden?.let { preferences[PreferencesKeys.FEED_HIDDEN_CARDS] = Json.encodeToString(it) }
            layoutMode?.let { preferences[PreferencesKeys.FEED_LAYOUT_MODE] = it }
            method?.let { preferences[PreferencesKeys.PRAYER_CALCULATION_METHOD] = it }
            madhhab?.let { preferences[PreferencesKeys.PRAYER_ASR_MADHHAB] = it }
            remindersJson?.let { preferences[PreferencesKeys.PRAYER_REMINDERS_JSON] = it }
            savedArticlesJson?.let { preferences[PreferencesKeys.SAVED_ARTICLES_JSON] = it }
            likedArticlesJson?.let { preferences[PreferencesKeys.LIKED_ARTICLES_JSON] = it }
            likedSourcesJson?.let { preferences[PreferencesKeys.LIKED_SOURCES_JSON] = it }
        }
    }

    suspend fun updateDataSaverEnabled(enabled: Boolean) {
        updateDataSaverEnabledFast(enabled)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_SAVER_ENABLED] = enabled
        }
    }

    suspend fun updateModuleIntegration(key: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            when (key) {
                "emailIntegrationEnabled" -> preferences[PreferencesKeys.EMAIL_INTEGRATION_ENABLED] = enabled
                "smsBridgeEnabled" -> preferences[PreferencesKeys.SMS_BRIDGE_ENABLED] = enabled
                "voipEnabled" -> preferences[PreferencesKeys.VOIP_ENABLED] = enabled
                "walletEnabled" -> preferences[PreferencesKeys.WALLET_ENABLED] = enabled
                "calendarSyncEnabled" -> preferences[PreferencesKeys.CALENDAR_SYNC_ENABLED] = enabled
                "notesSyncEnabled" -> preferences[PreferencesKeys.NOTES_SYNC_ENABLED] = enabled
                "cloudBackupEnabled" -> preferences[PreferencesKeys.CLOUD_BACKUP_ENABLED] = enabled
            }
        }
    }

    suspend fun updateFullSettings(settings: tn.keeftalk.chat.domain.model.UserSettings) {
        val localTheme = getThemeFast()
        context.dataStore.edit { preferences ->
            val updatedChatSettings = if (localTheme.isNotEmpty()) {
                settings.chatSettings.copy(theme = localTheme)
            } else {
                settings.chatSettings
            }
            preferences[PreferencesKeys.CHAT_SETTINGS_JSON] = Json.encodeToString(updatedChatSettings)
            preferences[PreferencesKeys.CALL_SETTINGS_JSON] = Json.encodeToString(settings.callSettings)
            preferences[PreferencesKeys.NOTE_SETTINGS_JSON] = Json.encodeToString(settings.noteSettings)
            preferences[PreferencesKeys.VAULT_SETTINGS_JSON] = Json.encodeToString(settings.vaultSettings)
            preferences[PreferencesKeys.CALENDAR_SETTINGS_JSON] = Json.encodeToString(settings.calendarSettings)
            preferences[PreferencesKeys.EMAIL_SETTINGS_JSON] = Json.encodeToString(settings.emailSettings)
            preferences[PreferencesKeys.PARENTAL_CONTROLS_JSON] = Json.encodeToString(settings.parentalControls)
            preferences[PreferencesKeys.BROWSER_SETTINGS_JSON] = Json.encodeToString(settings.browserSettings)
            preferences[PreferencesKeys.HABIT_SETTINGS_JSON] = Json.encodeToString(settings.habitSettings)
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = Json.encodeToString(settings.notificationSettings)
            
            // Sync individual fields for backward compatibility, preserving local theme if set
            if (localTheme.isNotEmpty()) {
                store.putString("theme_mode", localTheme)
                preferences[PreferencesKeys.THEME_MODE] = localTheme
            } else {
                store.putString("theme_mode", settings.chatSettings.theme)
                preferences[PreferencesKeys.THEME_MODE] = settings.chatSettings.theme
            }
            preferences[PreferencesKeys.FONT_SIZE] = settings.chatSettings.fontSize.toFloat()
            
            // Privacy Sync
            preferences[PreferencesKeys.READ_RECEIPTS_ENABLED] = settings.privacySettings.readReceiptsEnabled
            preferences[PreferencesKeys.TYPING_INDICATORS_ENABLED] = settings.privacySettings.typingIndicatorsEnabled
            preferences[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] = settings.privacySettings.profilePhotoVisibility.name
            preferences[PreferencesKeys.ABOUT_VISIBILITY] = settings.privacySettings.aboutVisibility.name
            preferences[PreferencesKeys.LAST_SEEN_VISIBILITY] = settings.privacySettings.lastSeenVisibility.name
            preferences[PreferencesKeys.CALL_PERMISSION] = settings.privacySettings.callPermission.name
            preferences[PreferencesKeys.GROUP_PERMISSION] = settings.privacySettings.groupPermission.name
            preferences[PreferencesKeys.SCREENSHOT_PROTECTION_ENABLED] = settings.privacySettings.screenshotProtectionEnabled
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] = settings.privacySettings.biometricLockEnabled
            preferences[PreferencesKeys.BIOMETRIC_TIMEOUT] = settings.privacySettings.biometricTimeoutMinutes
        }
    }
    
    val fullSettingsFlow: Flow<tn.keeftalk.chat.domain.model.UserSettings> = context.dataStore.data.map { preferences ->
        val userId = preferences[PreferencesKeys.USER_ID] ?: ""
        tn.keeftalk.chat.domain.model.UserSettings(
            userId = userId,
            chatSettings = preferences[PreferencesKeys.CHAT_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserChatSettings(userId = userId) }
            } ?: tn.keeftalk.chat.domain.model.UserChatSettings(userId = userId),
            callSettings = preferences[PreferencesKeys.CALL_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserCallSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserCallSettings(),
            noteSettings = preferences[PreferencesKeys.NOTE_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserNoteSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserNoteSettings(),
            vaultSettings = preferences[PreferencesKeys.VAULT_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserVaultSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserVaultSettings(),
            calendarSettings = preferences[PreferencesKeys.CALENDAR_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserCalendarSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserCalendarSettings(),
            emailSettings = preferences[PreferencesKeys.EMAIL_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserEmailSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserEmailSettings(),
            parentalControls = preferences[PreferencesKeys.PARENTAL_CONTROLS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserParentalControls() }
            } ?: tn.keeftalk.chat.domain.model.UserParentalControls(),
            browserSettings = preferences[PreferencesKeys.BROWSER_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserBrowserSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserBrowserSettings(),
            habitSettings = preferences[PreferencesKeys.HABIT_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserHabitSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserHabitSettings(),
            notificationSettings = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { tn.keeftalk.chat.domain.model.UserNotificationSettings() }
            } ?: tn.keeftalk.chat.domain.model.UserNotificationSettings()
        )
    }

    suspend fun clearAll() {
        store.putString("user_id", null)
        store.putString("theme_mode", null)
        store.putBoolean("is_logged", false)
        store.putString("instant_chat_cache", null)
        store.putString("user_storage_config", null)
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun getAssignedStorageConfig(): tn.keeftalk.chat.util.UserStorageConfig {
        val jsonStr = store.getString("user_storage_config", null)
        if (jsonStr != null) {
            try {
                return Json.decodeFromString<tn.keeftalk.chat.util.UserStorageConfig>(jsonStr)
            } catch (_: Exception) {}
        }
        return tn.keeftalk.chat.util.UserStorageConfig()
    }

    fun saveAssignedStorageConfig(config: tn.keeftalk.chat.util.UserStorageConfig) {
        try {
            val jsonStr = Json.encodeToString(config)
            store.putString("user_storage_config", jsonStr)
        } catch (_: Exception) {}
    }
}
