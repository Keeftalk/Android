package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class VaultItemWithFile(
    @Embedded val item: VaultItemEntity,
    @Relation(
        parentColumn = "file_id",
        entityColumn = "id",
    )
    val file: FileEntity
)

@Dao
interface VaultDao {
    @Transaction
    @Query("SELECT * FROM vault_items WHERE is_deleted = 0 AND folder_id IS :folderId ORDER BY created_at DESC")
    fun getItems(folderId: String?): Flow<List<VaultItemWithFile>>

    @Transaction
    @Query("SELECT * FROM vault_items WHERE is_deleted = 0 AND favorite = 1")
    fun getFavoriteItems(): Flow<List<VaultItemWithFile>>

    @Transaction
    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemById(id: String): VaultItemWithFile?

    @Transaction
    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getItemEntityById(id: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<VaultItemEntity>)

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Delete
    suspend fun deleteItem(item: VaultItemEntity)

    @Query("SELECT * FROM vault_folders WHERE parent_id IS :parentId")
    fun getFolders(parentId: String?): Flow<List<VaultFolderEntity>>

    @Query("SELECT * FROM vault_folders WHERE parent_id IS :parentId")
    suspend fun getFoldersOnce(parentId: String?): List<VaultFolderEntity>

    @Query("SELECT (SELECT COUNT(*) FROM vault_items WHERE folder_id = :folderId AND is_deleted = 0) + (SELECT COUNT(*) FROM vault_folders WHERE parent_id = :folderId)")
    suspend fun countTotalChildren(folderId: String): Int

    @Query("SELECT SUM(file_size) FROM files INNER JOIN vault_items ON files.id = vault_items.file_id WHERE vault_items.folder_id = :folderId AND vault_items.is_deleted = 0")
    suspend fun getFilesSizeInFolder(folderId: String): Long?

    @Query("SELECT * FROM vault_items WHERE folder_id = :folderId")
    suspend fun getItemsInFolderOnce(folderId: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_folders WHERE id = :id")
    suspend fun getFolderById(id: String): VaultFolderEntity?

    @Update
    suspend fun updateFolder(folder: VaultFolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: VaultFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: VaultFolderEntity)

    @Query("SELECT * FROM vault_tags")
    fun getTags(): Flow<List<VaultTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: VaultTagEntity)

    @Transaction
    @Query("SELECT * FROM vault_items WHERE is_deleted = 1")
    fun getTrashItems(): Flow<List<VaultItemWithFile>>

    @Transaction
    @Query("SELECT * FROM vault_items WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun getAllItems(): Flow<List<VaultItemWithFile>>

    @Query("SELECT SUM(file_size + IFNULL(thumbnail_size, 0)) FROM files INNER JOIN vault_items ON files.id = vault_items.file_id WHERE vault_items.is_deleted = 1")
    fun getTrashSizeFlow(): Flow<Long?>
}
