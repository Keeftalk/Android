package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keeftalk.chat.domain.model.AppNotification
import com.keeftalk.chat.domain.model.NotificationData
import com.keeftalk.chat.domain.model.NotificationPriority
import com.keeftalk.chat.domain.model.NotificationType

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val priority: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val chatId: String? = null,
    val userId: String? = null,
    val contactName: String? = null,
    val fullMessage: String? = null,
    val noteId: String? = null,
    val expiresAt: Long? = null,
    val sourceId: String? = null,
    val imageUrl: String? = null,
    val actionText: String? = null,
    val actionUrl: String? = null
) {
    fun toDomain(): AppNotification {
        return AppNotification(
            id = id,
            type = NotificationType.valueOf(type),
            priority = NotificationPriority.valueOf(priority),
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = isRead,
            data = NotificationData(
                chatId = chatId,
                userId = userId,
                contactName = contactName,
                fullMessage = fullMessage,
                noteId = noteId
            ),
            expiresAt = expiresAt,
            sourceId = sourceId,
            imageUrl = imageUrl,
            actionText = actionText,
            actionUrl = actionUrl
        )
    }

    companion object {
        fun fromDomain(notification: AppNotification): NotificationEntity {
            return NotificationEntity(
                id = notification.id,
                type = notification.type.name,
                priority = notification.priority.name,
                title = notification.title,
                message = notification.message,
                timestamp = notification.timestamp,
                isRead = notification.isRead,
                chatId = notification.data?.chatId,
                userId = notification.data?.userId,
                contactName = notification.data?.contactName,
                fullMessage = notification.data?.fullMessage,
                noteId = notification.data?.noteId,
                expiresAt = notification.expiresAt,
                sourceId = notification.sourceId,
                imageUrl = notification.imageUrl,
                actionText = notification.actionText,
                actionUrl = notification.actionUrl
            )
        }
    }
}
