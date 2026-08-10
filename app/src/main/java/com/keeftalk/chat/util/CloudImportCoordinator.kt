package com.keeftalk.chat.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.keeftalk.chat.data.cloud.GooglePhotosService
import com.keeftalk.chat.data.cloud.GoogleDriveService
import com.keeftalk.chat.data.cloud.DropboxService
import com.keeftalk.chat.data.cloud.DropboxItem
import com.keeftalk.chat.data.cloud.MediaItem
import com.keeftalk.chat.data.cloud.DriveFileMetadata
import com.keeftalk.chat.domain.repository.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CloudImportCoordinator(
    private val context: Context,
    private val vaultRepository: VaultRepository
) {
    private val TAG = "CloudImportCoordinator"

    sealed class ImportState {
        object Idle : ImportState()
        data class Progress(val current: Int, val total: Int, val percentage: Float) : ImportState()
        data class Success(val count: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    suspend fun importFromUris(uris: List<Uri>, parentId: String?) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        
        Log.i(TAG, "Starting batch import from ${uris.size} URIs")
        _importState.value = ImportState.Progress(0, uris.size, 0f)

        var successCount = 0
        uris.forEachIndexed { index, uri ->
            try {
                _importState.value = ImportState.Progress(index + 1, uris.size, index.toFloat() / uris.size)
                
                // 1. Copy to local temp file (Plaintext)
                val tempFile = StorageUtils.getFileFromUri(context, uri)
                if (tempFile != null && tempFile.exists()) {
                    try {
                        // 2. Pass to existing pipeline (Encryption -> Upload -> Vault)
                        val result = vaultRepository.uploadFile(tempFile, parentId) { _ -> }
                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "Successfully imported ${tempFile.name}")
                        } else {
                            Log.e(TAG, "Failed to upload ${tempFile.name}: ${result.exceptionOrNull()?.message}")
                        }
                    } finally {
                        // 3. MANDATORY CLEANUP of plaintext temp file
                        if (tempFile.exists()) {
                            val deleted = tempFile.delete()
                            Log.d(TAG, "Plaintext temp file deleted: $deleted | path=${tempFile.absolutePath}")
                        }
                    }
                } else {
                    Log.e(TAG, "Could not create temp file from URI: $uri")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing URI: $uri", e)
            }
        }

        handleImportResult(successCount, uris.size)
    }

    suspend fun importFromDrive(files: List<DriveFileMetadata>, driveService: GoogleDriveService, accessToken: String, parentId: String?) = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext
        
        Log.i(TAG, "Starting batch import from ${files.size} Drive files")
        _importState.value = ImportState.Progress(0, files.size, 0f)

        var successCount = 0
        files.forEachIndexed { index, file ->
            try {
                _importState.value = ImportState.Progress(index + 1, files.size, index.toFloat() / files.size)
                
                // 1. Download to local temp file (Plaintext)
                val tempFile = File(context.cacheDir, file.name)
                
                val downloadResult = driveService.downloadFile(file.id, accessToken, tempFile)
                if (downloadResult.isSuccess && tempFile.exists()) {
                    try {
                        // 2. Pass to existing pipeline (Encryption -> Upload -> Vault)
                        val result = vaultRepository.uploadFile(tempFile, parentId) { _ -> }
                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "Successfully imported ${tempFile.name}")
                        } else {
                            Log.e(TAG, "Failed to upload ${tempFile.name}: ${result.exceptionOrNull()?.message}")
                        }
                    } finally {
                        // 3. MANDATORY CLEANUP of plaintext temp file
                        if (tempFile.exists()) {
                            val deleted = tempFile.delete()
                            Log.d(TAG, "Plaintext temp file deleted: $deleted | path=${tempFile.absolutePath}")
                        }
                    }
                } else {
                    Log.e(TAG, "Could not download Drive file: ${file.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing Drive file: ${file.id}", e)
            }
        }

        handleImportResult(successCount, files.size)
    }

    suspend fun importFromPhotos(items: List<MediaItem>, photosService: GooglePhotosService, parentId: String?) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        
        Log.i(TAG, "Starting batch import from ${items.size} Photos")
        _importState.value = ImportState.Progress(0, items.size, 0f)

        var successCount = 0
        items.forEachIndexed { index, item ->
            try {
                _importState.value = ImportState.Progress(index + 1, items.size, index.toFloat() / items.size)
                
                // 1. Download to local temp file (Plaintext)
                val fileName = item.filename ?: "photo_${UUID.randomUUID()}.jpg"
                val tempFile = File(context.cacheDir, fileName)
                
                val downloadResult = photosService.downloadMedia(item.baseUrl, tempFile)
                if (downloadResult.isSuccess && tempFile.exists()) {
                    try {
                        // 2. Pass to existing pipeline (Encryption -> Upload -> Vault)
                        val result = vaultRepository.uploadFile(tempFile, parentId) { _ -> }
                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "Successfully imported ${tempFile.name}")
                        } else {
                            Log.e(TAG, "Failed to upload ${tempFile.name}: ${result.exceptionOrNull()?.message}")
                        }
                    } finally {
                        // 3. MANDATORY CLEANUP of plaintext temp file
                        if (tempFile.exists()) {
                            val deleted = tempFile.delete()
                            Log.d(TAG, "Plaintext temp file deleted: $deleted | path=${tempFile.absolutePath}")
                        }
                    }
                } else {
                    Log.e(TAG, "Could not download photo: ${item.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing photo: ${item.id}", e)
            }
        }

        handleImportResult(successCount, items.size)
    }

    suspend fun importFromDropbox(items: List<DropboxItem>, dropboxService: DropboxService, accessToken: String, parentId: String?) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        
        Log.i(TAG, "Starting batch import from ${items.size} Dropbox items")
        _importState.value = ImportState.Progress(0, items.size, 0f)

        var successCount = 0
        items.forEachIndexed { index, item ->
            try {
                _importState.value = ImportState.Progress(index + 1, items.size, index.toFloat() / items.size)
                
                // 1. Download to local temp file (Plaintext)
                val tempFile = File(context.cacheDir, item.name)
                
                val downloadResult = dropboxService.downloadFile(accessToken, item.pathDisplay, tempFile)
                if (downloadResult.isSuccess && tempFile.exists()) {
                    try {
                        // 2. Pass to existing pipeline (Encryption -> Upload -> Vault)
                        val result = vaultRepository.uploadFile(tempFile, parentId) { _ -> }
                        if (result.isSuccess) {
                            successCount++
                            Log.d(TAG, "Successfully imported ${tempFile.name}")
                        } else {
                            Log.e(TAG, "Failed to upload ${tempFile.name}: ${result.exceptionOrNull()?.message}")
                        }
                    } finally {
                        // 3. MANDATORY CLEANUP of plaintext temp file
                        if (tempFile.exists()) {
                            val deleted = tempFile.delete()
                            Log.d(TAG, "Plaintext temp file deleted: $deleted | path=${tempFile.absolutePath}")
                        }
                    }
                } else {
                    Log.e(TAG, "Could not download Dropbox file: ${item.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing Dropbox item: ${item.id}", e)
            }
        }

        handleImportResult(successCount, items.size)
    }

    private suspend fun handleImportResult(successCount: Int, total: Int) {
        if (successCount > 0) {
            _importState.value = ImportState.Success(successCount)
            Log.i(TAG, "Batch import completed: $successCount of $total items successful")
        } else {
            _importState.value = ImportState.Error("Failed to import selected items")
        }
        
        // Reset to Idle after a delay
        delay(3000)
        _importState.value = ImportState.Idle
    }
}
