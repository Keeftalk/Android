package com.keeftalk.chat.util

import android.content.Context
import android.util.Log
import com.keeftalk.chat.domain.model.File as KeeftalkFile
import com.keeftalk.chat.security.crypto.CryptoManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
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

    suspend fun downloadAndDecrypt(
        fileModel: KeeftalkFile,
        targetFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download for file: ${fileModel.fileName}")
            
            val supabase = supabaseProvider()
            val bucket = supabase.storage["files"]
            
            // 1. Extract remote path from storagePath URL
            // storagePath format: .../storage/v1/object/public/files/USER_ID/FILE_NAME
            val remotePath = fileModel.storagePath.substringAfter("files/")
            
            // 2. Download encrypted bytes
            val encryptedBytes = bucket.downloadPublic(remotePath)
            Log.d(TAG, "Download finished. Encrypted size: ${encryptedBytes.size}")

            // 3. Decrypt
            if (fileModel.encryptionMetadata == null) {
                // If not encrypted, just write to target
                targetFile.writeBytes(encryptedBytes)
                return@withContext Result.success(targetFile)
            }

            val meta: Map<String, String> = Json.decodeFromString(fileModel.encryptionMetadata)
            val iv = meta["mediaIv"] ?: throw Exception("Missing mediaIv")
            val encryptedMediaKey = meta["encryptedMediaKey"] ?: throw Exception("Missing encryptedMediaKey")
            
            val mediaKeyBytes = android.util.Base64.decode(encryptedMediaKey, android.util.Base64.NO_WRAP)
            val mediaKey = javax.crypto.spec.SecretKeySpec(mediaKeyBytes, "AES")
            
            val encryptedObj = com.keeftalk.chat.security.crypto.EncryptedObject(
                version = 1,
                keyId = "media",
                iv = iv,
                ciphertext = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
            )
            
            val decryptedBytes = cryptoManager.decryptMedia(encryptedObj, mediaKey)
            
            targetFile.writeBytes(decryptedBytes)
            Log.d(TAG, "Decryption finished. Decrypted size: ${decryptedBytes.size}")
            
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download/Decryption failed", e)
            Result.failure(e)
        }
    }

    suspend fun getTempFileForOpening(fileModel: KeeftalkFile): Result<File> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "vault_open_${System.currentTimeMillis()}_${fileModel.fileName}")
        downloadAndDecrypt(fileModel, tempFile)
    }
}
