package com.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.data.prefs.UserPreferences
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.PrivacyRepository
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.model.UserPrivacySettings
import com.keeftalk.chat.domain.model.PrivacyVisibility
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.keeftalk.chat.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.keeftalk.chat.domain.model.ChatNotificationSettings
import com.keeftalk.chat.data.prefs.NotificationPreferences

class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: com.keeftalk.chat.domain.repository.ChatRepository,
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = prefs.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val currentUserProfile: StateFlow<Profile?> = authRepository.currentUserProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val blockedUsers: Flow<List<User>> = chatRepository.getBlockedUsers()

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            prefs.updateThemeMode(themeMode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            prefs.updateDynamicColor(enabled)
        }
    }

    fun updatePrivacySetting(key: String, value: Any) {
        viewModelScope.launch {
            privacyRepository.updateSetting(key, value)
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            chatRepository.blockUser(userId, false)
        }
    }

    fun updateChatSettings(
        fontSize: Float? = null,
        autoDownload: Boolean? = null,
        quality: String? = null
    ) {
        viewModelScope.launch {
            prefs.updateChatSettings(
                fontSize = fontSize,
                autoDownload = autoDownload,
                quality = quality
            )
        }
    }

    fun updateNotificationPreferences(update: (com.keeftalk.chat.data.prefs.NotificationPreferences) -> com.keeftalk.chat.data.prefs.NotificationPreferences) {
        viewModelScope.launch {
            prefs.updateNotificationPreferences(update)
        }
    }

    fun sendTestNotification() {
        // We can use a shared flow or just call a repository method if it's there.
        // For simplicity, I'll assume we can use the same logic as MainViewModel or trigger it via repo.
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            privacyRepository.deleteAccount()
        }
    }

    fun downloadMyData(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            privacyRepository.downloadMyData()
                .onSuccess { onResult(it) }
                .onFailure { onResult(null) }
        }
    }
}
