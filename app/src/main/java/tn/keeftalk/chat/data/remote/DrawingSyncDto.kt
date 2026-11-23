package tn.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tn.keeftalk.chat.data.local.entities.DrawingEntity

@Serializable
data class DrawingSyncDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    @SerialName("canvas_width") val canvasWidth: Int = 1080,
    @SerialName("canvas_height") val canvasHeight: Int = 1920,
    @SerialName("background_color") val backgroundColorHex: String = "#FFFFFF",
    @SerialName("format_version") val formatVersion: Int = 1,
    @SerialName("content_json") val contentJson: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("content_hash") val contentHash: String? = null,
    val revision: Long = 1L,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(localNativeFilePath: String = "", localThumbnailPath: String = ""): DrawingEntity {
        return DrawingEntity(
            id = id,
            ownerUserId = userId ?: "local",
            title = title,
            nativeFilePath = localNativeFilePath,
            thumbnailPath = localThumbnailPath,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            backgroundColorHex = backgroundColorHex,
            formatVersion = formatVersion,
            contentHash = contentHash,
            revision = revision,
            isDeleted = isDeleted,
            syncStatus = "SYNCED",
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: DrawingEntity, userId: String, contentJson: String? = null, thumbnailUrl: String? = null): DrawingSyncDto {
            return DrawingSyncDto(
                id = entity.id,
                userId = userId,
                title = entity.title,
                canvasWidth = entity.canvasWidth,
                canvasHeight = entity.canvasHeight,
                backgroundColorHex = entity.backgroundColorHex,
                formatVersion = entity.formatVersion,
                contentJson = contentJson,
                thumbnailUrl = thumbnailUrl,
                contentHash = entity.contentHash,
                revision = entity.revision,
                isDeleted = entity.isDeleted,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
