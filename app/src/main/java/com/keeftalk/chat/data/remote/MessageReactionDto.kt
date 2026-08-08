package com.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("message_id")
    val messageId: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("emoji")
    val emoji: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)
