package com.keeftalk.chat.data.remote

import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MessageDto(
    val id: String = "",
    @SerialName("chat_id")
    val chatId: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    val content: String = "",
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("timestamp")
    val timestampMs: Long = 0,
    val type: String = "",
    val status: String = "",
    val metadata: JsonObject? = null,
    @SerialName("message_reactions")
    val reactions: List<MessageReactionDto> = emptyList(),
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    @SerialName("source_language")
    val sourceLanguage: String? = null,
    @SerialName("media_locked")
    val mediaLocked: Boolean = false,
    @SerialName("media_lock_updated_at")
    val mediaLockUpdatedAt: Long? = null,
    @SerialName("media_lock_updated_by")
    val mediaLockUpdatedBy: String? = null,
    val ciphertext: String? = null,
    @SerialName("crypto_version")
    val cryptoVersion: Int = 1,
    @SerialName("envelope_type")
    val envelopeType: Int = 100,
    val nonce: String? = null,
    @SerialName("message_attachment")
    val attachments: List<MessageAttachmentDto> = emptyList()
)

@Serializable
data class MessageAttachmentDto(
    val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("file_id") val fileId: String,
    val files: FileDto? = null
)

@Serializable
data class FileDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("file_hash") val fileHash: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("encryption_metadata") val encryptionMetadata: String? = null
)

@Serializable
data class TypingBroadcast(
    val chatId: String,
    val userId: String,
    val isTyping: Boolean
)
