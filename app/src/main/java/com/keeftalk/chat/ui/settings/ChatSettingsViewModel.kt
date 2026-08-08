package com.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.data.prefs.UserPreferences
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.UserChatSettings
import com.keeftalk.chat.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatSettingsViewModel(
    private val chatSettingsRepository: ChatSettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val chatSettings: StateFlow<UserChatSettings> = chatSettingsRepository.chatSettings
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
            chatSettingsRepository.updateSetting(key, value)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDynamicColor(enabled)
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = chatSettingsRepository.getCacheSize()
        }
    }

    fun clearCache(types: List<String>) {
        viewModelScope.launch {
            chatSettingsRepository.clearCache(types)
            refreshCacheSize()
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            chatSettingsRepository.resetToDefault()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            chatSettingsRepository.clearSearchHistory()
        }
    }

    fun clearRecentEmojis() {
        viewModelScope.launch {
            chatSettingsRepository.clearRecentEmojis()
        }
    }
}
