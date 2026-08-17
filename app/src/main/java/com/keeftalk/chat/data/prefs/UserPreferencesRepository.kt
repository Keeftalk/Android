package com.keeftalk.chat.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.keeftalk.chat.domain.model.AppCustomization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val store: com.keeftalk.chat.util.KeeftalkStore by lazy {
        com.keeftalk.chat.util.KeeftalkStore.getInstance()
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
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                userId = preferences[PreferencesKeys.USER_ID] ?: "",
                userName = preferences[PreferencesKeys.USER_NAME] ?: "",
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
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
                emailIntegrationEnabled = preferences[PreferencesKeys.EMAIL_INTEGRATION_ENABLED] ?: true,
                smsBridgeEnabled = preferences[PreferencesKeys.SMS_BRIDGE_ENABLED] ?: false,
                voipEnabled = preferences[PreferencesKeys.VOIP_ENABLED] ?: true,
                walletEnabled = preferences[PreferencesKeys.WALLET_ENABLED] ?: false,
                calendarSyncEnabled = preferences[PreferencesKeys.CALENDAR_SYNC_ENABLED] ?: true,
                notesSyncEnabled = preferences[PreferencesKeys.NOTES_SYNC_ENABLED] ?: true,
                cloudBackupEnabled = preferences[PreferencesKeys.CLOUD_BACKUP_ENABLED] ?: true
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
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.THEME_MODE] = themeMode }
    }

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

    suspend fun updateFullSettings(settings: com.keeftalk.chat.domain.model.UserSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHAT_SETTINGS_JSON] = Json.encodeToString(settings.chatSettings)
            preferences[PreferencesKeys.CALL_SETTINGS_JSON] = Json.encodeToString(settings.callSettings)
            preferences[PreferencesKeys.NOTE_SETTINGS_JSON] = Json.encodeToString(settings.noteSettings)
            preferences[PreferencesKeys.VAULT_SETTINGS_JSON] = Json.encodeToString(settings.vaultSettings)
            preferences[PreferencesKeys.CALENDAR_SETTINGS_JSON] = Json.encodeToString(settings.calendarSettings)
            preferences[PreferencesKeys.EMAIL_SETTINGS_JSON] = Json.encodeToString(settings.emailSettings)
            preferences[PreferencesKeys.PARENTAL_CONTROLS_JSON] = Json.encodeToString(settings.parentalControls)
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = Json.encodeToString(settings.notificationSettings)
            
            // Sync individual fields for backward compatibility
            preferences[PreferencesKeys.THEME_MODE] = settings.chatSettings.theme
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
    
    val fullSettingsFlow: Flow<com.keeftalk.chat.domain.model.UserSettings> = context.dataStore.data.map { preferences ->
        val userId = preferences[PreferencesKeys.USER_ID] ?: ""
        com.keeftalk.chat.domain.model.UserSettings(
            userId = userId,
            chatSettings = preferences[PreferencesKeys.CHAT_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserChatSettings(userId = userId) }
            } ?: com.keeftalk.chat.domain.model.UserChatSettings(userId = userId),
            callSettings = preferences[PreferencesKeys.CALL_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserCallSettings() }
            } ?: com.keeftalk.chat.domain.model.UserCallSettings(),
            noteSettings = preferences[PreferencesKeys.NOTE_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserNoteSettings() }
            } ?: com.keeftalk.chat.domain.model.UserNoteSettings(),
            vaultSettings = preferences[PreferencesKeys.VAULT_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserVaultSettings() }
            } ?: com.keeftalk.chat.domain.model.UserVaultSettings(),
            calendarSettings = preferences[PreferencesKeys.CALENDAR_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserCalendarSettings() }
            } ?: com.keeftalk.chat.domain.model.UserCalendarSettings(),
            emailSettings = preferences[PreferencesKeys.EMAIL_SETTINGS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserEmailSettings() }
            } ?: com.keeftalk.chat.domain.model.UserEmailSettings(),
            parentalControls = preferences[PreferencesKeys.PARENTAL_CONTROLS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserParentalControls() }
            } ?: com.keeftalk.chat.domain.model.UserParentalControls(),
            notificationSettings = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { com.keeftalk.chat.domain.model.UserNotificationSettings() }
            } ?: com.keeftalk.chat.domain.model.UserNotificationSettings()
        )
    }

    suspend fun clearAll() {
        store.putString("user_id", null)
        store.putString("theme_mode", null)
        store.putBoolean("is_logged", false)
        store.putString("instant_chat_cache", null)
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
