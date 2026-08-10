package com.keeftalk.chat.security.crypto

import kotlinx.serialization.Serializable

/**
 * Represents a wrapped key for a specific recipient or context.
 */
@Serializable
data class EncryptionEnvelope(
    val recipientId: String, // userId, chatId, or "OWNER"
    val wrappedKey: EncryptedObject,
    val type: EnvelopeType
)

enum class EnvelopeType {
    OWNER,      // Wrapped by user's FileProtectionKey
    CONVERSATION // Wrapped by a Per-Conversation Key
}

/**
 * Unified metadata block for files, containing encrypted attributes and key envelopes.
 */
@Serializable
data class FileEncryptionMetadata(
    val envelopes: List<EncryptionEnvelope>,
    val fileIv: String, // IV used for the physical file (BaseIV for v2)
    val thumbnailIv: String? = null, // Mandatory unique IV for thumbnail
    val encryptedAttributes: EncryptedObject? = null, // Encrypted JSON of FileName, MimeType, etc.
    val cryptoVersion: Int = 1,
    val chunkSize: Int? = null,
    val plaintextSize: Long? = null,
    val thumbnailPlaintextSize: Long? = null
)

@Serializable
data class FileAttributes(
    val fileName: String?,
    val mimeType: String?,
    val originalName: String? = null
)
