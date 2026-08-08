package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSecuritySettings(
    val userId: String,
    val twoFactorEnabled: Boolean = false,
    val recoveryEmail: String? = null,
    val recoveryEmailVerified: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockTimeoutSeconds: Int = 0,
    val biometricUnlockEnabled: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
