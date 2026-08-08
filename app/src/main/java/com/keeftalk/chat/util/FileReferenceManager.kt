package com.keeftalk.chat.util

import com.keeftalk.chat.domain.model.FileStatus
import com.keeftalk.chat.domain.repository.FileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileReferenceManager(
    private val fileRepository: FileRepository,
    private val supabaseProvider: suspend () -> SupabaseClient
) {
    suspend fun addReference(fileId: String) {
        fileRepository.incrementReferenceCount(fileId)
    }

    suspend fun removeReference(fileId: String) {
        fileRepository.decrementReferenceCount(fileId)
    }

    suspend fun deleteUnusedFiles() = withContext(Dispatchers.IO) {
        try {
            val pendingDeleteFiles = fileRepository.getFilesPendingDelete()
            if (pendingDeleteFiles.isEmpty()) return@withContext

            val supabase = supabaseProvider()
            val bucket = supabase.storage["files"]
            
            pendingDeleteFiles.forEach { file ->
                try {
                    val relativePath = if (file.storagePath.contains("/files/")) {
                        file.storagePath.substringAfter("/files/").substringBefore("?")
                    } else {
                        file.storagePath.substringAfterLast("/")
                    }
                    bucket.delete(relativePath)
                    fileRepository.deleteFilePermanently(file.id)
                } catch (e: Exception) {
                    // Log error or retry later
                }
            }
        } catch (e: Exception) {}
    }
}
