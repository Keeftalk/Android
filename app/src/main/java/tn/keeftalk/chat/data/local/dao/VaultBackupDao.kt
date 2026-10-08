package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.VaultBackupItemEntity

@Dao
interface VaultBackupDao {
    @Query("SELECT * FROM vault_backup_items ORDER BY created_at DESC")
    fun getAllBackupItemsFlow(): Flow<List<VaultBackupItemEntity>>

    @Query("SELECT * FROM vault_backup_items WHERE folder_id = :folderId ORDER BY created_at DESC")
    fun getBackupItemsByFolderFlow(folderId: String): Flow<List<VaultBackupItemEntity>>

    @Query("SELECT * FROM vault_backup_items WHERE status IN ('PENDING', 'FAILED') ORDER BY created_at ASC")
    suspend fun getPendingAndFailedItems(): List<VaultBackupItemEntity>

    @Query("SELECT * FROM vault_backup_items WHERE local_path = :path LIMIT 1")
    suspend fun getByLocalPath(path: String): VaultBackupItemEntity?

    @Query("SELECT * FROM vault_backup_items WHERE file_hash = :hash LIMIT 1")
    suspend fun getByFileHash(hash: String): VaultBackupItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: VaultBackupItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<VaultBackupItemEntity>)

    @Update
    suspend fun updateItem(item: VaultBackupItemEntity)

    @Query("UPDATE vault_backup_items SET status = :status, error_message = :errorMessage, vault_item_id = :vaultItemId, backed_up_at = :backedUpAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, errorMessage: String? = null, vaultItemId: String? = null, backedUpAt: Long? = null)

    @Query("DELETE FROM vault_backup_items WHERE folder_id = :folderId")
    suspend fun deleteByFolderId(folderId: String)

    @Query("SELECT COUNT(*) FROM vault_backup_items WHERE status = 'SUCCESS'")
    fun getBackedUpCountFlow(): Flow<Int>

    @Query("SELECT SUM(file_size) FROM vault_backup_items WHERE status = 'SUCCESS'")
    fun getBackedUpBytesFlow(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM vault_backup_items WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_backup_items WHERE status = 'FAILED'")
    fun getFailedCountFlow(): Flow<Int>
}
