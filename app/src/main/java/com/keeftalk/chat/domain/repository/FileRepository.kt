package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.File
import com.keeftalk.chat.domain.model.FileStatus
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun saveFile(file: File)
    suspend fun getFileById(id: String): File?
    suspend fun getFileByHash(hash: String): File?
    fun getAllActiveFiles(): Flow<List<File>>
    suspend fun getFilesPendingDelete(): List<File>
    suspend fun updateFileStatus(fileId: String, status: FileStatus)
    suspend fun incrementReferenceCount(fileId: String)
    suspend fun decrementReferenceCount(fileId: String)
    suspend fun deleteFilePermanently(fileId: String)
    suspend fun addEnvelopeToFile(fileId: String, envelope: com.keeftalk.chat.security.crypto.EncryptionEnvelope): Result<Unit>
    suspend fun reconcileReferenceCounts()
    suspend fun syncFiles()
    suspend fun ensureMediaLocal(file: File, chatId: String? = null): Result<java.io.File>
    suspend fun ensureThumbnailLocal(file: File, chatId: String? = null): Result<java.io.File>
    suspend fun getDecryptedFEK(file: File, chatId: String? = null): Result<javax.crypto.SecretKey>
}
