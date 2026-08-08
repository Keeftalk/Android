package com.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import com.keeftalk.chat.data.local.dao.FileDao
import com.keeftalk.chat.data.local.entities.FileEntity
import com.keeftalk.chat.data.local.entities.toDomain
import com.keeftalk.chat.data.local.entities.toEntity
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
import kotlinx.serialization.json.Json
import java.util.*
import javax.crypto.SecretKey

class FileRepositoryImpl(
    private val context: Context,
    private val fileDao: FileDao
) : FileRepository {

    private suspend fun getSupabase() = AppModule.provideSupabaseClientAsync(context)

    override suspend fun saveFile(file: File) {
        withContext(Dispatchers.IO) {
            val entity = file.toEntity()
            fileDao.insertFile(entity)
            try {
                getSupabase().postgrest["files"].upsert(entity)
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
            } catch (e: Exception) {}
        }
    }

    override suspend fun incrementReferenceCount(fileId: String) {
        withContext(Dispatchers.IO) {
            fileDao.incrementReferenceCount(fileId)
            try {
                val entity = fileDao.getFileById(fileId)
                if (entity != null) {
                    getSupabase().postgrest["files"].update(mapOf("reference_count" to entity.referenceCount)) {
                        filter { eq("id", fileId) }
                    }
                }
            } catch (e: Exception) {}
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

    override suspend fun getDecryptedFEK(file: File): Result<SecretKey> = withContext(Dispatchers.IO) {
        try {
            val metaJson = file.encryptionMetadata ?: return@withContext Result.failure(Exception("No encryption metadata"))
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(metaJson)
            
            // Try OWNER envelope first
            val ownerEnvelope = fileMeta.envelopes.find { it.type == EnvelopeType.OWNER }
            if (ownerEnvelope != null) {
                val fpk = KeyManager.getFileProtectionKey()
                return@withContext Result.success(StorageCryptoService.unwrapKey(ownerEnvelope.wrappedKey, fpk))
            }
            
            Result.failure(Exception("Direct FEK unwrapping failed. Recipient unwrap logic required."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureMediaLocal(file: File): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            val internalMediaDir = java.io.File(context.filesDir, "media/${file.fileType.name.lowercase(Locale.ROOT)}")
            if (!internalMediaDir.exists()) internalMediaDir.mkdirs()
            
            val destFile = java.io.File(internalMediaDir, "${file.id}_decrypted")
            if (destFile.exists()) return@withContext Result.success(destFile)

            // 1. Download Ciphertext
            val url = file.storagePath
            val ciphertextBytes = try {
                val supabase = getSupabase()
                val bucket = supabase.storage["files"]
                val relativePath = if (url.contains("/files/")) {
                    url.substringAfter("/files/").substringBefore("?")
                } else {
                    url.substringAfterLast("/")
                }
                bucket.downloadPublic(relativePath)
            } catch (e: Exception) {
                val supabase = getSupabase()
                val bucket = supabase.storage["files"]
                val relativePath = url.substringAfterLast("/")
                bucket.downloadAuthenticated(relativePath)
            }

            // 2. Unwrap FEK
            val fek = getDecryptedFEK(file).getOrThrow()
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(file.encryptionMetadata!!)

            // 3. Decrypt
            val encryptedObj = EncryptedObject(
                version = 1,
                keyId = "fek",
                iv = fileMeta.fileIv,
                ciphertext = java.util.Base64.getEncoder().encodeToString(ciphertextBytes)
            )
            
            val decryptedBytes = StorageCryptoService.decrypt(encryptedObj, fek)
            destFile.writeBytes(decryptedBytes)
            fileDao.updateLocalPath(file.id, destFile.absolutePath)
            
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
