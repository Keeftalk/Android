package com.keeftalk.chat.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.keeftalk.chat.domain.model.MediaAlbum
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaRepositoryImpl(
    private val context: Context
) : MediaRepository {

    override fun getMediaPagingData(bucketId: String?): Flow<PagingData<MediaItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 21,
                prefetchDistance = 7,
                enablePlaceholders = false,
                maxSize = 63
            ),
            pagingSourceFactory = { MediaPagingSource(context, bucketId) }
        ).flow
    }

    override fun getAlbums(): Flow<List<MediaAlbum>> = flow {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val albumMap = mutableMapOf<String, MediaAlbumBuilder>()
        var totalCount = 0
        var latestUri: android.net.Uri? = null

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                val id = cursor.getLong(idColumn)
                val mimeType = cursor.getString(mimeColumn)

                val uri = if (mimeType.startsWith("video/")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                if (latestUri == null) latestUri = uri
                totalCount++

                val builder = albumMap.getOrPut(bucketId) {
                    MediaAlbumBuilder(bucketId, bucketName, uri)
                }
                builder.count++
            }
        }

        val albums = albumMap.values.map { it.build() }.toMutableList()
        
        // Add "Recent" (virtual album) at the top
        latestUri?.let {
            albums.add(0, MediaAlbum("ALL", "Recent", it, totalCount))
        }

        emit(albums.sortedByDescending { it.id == "ALL" || it.name == "Camera" })
    }.flowOn(Dispatchers.IO)

    private class MediaAlbumBuilder(
        val id: String,
        val name: String,
        val thumbnailUri: android.net.Uri
    ) {
        var count: Int = 0
        fun build() = MediaAlbum(id, name, thumbnailUri, count)
    }
}
