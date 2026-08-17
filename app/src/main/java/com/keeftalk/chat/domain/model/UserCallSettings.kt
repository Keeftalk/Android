package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCallSettings(
    // Calling
    val incomingCallBehavior: String = "FULL_SCREEN", // FULL_SCREEN, HEADS_UP, SILENT
    val ringtone: String = "DEFAULT",
    val vibration: String = "NORMAL",
    val speakerOnStart: Boolean = false,
    val bluetoothAutoConnect: Boolean = true,
    val microphoneEnabledByDefault: Boolean = true,
    val cameraEnabledByDefault: Boolean = false,
    val videoQuality: String = "HD", // HD, STANDARD, LOW
    
    // Privacy
    val whoCanCallMe: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val unknownCallerBehavior: String = "ALLOW", // ALLOW, SILENCE, BLOCK
    val callHistoryRetentionDays: Int = 90,
    
    // Data
    val wifiOnlyCalling: Boolean = false,
    val dataSavingMode: Boolean = false
)
