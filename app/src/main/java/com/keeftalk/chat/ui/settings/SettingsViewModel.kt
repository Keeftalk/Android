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
import com.keeftalk.chat.domain.model.UserChatSettings
import com.keeftalk.chat.domain.model.UserSettings
import com.keeftalk.chat.domain.model.UserCallSettings
import com.keeftalk.chat.domain.model.UserNoteSettings
import com.keeftalk.chat.domain.model.UserVaultSettings
import com.keeftalk.chat.domain.model.UserCalendarSettings
import com.keeftalk.chat.domain.model.UserEmailSettings
import com.keeftalk.chat.domain.model.UserNotificationSettings
import com.keeftalk.chat.domain.model.UserParentalControls
import com.keeftalk.chat.domain.model.ChatNotificationSettings
import com.keeftalk.chat.data.prefs.NotificationPreferences
import com.keeftalk.chat.domain.model.SecurityEvent
import com.keeftalk.chat.domain.model.UserSecuritySettings
import com.keeftalk.chat.domain.model.UserSession
import com.keeftalk.chat.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    private val prefs: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: com.keeftalk.chat.domain.repository.ChatRepository,
    private val privacyRepository: PrivacyRepository,
    private val securityRepository: SecurityRepository,
    private val settingsRepository: com.keeftalk.chat.domain.repository.SettingsRepository
) : ViewModel() {

    init {
        settingsRepository.startSettingsSync()
    }

    val fullSettings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings(userId = ""))

    val currentUserProfile: StateFlow<Profile?> = authRepository.currentUserProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val blockedUsers: Flow<List<User>> = chatRepository.getBlockedUsers()

    val securitySettings: StateFlow<UserSecuritySettings?> = securityRepository.securitySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSessions: StateFlow<List<UserSession>> = securityRepository.activeSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityEvents: StateFlow<List<SecurityEvent>> = securityRepository.securityEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _securityUiState = MutableStateFlow(SecurityUiState())
    val securityUiState = _securityUiState.asStateFlow()

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
            settingsRepository.updatePrivacySettings { current ->
                when (key) {
                    "profilePhotoVisibility" -> current.copy(profilePhotoVisibility = PrivacyVisibility.fromString(value as String))
                    "aboutVisibility" -> current.copy(aboutVisibility = PrivacyVisibility.fromString(value as String))
                    "lastSeenVisibility" -> current.copy(lastSeenVisibility = PrivacyVisibility.fromString(value as String))
                    "connectionsVisibility" -> current.copy(connectionsVisibility = PrivacyVisibility.fromString(value as String))
                    "readReceiptsEnabled" -> current.copy(readReceiptsEnabled = value as Boolean)
                    "typingIndicatorsEnabled" -> current.copy(typingIndicatorsEnabled = value as Boolean)
                    "profileViewHistoryEnabled" -> current.copy(profileViewHistoryEnabled = value as Boolean)
                    "whoCanCallMe" -> current.copy(callPermission = PrivacyVisibility.fromString(value as String))
                    "whoCanAddToGroups" -> current.copy(groupPermission = PrivacyVisibility.fromString(value as String))
                    "screenshotProtectionEnabled" -> current.copy(screenshotProtectionEnabled = value as Boolean)
                    "biometricLockEnabled" -> current.copy(biometricLockEnabled = value as Boolean)
                    "biometricTimeout" -> current.copy(biometricTimeoutMinutes = value as Int)
                    else -> current
                }
            }
            // Also notify legacy repository for any non-JSONB specific logic
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

    fun updateNotificationPreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            prefs.updateNotificationPreferences(update)
        }
    }

    fun updateProfile(
        fullName: String? = null,
        username: String? = null,
        bio: String? = null
    ) {
        viewModelScope.launch {
            currentUserProfile.value?.let { current ->
                val updated = current.copy(
                    fullName = fullName ?: current.fullName,
                    username = username ?: current.username,
                    bio = bio ?: current.bio
                )
                authRepository.updateProfile(updated)
            }
        }
    }

    fun updateModuleIntegration(key: String, enabled: Boolean) {
        viewModelScope.launch {
            prefs.updateModuleIntegration(key, enabled)
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

    // Security Methods
    fun enable2FA(pin: String, recoveryEmail: String?) {
        viewModelScope.launch {
            _securityUiState.update { it.copy(isLoading = true) }
            val result = securityRepository.enableTwoStepVerification(pin, recoveryEmail)
            _securityUiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "Two-step verification enabled" else null) }
        }
    }

    fun disable2FA(pin: String) {
        viewModelScope.launch {
            _securityUiState.update { it.copy(isLoading = true) }
            val result = securityRepository.disableTwoStepVerification(pin)
            _securityUiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "Two-step verification disabled" else null) }
        }
    }

    fun updateAppLock(enabled: Boolean, timeoutSeconds: Int, biometricEnabled: Boolean) {
        viewModelScope.launch {
            securityRepository.updateAppLockSettings(enabled, timeoutSeconds, biometricEnabled)
        }
    }

    // New Integrated Settings Updates
    fun updateChatSettings(update: (UserChatSettings) -> UserChatSettings) {
        viewModelScope.launch { settingsRepository.updateChatSettings(update) }
    }

    fun updateCallSettings(update: (UserCallSettings) -> UserCallSettings) {
        viewModelScope.launch { settingsRepository.updateCallSettings(update) }
    }

    fun updateNoteSettings(update: (UserNoteSettings) -> UserNoteSettings) {
        viewModelScope.launch { settingsRepository.updateNoteSettings(update) }
    }

    fun updateVaultSettings(update: (UserVaultSettings) -> UserVaultSettings) {
        viewModelScope.launch { settingsRepository.updateVaultSettings(update) }
    }

    fun updateCalendarSettings(update: (UserCalendarSettings) -> UserCalendarSettings) {
        viewModelScope.launch { settingsRepository.updateCalendarSettings(update) }
    }

    fun updateEmailSettings(update: (UserEmailSettings) -> UserEmailSettings) {
        viewModelScope.launch { settingsRepository.updateEmailSettings(update) }
    }

    fun updateNotificationSettings(update: (UserNotificationSettings) -> UserNotificationSettings) {
        viewModelScope.launch { settingsRepository.updateNotificationSettings(update) }
    }

    fun updateParentalControls(update: (UserParentalControls) -> UserParentalControls) {
        viewModelScope.launch { settingsRepository.updateParentalControls(update) }
    }
    
    override fun onCleared() {
        super.onCleared()
        settingsRepository.shutdown()
    }

    fun logoutSession(sessionId: String) {
        viewModelScope.launch {
            securityRepository.logoutSession(sessionId)
        }
    }

    fun logoutAllOtherSessions() {
        viewModelScope.launch {
            securityRepository.logoutAllOtherSessions()
        }
    }

    fun changePassword(current: String, new: String) {
        viewModelScope.launch {
            _securityUiState.update { it.copy(isLoading = true) }
            val result = securityRepository.changePassword(current, new)
            _securityUiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "Password changed successfully" else null) }
        }
    }

    fun clearSecurityError() {
        _securityUiState.update { it.copy(error = null) }
    }

    fun clearSecuritySuccessMessage() {
        _securityUiState.update { it.copy(successMessage = null) }
    }
}

data class SecurityUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
