package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.AppNotification
import com.keeftalk.chat.domain.model.NotificationData
import com.keeftalk.chat.domain.model.NotificationPriority
import com.keeftalk.chat.domain.model.NotificationType
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val priority: String = "NORMAL",
    val title: String,
    val message: String,
    val data: JsonObject? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("nudge_count") val nudgeCount: Int = 1,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at") val createdAt: Long = 0
) {
    fun toDomain(): AppNotification {
        val dataObj = data?.let {
            NotificationData(
                chatId = it["chat_id"]?.jsonPrimitive?.contentOrNull,
                userId = it["user_id"]?.jsonPrimitive?.contentOrNull,
                contactName = it["contact_name"]?.jsonPrimitive?.contentOrNull,
                fullMessage = it["full_message"]?.jsonPrimitive?.contentOrNull,
                noteId = it["note_id"]?.jsonPrimitive?.contentOrNull
            )
        }
        
        return AppNotification(
            id = id,
            type = try { NotificationType.valueOf(type) } catch (e: Exception) { NotificationType.SYSTEM },
            priority = try { NotificationPriority.valueOf(priority) } catch (e: Exception) { NotificationPriority.NORMAL },
            title = title,
            message = message,
            timestamp = createdAt,
            isRead = isRead,
            data = dataObj,
            expiresAt = expiresAt,
            sourceId = sourceId,
            nudgeCount = nudgeCount
        )
    }
}
