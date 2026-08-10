package com.keeftalk.chat.util

import android.content.Context
import android.util.Log
import com.keeftalk.chat.domain.model.File as KeeftalkFile
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readFully
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.*

class FileDownloadManager(
    private val context: Context,
    private val cryptoManager: CryptoManager,
    private val supabaseProvider: suspend () -> SupabaseClient
) {
    private val TAG = "FileDownloadManager"
    private val CHUNK_SIZE = 1024 * 1024 // 1 MiB

    suspend fun downloadAndDecrypt(
        fileModel: KeeftalkFile,
        targetFile: File,
        onProgress: (Float) -> Unit = {},
        chatId: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i("FILE_PIPELINE", "DOWNLOAD_AND_DECRYPT_START_V2 | fileId=${fileModel.id} | target=${targetFile.absolutePath}")
            
            val metaJson = fileModel.encryptionMetadata ?: throw Exception("Missing encryption metadata")
            val fileMeta = Json.decodeFromString<FileEncryptionMetadata>(metaJson)
            val fek = AppModule.provideFileRepository(context).getDecryptedFEK(fileModel, chatId).getOrThrow()
            
            val baseIv = Base64.getDecoder().decode(fileMeta.fileIv)
            val plaintextSize = fileMeta.plaintextSize ?: throw Exception("Missing plaintextSize in metadata")
            val ciphertextSize = fileModel.fileSize ?: throw Exception("Missing fileSize in database")
            
            val supabase = supabaseProvider()
            val bucket = supabase.storage["files"]
            
            val remotePath = if (fileModel.storagePath.contains("/files/")) {
                fileModel.storagePath.substringAfter("/files/").substringBefore("?")
            } else {
                fileModel.storagePath.substringAfterLast("/")
            }

            targetFile.parentFile?.mkdirs()
            val outputStream = FileOutputStream(targetFile)
            val channel = ByteChannel()
            
            try {
                val downloadJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bucket.downloadAuthenticated(remotePath, channel)
                    } catch (e: Exception) {
                        Log.e(TAG, "Network download failed", e)
                    } finally {
                        channel.close()
                    }
                }
                
                var chunkIndex = 0
                var totalDecrypted = 0L
                var totalReadFromNetwork = 0L
                
                while (totalDecrypted < plaintextSize) {
                    val remainingPlaintext = plaintextSize - totalDecrypted
                    val currentPlaintextChunkSize = if (remainingPlaintext > CHUNK_SIZE) CHUNK_SIZE else remainingPlaintext.toInt()
                    val currentEncryptedChunkSize = currentPlaintextChunkSize + 16
                    
                    val encryptedBuffer = ByteArray(currentEncryptedChunkSize)
                    channel.readFully(encryptedBuffer)
                    totalReadFromNetwork += currentEncryptedChunkSize
                    
                    val decryptedChunk = StorageCryptoService.decryptChunk(
                        encryptedData = encryptedBuffer,
                        key = fek,
                        baseIv = baseIv,
                        chunkIndex = chunkIndex,
                        fileId = fileModel.id
                    )
                    
                    outputStream.write(decryptedChunk)
                    
                    totalDecrypted += decryptedChunk.size
                    chunkIndex++
                    onProgress(totalReadFromNetwork.toFloat() / ciphertextSize)
                }
                
                downloadJob.join()
                
                if (targetFile.length() != plaintextSize) {
                    throw Exception("Integrity check failed: Plaintext size mismatch. Expected $plaintextSize, got ${targetFile.length()}")
                }
                
                Log.i("FILE_PIPELINE", "COMPLETE_V2 | fileId=${fileModel.id} | size=${targetFile.length()}")
                Result.success(targetFile)
                
            } catch (e: Exception) {
                outputStream.close()
                if (targetFile.exists()) targetFile.delete()
                throw e
            } finally {
                try { outputStream.close() } catch (_: Exception) {}
            }
            
        } catch (e: Exception) {
            Log.e("FILE_PIPELINE", "FAILED_V2 | stage=downloadAndDecrypt | message=${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getTempFileForOpening(fileModel: KeeftalkFile): Result<File> = withContext(Dispatchers.IO) {
        val fileName = fileModel.fileName ?: "file_${fileModel.id}"
        val tempFile = File(context.cacheDir, "vault_open_${System.currentTimeMillis()}_$fileName")
        downloadAndDecrypt(fileModel, tempFile)
    }
}
