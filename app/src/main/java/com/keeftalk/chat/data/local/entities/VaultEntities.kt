package com.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import com.keeftalk.chat.domain.model.VaultItem
import com.keeftalk.chat.domain.model.VaultFolder
import com.keeftalk.chat.domain.model.VaultTag
import com.keeftalk.chat.data.local.dao.VaultItemWithFile

@Serializable
@Entity(
    tableName = "vault_items",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_id"]
        ),
        ForeignKey(
            entity = VaultFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["file_id"]),
        Index(value = ["folder_id"]),
        Index(value = ["user_id"])
    ]
)
data class VaultItemEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") @ColumnInfo(name = "user_id") val userId: String,
    @SerialName("file_id") @ColumnInfo(name = "file_id") val fileId: String,
    @SerialName("folder_id") @ColumnInfo(name = "folder_id") val folderId: String?,
    val title: String,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    @SerialName("is_deleted") @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("deleted_at") @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)

fun VaultItemEntity.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("file_id", fileId)
    put("folder_id", folderId)
    put("title", title)
    put("created_at", TimestampSerializer.formatTimestamp(createdAt))
    put("favorite", favorite)
    put("locked", locked)
    put("metadata", Json.encodeToJsonElement(metadata))
    put("tags", Json.encodeToJsonElement(tags))
    put("is_deleted", isDeleted)
    put("deleted_at", deletedAt?.let { TimestampSerializer.formatTimestamp(it) })
}

fun VaultItemEntity.toDomain(file: com.keeftalk.chat.domain.model.File? = null) = com.keeftalk.chat.domain.model.VaultItem(
    id = id,
    userId = userId,
    file = file,
    folderId = folderId,
    title = title,
    createdAt = createdAt,
    favorite = favorite,
    locked = locked,
    metadata = metadata,
    tags = tags,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun com.keeftalk.chat.domain.model.VaultItem.toEntity() = VaultItemEntity(
    id = id,
    userId = userId,
    fileId = file?.id ?: "",
    folderId = folderId,
    title = title,
    createdAt = createdAt,
    favorite = favorite,
    locked = locked,
    metadata = metadata,
    tags = tags,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun com.keeftalk.chat.data.local.dao.VaultItemWithFile.toDomain() = item.toDomain(file.toDomain())

@Serializable
@Entity(tableName = "vault_folders")
data class VaultFolderEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") @ColumnInfo(name = "user_id") val userId: String = "",
    val name: String,
    val color: Int?,
    val icon: String?,
    @SerialName("parent_id") @ColumnInfo(name = "parent_id") val parentId: String?,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long
)

fun VaultFolderEntity.toSupabaseJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("user_id", userId)
    put("name", name)
    put("color", color)
    put("icon", icon)
    put("parent_id", parentId)
    put("created_at", TimestampSerializer.formatTimestamp(createdAt))
}

@Serializable
@Entity(tableName = "vault_tags")
data class VaultTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int
)

fun VaultFolderEntity.toDomain() = VaultFolder(
    id = id,
    userId = userId,
    name = name,
    color = color,
    icon = icon,
    parentId = parentId,
    createdAt = createdAt
)

fun VaultFolder.toEntity() = VaultFolderEntity(
    id = id,
    userId = userId,
    name = name,
    color = color,
    icon = icon,
    parentId = parentId,
    createdAt = createdAt
)

fun VaultTagEntity.toDomain() = VaultTag(
    id = id,
    name = name,
    color = color
)

fun VaultTag.toEntity() = VaultTagEntity(
    id = id,
    name = name,
    color = color
)
