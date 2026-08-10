package com.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

@Serializable
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val id: String,
    @SerialName("owner_id") @ColumnInfo(name = "owner_id") val ownerId: String,
    @SerialName("storage_path") @ColumnInfo(name = "storage_path") val storagePath: String,
    @SerialName("file_hash") @ColumnInfo(name = "file_hash") val fileHash: String?,
    @SerialName("file_name") @ColumnInfo(name = "file_name") val fileName: String?,
    @SerialName("mime_type") @ColumnInfo(name = "mime_type") val mimeType: String?,
    @SerialName("file_size") @ColumnInfo(name = "file_size") val fileSize: Long?,
    @SerialName("file_type") @ColumnInfo(name = "file_type") val fileType: String?,
    @SerialName("source_type") @ColumnInfo(name = "source_type") val sourceType: String,
    @SerialName("width") val width: Int?,
    @SerialName("height") val height: Int?,
    @SerialName("duration") val duration: Int?,
    @SerialName("thumbnail_path") @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
    @SerialName("thumbnail_remote_path") @ColumnInfo(name = "thumbnail_remote_path") val thumbnailRemotePath: String? = null,
    @SerialName("thumbnail_local_path") @ColumnInfo(name = "thumbnail_local_path") val thumbnailLocalPath: String? = null,
    @SerialName("thumbnail_size") @ColumnInfo(name = "thumbnail_size") val thumbnailSize: Long? = null,
    @SerialName("thumbnail_width") @ColumnInfo(name = "thumbnail_width") val thumbnailWidth: Int? = null,
    @SerialName("thumbnail_height") @ColumnInfo(name = "thumbnail_height") val thumbnailHeight: Int? = null,
    @SerialName("thumbnail_hmac") @ColumnInfo(name = "thumbnail_hmac") val thumbnailHmac: String? = null,
    @Transient @ColumnInfo(name = "local_path") val localPath: String? = null,
    @SerialName("encryption_metadata") @ColumnInfo(name = "encryption_metadata") val encryptionMetadata: JsonElement?,
    @SerialName("reference_count") @ColumnInfo(name = "reference_count") val referenceCount: Int = 0,
    @SerialName("status") val status: String = "ACTIVE",
    @SerialName("security_metadata") @ColumnInfo(name = "security_metadata") val securityMetadata: String?,
    @Serializable(with = TimestampSerializer::class) @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @Serializable(with = TimestampSerializer::class) @SerialName("updated_at") @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @Serializable(with = TimestampSerializer::class) @SerialName("deleted_at") @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)

fun FileEntity.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("owner_id", ownerId)
    put("storage_path", storagePath)
    put("file_hash", fileHash)
    put("file_name", fileName)
    put("mime_type", mimeType)
    put("file_size", fileSize)
    put("file_type", fileType)
    put("source_type", sourceType)
    put("width", width)
    put("height", height)
    put("duration", duration)
    put("thumbnail_path", thumbnailPath)
    put("thumbnail_remote_path", thumbnailRemotePath)
    put("thumbnail_size", thumbnailSize)
    put("thumbnail_width", thumbnailWidth)
    put("thumbnail_height", thumbnailHeight)
    put("thumbnail_hmac", thumbnailHmac)
    put("encryption_metadata", encryptionMetadata ?: JsonNull)
    put("reference_count", referenceCount)
    put("status", status)
    put("security_metadata", securityMetadata)
    put("created_at", TimestampSerializer.formatTimestamp(createdAt))
    put("updated_at", TimestampSerializer.formatTimestamp(updatedAt))
    put("deleted_at", deletedAt?.let { TimestampSerializer.formatTimestamp(it) })
}

fun FileEntity.toDomain() = com.keeftalk.chat.domain.model.File(
    id = id,
    ownerId = ownerId,
    storagePath = storagePath,
    fileHash = fileHash,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    fileType = try { com.keeftalk.chat.domain.model.FileType.valueOf(fileType ?: "OTHER") } catch(e: Exception) { com.keeftalk.chat.domain.model.FileType.OTHER },
    sourceType = try { com.keeftalk.chat.domain.model.SourceType.valueOf(sourceType) } catch(e: Exception) { com.keeftalk.chat.domain.model.SourceType.UPLOAD },
    width = width,
    height = height,
    duration = duration,
    thumbnailPath = thumbnailPath,
    thumbnailRemotePath = thumbnailRemotePath,
    thumbnailLocalPath = thumbnailLocalPath,
    thumbnailSize = thumbnailSize,
    thumbnailWidth = thumbnailWidth,
    thumbnailHeight = thumbnailHeight,
    thumbnailHmac = thumbnailHmac,
    localPath = localPath,
    encryptionMetadata = encryptionMetadata?.let { Json.encodeToString(it) },
    referenceCount = referenceCount,
    status = try { com.keeftalk.chat.domain.model.FileStatus.valueOf(status) } catch(e: Exception) { com.keeftalk.chat.domain.model.FileStatus.ACTIVE },
    securityMetadata = securityMetadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun com.keeftalk.chat.domain.model.File.toEntity() = FileEntity(
    id = id,
    ownerId = ownerId,
    storagePath = storagePath,
    fileHash = fileHash,
    fileName = fileName,
    mimeType = mimeType,
    fileSize = fileSize,
    fileType = fileType.name,
    sourceType = sourceType.name,
    width = width,
    height = height,
    duration = duration,
    thumbnailPath = thumbnailPath,
    thumbnailRemotePath = thumbnailRemotePath,
    thumbnailLocalPath = thumbnailLocalPath,
    thumbnailSize = thumbnailSize,
    thumbnailWidth = thumbnailWidth,
    thumbnailHeight = thumbnailHeight,
    thumbnailHmac = thumbnailHmac,
    localPath = localPath,
    encryptionMetadata = encryptionMetadata?.let { Json.parseToJsonElement(it) },
    referenceCount = referenceCount,
    status = status.name,
    securityMetadata = securityMetadata,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
