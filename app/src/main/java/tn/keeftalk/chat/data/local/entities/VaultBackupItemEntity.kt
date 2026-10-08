package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import tn.keeftalk.chat.domain.model.BackupStatus
import tn.keeftalk.chat.domain.model.VaultBackupItem
import java.util.UUID

@Entity(
    tableName = "vault_backup_items",
    indices = [
        Index(value = ["folder_id"]),
        Index(value = ["file_hash"]),
        Index(value = ["status"]),
        Index(value = ["local_path"], unique = true)
    ]
)
data class VaultBackupItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "folder_id") val folderId: String,
    @ColumnInfo(name = "local_path") val localPath: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "file_hash") val fileHash: String,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "vault_item_id") val vaultItemId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "backed_up_at") val backedUpAt: Long? = null
)

fun VaultBackupItemEntity.toDomain() = VaultBackupItem(
    id = id,
    folderId = folderId,
    localPath = localPath,
    fileName = fileName,
    fileSize = fileSize,
    lastModified = lastModified,
    fileHash = fileHash,
    mimeType = mimeType,
    status = try { BackupStatus.valueOf(status) } catch (_: Exception) { BackupStatus.PENDING },
    errorMessage = errorMessage,
    vaultItemId = vaultItemId,
    createdAt = createdAt,
    backedUpAt = backedUpAt
)

fun VaultBackupItem.toEntity() = VaultBackupItemEntity(
    id = id,
    folderId = folderId,
    localPath = localPath,
    fileName = fileName,
    fileSize = fileSize,
    lastModified = lastModified,
    fileHash = fileHash,
    mimeType = mimeType,
    status = status.name,
    errorMessage = errorMessage,
    vaultItemId = vaultItemId,
    createdAt = createdAt,
    backedUpAt = backedUpAt
)
