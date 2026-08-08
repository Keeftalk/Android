package com.keeftalk.chat.util

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.util.NotesLogger
import okio.buffer
import okio.source
import java.io.File

data class MediaDimensions(val width: Int, val height: Int, val duration: Long? = null)

class KeeftalkMediaFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val uriString = data.toString()
        val mediaId = if (uriString.contains("/image/")) {
            uriString.removePrefix("keeftalk-media://image/")
        } else if (uriString.contains("/video/")) {
            uriString.removePrefix("keeftalk-media://video/")
        } else {
            uriString.removePrefix("keeftalk-media://")
        }

        val traceId = mediaId.take(8)
        NotesLogger.i("MEDIA_DEBUG", "[traceId=$traceId] STAGE 8: KeeftalkMediaFetcher.fetch() started for ID: $mediaId")
        
        val repository = AppModule.provideFileRepository(context)
        
        // Retry a few times in case the DB write is still in progress
        var file = repository.getFileById(mediaId)
        if (file == null) {
            NotesLogger.v("MEDIA_DEBUG", "[traceId=$traceId] File not found in DB immediately, retrying in 500ms...")
            kotlinx.coroutines.delay(500)
            file = repository.getFileById(mediaId)
        }
        if (file == null) {
            NotesLogger.v("MEDIA_DEBUG", "[traceId=$traceId] File still not found, final retry in 1000ms...")
            kotlinx.coroutines.delay(1000)
            file = repository.getFileById(mediaId)
        }

        if (file == null) {
            NotesLogger.e("MEDIA_DEBUG", "[traceId=$traceId] STAGE 8.1: File NOT FOUND in local DB after retries. Aborting.")
            return null
        }

        val uriToFetch = Uri.parse(file.storagePath)

        val result = try {
            imageLoader.components.newFetcher(uriToFetch, options, imageLoader)
        } catch (e: Exception) {
            NotesLogger.e("MEDIA_DEBUG", "[traceId=$traceId] STAGE 8.3: Error creating sub-fetcher for $uriToFetch", throwable = e)
            null
        } ?: return null

        val (fetcher, fetcherOptions) = result
        NotesLogger.i("MEDIA_DEBUG", "[traceId=$traceId] STAGE 8.4: Delegating to sub-fetcher: ${fetcher.javaClass.simpleName}")
        return fetcher.fetch()
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == "keeftalk-media") {
                return KeeftalkMediaFetcher(context, data, options, imageLoader)
            }
            return null
        }
    }
}

object MediaUtils {
    private const val TAG = "MEDIA_UTILS"

    fun getDimensions(context: Context, uriString: String): MediaDimensions? {
        val uri = Uri.parse(uriString)
        val contentResolver = context.contentResolver
        
        // Better type detection that works for file:// and content://
        val type = if (uri.scheme == "file") {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uriString)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: ""
        } else {
            contentResolver.getType(uri) ?: ""
        }

        return try {
            if (type.startsWith("image") || uriString.lowercase().run { contains(".jpg") || contains(".png") || contains(".jpeg") || contains(".webp") }) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                context.contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, options)
                }
                
                if (options.outWidth > 0 && options.outHeight > 0) {
                    NotesLogger.d(TAG, "Image dimensions detected: ${options.outWidth}x${options.outHeight} for $uriString")
                    MediaDimensions(options.outWidth, options.outHeight)
                } else {
                    // Fallback: try to decode directly if it's a file
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath, options)
                            if (options.outWidth > 0) {
                                return MediaDimensions(options.outWidth, options.outHeight)
                            }
                        }
                    }
                    NotesLogger.w(TAG, "Could not detect image dimensions for $uriString")
                    null
                }
            } else if (type.startsWith("video") || uriString.lowercase().run { contains(".mp4") || contains(".mov") || contains(".webm") }) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    
                    if (width > 0 && height > 0) {
                        NotesLogger.d(TAG, "Video dimensions: ${width}x${height}, duration: $duration")
                        MediaDimensions(width, height, duration)
                    } else {
                        NotesLogger.w(TAG, "Could not detect video dimensions for $uriString")
                        null
                    }
                } finally {
                    retriever.release()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            NotesLogger.e(TAG, "Error getting media dimensions for $uriString", throwable = e)
            null
        }
    }
}
