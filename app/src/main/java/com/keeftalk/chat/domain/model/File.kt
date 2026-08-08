package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class File(
    val id: String,
    val ownerId: String,
    val storagePath: String,
    val fileHash: String?,
    val fileName: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val fileType: FileType,
    val sourceType: SourceType,
    val width: Int?,
    val height: Int?,
    val duration: Int?,
    val thumbnailPath: String?,
    val localPath: String? = null,
    val encryptionMetadata: String?,
    val referenceCount: Int,
    val status: FileStatus,
    val securityMetadata: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

enum class FileType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
}

enum class SourceType {
    UPLOAD, CHAT, NOTE, AGENDA
}

enum class FileStatus {
    ACTIVE, PENDING_DELETE, DELETED
}
