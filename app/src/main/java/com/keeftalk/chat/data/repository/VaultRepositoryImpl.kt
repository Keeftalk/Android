package com.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import com.keeftalk.chat.data.local.dao.VaultDao
import com.keeftalk.chat.data.local.dao.VaultSyncQueueDao
import com.keeftalk.chat.data.local.entities.*
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.VaultRepository
import com.keeftalk.chat.util.NotesLogger
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes

class VaultRepositoryImpl(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val syncQueueDao: VaultSyncQueueDao,
    private val fileDao: com.keeftalk.chat.data.local.dao.FileDao,
    private val fileUploadManager: com.keeftalk.chat.util.FileUploadManager
) : VaultRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    init {
        scope.launch {
            while (isActive) {
                processSyncQueue()
                delay(1.minutes)
            }
        }
    }

    override fun getItems(parentId: String?): Flow<List<VaultItem>> {
        return vaultDao.getItems(parentId).map { entities -> 
            entities.map { decryptVaultItem(it.toDomain()) } 
        }
    }

    private suspend fun decryptVaultItem(item: VaultItem): VaultItem {
        val file = item.file ?: return item
        val metaJson = file.encryptionMetadata ?: return item
        
        return try {
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(metaJson)
            val fek = AppModule.provideFileRepository(context).getDecryptedFEK(file).getOrNull() ?: return item
            
            if (fileMeta.encryptedAttributes != null) {
                val decryptedBytes = StorageCryptoService.decrypt(fileMeta.encryptedAttributes, fek)
                val attributes = Json.decodeFromString<FileAttributes>(String(decryptedBytes, Charsets.UTF_8))
                item.copy(title = attributes.originalName ?: item.title)
            } else {
                item
            }
        } catch (e: Exception) {
            item
        }
    }

    override fun getFavoriteItems(): Flow<List<VaultItem>> {
        return vaultDao.getFavoriteItems().map { entities -> 
            entities.map { decryptVaultItem(it.toDomain()) } 
        }
    }

    override fun getFolders(parentId: String?): Flow<List<VaultFolder>> {
        return vaultDao.getFolders(parentId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTags(): Flow<List<VaultTag>> {
        return vaultDao.getTags().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTrashItems(): Flow<List<VaultItem>> {
        return vaultDao.getTrashItems().map { entities -> 
            entities.map { decryptVaultItem(it.toDomain()) } 
        }
    }

    override suspend fun downloadFile(item: VaultItem, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileModel = item.file ?: throw Exception("File data missing")
            val downloadManager = AppModule.provideFileDownloadManager(context)
            
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, item.title)
            
            downloadManager.downloadAndDecrypt(fileModel, targetFile, onProgress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDecryptedFile(item: VaultItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileModel = item.file ?: throw Exception("File data missing")
            val fileRepo = AppModule.provideFileRepository(context)
            fileRepo.ensureMediaLocal(fileModel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(file: File, parentId: String?, onProgress: (Float) -> Unit): Result<VaultItem> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
            
            val result = fileUploadManager.uploadFile(file, SourceType.UPLOAD, userId)
            
            if (result.isSuccess) {
                val uploadedFile = result.getOrThrow()
                val vaultItem = VaultItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    file = uploadedFile,
                    folderId = parentId,
                    title = "[Encrypted]"
                )
                
                vaultDao.insertItem(vaultItem.toEntity())
                
                // PART 4: Sync Vault record to cloud
                supabase.postgrest["vault_items"].upsert(vaultItem.toEntity())
                
                Result.success(decryptVaultItem(vaultItem))
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Upload failed"))
            }
        } catch (e: Exception) {
            NotesLogger.e("VAULT", "Upload failed", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun createFolder(name: String, parentId: String?, color: Int?, icon: String?): Result<VaultFolder> = withContext(Dispatchers.IO) {
        try {
            val folder = VaultFolder(
                id = UUID.randomUUID().toString(),
                name = name,
                parentId = parentId,
                color = color,
                icon = icon
            )
            vaultDao.insertFolder(folder.toEntity())
            getSupabase().postgrest["vault_folders"].upsert(folder.toEntity())
            Result.success(folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val updated = item.copy(favorite = !item.favorite)
            vaultDao.updateItem(updated)
            getSupabase().postgrest["vault_items"].update(mapOf("favorite" to updated.favorite)) { filter { eq("id", itemId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameItem(itemId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val updated = item.copy(title = newName)
            vaultDao.updateItem(updated)
            getSupabase().postgrest["vault_items"].update(mapOf("title" to newName)) { filter { eq("id", itemId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveItem(itemId: String, newParentId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val updated = item.copy(folderId = newParentId)
            vaultDao.updateItem(updated)
            getSupabase().postgrest["vault_items"].update(mapOf("folder_id" to newParentId)) { filter { eq("id", itemId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteItem(itemId: String, permanent: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            if (permanent) {
                vaultDao.deleteItem(item)
                getSupabase().postgrest["vault_items"].delete { filter { eq("id", itemId) } }
                val referenceManager = AppModule.provideFileReferenceManager(context)
                referenceManager.removeReference(item.fileId)
            } else {
                val updated = item.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
                vaultDao.updateItem(updated)
                getSupabase().postgrest["vault_items"].update(mapOf("is_deleted" to true, "deleted_at" to updated.deletedAt)) { filter { eq("id", itemId) } }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val updated = item.copy(isDeleted = false, deletedAt = null)
            vaultDao.updateItem(updated)
            getSupabase().postgrest["vault_items"].update(mapOf("is_deleted" to false, "deleted_at" to null)) { filter { eq("id", itemId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setLocked(itemId: String, locked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val updated = item.copy(locked = locked)
            vaultDao.updateItem(updated)
            getSupabase().postgrest["vault_items"].update(mapOf("locked" to locked)) { filter { eq("id", itemId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSharedItems(): Flow<List<VaultItem>> {
        return vaultDao.getAllItems().map { entities -> 
            entities.map { decryptVaultItem(it.toDomain()) }.filter { (it.metadata as Map<String, String>)["isShared"] == "true" }
        }
    }

    override suspend fun markItemAsShared(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val newMetadata = entity.metadata.toMutableMap().apply { put("isShared", "true") }
            val updated = entity.copy(metadata = newMetadata)
            vaultDao.updateItem(updated)
            getSupabase().postgrest["vault_items"].update(mapOf("metadata" to newMetadata)) { filter { eq("id", itemId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStorageInfo(): Flow<VaultStorageInfo> {
        return vaultDao.getAllItems().map { items ->
            val domainItems = items.map { it.toDomain() }
            val total = domainItems.sumOf { it.file?.fileSize ?: 0L }
            val count = domainItems.size
            val categories = domainItems.groupBy { it.file?.fileType ?: FileType.OTHER }.mapValues { it.value.sumOf { item -> item.file?.fileSize ?: 0L } }
            
            val vaultCategories = categories.mapKeys { 
                when(it.key) {
                    FileType.IMAGE -> VaultItemType.PHOTO
                    FileType.VIDEO -> VaultItemType.VIDEO
                    FileType.AUDIO -> VaultItemType.AUDIO
                    FileType.DOCUMENT -> VaultItemType.DOCUMENT
                    else -> VaultItemType.OTHER
                }
            }
            VaultStorageInfo(total, total, 0, count, vaultCategories)
        }
    }

    override suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            
            // 1. Sync Folders
            val remoteFolders = supabase.postgrest["vault_folders"].select { filter { eq("user_id", userId) } }.decodeList<VaultFolderEntity>()
            remoteFolders.forEach { vaultDao.insertFolder(it) }
            
            // 2. Sync Items (and their associated Files)
            val remoteItems = supabase.postgrest["vault_items"].select { 
                filter { eq("user_id", userId) } 
            }.decodeList<VaultItemEntity>()
            
            remoteItems.forEach { item ->
                // Fetch associated File record if missing
                val existingFile = fileDao.getFileById(item.fileId)
                if (existingFile == null) {
                    try {
                        val fileEntity = supabase.postgrest["files"].select { filter { eq("id", item.fileId) } }.decodeSingle<FileEntity>()
                        fileDao.insertFile(fileEntity)
                    } catch (e: Exception) {
                        Log.e("VAULT_SYNC", "Failed to sync file ${item.fileId} for item ${item.id}")
                    }
                }
                vaultDao.insertItem(item)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun shutdown() {
        NotesLogger.i("REPO", "Shutting down VaultRepository")
        scope.cancel()
    }

    private suspend fun processSyncQueue() = withContext(Dispatchers.IO) {
    }
}
