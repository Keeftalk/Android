package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSmsSettings(
    val smsBridgeEnabled: Boolean = false,
    val deliveryReportsEnabled: Boolean = true,
    val mmsAutoDownload: Boolean = true,
    val retentionDays: Int = 90,
    val encryptSmsThreads: Boolean = true
)
