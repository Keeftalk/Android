package com.keeftalk.chat.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import java.util.Base64
import com.keeftalk.chat.data.local.dao.FileDao
import com.keeftalk.chat.data.local.entities.FileEntity
import com.keeftalk.chat.data.local.entities.toDomain
import com.keeftalk.chat.data.local.entities.toEntity
import com.keeftalk.chat.data.local.entities.toSupabaseJson
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.File
import com.keeftalk.chat.domain.model.FileStatus
import com.keeftalk.chat.domain.repository.FileRepository
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.*
import javax.crypto.SecretKey

class FileRepositoryImpl(
    private val context: Context,
    private val fileDao: FileDao,
) : FileRepository {

    private suspend fun getSupabase() = AppModule.provideSupabaseClientAsync(context)

    override suspend fun saveFile(file: File) {
        withContext(Dispatchers.IO) {
            val entity = file.toEntity()
            fileDao.insertFile(entity)
            try {
                getSupabase().postgrest["files"].upsert(entity.toSupabaseJson())
            } catch (e: Exception) {
                Log.e("FileRepositoryImpl", "Upsert failed for ${entity.id}: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun getFileById(id: String): File? = withContext(Dispatchers.IO) {
        fileDao.getFileById(id)?.toDomain()
    }

    override suspend fun getFileByHash(hash: String): File? = withContext(Dispatchers.IO) {
        fileDao.getFileByHash(hash)?.toDomain()
    }

    override fun getAllActiveFiles(): Flow<List<File>> {
        return fileDao.getAllActiveFiles().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getFilesPendingDelete(): List<File> = withContext(Dispatchers.IO) {
        fileDao.getFilesPendingDelete().map { it.toDomain() }
    }

    override suspend fun updateFileStatus(fileId: String, status: FileStatus) {
        withContext(Dispatchers.IO) {
            val deletedAt = if (status == FileStatus.DELETED) System.currentTimeMillis() else null
            fileDao.updateFileStatus(fileId, status.name, deletedAt)
            try {
                getSupabase().postgrest["files"].update(mapOf("status" to status.name, "deleted_at" to deletedAt)) {
                    filter { eq("id", fileId) }
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun incrementReferenceCount(fileId: String) {
        withContext(Dispatchers.IO) {
            fileDao.incrementReferenceCount(fileId)
            try {
                fileDao.getFileById(fileId)?.let { entity ->
                    getSupabase().postgrest["files"].update(mapOf("reference_count" to entity.referenceCount)) {
                        filter { eq("id", fileId) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun decrementReferenceCount(fileId: String) {
        withContext(Dispatchers.IO) {
            fileDao.decrementReferenceCount(fileId)
            val file = fileDao.getFileById(fileId)
            if (file != null) {
                try {
                    getSupabase().postgrest["files"].update(mapOf("reference_count" to file.referenceCount)) {
                        filter { eq("id", fileId) }
                    }
                } catch (e: Exception) {}
                
                if (file.referenceCount <= 0) {
                    updateFileStatus(fileId, FileStatus.PENDING_DELETE)
                }
            }
        }
    }

    override suspend fun deleteFilePermanently(fileId: String) {
        withContext(Dispatchers.IO) {
            val entity = fileDao.getFileById(fileId)
            if (entity != null) {
                fileDao.deleteFile(entity)
                try {
                    getSupabase().postgrest["files"].delete { filter { eq("id", fileId) } }
                } catch (e: Exception) {}
            }
        }
    }

    override suspend fun reconcileReferenceCounts() {
        withContext(Dispatchers.IO) {
            val fileIds = fileDao.getAllFileIds()
            fileIds.forEach { fileId ->
                val total = fileDao.countMessageAttachments(fileId) +
                            fileDao.countNoteAttachments(fileId) +
                            fileDao.countAgendaAttachments(fileId) +
                            fileDao.countVaultItems(fileId)
                
                fileDao.updateReferenceCount(fileId, total)
                
                if (total <= 0) {
                    updateFileStatus(fileId, FileStatus.PENDING_DELETE)
                } else {
                    updateFileStatus(fileId, FileStatus.ACTIVE)
                }
            }
        }
    }

    override suspend fun addEnvelopeToFile(fileId: String, envelope: EncryptionEnvelope): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            supabase.postgrest.rpc(
                "add_file_envelope",
                buildJsonObject {
                    put("target_file_id", fileId)
                    put("new_envelope", Json.encodeToJsonElement(envelope))
                }
            )
            
            // Sync local
            val entity = fileDao.getFileById(fileId)
            if (entity != null) {
                val currentMeta = entity.encryptionMetadata?.let { Json.decodeFromJsonElement<FileEncryptionMetadata>(it) }
                if (currentMeta != null) {
                    val updatedEnvelopes = (currentMeta.envelopes + envelope).distinctBy { it.recipientId + it.type.name }
                    val updatedMeta = currentMeta.copy(envelopes = updatedEnvelopes)
                    fileDao.updateFile(entity.copy(encryptionMetadata = Json.encodeToJsonElement(updatedMeta)))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FileRepositoryImpl", "addEnvelopeToFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFiles() {
        withContext(Dispatchers.IO) {
            try {
                val remoteFiles = getSupabase().postgrest["files"].select().decodeList<FileEntity>()
                remoteFiles.forEach { entity ->
                    fileDao.insertFile(entity)
                }
            } catch (e: Exception) {
                Log.e("FileRepositoryImpl", "syncFiles failed: ${e.message}")
            }
        }
    }

    override suspend fun getDecryptedFEK(file: File, chatId: String?): Result<SecretKey> = withContext(Dispatchers.IO) {
        try {
            val metaJson = file.encryptionMetadata ?: run {
                Log.e("FILE_PIPELINE", "FEK_UNWRAP_FAILED | fileId=${file.id} | message=No encryption metadata")
                return@withContext Result.failure(Exception("No encryption metadata"))
            }
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(metaJson)
            
            // 1. Try CONVERSATION envelope first if chatId provided (Strict context separation)
            if (chatId != null) {
                val convEnvelope = fileMeta.envelopes.find { it.recipientId == chatId && it.type == EnvelopeType.CONVERSATION }
                if (convEnvelope != null) {
                    val pck = AppModule.provideConversationKeyManager(context).getOrLoadKey(chatId)
                    if (pck != null) {
                        return@withContext Result.success(StorageCryptoService.unwrapKey(convEnvelope.wrappedKey, pck))
                    }
                }
            }

            // 2. Fall back to OWNER envelope ONLY for the file owner.
            // This allows the owner to view files in chat contexts without needing a chat-specific envelope,
            // while preventing recipients from attempting to use the OWNER path.
            val ownerEnvelope = fileMeta.envelopes.find { it.type == EnvelopeType.OWNER }
            if (ownerEnvelope != null) {
                val currentUserId = AppModule.provideUserPreferencesRepository(context).getUserIdFast()
                if (file.ownerId == currentUserId) {
                    try {
                        if (!KeyManager.isInitialized()) {
                            KeyManager.restoreAEK(context)
                        }
                        val fpk = KeyManager.getFileProtectionKey()
                        return@withContext Result.success(StorageCryptoService.unwrapKey(ownerEnvelope.wrappedKey, fpk))
                    } catch (e: Exception) {
                        Log.d("FILE_PIPELINE", "OWNER_UNWRAP_FAILED | fileId=${file.id} | message=${e.message}")
                    }
                } else {
                    Log.d("FILE_PIPELINE", "OWNER_UNWRAP_SKIPPED | fileId=${file.id} | reason=User is not owner")
                }
            }
            
            Log.e("FILE_PIPELINE", "FEK_UNWRAP_FAILED | fileId=${file.id} | message=No valid envelope found for context")
            Result.failure(Exception("Direct FEK unwrapping failed. Recipient unwrap logic required."))
        } catch (e: Exception) {
            Log.e("FILE_PIPELINE", "FEK_UNWRAP_FAILED | fileId=${file.id} | message=${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun ensureMediaLocal(file: File, chatId: String?): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            Log.i("FILE_PIPELINE", "ENSURE_MEDIA_LOCAL_V2 | fileId=${file.id}")
            
            val internalMediaDir = java.io.File(context.filesDir, "media/${file.fileType.name.lowercase(Locale.ROOT)}")
            if (!internalMediaDir.exists()) internalMediaDir.mkdirs()
            
            val destFile = java.io.File(internalMediaDir, "${file.id}_decrypted")
            if (destFile.exists()) {
                Log.d("FILE_PIPELINE", "DECRYPTION_SKIPPED | fileId=${file.id} | message=Decrypted file already exists")
                return@withContext Result.success(destFile)
            }

            val downloadManager = AppModule.provideFileDownloadManager(context)
            val result = downloadManager.downloadAndDecrypt(file, destFile, {}, chatId)
            
            if (result.isSuccess) {
                fileDao.updateLocalPath(file.id, destFile.absolutePath)
            }
            
            result
        } catch (e: Exception) {
            Log.e("FILE_PIPELINE", "ENSURE_MEDIA_FAILED_V2 | fileId=${file.id} | message=${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun ensureThumbnailLocal(file: File, chatId: String?): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            if (file.thumbnailRemotePath == null) {
                Log.w("VAULT_THUMB", "[VaultThumbnail] fileId=${file.id} | source=NONE | status=MISSING_REMOTE_PATH")
                return@withContext Result.failure(Exception("No remote thumbnail"))
            }
            
            val thumbCacheDir = java.io.File(context.cacheDir, "thumbnails")
            if (!thumbCacheDir.exists()) thumbCacheDir.mkdirs()
            
            val destFile = java.io.File(thumbCacheDir, "${file.id}_thumb_decrypted")
            if (destFile.exists()) {
                Log.d("VAULT_THUMB", "[VaultThumbnail] fileId=${file.id} | source=LOCAL_CACHE | status=HIT")
                return@withContext Result.success(destFile)
            }

            // For thumbnails, we create a mock File model to reuse the streaming downloader
            // because they are now also cryptoVersion 2
            val metaJson = file.encryptionMetadata ?: throw Exception("Missing metadata")
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(metaJson)
            
            // Adjust metadata for thumbnail decryption
            val thumbMeta = fileMeta.copy(
                fileIv = fileMeta.thumbnailIv ?: throw Exception("No thumbnail IV"),
                plaintextSize = fileMeta.thumbnailPlaintextSize ?: throw Exception("No thumbnail plaintextSize")
            )
            
            val thumbFileModel = file.copy(
                id = file.id + "_thumb", // Bind AAD to thumb context
                storagePath = file.thumbnailRemotePath!!,
                encryptionMetadata = Json.encodeToString(thumbMeta),
                fileSize = file.thumbnailSize
            )

            val downloadManager = AppModule.provideFileDownloadManager(context)
            val result = downloadManager.downloadAndDecrypt(thumbFileModel, destFile, {}, chatId)
            
            if (result.isSuccess) {
                fileDao.updateThumbnailLocalPath(file.id, destFile.absolutePath)
            }
            
            result
        } catch (e: Exception) {
            Log.e("VAULT_THUMB", "[VaultThumbnail] fileId=${file.id} | source=THUMBNAIL | action=PIPELINE_FAILED | message=${e.message}")
            Result.failure(e)
        }
    }
}
