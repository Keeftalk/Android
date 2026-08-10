package com.keeftalk.chat.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.keeftalk.chat.domain.model.DocumentModel
import com.keeftalk.chat.domain.model.DocumentType
import com.keeftalk.chat.domain.model.MediaItem
import java.util.UUID

object ShareIntentUtils {

    fun resolveToMediaItems(context: Context, uris: List<Uri>): List<MediaItem> {
        return uris.filter { isMedia(context, it) }.mapNotNull { uri ->
            try {
                val metadata = resolveMetadata(context, uri)
                val dimensions = MediaUtils.getDimensions(context, uri.toString())
                
                MediaItem(
                    id = System.currentTimeMillis() + UUID.randomUUID().hashCode(),
                    uri = uri,
                    displayName = metadata.name,
                    mimeType = metadata.mimeType ?: "image/jpeg",
                    dateModified = System.currentTimeMillis(),
                    size = metadata.size,
                    width = dimensions?.width ?: 1080,
                    height = dimensions?.height ?: 1920,
                    duration = dimensions?.duration,
                    bucketId = "shared",
                    bucketName = "Shared"
                )
            } catch (e: Exception) {
                Log.e("ShareIntentUtils", "Failed to resolve media item: $uri", e)
                null
            }
        }
    }

    fun resolveToDocumentModels(context: Context, uris: List<Uri>): List<DocumentModel> {
        return uris.filter { !isMedia(context, it) }.mapNotNull { uri ->
            try {
                val metadata = resolveMetadata(context, uri)
                val mimeType = metadata.mimeType ?: "application/octet-stream"
                
                DocumentModel(
                    id = System.currentTimeMillis() + UUID.randomUUID().hashCode(),
                    uri = uri,
                    name = metadata.name,
                    size = formatSize(metadata.size),
                    mimeType = mimeType,
                    date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                    type = getDocumentType(mimeType)
                )
            } catch (e: Exception) {
                Log.e("ShareIntentUtils", "Failed to resolve document model: $uri", e)
                null
            }
        }
    }

    private data class FileMetadata(val name: String, val size: Long, val mimeType: String?)

    private fun resolveMetadata(context: Context, uri: Uri): FileMetadata {
        var name = "file_${UUID.randomUUID()}"
        var size = 0L
        val contentResolver = context.contentResolver
        
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val resolvedName = cursor.getString(nameIndex)
                    if (!resolvedName.isNullOrBlank()) name = resolvedName
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        
        val mimeType = contentResolver.getType(uri) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
        
        return FileMetadata(name, size, mimeType)
    }

    private fun isMedia(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        return mimeType.startsWith("image/") || mimeType.startsWith("video/")
    }

    private fun getDocumentType(mimeType: String): DocumentType {
        return when {
            mimeType == "application/pdf" -> DocumentType.PDF
            mimeType.contains("msword") || mimeType.contains("officedocument.wordprocessingml") -> DocumentType.WORD
            mimeType.contains("ms-excel") || mimeType.contains("officedocument.spreadsheetml") -> DocumentType.EXCEL
            mimeType.contains("ms-powerpoint") || mimeType.contains("officedocument.presentationml") -> DocumentType.PPT
            mimeType.contains("zip") || mimeType.contains("rar") -> DocumentType.ZIP
            mimeType.contains("text/") -> DocumentType.OTHER
            else -> DocumentType.OTHER
        }
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
