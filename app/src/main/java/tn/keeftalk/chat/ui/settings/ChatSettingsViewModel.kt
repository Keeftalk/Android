package tn.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tn.keeftalk.chat.data.prefs.UserPreferences
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.domain.model.UserChatSettings
import tn.keeftalk.chat.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import tn.keeftalk.chat.domain.repository.AuthRepository

class ChatSettingsViewModel(
    private val settingsRepository: tn.keeftalk.chat.domain.repository.SettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    val chatSettings: StateFlow<UserChatSettings> = settingsRepository.settings
        .map { it.chatSettings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserChatSettings(userId = ""))

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _cacheSize = MutableStateFlow<Map<String, Long>>(emptyMap())
    val cacheSize = _cacheSize.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun updateSetting(key: String, value: Any) {
        viewModelScope.launch {
            // Map legacy individual updates to unified model
            settingsRepository.updateChatSettings { current ->
                when (key) {
                    "theme" -> current.copy(theme = value as String)
                    "font_size" -> current.copy(fontSize = (value as Number).toInt())
                    "auto_download_mobile" -> current.copy(autoDownloadMobile = value as List<String>)
                    "auto_download_wifi" -> current.copy(autoDownloadWifi = value as List<String>)
                    "auto_download_roaming" -> current.copy(autoDownloadRoaming = value as List<String>)
                    "upload_photo_quality" -> current.copy(uploadPhotoQuality = value as String)
                    "upload_video_quality" -> current.copy(uploadVideoQuality = value as String)
                    "autoplay_gifs" -> current.copy(autoplayGifs = value as Boolean)
                    "autoplay_videos" -> current.copy(autoplayVideos = value as Boolean)
                    "save_to_gallery" -> current.copy(saveToGallery = value as Boolean)
                    "enter_key_behavior" -> current.copy(enterKeyBehavior = value as String)
                    "link_previews_enabled" -> current.copy(linkPreviewsEnabled = value as Boolean)
                    "wallpaper_type" -> current.copy(wallpaperType = value as String)
                    "wallpaper_reference" -> current.copy(wallpaperReference = value as String)
                    else -> current
                }
            }
        }
    }

    fun updateWallpaper(type: String, reference: String) {
        viewModelScope.launch {
            settingsRepository.updateChatSettings { current ->
                current.copy(wallpaperType = type, wallpaperReference = reference)
            }
            userPreferencesRepository.updateChatSettings(wallpaper = reference)
        }
    }

    fun uploadWallpaper(byteArray: ByteArray, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            val auth = authRepository
            if (auth == null) {
                onResult(Result.failure(Exception("AuthRepository not available")))
                return@launch
            }
            auth.uploadWallpaper(byteArray).onSuccess { url ->
                updateWallpaper("CUSTOM", url)
                onResult(Result.success(url))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDynamicColor(enabled)
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = settingsRepository.getCacheSize()
        }
    }

    fun clearCache(types: List<String>) {
        viewModelScope.launch {
            settingsRepository.clearCache(types)
            refreshCacheSize()
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            settingsRepository.resetToDefault()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            settingsRepository.clearSearchHistory()
        }
    }

    fun clearRecentEmojis() {
        viewModelScope.launch {
            settingsRepository.clearRecentEmojis()
        }
    }
}
