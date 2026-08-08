package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class VaultItemWithFile(
    @Embedded val item: VaultItemEntity,
    @Relation(
        parentColumn = "file_id",
        entityColumn = "id"
    )
    val file: FileEntity
)

@Dao
interface VaultDao {
    @Transaction
    @Query("SELECT * FROM vault_items WHERE is_deleted = 0 AND folder_id IS :folderId")
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

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Delete
    suspend fun deleteItem(item: VaultItemEntity)

    @Query("SELECT * FROM vault_folders WHERE parent_id IS :parentId")
    fun getFolders(parentId: String?): Flow<List<VaultFolderEntity>>

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
    @Query("SELECT * FROM vault_items WHERE is_deleted = 0")
    fun getAllItems(): Flow<List<VaultItemWithFile>>
}
