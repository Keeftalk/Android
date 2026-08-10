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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes

class VaultRepositoryImpl(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val syncQueueDao: VaultSyncQueueDao,
    private val fileDao: com.keeftalk.chat.data.local.dao.FileDao,
    private val fileUploadManager: com.keeftalk.chat.util.FileUploadManager,
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
            decryptItems(entities.map { it.toDomain() })
        }
    }

    private suspend fun decryptVaultItem(item: VaultItem): VaultItem {
        val file = item.file ?: return item
        val metaJson = file.encryptionMetadata ?: return item
        
        return try {
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(metaJson)
            val fek = AppModule.provideFileRepository(context).getDecryptedFEK(file, null).getOrNull() ?: return item
            
            if (fileMeta.encryptedAttributes != null) {
                val decryptedBytes = StorageCryptoService.decrypt(fileMeta.encryptedAttributes, fek)
                val attributes = Json.decodeFromString<FileAttributes>(String(decryptedBytes, Charsets.UTF_8))
                item.copy(title = attributes.originalName ?: item.title)
            } else {
                item
            }
        } catch (e: Exception) {
            Log.w("VAULT_PIPELINE", "DECRYPT_ITEM_TITLE_FAILED | itemId=${item.id} | error=${e.message}")
            item
        }
    }

    private suspend fun decryptItems(items: List<VaultItem>): List<VaultItem> {
        val result = mutableListOf<VaultItem>()
        for (item in items) {
            result.add(decryptVaultItem(item))
        }
        return result
    }

    override fun getFavoriteItems(): Flow<List<VaultItem>> {
        return vaultDao.getFavoriteItems().map { entities -> 
            decryptItems(entities.map { it.toDomain() })
        }
    }

    override fun getFolders(parentId: String?): Flow<List<VaultFolder>> {
        return combine(
            vaultDao.getFolders(parentId),
            vaultDao.getAllItems() 
        ) { folderEntities, _ ->
            folderEntities.map { entity ->
                // Direct child count (Premium Explorer style)
                val count = vaultDao.countTotalChildren(entity.id)
                // Recursive total size (User requirement)
                val size = calculateFolderSizeRecursive(entity.id)
                entity.toDomain().copy(itemCount = count, totalSize = size)
            }
        }
    }

    private suspend fun calculateFolderItemCountRecursive(folderId: String): Int {
        var total = vaultDao.getItemsInFolderOnce(folderId).size
        val subfolders = vaultDao.getFoldersOnce(folderId)
        total += subfolders.size // Count immediate subfolders as items too
        subfolders.forEach { sub ->
            total += calculateFolderItemCountRecursive(sub.id)
        }
        return total
    }

    private suspend fun calculateFolderSizeRecursive(folderId: String): Long {
        var total = vaultDao.getFilesSizeInFolder(folderId) ?: 0L
        val subfolders = vaultDao.getFoldersOnce(folderId)
        subfolders.forEach { sub ->
            total += calculateFolderSizeRecursive(sub.id)
        }
        return total
    }

    override fun getTags(): Flow<List<VaultTag>> {
        return vaultDao.getTags().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTrashItems(): Flow<List<VaultItem>> {
        return vaultDao.getTrashItems().map { entities -> 
            decryptItems(entities.map { it.toDomain() })
        }
    }

    override suspend fun downloadFile(item: VaultItem, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i("VAULT_PIPELINE", "DOWNLOAD_START | itemId=${item.id} | title=${item.title}")
            val fileModel = item.file ?: throw Exception("File data missing for item ${item.id}")
            val downloadManager = AppModule.provideFileDownloadManager(context)
            
            // 1. Download to private cache first
            val tempFile = File(context.cacheDir, "download_temp_${item.id}")
            val downloadResult = downloadManager.downloadAndDecrypt(fileModel, tempFile, onProgress)
            
            if (downloadResult.isSuccess) {
                val decryptedFile = downloadResult.getOrThrow()
                // 2. Export to public Downloads folder via MediaStore
                val publicUri = com.keeftalk.chat.util.StorageUtils.saveFileToPublicDownloads(
                    context = context,
                    sourceFile = decryptedFile,
                    displayName = item.title,
                    mimeType = fileModel.mimeType
                )
                
                if (publicUri != null) {
                    Log.i("VAULT_PIPELINE", "DOWNLOAD_EXPORT_SUCCESS | uri=$publicUri")
                    Result.success(decryptedFile)
                } else {
                    throw Exception("Failed to save file to public Downloads")
                }
            } else {
                val error = downloadResult.exceptionOrNull() ?: Exception("Decryption failed")
                Log.e("VAULT_PIPELINE", "DOWNLOAD_FAILED | itemId=${item.id} | message=${error.message}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e("VAULT_PIPELINE", "DOWNLOAD_ERROR | itemId=${item.id} | message=${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getDecryptedFile(item: VaultItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d("VAULT_PIPELINE", "GET_DECRYPTED_FILE_START | itemId=${item.id}")
            val fileModel = item.file ?: throw Exception("File data missing for item ${item.id}")
            val fileRepo = AppModule.provideFileRepository(context)
            fileRepo.ensureMediaLocal(fileModel, null)
        } catch (e: Exception) {
            Log.e("VAULT_PIPELINE", "GET_DECRYPTED_FILE_FAILED | itemId=${item.id} | message=${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(file: File, parentId: String?, onProgress: (Float) -> Unit): Result<VaultItem> = withContext(Dispatchers.IO) {
        try {
            Log.i("VAULT_PIPELINE", "START | fileName=${file.name} | parentId=$parentId")
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
            
            val result = fileUploadManager.uploadFile(file, SourceType.UPLOAD, userId)
            
            if (result.isSuccess) {
                val uploadedFile = result.getOrThrow()
                Log.d("VAULT_PIPELINE", "FILE_UPLOAD SUCCESS | fileId=${uploadedFile.id}")
                
                val vaultItem = VaultItem(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    file = uploadedFile,
                    folderId = parentId,
                    title = file.name
                )
                
                vaultDao.insertItem(vaultItem.toEntity())
                Log.d("VAULT_PIPELINE", "LOCAL_DB_INSERT SUCCESS | itemId=${vaultItem.id}")
                
                // PART 4: Sync Vault record to cloud
                val payload = Json.encodeToString(vaultItem.toEntity())
                syncQueueDao.insert(VaultSyncQueueEntity(itemId = vaultItem.id, operation = "SAVE", payload = payload))
                Log.i("VAULT_PIPELINE", "SYNC_QUEUE_INSERTED | itemId=${vaultItem.id}")
                
                scope.launch { processSyncQueue() }
                
                Result.success(decryptVaultItem(vaultItem))
            } else {
                val error = result.exceptionOrNull() ?: Exception("Upload failed")
                Log.e("VAULT_PIPELINE", "FAILED | stage=fileUpload | message=${error.message}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e("VAULT_PIPELINE", "FAILED | stage=uploadFile | message=${e.message}", e)
            NotesLogger.e("VAULT", "Upload failed", throwable = e)
            Result.failure(e)
        }
    }

    override suspend fun createFolder(name: String, parentId: String?, color: Int?, icon: String?): Result<VaultFolder> = withContext(Dispatchers.IO) {
        try {
            val userId = getSupabase().auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
            val folder = VaultFolder(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                parentId = parentId,
                color = color,
                icon = icon
            )
            vaultDao.insertFolder(folder.toEntity())
            val payload = Json.encodeToString(folder.toEntity())
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = folder.id, operation = "CREATE_FOLDER", payload = payload))
            scope.launch { processSyncQueue() }
            Result.success(folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFolder(folder: VaultFolder): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            vaultDao.updateFolder(folder.toEntity())
            val payload = Json.encodeToString(folder.toEntity())
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = folder.id, operation = "SAVE_FOLDER", payload = payload))
            scope.launch { processSyncQueue() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFolder(folderId: String): VaultFolder? = withContext(Dispatchers.IO) {
        vaultDao.getFolderById(folderId)?.toDomain()
    }

    override suspend fun renameFolder(folderId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val folder = vaultDao.getFolderById(folderId) ?: throw Exception("Folder not found")
            val updated = folder.copy(name = newName)
            vaultDao.updateFolder(updated)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = folderId, operation = "RENAME_FOLDER", payload = newName))
            scope.launch { processSyncQueue() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveFolder(folderId: String, newParentId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validation: Cannot move folder into itself or its descendants
            if (folderId == newParentId) throw Exception("Cannot move folder into itself")
            
            // Check if newParentId is a descendant of folderId
            var currentId = newParentId
            while (currentId != null) {
                if (currentId == folderId) throw Exception("Cannot move folder into its own descendant")
                currentId = vaultDao.getFolderById(currentId)?.parentId
            }

            val folder = vaultDao.getFolderById(folderId) ?: throw Exception("Folder not found")
            val updated = folder.copy(parentId = newParentId)
            vaultDao.updateFolder(updated)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = folderId, operation = "MOVE_FOLDER", payload = newParentId))
            scope.launch { processSyncQueue() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFolder(folderId: String, permanent: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Delete all items in this folder
            val items = vaultDao.getItemsInFolderOnce(folderId)
            items.forEach { deleteItem(it.id, permanent) }

            // 2. Delete all subfolders recursively
            val subfolders = vaultDao.getFoldersOnce(folderId)
            subfolders.forEach { deleteFolder(it.id, permanent) }

            // 3. Delete the folder itself
            val folder = vaultDao.getFolderById(folderId) ?: throw Exception("Folder not found")
            
            // Note: Since folders don't have isDeleted yet, we permanently delete the record
            // but the files inside will be in the Trash if permanent=false
            vaultDao.deleteFolder(folder)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = folderId, operation = "DELETE_FOLDER", payload = null))
            
            scope.launch { processSyncQueue() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val updated = item.copy(favorite = !item.favorite)
            vaultDao.updateItem(updated)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "FAVORITE", payload = updated.favorite.toString()))
            scope.launch { processSyncQueue() }
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
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "RENAME", payload = newName))
            scope.launch { processSyncQueue() }
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
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "MOVE", payload = newParentId))
            scope.launch { processSyncQueue() }
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
                syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "DELETE", payload = null))
                val referenceManager = AppModule.provideFileReferenceManager(context)
                referenceManager.removeReference(item.fileId)
            } else {
                val updated = item.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
                vaultDao.updateItem(updated)
                val payload = Json.encodeToString(updated)
                syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "SAVE", payload = payload))
            }
            scope.launch { processSyncQueue() }
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
            // Use SAVE operation to sync the state correctly with ISO timestamps
            val payload = Json.encodeToString(updated)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "SAVE", payload = payload))
            scope.launch { processSyncQueue() }
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
            val payload = Json.encodeToString(updated)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "SAVE", payload = payload))
            scope.launch { processSyncQueue() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSharedItems(): Flow<List<VaultItem>> {
        return vaultDao.getAllItems().map { entities -> 
            decryptItems(entities.map { it.toDomain() }).filter { it.metadata["isShared"] == "true" }
        }
    }

    override fun getAllItems(): Flow<List<VaultItem>> {
        return vaultDao.getAllItems().map { entities -> 
            decryptItems(entities.map { it.toDomain() })
        }
    }

    override suspend fun markItemAsShared(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = vaultDao.getItemEntityById(itemId) ?: throw Exception("Item not found")
            val newMetadata = entity.metadata.toMutableMap().apply { put("isShared", "true") }
            val updated = entity.copy(metadata = newMetadata)
            vaultDao.updateItem(updated)
            val payload = Json.encodeToString(updated)
            syncQueueDao.insert(VaultSyncQueueEntity(itemId = itemId, operation = "SAVE", payload = payload))
            scope.launch { processSyncQueue() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureThumbnail(item: VaultItem, chatId: String?): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val fileModel = item.file ?: return@withContext Result.failure(Exception("File missing"))
            
            // 1. Check local cache (decrypted)
            if (!fileModel.thumbnailLocalPath.isNullOrEmpty() && File(fileModel.thumbnailLocalPath).exists()) {
                return@withContext Result.success(fileModel.thumbnailLocalPath)
            }

            // 2. Try remote sync if path available
            if (!fileModel.thumbnailRemotePath.isNullOrEmpty()) {
                val fileRepo = AppModule.provideFileRepository(context)
                val result = fileRepo.ensureThumbnailLocal(fileModel, chatId)
                if (result.isSuccess) {
                    return@withContext Result.success(result.getOrThrow().absolutePath)
                }
            }

            // 3. DO NOT fall back to original media for Home previews
            // Logging suspicious state
            if (!fileModel.localPath.isNullOrEmpty() && File(fileModel.localPath).exists()) {
                 Log.w("VAULT_THUMB", "[VaultThumbnail] fileId=${item.file.id} | status=THUMB_MISSING_BUT_ORIGINAL_LOCAL | action=NONE_FALLBACK_PREVENTED")
            } else {
                 Log.d("VAULT_THUMB", "[VaultThumbnail] fileId=${item.file.id} | status=NOT_AVAILABLE")
            }

            Result.success(null)
        } catch (e: Exception) {
            Log.e("VAULT_THUMB", "[VaultThumbnail] fileId=${item.id} | action=ENSURE_FAILED", e)
            Result.failure(e)
        }
    }

    override suspend fun getStorageInfo(): Flow<VaultStorageInfo> {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return flowOf(
            VaultStorageInfo(0, VAULT_STORAGE_LIMIT, 0, 0, 0, emptyMap())
        )

        return combine(
            fileDao.getAccountCloudBytesFlow(userId),
            vaultDao.getTrashSizeFlow(),
            vaultDao.getAllItems()
        ) { cloudBytes, trashBytes, allItems ->
            val cloudTotal = cloudBytes ?: 0L
            val trashTotal = trashBytes ?: 0L
            val domainItems = allItems.map { it.toDomain() }
            val count = domainItems.size
            
            val categories = domainItems.groupBy { it.file?.fileType ?: FileType.OTHER }
                .mapValues { entry -> 
                    entry.value.sumOf { item -> 
                        (item.file?.fileSize ?: 0L) + (item.file?.thumbnailSize ?: 0L)
                    } 
                }

            val vaultCategories = categories.mapKeys { entry ->
                when(entry.key) {
                    FileType.IMAGE -> VaultItemType.PHOTO
                    FileType.VIDEO -> VaultItemType.VIDEO
                    FileType.AUDIO -> VaultItemType.AUDIO
                    FileType.DOCUMENT -> VaultItemType.DOCUMENT
                    else -> VaultItemType.OTHER
                }
            }

            VaultStorageInfo(
                cloudBytesUsed = cloudTotal,
                cloudBytesLimit = VAULT_STORAGE_LIMIT,
                localCacheBytesUsed = getLocalCacheSize(),
                trashBytesUsed = trashTotal,
                itemsCount = count,
                categories = vaultCategories
            )
        }
    }

    private fun getLocalCacheSize(): Long {
        var total = 0L
        
        // Decrypted media files
        val mediaDir = File(context.filesDir, "media")
        if (mediaDir.exists()) {
            total += mediaDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        
        // Decrypted thumbnails
        val thumbCacheDir = File(context.cacheDir, "thumbnails")
        if (thumbCacheDir.exists()) {
            total += thumbCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        
        return total
    }

    override suspend fun clearLocalCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val mediaDir = File(context.filesDir, "media")
            if (mediaDir.exists()) mediaDir.deleteRecursively()
            
            val thumbCacheDir = File(context.cacheDir, "thumbnails")
            if (thumbCacheDir.exists()) thumbCacheDir.deleteRecursively()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            
            // 0. Process outgoing sync queue
            processSyncQueue()

            // 1. Sync Folders
            val remoteFolders = supabase.postgrest["vault_folders"].select { filter { eq("user_id", userId) } }.decodeList<VaultFolderEntity>()
            if (remoteFolders.isNotEmpty()) {
                remoteFolders.forEach { vaultDao.insertFolder(it) }
            }
            
            // 2. Sync Items (and their associated Files)
            val remoteItems = supabase.postgrest["vault_items"].select { 
                filter { eq("user_id", userId) } 
            }.decodeList<VaultItemEntity>()
            
            if (remoteItems.isNotEmpty()) {
                val fileIdsToFetch = remoteItems.asSequence().map { it.fileId }.toSet()
                val existingFileIds = fileDao.getAllFileIds().toSet()
                val missingFileIds = fileIdsToFetch - existingFileIds
                
                if (missingFileIds.isNotEmpty()) {
                    // Fetch missing files in batches to avoid Supabase/Postgrest limits
                    missingFileIds.chunked(100).forEach { batch ->
                        try {
                            val missingFiles = supabase.postgrest["files"].select { 
                                filter { isIn("id", batch) } 
                            }.decodeList<FileEntity>()
                            fileDao.insertFiles(missingFiles)
                        } catch (e: Exception) {
                            Log.e("VAULT_SYNC", "Failed to sync file batch: ${e.message}")
                        }
                    }
                }
                
                // Bulk insert items
                vaultDao.insertItems(remoteItems)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VAULT_SYNC", "Sync failed", e)
            Result.failure(e)
        }
    }

    override fun shutdown() {
        NotesLogger.i("REPO", "Shutting down VaultRepository")
        scope.cancel()
    }

    private suspend fun processSyncQueue() = withContext(Dispatchers.IO) {
        val pending = syncQueueDao.getPendingItems()
        if (pending.isEmpty()) return@withContext
        
        Log.i("VAULT_SYNC", "Starting sync queue processing. Pending items: ${pending.size}")
        val supabase = getSupabase()
        pending.forEach { item ->
            try {
                Log.d("VAULT_SYNC", "Processing item: id=${item.id} | operation=${item.operation} | itemId=${item.itemId}")
                when (item.operation) {
                    "SAVE" -> {
                        val entity = Json.decodeFromString<VaultItemEntity>(item.payload!!)
                        supabase.postgrest["vault_items"].upsert(entity.toSupabaseJson())
                    }
                    "DELETE" -> supabase.postgrest["vault_items"].delete { filter { eq("id", item.itemId) } }
                    "FAVORITE" -> {
                        val favorite = item.payload?.toBoolean() ?: false
                        supabase.postgrest["vault_items"].update(mapOf("favorite" to favorite)) { filter { eq("id", item.itemId) } }
                    }
                    "RENAME" -> {
                        supabase.postgrest["vault_items"].update(mapOf("title" to item.payload!!)) { filter { eq("id", item.itemId) } }
                    }
                    "MOVE" -> {
                        supabase.postgrest["vault_items"].update(mapOf("folder_id" to item.payload)) { filter { eq("id", item.itemId) } }
                    }
                    "CREATE_FOLDER" -> {
                        val entity = Json.decodeFromString<VaultFolderEntity>(item.payload!!)
                        supabase.postgrest["vault_folders"].upsert(entity.toSupabaseJson())
                    }
                    "RENAME_FOLDER" -> {
                        supabase.postgrest["vault_folders"].update(mapOf("name" to item.payload!!)) { filter { eq("id", item.itemId) } }
                    }
                    "SAVE_FOLDER" -> {
                        val entity = Json.decodeFromString<VaultFolderEntity>(item.payload!!)
                        supabase.postgrest["vault_folders"].upsert(entity.toSupabaseJson())
                    }
                    "MOVE_FOLDER" -> {
                        supabase.postgrest["vault_folders"].update(mapOf("parent_id" to item.payload)) { filter { eq("id", item.itemId) } }
                    }
                    "DELETE_FOLDER" -> {
                        supabase.postgrest["vault_folders"].delete { filter { eq("id", item.itemId) } }
                    }
                }
                syncQueueDao.delete(item)
                Log.i("VAULT_SYNC", "Item sync SUCCESS: id=${item.id}")
            } catch (e: Exception) {
                Log.e("VAULT_SYNC", "Item sync FAILED: id=${item.id} | message=${e.message}", e)
                if (item.retryCount < 5) syncQueueDao.update(item.copy(retryCount = item.retryCount + 1))
                else syncQueueDao.update(item.copy(status = "FAILED"))
            }
        }
    }
}
