package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPrivacySettings(
    val userId: String,
    val profilePhotoVisibility: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val aboutVisibility: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val lastSeenVisibility: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val readReceiptsEnabled: Boolean = true,
    val typingIndicatorsEnabled: Boolean = true,
    val profileViewHistoryEnabled: Boolean = false,
    val callPermission: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val groupPermission: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val screenshotProtectionEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val biometricTimeoutMinutes: Int = 0
)

enum class PrivacyVisibility {
    EVERYONE, CONTACTS, NOBODY;

    companion object {
        fun fromString(value: String?): PrivacyVisibility {
            return entries.find { it.name == value } ?: EVERYONE
        }
    }
}
