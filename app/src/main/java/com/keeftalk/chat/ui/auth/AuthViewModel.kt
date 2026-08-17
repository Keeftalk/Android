package com.keeftalk.chat.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.service.CountryService
import com.keeftalk.chat.domain.service.PhoneNumberService
import com.keeftalk.chat.util.AuthUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class AuthViewModel(
    val authRepository: AuthRepository,
    private val countryService: CountryService,
    private val phoneNumberService: PhoneNumberService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _formType = MutableStateFlow(AuthFormType.LOGIN)
    val formType = _formType.asStateFlow()

    val currentCountry = countryService.currentRegion

    fun onCountryChange(country: com.keeftalk.chat.domain.model.Country) {
        viewModelScope.launch {
            countryService.setAccountCountry(country)
            // Re-format phone number with new country code if needed
            onSignupPhoneChange(_signupPhone.value)
        }
    }

    // Login Form State
    private val _loginIdentifier = MutableStateFlow("")
    val loginIdentifier = _loginIdentifier.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword = _loginPassword.asStateFlow()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe = _rememberMe.asStateFlow()

    // Sign Up Form State
    private val _signupFullName = MutableStateFlow("")
    val signupFullName = _signupFullName.asStateFlow()

    private val _signupEmail = MutableStateFlow("")
    val signupEmail = _signupEmail.asStateFlow()

    private val _signupPhone = MutableStateFlow("")
    val signupPhone = _signupPhone.asStateFlow()

    private val _signupPassword = MutableStateFlow("")
    val signupPassword = _signupPassword.asStateFlow()

    private val _agreeToTerms = MutableStateFlow(false)
    val agreeToTerms = _agreeToTerms.asStateFlow()

    private val _signupErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val signupErrors = _signupErrors.asStateFlow()

    // Verification State
    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown = _resendCountdown.asStateFlow()

    val isEmailVerified = authRepository.isEmailVerified

    // Validation States
    val isLoginValid = combine(loginIdentifier, loginPassword) { id, pass ->
        id.isNotBlank() && pass.length >= 6
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val signupPasswordStrength = signupPassword.map { password ->
        when {
            password.isEmpty() -> 0f
            password.length < 6 -> 0.3f
            password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 1f
            else -> 0.6f
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val isSignupValid = combine(
        signupFullName,
        signupEmail,
        signupPhone,
        signupPassword,
        agreeToTerms
    ) { args: Array<Any?> ->
        val name = args[0] as String
        val email = args[1] as String
        val phone = args[2] as String
        val pass = args[3] as String
        val agree = args[4] as Boolean

        name.isNotBlank() && 
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
        phoneNumberService.isValid(phone, currentCountry.value.isoCode) &&
        pass.length >= 6 && 
        agree
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Automatically transition to Success when email is verified
        isEmailVerified
            .onEach { verified ->
                if (verified && _formType.value == AuthFormType.VERIFY_EMAIL) {
                    _uiState.value = AuthUiState.Success
                }
            }
            .launchIn(viewModelScope)
    }

    fun setFormType(type: AuthFormType) {
        _formType.value = type
        _uiState.value = AuthUiState.Idle
    }

    fun onLoginIdentifierChange(value: String) { _loginIdentifier.value = value }
    fun onLoginPasswordChange(value: String) { _loginPassword.value = value }
    fun onRememberMeChange(value: Boolean) { _rememberMe.value = value }

    fun onSignupFullNameChange(value: String) { 
        _signupFullName.value = value 
        _signupErrors.value = _signupErrors.value.toMutableMap().apply { remove("fullName") }
    }
    fun onSignupEmailChange(value: String) { 
        _signupEmail.value = value 
        _signupErrors.value = _signupErrors.value.toMutableMap().apply { remove("email") }
    }
    fun onSignupPhoneChange(value: String) {
        // Keep raw digits in state, let VisualTransformation handle display
        _signupPhone.value = value.filter { it.isDigit() || it == '+' }
        _signupErrors.value = _signupErrors.value.toMutableMap().apply { remove("phone") }
    }
    fun onSignupPasswordChange(value: String) { 
        _signupPassword.value = value 
        _signupErrors.value = _signupErrors.value.toMutableMap().apply { remove("password") }
    }
    fun onAgreeToTermsChange(value: Boolean) { 
        _agreeToTerms.value = value 
        _signupErrors.value = _signupErrors.value.toMutableMap().apply { remove("terms") }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(_loginIdentifier.value, _loginPassword.value)
                .onSuccess {
                    // Ensure session is fresh and wait for verification status to settle
                    viewModelScope.launch {
                        try {
                            authRepository.refreshSession()
                            // The flow is now filtered in AuthRepositoryImpl to wait for non-transitional state
                            val verified = authRepository.isEmailVerified.first()
                            Log.d("AuthViewModel", "Login successful, verification status: $verified")
                            
                            if (verified) {
                                _uiState.value = AuthUiState.Success
                            } else {
                                _formType.value = AuthFormType.VERIFY_EMAIL
                                _uiState.value = AuthUiState.Idle
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Post-login verification check failed", e)
                            _uiState.value = AuthUiState.Error("Verification check failed. Please try again.")
                        }
                    }
                }
                .onFailure {
                    val error = it.message ?: "Login failed"
                    if (error == "email_not_confirmed") {
                        // If the login identifier was an email, keep it for resend/display
                        if (AuthUtils.detectIdentifierType(_loginIdentifier.value) == AuthUtils.IdentifierType.EMAIL) {
                            _signupEmail.value = _loginIdentifier.value
                        }
                        _signupPassword.value = _loginPassword.value
                        
                        // Switch to verification screen if email not confirmed
                        _formType.value = AuthFormType.VERIFY_EMAIL
                        _uiState.value = AuthUiState.Idle
                    } else if (error.contains("Wrong encryption password")) {
                        _uiState.value = AuthUiState.EncryptionError(error)
                    } else {
                        _uiState.value = AuthUiState.Error(error)
                    }
                }
        }
    }

    fun performSecurityReset() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.resetSecuritySettings()
                .onSuccess {
                    _uiState.value = AuthUiState.Idle
                    login() // Retry login which will now initialize a new key
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Reset failed")
                }
        }
    }

    fun signup() {
        if (!validateSignup()) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Automatically generate username
            val sanitizedName = _signupFullName.value.lowercase()
                .replace(Regex("[^a-z0-9]"), "")
            val randomSuffix = (1000..9999).random()
            var targetUsername = "${sanitizedName}${randomSuffix}"

            // Final availability check
            val isAvailable = authRepository.checkUsernameAvailability(targetUsername).getOrDefault(false)
            if (!isAvailable) {
                val nextSuffix = (1000..9999).random()
                targetUsername = "${sanitizedName}${nextSuffix}"
            }

            val normalizedPhone = phoneNumberService.normalizeToE164(_signupPhone.value, currentCountry.value.isoCode)
                ?: _signupPhone.value

            // Store the detected country as the account country on signup
            countryService.setAccountCountry(currentCountry.value)

            authRepository.signup(
                _signupFullName.value,
                targetUsername,
                _signupEmail.value,
                normalizedPhone,
                _signupPassword.value,
                currentCountry.value.name,
                currentCountry.value.isoCode,
                currentCountry.value.phoneCode
            )
                .onSuccess {
                    _formType.value = AuthFormType.VERIFY_EMAIL
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure {
                    val message = it.message ?: "Signup failed"
                    if (message.contains("Error sending confirmation email", ignoreCase = true)) {
                        _uiState.value = AuthUiState.Error(
                            "Account created, but we couldn't send the verification email. " +
                            "Please try logging in and clicking 'Resend' from the verification screen."
                        )
                    } else {
                        _uiState.value = AuthUiState.Error(message)
                    }
                }
        }
    }

    private fun validateSignup(): Boolean {
        val errors = mutableMapOf<String, String>()
        
        if (_signupFullName.value.isBlank()) {
            errors["fullName"] = "Full name is required"
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_signupEmail.value).matches()) {
            errors["email"] = "Invalid email address"
        }
        
        if (!phoneNumberService.isValid(_signupPhone.value, currentCountry.value.isoCode)) {
            errors["phone"] = "Invalid phone number"
        }
        
        if (_signupPassword.value.length < 6) {
            errors["password"] = "Password must be at least 6 characters"
        }
        
        if (!_agreeToTerms.value) {
            errors["terms"] = "You must agree to the Terms & Privacy Policy"
        }
        
        _signupErrors.value = errors
        return errors.isEmpty()
    }

    fun resendVerification() {
        if (_resendCountdown.value > 0) return
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // Determine which email to resend to
            val email = _signupEmail.value.ifBlank {
                if (AuthUtils.detectIdentifierType(_loginIdentifier.value) == AuthUtils.IdentifierType.EMAIL) {
                    _loginIdentifier.value
                } else null
            }

            authRepository.resendVerificationEmail(email)
                .onSuccess {
                    startResendCountdown()
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to resend email")
                }
        }
    }

    private fun startResendCountdown() {
        viewModelScope.launch {
            _resendCountdown.value = 60
            while (_resendCountdown.value > 0) {
                kotlinx.coroutines.delay(1000)
                _resendCountdown.value -= 1
            }
        }
    }

    fun checkVerificationStatus() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // 1. Try to refresh existing session if any
            authRepository.refreshSession()
            
            var verified = authRepository.isEmailVerified.first()
            
            // 2. If still not verified, try to log in if we have credentials (from signup)
            if (!verified && _signupEmail.value.isNotBlank() && _signupPassword.value.isNotBlank()) {
                authRepository.login(_signupEmail.value, _signupPassword.value)
                verified = authRepository.isEmailVerified.first()
            }
            
            // 3. If still not verified, try to log in if we have credentials (from login attempt)
            if (!verified && _loginIdentifier.value.isNotBlank() && _loginPassword.value.isNotBlank()) {
                authRepository.login(_loginIdentifier.value, _loginPassword.value)
                verified = authRepository.isEmailVerified.first()
            }

            if (verified) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error("Email not verified yet. Please check your inbox and click the verification link.")
            }
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.updateEmail(newEmail)
                .onSuccess {
                    _signupEmail.value = newEmail
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to update email")
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.deleteAccount()
                .onSuccess {
                    _formType.value = AuthFormType.LOGIN
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to delete account")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _formType.value = AuthFormType.LOGIN
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

enum class AuthFormType {
    LOGIN, SIGNUP, VERIFY_EMAIL
}

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class EncryptionError(val message: String) : AuthUiState()
}
