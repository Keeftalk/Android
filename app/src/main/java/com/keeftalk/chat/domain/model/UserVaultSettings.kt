package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserVaultSettings(
    // Security
    val autoLockVault: Boolean = true,
    val autoLockTimeoutMinutes: Int = 1,
    val requireBiometricAuth: Boolean = true,
    val requireAppAuth: Boolean = false,
    val hideVaultPreviews: Boolean = true,
    val screenshotProtection: Boolean = true,
    
    // Files
    val autoImportBehavior: String = "ASK", // ASK, AUTO_MOVE, AUTO_COPY, DISABLED
    val generateThumbnails: Boolean = true,
    val mediaQuality: String = "ORIGINAL", // ORIGINAL, HIGH, COMPRESSED
    val localCacheEnabled: Boolean = true,
    val autoCleanupCache: Boolean = true,
    val downloadBehavior: String = "WIFI_ONLY", // ALWAYS, WIFI_ONLY, MANUAL
    
    // Sharing
    val shareToChatBehavior: String = "ENCRYPTED_LINK", // ENCRYPTED_LINK, DIRECT_COPY
    val zeroCopySharingEnabled: Boolean = true,
    val sharedFileAccessTimeoutHours: Int = 24
)
