package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "drawings",
    indices = [
        Index(value = ["is_deleted"]),
        Index(value = ["updated_at"])
    ]
)
data class DrawingEntity(
    @PrimaryKey val id: String,
    @SerialName("owner_user_id") @ColumnInfo(name = "owner_user_id") val ownerUserId: String = "local",
    val title: String,
    @SerialName("native_file_path") @ColumnInfo(name = "native_file_path") val nativeFilePath: String = "",
    @SerialName("thumbnail_path") @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String = "",
    @SerialName("canvas_width") @ColumnInfo(name = "canvas_width") val canvasWidth: Int = 1080,
    @SerialName("canvas_height") @ColumnInfo(name = "canvas_height") val canvasHeight: Int = 1920,
    @SerialName("background_color") @ColumnInfo(name = "background_color") val backgroundColorHex: String = "#FFFFFF",
    @SerialName("format_version") @ColumnInfo(name = "format_version") val formatVersion: Int = 1,
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String? = null,
    @ColumnInfo(name = "revision") val revision: Long = 1L,
    @SerialName("is_deleted") @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @SerialName("sync_status") @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "drawing_sync_queue")
data class DrawingSyncQueueEntity(
    @PrimaryKey val id: String,
    val action: String, // INSERT, UPDATE, DELETE, UPSERT
    @SerialName("entity_type") @ColumnInfo(name = "entity_type") val entityType: String = "DRAWING",
    @SerialName("payload_json") @ColumnInfo(name = "payload_json") val payloadJson: String,
    val status: String = "PENDING",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
