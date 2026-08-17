package com.keeftalk.chat.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.security.crypto.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecurityRecoveryViewModel(
    private val authRepository: AuthRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _isRecovering = MutableStateFlow(false)
    val isRecovering = _isRecovering.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun recover(password: String) {
        if (password.isBlank()) {
            _error.value = "Password cannot be empty"
            return
        }

        viewModelScope.launch {
            _isRecovering.value = true
            _error.value = null
            try {
                val result = authRepository.recoverSecurityContext(password)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Recovery failed"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Recovery failed. Please check your password."
            } finally {
                _isRecovering.value = false
            }
        }
    }
}
