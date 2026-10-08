package tn.keeftalk.chat.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.TwoFactorRepository

sealed class TwoFactorUiState {
    object Idle : TwoFactorUiState()
    object Loading : TwoFactorUiState()
    data class Error(val message: String) : TwoFactorUiState()
    data class TotpEnrollmentStarted(val data: TotpEnrollmentData) : TwoFactorUiState()
    data class OtpChallengeSent(val challengeResponse: OtpChallengeResponse, val method: TwoFactorMethod) : TwoFactorUiState()
    data class BackupCodesGenerated(val codes: List<String>) : TwoFactorUiState()
    data class Success(val message: String) : TwoFactorUiState()
}

class TwoFactorViewModel(
    private val repository: TwoFactorRepository
) : ViewModel() {

    val status = repository.status

    private val _uiState = MutableStateFlow<TwoFactorUiState>(TwoFactorUiState.Idle)
    val uiState: StateFlow<TwoFactorUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            repository.get2FAStatus()
        }
    }

    fun resetUiState() {
        _uiState.value = TwoFactorUiState.Idle
    }

    fun beginTotpEnrollment() {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.beginTotpEnrollment()
                .onSuccess { data ->
                    _uiState.value = TwoFactorUiState.TotpEnrollmentStarted(data)
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Failed to initialize TOTP")
                }
        }
    }

    fun verifyTotpEnrollment(code: String) {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.verifyTotpEnrollment(code)
                .onSuccess {
                    _uiState.value = TwoFactorUiState.Success("Authenticator app enabled successfully!")
                    refreshStatus()
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Invalid 6-digit TOTP code")
                }
        }
    }

    fun requestEmailOtp() {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.requestEmailOtp("ENROLLMENT")
                .onSuccess { response ->
                    if (response.success) {
                        _uiState.value = TwoFactorUiState.OtpChallengeSent(response, TwoFactorMethod.EMAIL)
                    } else {
                        _uiState.value = TwoFactorUiState.Error(response.message ?: "Failed to send email verification code")
                    }
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Failed to request email code")
                }
        }
    }

    fun requestSmsOtp() {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.requestSmsOtp("ENROLLMENT")
                .onSuccess { response ->
                    if (response.success) {
                        _uiState.value = TwoFactorUiState.OtpChallengeSent(response, TwoFactorMethod.SMS)
                    } else {
                        _uiState.value = TwoFactorUiState.Error(response.message ?: "Failed to send SMS code")
                    }
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Failed to request SMS code")
                }
        }
    }

    fun verifyOtpChallenge(challengeId: String, code: String) {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.verifyOtpChallenge(challengeId, code)
                .onSuccess {
                    _uiState.value = TwoFactorUiState.Success("Verification code confirmed!")
                    refreshStatus()
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Invalid verification code")
                }
        }
    }

    fun generateBackupCodes() {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.generateBackupCodes()
                .onSuccess { codes ->
                    _uiState.value = TwoFactorUiState.BackupCodesGenerated(codes)
                    refreshStatus()
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Failed to generate backup codes")
                }
        }
    }

    fun disable2FAMethod(method: TwoFactorMethod) {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.disable2FAMethod(method)
                .onSuccess {
                    _uiState.value = TwoFactorUiState.Success("${method.name} factor disabled.")
                    refreshStatus()
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Failed to disable factor")
                }
        }
    }

    fun setPreferredMethod(method: TwoFactorMethod) {
        viewModelScope.launch {
            _uiState.value = TwoFactorUiState.Loading
            repository.setPreferredMethod(method)
                .onSuccess {
                    _uiState.value = TwoFactorUiState.Success("Preferred 2FA method updated.")
                    refreshStatus()
                }
                .onFailure { error ->
                    _uiState.value = TwoFactorUiState.Error(error.message ?: "Failed to set preferred method")
                }
        }
    }
}
