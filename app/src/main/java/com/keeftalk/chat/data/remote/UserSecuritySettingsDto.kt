package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.UserSecuritySettings
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSecuritySettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("two_factor_enabled") val twoFactorEnabled: Boolean = false,
    @SerialName("pin_hash") val pinHash: String? = null,
    @SerialName("recovery_email") val recoveryEmail: String? = null,
    @SerialName("recovery_email_verified") val recoveryEmailVerified: Boolean = false,
    @SerialName("app_lock_enabled") val appLockEnabled: Boolean = false,
    @SerialName("app_lock_timeout_seconds") val appLockTimeoutSeconds: Int = 0,
    @SerialName("biometric_unlock_enabled") val biometricUnlockEnabled: Boolean = false,
    @SerialName("encrypted_account_key") val encryptedAccountKey: String? = null,
    @SerialName("key_salt") val keySalt: String? = null,
    @SerialName("key_nonce") val keyNonce: String? = null,
    @SerialName("verification_tag") val verificationTag: String? = null,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at") val createdAt: Long = 0,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("updated_at") val updatedAt: Long = 0
) {
    fun toDomain() = UserSecuritySettings(
        userId = userId,
        twoFactorEnabled = twoFactorEnabled,
        recoveryEmail = recoveryEmail,
        recoveryEmailVerified = recoveryEmailVerified,
        appLockEnabled = appLockEnabled,
        appLockTimeoutSeconds = appLockTimeoutSeconds,
        biometricUnlockEnabled = biometricUnlockEnabled,
        encryptedAccountKey = encryptedAccountKey,
        keySalt = keySalt,
        keyNonce = keyNonce,
        verificationTag = verificationTag,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(settings: UserSecuritySettings, pinHash: String? = null) = UserSecuritySettingsDto(
            userId = settings.userId,
            twoFactorEnabled = settings.twoFactorEnabled,
            pinHash = pinHash,
            recoveryEmail = settings.recoveryEmail,
            recoveryEmailVerified = settings.recoveryEmailVerified,
            appLockEnabled = settings.appLockEnabled,
            appLockTimeoutSeconds = settings.appLockTimeoutSeconds,
            biometricUnlockEnabled = settings.biometricUnlockEnabled,
            createdAt = settings.createdAt,
            updatedAt = settings.updatedAt
        )
    }
}
