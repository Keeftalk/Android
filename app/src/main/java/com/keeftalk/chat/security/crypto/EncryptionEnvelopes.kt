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
    val fileIv: String, // IV used for the physical file
    val encryptedAttributes: EncryptedObject? = null // Encrypted JSON of FileName, MimeType, etc.
)

@Serializable
data class FileAttributes(
    val fileName: String?,
    val mimeType: String?,
    val originalName: String? = null
)
