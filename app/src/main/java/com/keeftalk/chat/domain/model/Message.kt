package com.keeftalk.chat.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val type: MessageType,
    val attachments: List<File> = emptyList(),
    val reactions: List<MessageReaction> = emptyList(),
    val replyToId: String? = null,
    val replyTo: Message? = null,
    val translatedContent: String? = null,
    val sourceLanguage: String? = null,
    val mediaLocked: Boolean = false,
    val mediaLockUpdatedAt: Long? = null,
    val mediaLockUpdatedBy: String? = null,
    val cryptoVersion: Int = 0,
    val decryptionState: DecryptionState = DecryptionState.SUCCESS
) {
    val fileUrl: String? get() = attachments.firstOrNull()?.storagePath
    val fileName: String? get() = attachments.firstOrNull()?.fileName
    val fileSize: Long? get() = attachments.firstOrNull()?.fileSize
    val localFilePath: String? get() = attachments.firstOrNull()?.localPath
    val thumbnailUrl: String? get() = attachments.firstOrNull()?.thumbnailLocalPath ?: attachments.firstOrNull()?.thumbnailRemotePath
    val width: Int? get() = attachments.firstOrNull()?.width
    val height: Int? get() = attachments.firstOrNull()?.height

    val effectiveFileType: FileType? get() = attachments.firstOrNull()?.fileType
}

@Stable
@Immutable
data class MessageReaction(
    val userId: String,
    val emoji: String
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, FCM_RECEIVED, SEEN, FAILED
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE, FILE, PDF, LOCATION, CONTACT, POLL, CALL_LOG, SHARED_NOTE,
    SHARED_EMAIL, SHARED_VAULT_FILE, SHARED_AGENDA
}

enum class DecryptionState {
    SUCCESS, PENDING, RETRY_REQUIRED, PERMANENT_FAILURE
}
