package com.keeftalk.chat.feature.auth.forgotpassword.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.feature.auth.forgotpassword.state.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow(ResetPasswordState())
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState.asStateFlow()

    private var countdownJob: Job? = null

    fun onIdentifierChange(identifier: String) {
        _forgotPasswordState.update { it.copy(identifier = identifier, error = null) }
    }

    fun findAccount() {
        val identifier = _forgotPasswordState.value.identifier
        if (identifier.isBlank()) return

        viewModelScope.launch {
            _forgotPasswordState.update { it.copy(isLoading = true, error = null) }
            authRepository.findProfilesByIdentifier(identifier)
                .onSuccess { profiles ->
                    when {
                        profiles.isEmpty() -> {
                            _forgotPasswordState.update { it.copy(isLoading = false, error = "No account found with this information") }
                        }
                        profiles.size == 1 -> {
                            _forgotPasswordState.update { it.copy(
                                isLoading = false,
                                foundProfiles = profiles,
                                selectedProfile = profiles[0],
                                step = ForgotPasswordStep.CONFIRM_PROFILE
                            ) }
                        }
                        else -> {
                            _forgotPasswordState.update { it.copy(
                                isLoading = false,
                                foundProfiles = profiles,
                                step = ForgotPasswordStep.SELECT_PROFILE
                            ) }
                        }
                    }
                }
                .onFailure { e ->
                    _forgotPasswordState.update { it.copy(isLoading = false, error = e.message ?: "Search failed") }
                }
        }
    }

    fun selectProfile(profile: Profile) {
        _forgotPasswordState.update { it.copy(selectedProfile = profile, step = ForgotPasswordStep.CONFIRM_PROFILE) }
    }

    fun sendResetLink() {
        val profile = _forgotPasswordState.value.selectedProfile ?: return
        val email = profile.email ?: return

        viewModelScope.launch {
            _forgotPasswordState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.resetPassword(email)
            result.onSuccess {
                _forgotPasswordState.update { it.copy(isLoading = false, step = ForgotPasswordStep.CONFIRMATION) }
                startCountdown()
            }.onFailure { e ->
                _forgotPasswordState.update { it.copy(isLoading = false, error = e.message ?: "Failed to send reset link") }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _forgotPasswordState.update { it.copy(countdown = 60) }
            while (_forgotPasswordState.value.countdown > 0) {
                delay(1000)
                _forgotPasswordState.update { it.copy(countdown = it.countdown - 1) }
            }
        }
    }

    fun onNewPasswordChange(password: String) {
        _resetPasswordState.update { 
            val requirements = calculateRequirements(password, it.confirmPassword)
            it.copy(newPassword = password, passwordRequirements = requirements, error = null) 
        }
    }

    fun onConfirmPasswordChange(password: String) {
        _resetPasswordState.update { 
            val requirements = calculateRequirements(it.newPassword, password)
            it.copy(confirmPassword = password, passwordRequirements = requirements, error = null) 
        }
    }

    fun togglePasswordVisibility() {
        _resetPasswordState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _resetPasswordState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun updatePassword() {
        if (!_resetPasswordState.value.passwordRequirements.allMet) return

        viewModelScope.launch {
            _resetPasswordState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.updatePassword(_resetPasswordState.value.newPassword)
            result.onSuccess {
                _resetPasswordState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                _resetPasswordState.update { it.copy(isLoading = false, error = e.message ?: "Failed to update password") }
            }
        }
    }

    private fun calculateRequirements(password: String, confirm: String): PasswordRequirements {
        return PasswordRequirements(
            hasMinLength = password.length >= 8,
            hasUppercase = password.any { it.isUpperCase() },
            hasLowercase = password.any { it.isLowerCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecialChar = password.any { !it.isLetterOrDigit() },
            passwordsMatch = password == confirm && password.isNotEmpty()
        )
    }
    
    fun resetForgotPasswordState() {
        _forgotPasswordState.update { ForgotPasswordState() }
        countdownJob?.cancel()
    }

    fun backToIdentity() {
        _forgotPasswordState.update { it.copy(step = ForgotPasswordStep.ENTER_IDENTITY, foundProfiles = emptyList(), selectedProfile = null) }
    }
}
