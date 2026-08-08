package com.keeftalk.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.FileRepository
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.crypto.Mac

class FileUploadManager(
    private val context: Context,
    private val fileRepository: FileRepository,
    private val cryptoManager: CryptoManager,
    private val supabaseProvider: suspend () -> SupabaseClient
) {
    private val TAG = "FileUploadManager"

    suspend fun uploadFile(
        file: File,
        sourceType: SourceType,
        ownerId: String,
        onProgress: (Float) -> Unit = {}
    ): Result<com.keeftalk.chat.domain.model.File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting secure upload for file: ${file.absolutePath}, sourceType: $sourceType")
            
            // 1. Pre-processing
            val processedFile = compressIfNeeded(file)
            val thumbnailPath = generateThumbnail(processedFile)
            
            // 2. Per-User Deduplication (HMAC)
            val fpk = KeyManager.getFileProtectionKey()
            val hash = calculateHMAC(processedFile, fpk)
            Log.d(TAG, "File HMAC: $hash")
            
            // 3. Deduplication Check
            val existingFile = fileRepository.getFileByHash(hash)
            var targetId = UUID.randomUUID().toString()

            if (existingFile != null) {
                Log.d(TAG, "File exists via HMAC, verifying remote: ${existingFile.id}")
                val supabase = supabaseProvider()
                val bucket = supabase.storage["files"]
                
                val remoteRecord = try {
                    supabase.postgrest["files"].select {
                        filter { eq("id", existingFile.id) }
                    }.decodeSingleOrNull<JsonObject>()
                } catch (e: Exception) { null }

                val isRemotePresent = if (remoteRecord != null) {
                    try {
                        val path = existingFile.storagePath.substringAfter("files/")
                        bucket.info(path)
                        true
                    } catch (e: Exception) { false }
                } else false

                if (isRemotePresent) {
                    Log.d(TAG, "Remote file present, reusing.")
                    fileRepository.incrementReferenceCount(existingFile.id)
                    return@withContext Result.success(existingFile)
                } else {
                    targetId = existingFile.id
                }
            }

            // 4. Secure Encryption (Random FEK)
            val fileBytes = processedFile.readBytes()
            val fek = StorageCryptoService.generateRandomKey()
            val encryptedObj = StorageCryptoService.encrypt(fileBytes, fek)
            val encryptedBytes = java.util.Base64.getDecoder().decode(encryptedObj.ciphertext)
            
            // 5. Envelope Creation (OWNER)
            val wrappedFek = StorageCryptoService.wrapKey(fek, fpk)
            val ownerEnvelope = EncryptionEnvelope(
                recipientId = "OWNER",
                wrappedKey = wrappedFek,
                type = EnvelopeType.OWNER
            )

            // 6. Encrypted Metadata
            val mimeType = context.contentResolver.getType(Uri.fromFile(processedFile)) ?: getMimeTypeFromFile(processedFile)
            val attributes = FileAttributes(
                fileName = processedFile.name,
                mimeType = mimeType,
                originalName = file.name
            )
            val encryptedAttributes = StorageCryptoService.encrypt(
                Json.encodeToString(attributes).toByteArray(Charsets.UTF_8),
                fek
            )

            val fileMeta = FileEncryptionMetadata(
                envelopes = listOf(ownerEnvelope),
                fileIv = encryptedObj.iv,
                encryptedAttributes = encryptedAttributes
            )

            // 7. Cloud Upload (Opaque Path)
            val supabase = supabaseProvider()
            val bucket = supabase.storage["files"]
            val storageUuid = UUID.randomUUID().toString()
            val remotePath = "${ownerId}/$storageUuid" // Opaque random path
            
            bucket.upload(remotePath, encryptedBytes) {
                upsert = true
            }
            val storagePath = bucket.publicUrl(remotePath)

            // 8. Database Record
            val dimensions = MediaUtils.getDimensions(context, Uri.fromFile(processedFile).toString())
            
            val newFile = com.keeftalk.chat.domain.model.File(
                id = targetId,
                ownerId = ownerId,
                storagePath = storagePath,
                fileHash = hash,
                fileName = null,
                mimeType = null,
                fileSize = encryptedBytes.size.toLong(),
                fileType = getFileType(mimeType),
                sourceType = sourceType,
                width = dimensions?.width,
                height = dimensions?.height,
                duration = dimensions?.duration?.toInt(),
                thumbnailPath = thumbnailPath,
                localPath = file.absolutePath,
                encryptionMetadata = Json.encodeToString(fileMeta),
                referenceCount = 1,
                status = FileStatus.ACTIVE,
                securityMetadata = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null
            )

            fileRepository.saveFile(newFile)
            Result.success(newFile)
        } catch (e: Exception) {
            Log.e(TAG, "Secure upload failed", e)
            Result.failure(e)
        }
    }

    private fun compressIfNeeded(file: File): File {
        val mimeType = context.contentResolver.getType(Uri.fromFile(file))
        if (mimeType?.startsWith("image") == true) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return file
                val compressedFile = File(context.cacheDir, "compressed_${file.name}")
                FileOutputStream(compressedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                return compressedFile
            } catch (e: Exception) { return file }
        }
        return file
    }

    private fun generateThumbnail(file: File): String? {
        return try {
            val mimeType = context.contentResolver.getType(Uri.fromFile(file))
            val bitmap = if (mimeType?.startsWith("video") == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    ThumbnailUtils.createVideoThumbnail(file, Size(200, 200), null)
                } else {
                    ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
                }
            } else if (mimeType?.startsWith("image") == true) {
                ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.absolutePath), 200, 200)
            } else null

            bitmap?.let {
                val thumbFile = File(context.cacheDir, "thumb_${file.name}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 70, out)
                }
                thumbFile.absolutePath
            }
        } catch (e: Exception) { null }
    }

    private fun calculateHMAC(file: File, key: javax.crypto.SecretKey): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                mac.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return mac.doFinal().joinToString("") { "%02x".format(it) }
    }

    private fun getFileType(mimeType: String?): FileType {
        return when {
            mimeType?.startsWith("image") == true -> FileType.IMAGE
            mimeType?.startsWith("video") == true -> FileType.VIDEO
            mimeType?.startsWith("audio") == true -> FileType.AUDIO
            mimeType?.contains("pdf") == true || mimeType?.contains("document") == true -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }

    private fun getMimeTypeFromFile(file: File): String? {
        val extension = file.extension.lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
