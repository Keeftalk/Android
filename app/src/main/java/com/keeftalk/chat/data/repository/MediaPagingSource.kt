package com.keeftalk.chat.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.keeftalk.chat.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPagingSource(
    private val context: Context,
    private val bucketId: String? = null
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> = withContext(Dispatchers.IO) {
        try {
            val page = params.key ?: 0
            val limit = params.loadSize
            val offset = page * limit

            val mediaItems = fetchMedia(limit, offset)
            
            LoadResult.Page(
                data = mediaItems,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (mediaItems.size < limit) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun fetchMedia(limit: Int, offset: Int): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.ORIENTATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.VideoColumns.DURATION)
            }
        }.toTypedArray()

        val selection = StringBuilder().apply {
            append("(")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}")
            append(" OR ")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}")
            append(")")
            if (bucketId != null && bucketId != "ALL") {
                append(" AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?")
            }
        }.toString()

        val selectionArgs = if (bucketId != null && bucketId != "ALL") arrayOf(bucketId) else null

        val queryCursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val queryArgs = android.os.Bundle().apply {
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                putStringArray(
                    android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED)
                )
                putInt(
                    android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                if (selection.isNotEmpty()) {
                    putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                }
            }
            context.contentResolver.query(collection, projection, queryArgs, null)
        } else {
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT $limit OFFSET $offset"
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
        }

        queryCursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.ORIENTATION)
            val durationColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mimeType = cursor.getString(mimeColumn)
                val orientation = cursor.getInt(orientationColumn)
                val rawWidth = cursor.getInt(widthColumn)
                val rawHeight = cursor.getInt(heightColumn)
                
                // Swap dimensions if orientation is 90 or 270
                val (width, height) = if (orientation == 90 || orientation == 270) {
                    rawHeight to rawWidth
                } else {
                    rawWidth to rawHeight
                }

                val contentUri = if (mimeType.startsWith("video/")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                items.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = cursor.getString(nameColumn),
                        mimeType = mimeType,
                        dateModified = cursor.getLong(dateColumn),
                        size = cursor.getLong(sizeColumn),
                        width = width,
                        height = height,
                        duration = if (durationColumn != -1) cursor.getLong(durationColumn) else null,
                        bucketId = cursor.getString(bucketIdColumn),
                        bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                    )
                )
            }
        }
        return items
    }
}
