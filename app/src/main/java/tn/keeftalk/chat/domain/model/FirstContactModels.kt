package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FirstContactPolicy(
    val model: String = "ONE_MESSAGE",
    val maxMessagesBeforeReply: Int = 1,
    val maxFirstContactsPerDay: Int = 10,
    val cooldownSeconds: Int = 30,
    val allowText: Boolean = true,
    val allowImages: Boolean = false,
    val allowVideos: Boolean = false,
    val allowFiles: Boolean = false,
    val allowVoice: Boolean = false,
    val allowLinks: Boolean = false,
    val requireRecipientAcceptance: Boolean = false,
    val policyVersion: Int = 1
)

@Serializable
data class CanMessageResult(
    val allowed: Boolean,
    val reason: String? = null,
    val isNormalChat: Boolean = false,
    val canInitiateFirstContact: Boolean = false,
    val requestId: String? = null,
    val remainingMessages: Int? = null,
    val incomingRequest: Boolean = false
)

@Serializable
data class MessageRequest(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val conversationId: String,
    val status: String,
    val policyVersion: Int,
    val messagesSent: Int,
    val recipientReplied: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
