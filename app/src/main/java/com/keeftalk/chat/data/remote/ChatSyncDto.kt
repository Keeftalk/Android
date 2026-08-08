package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatMembershipDto(
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("joined_at") val joinedAt: Long = 0,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("role") val role: String = "member",
    @SerialName("notification_settings") val notificationSettings: JsonObject? = null,
    @SerialName("theme_id") val themeId: String? = null,
    @SerialName("last_read_message_id") val lastReadMessageId: String? = null,
    @SerialName("last_delivered_message_id") val lastDeliveredMessageId: String? = null,
    @SerialName("last_read_at") val lastReadAt: String? = null,
    @SerialName("last_delivered_at") val lastDeliveredAt: String? = null,
    val chats: ChatDto? = null
)

@Serializable
data class ChatDto(
    val id: String = "",
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val type: String = "",
    @SerialName("last_message_text") val lastMessage: String? = null,
    @SerialName("last_message_time") val lastMessageTime: String? = null,
    @SerialName("last_message_time_ms") val lastMessageTimeMs: Long? = null,
    @SerialName("last_message_status") val lastMessageStatus: String? = null,
    @SerialName("last_message_sender_id") val lastMessageSenderId: String? = null,
    @SerialName("last_message_id") val lastMessageId: String? = null,
    @SerialName("auto_delete_timer") val autoDeleteTimer: Long? = null,
    @SerialName("container_id") val containerId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("chat_members") val members: List<ChatMemberSyncDto> = emptyList(),
    @SerialName("auto_translate_enabled") val autoTranslateEnabled: Boolean = false
)

@Serializable
data class ChatMemberSyncDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("last_read_message_id") val lastReadMessageId: String? = null,
    @SerialName("last_delivered_message_id") val lastDeliveredMessageId: String? = null,
    @SerialName("last_read_at") val lastReadAt: String? = null,
    @SerialName("last_delivered_at") val lastDeliveredAt: String? = null,
    val profiles: Profile? = null
)
