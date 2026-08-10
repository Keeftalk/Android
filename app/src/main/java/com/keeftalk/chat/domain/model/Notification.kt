package com.keeftalk.chat.domain.model

enum class NotificationType {
    MESSAGE,
    CALL,
    SYSTEM,
    PROFILE_VIEW,
    CONTACT_JOINED,
    BROADCAST,
    REMINDER,
    NOTE_SHARE,
    NUDGE
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val data: NotificationData? = null,
    val expiresAt: Long? = null,
    val sourceId: String? = null,
    val nudgeCount: Int = 1,
    val imageUrl: String? = null,
    val actionText: String? = null,
    val actionUrl: String? = null
)

data class NotificationData(
    val chatId: String? = null,
    val userId: String? = null,
    val contactName: String? = null,
    val fullMessage: String? = null,
    val noteId: String? = null
)
