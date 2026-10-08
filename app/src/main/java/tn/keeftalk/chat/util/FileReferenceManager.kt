package tn.keeftalk.chat.util

import tn.keeftalk.chat.domain.repository.FileRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileReferenceManager(
    private val fileRepository: FileRepository,
    private val supabaseProvider: suspend () -> SupabaseClient,
    private val r2StorageClient: R2StorageClient = R2StorageClient(supabaseProvider)
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

            pendingDeleteFiles.forEach { file ->
                try {
                    r2StorageClient.deleteObject(fileId = file.id, path = file.storagePath)
                    fileRepository.deleteFilePermanently(file.id)
                } catch (_: Exception) {
                    // Log error or retry later
                }
            }
        } catch (_: Exception) {}
    }
}
