package com.keeftalk.chat.data.cloud

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.ListFolderResult
import com.dropbox.core.v2.files.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DropboxItem(
    val id: String,
    val name: String,
    val pathDisplay: String,
    val isFolder: Boolean,
    val size: Long = 0,
    val modifiedDate: Long = 0,
    val mimeType: String? = null
)

class DropboxService {

    private fun getClient(accessToken: String): DbxClientV2 {
        val config = DbxRequestConfig.newBuilder("Keeftalk").build()
        return DbxClientV2(config, accessToken)
    }

    suspend fun listFolder(accessToken: String, path: String = "", cursor: String? = null): Result<Pair<List<DropboxItem>, String?>> = withContext(Dispatchers.IO) {
        try {
            val client = getClient(accessToken)
            val result: ListFolderResult = if (cursor != null) {
                client.files().listFolderContinue(cursor)
            } else {
                client.files().listFolderBuilder(path).withRecursive(false).withIncludeMediaInfo(true).start()
            }

            val items = result.entries.map { it.toDropboxItem() }
            val nextCursor = if (result.hasMore) result.cursor else null
            Result.success(items to nextCursor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(accessToken: String, path: String, targetFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getClient(accessToken)
            targetFile.outputStream().use { output ->
                client.files().download(path).download(output)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Metadata.toDropboxItem(): DropboxItem {
        return when (this) {
            is FileMetadata -> DropboxItem(
                id = id,
                name = name,
                pathDisplay = pathDisplay,
                isFolder = false,
                size = size,
                modifiedDate = serverModified.time,
                mimeType = null // Dropbox API v2 doesn't provide MIME type directly in listFolder
            )
            is FolderMetadata -> DropboxItem(
                id = id,
                name = name,
                pathDisplay = pathDisplay,
                isFolder = true
            )
            else -> DropboxItem(id = "", name = name, pathDisplay = pathDisplay, isFolder = false)
        }
    }
}
