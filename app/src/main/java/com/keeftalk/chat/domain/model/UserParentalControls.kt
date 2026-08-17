package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserParentalControls(
    val isEnabled: Boolean = false,
    val supervisorId: String? = null,
    
    // Communication
    val allowedContactLevel: String = "EVERYONE", // EVERYONE, CONTACTS, WHITELIST
    val canMessageUnknown: Boolean = true,
    val canCallUnknown: Boolean = true,
    val canJoinGroups: Boolean = true,
    val communicationWhitelist: List<String> = emptyList(),
    
    // Content
    val mediaFilterLevel: String = "OFF", // OFF, SOFT, STRICT
    val blockExternalLinks: Boolean = false,
    val restrictMediaDownloads: Boolean = false,
    val maxDownloadSizeMb: Int = 100,
    
    // Time
    val dailyTimeLimitMinutes: Int = 0, // 0 = no limit
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "21:00",
    val quietHoursEnd: String = "07:00",
    val restrictedDays: List<Int> = emptyList(), // 1=Sun, 2=Mon...
    
    // Privacy
    val hideProfileFromSearch: Boolean = false,
    val disableContactDiscovery: Boolean = false,
    val shareLocationWithSupervisor: Boolean = false,
    
    // Notifications
    val notifySupervisorOnSecurityAlerts: Boolean = true,
    val notifySupervisorOnNewContacts: Boolean = false
)
