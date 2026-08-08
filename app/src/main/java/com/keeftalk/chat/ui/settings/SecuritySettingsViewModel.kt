package com.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.SecurityEvent
import com.keeftalk.chat.domain.model.UserSecuritySettings
import com.keeftalk.chat.domain.model.UserSession
import com.keeftalk.chat.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    val securitySettings: StateFlow<UserSecuritySettings?> = securityRepository.securitySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSessions: StateFlow<List<UserSession>> = securityRepository.activeSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityEvents: StateFlow<List<SecurityEvent>> = securityRepository.securityEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkBiometricSupport()
    }

    private fun checkBiometricSupport() {
        // This will be set by the UI since we need context/activity for full check
        // But we can do a basic check if possible
    }

    fun setBiometricSupport(supported: Boolean) {
        _uiState.update { it.copy(isBiometricSupported = supported) }
    }

    fun enable2FA(pin: String, recoveryEmail: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = securityRepository.enableTwoStepVerification(pin, recoveryEmail)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "Two-step verification enabled" else null) }
        }
    }

    fun disable2FA(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = securityRepository.disableTwoStepVerification(pin)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "Two-step verification disabled" else null) }
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = securityRepository.changePin(oldPin, newPin)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "PIN changed successfully" else null) }
        }
    }

    fun updateAppLock(enabled: Boolean, timeoutSeconds: Int, biometricEnabled: Boolean) {
        viewModelScope.launch {
            securityRepository.updateAppLockSettings(enabled, timeoutSeconds, biometricEnabled)
        }
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
            _uiState.update { it.copy(isLoading = true) }
            val result = securityRepository.changePassword(current, new)
            _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message, successMessage = if (result.isSuccess) "Password changed successfully" else null) }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            securityRepository.getSessions()
            securityRepository.getSecurityEvents()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

data class SecurityUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isBiometricSupported: Boolean = false
)
