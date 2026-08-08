package com.keeftalk.chat.feature.auth.forgotpassword.state

data class ForgotPasswordState(
    val identifier: String = "",
    val foundProfiles: List<com.keeftalk.chat.domain.model.Profile> = emptyList(),
    val selectedProfile: com.keeftalk.chat.domain.model.Profile? = null,
    val step: ForgotPasswordStep = ForgotPasswordStep.ENTER_IDENTITY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val countdown: Int = 0
)

enum class ForgotPasswordStep {
    ENTER_IDENTITY,
    SELECT_PROFILE,
    CONFIRM_PROFILE,
    CONFIRMATION
}

data class ResetPasswordState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val passwordRequirements: PasswordRequirements = PasswordRequirements()
)

data class PasswordRequirements(
    val hasMinLength: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false,
    val passwordsMatch: Boolean = false
) {
    val allMet: Boolean get() = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar && passwordsMatch
}
