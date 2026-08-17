package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VaultRepository {
    fun getItems(parentId: String? = null): Flow<List<VaultItem>>
    fun getFavoriteItems(): Flow<List<VaultItem>>
    fun getFolders(parentId: String? = null): Flow<List<VaultFolder>>
    fun getTags(): Flow<List<VaultTag>>
    fun getTrashItems(): Flow<List<VaultItem>>
    fun getSharedItems(): Flow<List<VaultItem>>
    suspend fun markItemAsShared(itemId: String): Result<Unit>

    val activeUploads: Flow<Map<String, UploadProgress>>
    fun cancelUpload(id: String)

    suspend fun downloadFile(item: VaultItem, onProgress: (Float) -> Unit = {}): Result<File>
    suspend fun getDecryptedFile(item: VaultItem): Result<File>

    suspend fun uploadFile(file: File, parentId: String? = null, onProgress: (Long, Long) -> Unit): Result<VaultItem>
    suspend fun createFolder(name: String, parentId: String? = null, color: Int? = null, icon: String? = null): Result<VaultFolder>
    suspend fun updateFolder(folder: VaultFolder): Result<Unit>
    suspend fun getFolder(folderId: String): VaultFolder?
    suspend fun toggleFavorite(itemId: String): Result<Unit>
    suspend fun renameItem(itemId: String, newName: String): Result<Unit>
    suspend fun renameFolder(folderId: String, newName: String): Result<Unit>
    suspend fun moveItem(itemId: String, newParentId: String?): Result<Unit>
    suspend fun moveFolder(folderId: String, newParentId: String?): Result<Unit>
    suspend fun deleteItem(itemId: String, permanent: Boolean = false): Result<Unit>
    suspend fun deleteFolder(folderId: String, permanent: Boolean = false): Result<Unit>
    suspend fun restoreItem(itemId: String): Result<Unit>
    suspend fun setLocked(itemId: String, locked: Boolean): Result<Unit>

    fun getAllItems(): Flow<List<VaultItem>>

    suspend fun ensureThumbnail(item: VaultItem, chatId: String? = null): Result<String?>

    suspend fun getStorageInfo(): Flow<VaultStorageInfo>
    suspend fun clearLocalCache(): Result<Unit>
    
    suspend fun sync(): Result<Unit>
    fun shutdown()
}
