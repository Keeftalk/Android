package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserEmailSettings(
    // Mail
    val defaultInbox: String = "ALL_INBOXES",
    val conversationViewEnabled: Boolean = true,
    val markAsReadDelaySeconds: Int = 0,
    val archiveAfterAction: Boolean = false,
    val swipeLeftAction: String = "ARCHIVE",
    val swipeRightAction: String = "DELETE",
    val defaultReplyBehavior: String = "REPLY", // REPLY, REPLY_ALL
    
    // Notifications
    val notificationsEnabled: Boolean = true,
    val notifyForImportantOnly: Boolean = false,
    val showNotificationPreview: Boolean = true,
    val sound: String = "DEFAULT",
    
    // Sync
    val syncFrequencyMinutes: Int = 15,
    val syncDaysToKeep: Int = 30,
    val downloadAttachmentsWifiOnly: Boolean = true,
    val offlineMailEnabled: Boolean = true,
    
    // Security
    val requireAuthToOpen: Boolean = false,
    val blockExternalImages: Boolean = true,
    val confirmBeforeSending: Boolean = false,
    val useSecureLinkHandling: Boolean = true
)
