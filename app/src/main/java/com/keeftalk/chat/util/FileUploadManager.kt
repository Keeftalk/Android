package com.keeftalk.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.FileRepository
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.UploadData
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.flow.first
import java.util.Base64
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val authRepository: com.keeftalk.chat.domain.repository.AuthRepository,
    private val securityManager: com.keeftalk.chat.security.crypto.SecurityManager,
    private val supabaseProvider: suspend () -> SupabaseClient,
) {
    private val tag = "FileUploadManager"
    private val CHUNK_SIZE = 1024 * 1024 // 1 MiB

    suspend fun uploadFile(
        file: File,
        sourceType: SourceType,
        ownerId: String,
        onProgress: (uploaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<com.keeftalk.chat.domain.model.File> = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        var processedFile: File? = null
        try {
            Log.i("FILE_PIPELINE", "ORIGINAL_UPLOAD_START | fileId=$fileId | localPath=${file.absolutePath} | sourceType=$sourceType")
            
            val profile = authRepository.currentUserProfile.first() ?: throw Exception("Profile not found")
            val plan = profile.planType

            // 1. Enforce Max File Size
            val maxFileSize = when (plan) {
                SubscriptionPlan.FREE -> 100L * 1024 * 1024
                SubscriptionPlan.PLUS_MONTHLY, SubscriptionPlan.PLUS_YEARLY -> 1024L * 1024 * 1024
                else -> 5L * 1024 * 1024 * 1024
            }

            if (file.length() > maxFileSize) {
                return@withContext Result.failure(Exception("File too large for your plan. Maximum is ${maxFileSize / (1024*1024)}MB."))
            }

            // 2. Pre-processing (Memory-efficient compression)
            processedFile = if (plan == SubscriptionPlan.FREE) {
                compressForFreePlan(file)
            } else {
                compressIfNeeded(file)
            }

            // 3. Quota check (Client-side fail-fast, server will also enforce)
            if ((profile.storageUsed + processedFile!!.length()) > profile.storageLimit) {
                return@withContext Result.failure(Exception("Storage quota exceeded. Please upgrade your plan."))
            }

            val plaintextSize = processedFile!!.length()
            
            // 4. Per-User Deduplication (HMAC)
            // ensure AEK is ready (will suspend if recovery is needed)
            securityManager.getEncryptionContext()
            
            val fpk = KeyManager.getFileProtectionKey()
            val hash = calculateHMAC(processedFile!!, fpk)
            
            // 5. Deduplication Check
            val existingFile = fileRepository.getFileByHash(hash)
            if (existingFile != null) {
                Log.i("FILE_PIPELINE", "DEDUPLICATION MATCH | fileId=${existingFile.id}")
                val supabase = supabaseProvider()
                val bucket = supabase.storage["files"]
                
                val isRemotePresent = try {
                    val path = "$ownerId/${existingFile.id}/original"
                    bucket.info(path)
                    true
                } catch (_: Exception) { false }

                if (isRemotePresent) {
                    Log.i("FILE_PIPELINE", "REMOTE PRESENT | Reusing fileId=${existingFile.id}")
                    fileRepository.incrementReferenceCount(existingFile.id)
                    
                    // Cleanup
                    if (processedFile != file && processedFile!!.exists()) {
                        processedFile!!.delete()
                    }
                    
                    return@withContext Result.success(existingFile)
                }
            }

            // 6. Secure Encryption Setup (v2)
            Log.d("FILE_PIPELINE", "ENCRYPTION_SETUP_V2")
            val fek = StorageCryptoService.generateRandomKey()
            val baseIv = StorageCryptoService.generateBaseIv()
            
            val numFullChunks = (plaintextSize / CHUNK_SIZE).toInt()
            val lastChunkSize = (plaintextSize % CHUNK_SIZE).toInt()
            val totalChunks = if (lastChunkSize > 0) numFullChunks + 1 else numFullChunks
            
            // Ciphertext size = Plaintext + (16 bytes tag per chunk)
            val ciphertextSize = plaintextSize + (totalChunks * 16)
            
            // 7. Cloud Upload via Streaming Channel
            val supabase = supabaseProvider()
            val bucket = supabase.storage["files"]
            val remotePath = "${ownerId}/$fileId/original"
            
            val limiter = BandwidthLimiter(BandwidthPolicy.getUploadLimit(plan))
            
            // Use the current coroutine scope for the channel writer to ensure structured concurrency
            val channel = writer {
                val buffer = ByteArray(CHUNK_SIZE)
                processedFile!!.inputStream().use { input ->
                    var chunkIndex = 0
                    var totalRead = 0L
                    
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        
                        val dataToEncrypt = if (read == CHUNK_SIZE) buffer else buffer.copyOf(read)
                        val encryptedChunk = StorageCryptoService.encryptChunk(
                            data = dataToEncrypt,
                            key = fek,
                            baseIv = baseIv,
                            chunkIndex = chunkIndex,
                            fileId = fileId
                        )
                        
                        channel.writeFully(encryptedChunk)
                        
                        // Apply bandwidth throttling
                        limiter.throttle(encryptedChunk.size)
                        
                        totalRead += read
                        chunkIndex++
                        onProgress(totalRead, plaintextSize)
                    }
                }
            }.channel

            bucket.upload(remotePath, UploadData(channel, ciphertextSize)) { upsert = true }
            
            // VERIFY ORIGINAL
            try {
                bucket.info(remotePath)
                Log.i("FILE_PIPELINE", "ORIGINAL_UPLOAD_SUCCESS | remotePath=$remotePath")
            } catch (e: Exception) {
                Log.e("FILE_PIPELINE", "FAILED stage=VERIFY_ORIGINAL reason=Remote object not found after upload")
                throw Exception("Upload verification failed")
            }

            // 8. Thumbnail Generation (Memory efficient)
            var thumbnailRemotePath: String? = null
            var thumbnailSize: Long? = null
            var thumbnailWidth: Int? = null
            var thumbnailHeight: Int? = null
            var thumbnailIv: String? = null
            var thumbnailLocalPath: String? = null
            var thumbnailPlaintextSize: Long? = null

            try {
                val thumbBitmap = generateThumbnailBitmap(processedFile!!)
                if (thumbBitmap != null) {
                    Log.d("FILE_PIPELINE", "THUMBNAIL_GENERATION_SUCCESS")
                    thumbnailWidth = thumbBitmap.width
                    thumbnailHeight = thumbBitmap.height
                    
                    val thumbFile = File(context.cacheDir, "thumb_${fileId}.jpg")
                    FileOutputStream(thumbFile).use { out ->
                        thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    }
                    thumbnailLocalPath = thumbFile.absolutePath
                    val thumbBytes = thumbFile.readBytes()
                    thumbnailPlaintextSize = thumbBytes.size.toLong()
                    
                    // Encrypt Thumbnail (v2)
                    val thumbBaseIv = StorageCryptoService.generateBaseIv()
                    val encryptedThumb = StorageCryptoService.encryptChunk(
                        data = thumbBytes,
                        key = fek,
                        baseIv = thumbBaseIv,
                        chunkIndex = 0,
                        fileId = fileId + "_thumb"
                    )
                    thumbnailSize = encryptedThumb.size.toLong()
                    thumbnailIv = Base64.getEncoder().encodeToString(thumbBaseIv)
                    
                    val thumbRemotePath = "${ownerId}/$fileId/thumbnail"
                    bucket.upload(thumbRemotePath, encryptedThumb) { upsert = true }
                    
                    thumbnailRemotePath = bucket.publicUrl(thumbRemotePath)
                    Log.i("FILE_PIPELINE", "THUMBNAIL_UPLOAD_SUCCESS | remotePath=$thumbnailRemotePath")
                    
                    // Cleanup local thumbnail if needed (it will be saved to db as thumbnailLocalPath though)
                }
            } catch (e: Exception) {
                Log.w("FILE_PIPELINE", "THUMBNAIL_FAILED | reason=${e.message}")
            }

            // 9. Metadata & Envelopes
            val wrappedFek = StorageCryptoService.wrapKey(fek, fpk)
            val ownerEnvelope = EncryptionEnvelope(
                recipientId = "OWNER",
                wrappedKey = wrappedFek,
                type = EnvelopeType.OWNER
            )

            val mimeType = context.contentResolver.getType(Uri.fromFile(processedFile!!)) ?: getMimeTypeFromFile(processedFile!!)
            val attributes = FileAttributes(
                fileName = processedFile!!.name,
                mimeType = mimeType,
                originalName = file.name
            )
            
            // Attributes are still small, can use v1 style root encryption
            val encryptedAttributes = StorageCryptoService.encrypt(
                Json.encodeToString(attributes).toByteArray(Charsets.UTF_8),
                fek
            )

            val fileMeta = FileEncryptionMetadata(
                envelopes = listOf(ownerEnvelope),
                fileIv = Base64.getEncoder().encodeToString(baseIv),
                thumbnailIv = thumbnailIv,
                encryptedAttributes = encryptedAttributes,
                cryptoVersion = 2,
                chunkSize = CHUNK_SIZE,
                plaintextSize = plaintextSize,
                thumbnailPlaintextSize = thumbnailPlaintextSize
            )

            // 10. Database Record
            val dimensions = MediaUtils.getDimensions(context, Uri.fromFile(processedFile).toString())
            val storagePath = bucket.publicUrl(remotePath)
            
            val newFile = com.keeftalk.chat.domain.model.File(
                id = fileId,
                ownerId = ownerId,
                storagePath = storagePath,
                fileHash = hash,
                fileName = null,
                mimeType = null,
                fileSize = ciphertextSize, // Use ciphertext size for database column
                fileType = getFileType(mimeType),
                sourceType = sourceType,
                width = dimensions?.width,
                height = dimensions?.height,
                duration = dimensions?.duration?.toInt(),
                thumbnailPath = thumbnailLocalPath,
                thumbnailRemotePath = thumbnailRemotePath,
                thumbnailLocalPath = thumbnailLocalPath,
                thumbnailSize = thumbnailSize,
                thumbnailWidth = thumbnailWidth,
                thumbnailHeight = thumbnailHeight,
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
            Log.i("FILE_PIPELINE", "COMPLETE | fileId=$fileId | plaintextSize=$plaintextSize | ciphertextSize=$ciphertextSize")
            
            // 11. Optimistic Storage Update
            val totalBytesAdded = ciphertextSize + (thumbnailSize ?: 0L)
            authRepository.optimisticUpdateStorageUsed(ownerId, totalBytesAdded)
            
            // Trigger background refresh to sync with server truth
            // Use a repository-scoped launch if possible, but here we just need it to happen
            // Dispatchers.IO + GlobalScope is bad, but CoroutineScope(Dispatchers.IO) here is also transient.
            // For now, keeping it as is or moving to a better scope if available.
            CoroutineScope(Dispatchers.IO).launch {
                authRepository.refreshProfile(ownerId)
            }

            Result.success(newFile)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.w("FILE_PIPELINE", "UPLOAD_CANCELLED | fileId=$fileId")
                throw e
            }
            Log.e("FILE_PIPELINE", "FAILED stage=uploadFile fileId=$fileId reason=${e.message}", e)
            Result.failure(e)
        } finally {
            // Cleanup processed file (compressed copy) always
            if (processedFile != null && processedFile != file && processedFile!!.exists()) {
                processedFile!!.delete()
                Log.d("FILE_PIPELINE", "CLEANUP | Deleted processed file: ${processedFile!!.name}")
            }
        }
    }

    private fun compressForFreePlan(file: File): File {
        val mimeType = getMimeTypeFromFile(file)
        if (mimeType?.startsWith("image") == true) {
            return try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                
                // FREE Plan: Target around 1200px (standard compressed)
                options.inSampleSize = calculateInSampleSize(options, 1200, 1200)
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return file
                val compressedFile = File(context.cacheDir, "free_compressed_${file.name}")
                FileOutputStream(compressedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out) // Lower quality for FREE
                }
                compressedFile
            } catch (e: Exception) { file }
        } else if (mimeType?.startsWith("video") == true) {
            // For videos, in a real app we'd use Media3 Transformer to transcode to 720p/low-bitrate
            // For now, we'll assume it's "compressed" by the OS or just return as is if no transcoder implemented.
            return file
        }
        return file
    }

    private fun compressIfNeeded(file: File): File {
        val mimeType = getMimeTypeFromFile(file)
        if ((mimeType?.startsWith("image") == true) && file.length() > (1024 * 1024)) { // Only compress images > 1MB
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                
                // Target around 1600px for "original" if it's huge
                options.inSampleSize = calculateInSampleSize(options, 1600, 1600)
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return file
                val compressedFile = File(context.cacheDir, "compressed_${file.name}")
                FileOutputStream(compressedFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                return compressedFile
            } catch (e: Exception) { return file }
        }
        return file
    }

    fun generateThumbnail(file: File): String? {
        return try {
            val bitmap = generateThumbnailBitmap(file) ?: return null
            val thumbFile = File(context.cacheDir, "thumb_${UUID.randomUUID()}.jpg")
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            thumbFile.absolutePath
        } catch (e: Exception) { null }
    }

    private fun generateThumbnailBitmap(file: File): Bitmap? {
        val maxThumbSize = 700
        return try {
            val mimeType = getMimeTypeFromFile(file)
            val rawBitmap = when {
                mimeType?.startsWith("video") == true -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        ThumbnailUtils.createVideoThumbnail(file, Size(maxThumbSize, maxThumbSize), null)
                    } else {
                        @Suppress("DEPRECATION")
                        ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
                    }
                }
                mimeType?.startsWith("image") == true -> {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, maxThumbSize, maxThumbSize)
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeFile(file.absolutePath, options)
                }
                mimeType == "application/pdf" -> {
                    generatePdfThumbnail(file)
                }
                else -> null
            }

            // Strictly enforce 700px max dimension while preserving aspect ratio
            rawBitmap?.let { bmp ->
                val width = bmp.width
                val height = bmp.height
                if (width > maxThumbSize || height > maxThumbSize) {
                    val scale = maxThumbSize.toFloat() / maxOf(width, height)
                    val newWidth = (width * scale).toInt()
                    val newHeight = (height * scale).toInt()
                    bmp.scale(newWidth, newHeight, true)
                } else {
                    bmp
                }
            }
        } catch (e: Exception) { 
            Log.w(tag, "generateThumbnailBitmap failed: ${e.message}")
            null 
        }
    }

    private fun generatePdfThumbnail(file: File): Bitmap? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(page.width / 2, page.height / 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                bitmap
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (e: Exception) { null }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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
        val type = mimeType?.lowercase() ?: ""
        return when {
            type.startsWith("image") -> FileType.IMAGE
            type.startsWith("video") -> FileType.VIDEO
            type.startsWith("audio") -> FileType.AUDIO
            type.contains("pdf") || 
            type.contains("msword") || 
            type.contains("officedocument") || 
            type.contains("text/plain") ||
            type.contains("javascript") ||
            type.contains("python") ||
            type.contains("json") -> FileType.DOCUMENT
            else -> FileType.OTHER // Includes zip, rar, etc.
        }
    }

    private fun getMimeTypeFromFile(file: File): String? {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
