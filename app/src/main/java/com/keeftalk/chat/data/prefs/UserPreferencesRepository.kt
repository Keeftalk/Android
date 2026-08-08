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
                } ?: AppCustomization()
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
