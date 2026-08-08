package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.PrivacyVisibility
import com.keeftalk.chat.domain.model.UserPrivacySettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPrivacySettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_photo_visibility") val profilePhotoVisibility: String = "EVERYONE",
    @SerialName("about_visibility") val aboutVisibility: String = "EVERYONE",
    @SerialName("last_seen_visibility") val lastSeenVisibility: String = "EVERYONE",
    @SerialName("read_receipts_enabled") val readReceiptsEnabled: Boolean = true,
    @SerialName("typing_indicators_enabled") val typingIndicatorsEnabled: Boolean = true,
    @SerialName("profile_view_history_enabled") val profileViewHistoryEnabled: Boolean = false,
    @SerialName("call_permission") val callPermission: String = "EVERYONE",
    @SerialName("group_permission") val groupPermission: String = "EVERYONE",
    @SerialName("screenshot_protection_enabled") val screenshotProtectionEnabled: Boolean = false,
    @SerialName("biometric_lock_enabled") val biometricLockEnabled: Boolean = false,
    @SerialName("biometric_timeout_minutes") val biometricTimeoutMinutes: Int = 0
) {
    fun toDomain() = UserPrivacySettings(
        userId = userId,
        profilePhotoVisibility = PrivacyVisibility.fromString(profilePhotoVisibility),
        aboutVisibility = PrivacyVisibility.fromString(aboutVisibility),
        lastSeenVisibility = PrivacyVisibility.fromString(lastSeenVisibility),
        readReceiptsEnabled = readReceiptsEnabled,
        typingIndicatorsEnabled = typingIndicatorsEnabled,
        profileViewHistoryEnabled = profileViewHistoryEnabled,
        callPermission = PrivacyVisibility.fromString(callPermission),
        groupPermission = PrivacyVisibility.fromString(groupPermission),
        screenshotProtectionEnabled = screenshotProtectionEnabled,
        biometricLockEnabled = biometricLockEnabled,
        biometricTimeoutMinutes = biometricTimeoutMinutes
    )

    companion object {
        fun fromDomain(settings: UserPrivacySettings) = UserPrivacySettingsDto(
            userId = settings.userId,
            profilePhotoVisibility = settings.profilePhotoVisibility.name,
            aboutVisibility = settings.aboutVisibility.name,
            lastSeenVisibility = settings.lastSeenVisibility.name,
            readReceiptsEnabled = settings.readReceiptsEnabled,
            typingIndicatorsEnabled = settings.typingIndicatorsEnabled,
            profileViewHistoryEnabled = settings.profileViewHistoryEnabled,
            callPermission = settings.callPermission.name,
            groupPermission = settings.groupPermission.name,
            screenshotProtectionEnabled = settings.screenshotProtectionEnabled,
            biometricLockEnabled = settings.biometricLockEnabled,
            biometricTimeoutMinutes = settings.biometricTimeoutMinutes
        )
    }
}
