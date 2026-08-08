package com.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    ]
)
data class VaultItemEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") @ColumnInfo(name = "user_id") val userId: String,
    @SerialName("file_id") @ColumnInfo(name = "file_id") val fileId: String,
    @SerialName("folder_id") @ColumnInfo(name = "folder_id") val folderId: String?,
    val title: String,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    @SerialName("is_deleted") @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @SerialName("deleted_at") @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)

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
    val name: String,
    val color: Int?,
    val icon: String?,
    @SerialName("parent_id") @ColumnInfo(name = "parent_id") val parentId: String?,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long
)

@Serializable
@Entity(tableName = "vault_tags")
data class VaultTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int
)

fun VaultFolderEntity.toDomain() = VaultFolder(
    id = id,
    name = name,
    color = color,
    icon = icon,
    parentId = parentId,
    createdAt = createdAt
)

fun VaultFolder.toEntity() = VaultFolderEntity(
    id = id,
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
