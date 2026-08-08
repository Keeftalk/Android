package com.keeftalk.chat.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import com.keeftalk.chat.domain.model.DocumentFolder
import com.keeftalk.chat.domain.model.DocumentModel
import com.keeftalk.chat.domain.model.DocumentType
import com.keeftalk.chat.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.LinkedList

import com.keeftalk.chat.data.local.dao.MessageDao
import androidx.core.net.toUri

class DocumentRepositoryImpl(
    private val context: Context,
    private val messageDao: MessageDao? = null
) : DocumentRepository {

    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    private val fileProjection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Files.FileColumns.RELATIVE_PATH else MediaStore.Files.FileColumns.DATA
    )

    override fun getRecentDocuments(): Flow<List<DocumentModel>> = flow {
        val docs = mutableListOf<DocumentModel>()
        if (isAllFilesAccessGranted()) {
            docs.addAll(scanRootDirectoryForDocuments())
        } else {
            docs.addAll(fetchDocumentsFromAllVolumes(null))
        }
        emit(docs.distinctBy { it.uri.toString() }.sortedByDescending { it.id })
    }.flowOn(Dispatchers.IO)

    override fun getFolders(): Flow<List<DocumentFolder>> = flow {
        val foldersMap = mutableMapOf<String, Triple<String, String, Int>>()
        
        if (isAllFilesAccessGranted()) {
            scanFoldersDirectly(foldersMap)
        } else {
            val selection = getDocumentSelection()
            val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.getExternalVolumeNames(context)
            } else {
                setOf("external")
            }

            volumes.forEach { volume ->
                val collection = MediaStore.Files.getContentUri(volume)
                scanVolumeForFolders(collection, selection, foldersMap)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val downloadsCollection = MediaStore.Downloads.getContentUri(volume)
                    scanVolumeForFolders(downloadsCollection, selection, foldersMap)
                }
            }
        }

        val folderList = foldersMap.map { (path, triple) ->
            DocumentFolder(path, triple.first, triple.second, triple.third)
        }.sortedWith(compareByDescending<DocumentFolder> { it.name.contains("Download", ignoreCase = true) }
            .thenByDescending { it.documentCount }).toMutableList()

        val totalCount = folderList.sumOf { it.documentCount }
        folderList.add(0, DocumentFolder("ALL_RECENT", "Recent", "", totalCount))

        emit(folderList)
    }.flowOn(Dispatchers.IO)

    private fun isAllFilesAccessGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Legacy storage permission handled elsewhere
        }
    }

    private fun scanRootDirectoryForDocuments(): List<DocumentModel> {
        val root = Environment.getExternalStorageDirectory()
        val results = mutableListOf<DocumentModel>()
        recursiveFileScan(root, results, 1000)
        return results
    }

    private fun recursiveFileScan(dir: File, results: MutableList<DocumentModel>, limit: Int) {
        if (results.size >= limit) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (file.name.startsWith(".") || file.name == "Android") continue
                recursiveFileScan(file, results, limit)
            } else {
                if (isExcludedMedia(file.name, "")) continue
                results.add(fileToModel(file))
            }
            if (results.size >= limit) break
        }
    }

    private fun scanFoldersDirectly(foldersMap: MutableMap<String, Triple<String, String, Int>>) {
        val root = Environment.getExternalStorageDirectory()
        val queue = LinkedList<File>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val dir = queue.poll() ?: continue
            val files = dir.listFiles() ?: continue
            var count = 0
            for (file in files) {
                if (file.isDirectory) {
                    if (!file.name.startsWith(".") && file.name != "Android") {
                        queue.add(file)
                    }
                } else {
                    if (!isExcludedMedia(file.name, "")) {
                        count++
                    }
                }
            }
            if (count > 0) {
                val path = dir.absolutePath
                val name = if (path == root.absolutePath) "Recent" else dir.name
                foldersMap[path] = Triple(name, path, count)
            }
        }
    }

    private fun fileToModel(file: File): DocumentModel {
        return DocumentModel(
            id = file.hashCode().toLong(),
            uri = Uri.fromFile(file),
            name = file.name,
            size = Formatter.formatFileSize(context, file.length()),
            mimeType = "",
            date = dateFormat.format(Date(file.lastModified())),
            type = getDocumentType(file.name, "")
        )
    }

    private fun scanVolumeForFolders(collection: Uri, selection: String?, foldersMap: MutableMap<String, Triple<String, String, Int>>) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Files.FileColumns.RELATIVE_PATH else MediaStore.Files.FileColumns.DATA
        )

        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val fileNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                }

                while (cursor.moveToNext()) {
                    val fileName = cursor.getString(fileNameCol) ?: ""
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    val rawPath = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""

                    if (isExcludedMedia(fileName, mimeType)) continue

                    val parentPath = extractParentPath(rawPath)
                    val folderName = extractFolderName(parentPath)

                    val current = foldersMap.getOrDefault(parentPath, Triple(folderName, parentPath, 0))
                    foldersMap[parentPath] = Triple(current.first, current.second, current.third + 1)
                }
            }
        } catch (e: Exception) { }
    }

    private fun extractParentPath(path: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path.trim('/')
        } else {
            val file = java.io.File(path)
            file.parent ?: ""
        }
    }

    private fun extractFolderName(path: String): String {
        if (path.isEmpty() || path == "0") return "Documents"
        val segments = path.trim('/').split('/')
        val last = segments.lastOrNull() ?: "Documents"
        return if (last == "0") "Documents" else last
    }

    override fun getDocumentsInFolder(bucketId: String): Flow<List<DocumentModel>> = flow {
        if (isAllFilesAccessGranted() && bucketId != "ALL_RECENT") {
            val dir = File(bucketId)
            val results = dir.listFiles()?.filter { !it.isDirectory && !isExcludedMedia(it.name, "") }
                ?.map { fileToModel(it) } ?: emptyList()
            emit(results.sortedByDescending { it.id })
        } else {
            emit(fetchDocumentsFromAllVolumes(bucketId))
        }
    }.flowOn(Dispatchers.IO)

    private fun fetchDocumentsFromAllVolumes(folderPath: String?): List<DocumentModel> {
        val documents = mutableListOf<DocumentModel>()
        val selection = getDocumentSelection()
        
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context)
        } else {
            setOf("external")
        }

        volumes.forEach { volume ->
            val filesCollection = MediaStore.Files.getContentUri(volume)
            documents.addAll(fetchFromCollection(filesCollection, folderPath, selection))
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val downloadsCollection = MediaStore.Downloads.getContentUri(volume)
                documents.addAll(fetchFromCollection(downloadsCollection, folderPath, selection))
            }
        }

        return documents.distinctBy { it.uri.toString() }.sortedByDescending { it.id }
    }

    private fun fetchFromCollection(collection: Uri, folderPath: String?, baseSelection: String?): List<DocumentModel> {
        var selection = baseSelection
        var selectionArgs: Array<String>? = null
        
        if (folderPath != null && folderPath != "ALL_RECENT") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selection = if (selection != null) "$selection AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?" else "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
                selectionArgs = arrayOf("$folderPath%")
            } else {
                selection = if (selection != null) "$selection AND ${MediaStore.Files.FileColumns.DATA} LIKE ?" else "${MediaStore.Files.FileColumns.DATA} LIKE ?"
                selectionArgs = arrayOf("$folderPath/%")
            }
        }

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val results = mutableListOf<DocumentModel>()

        try {
            context.contentResolver.query(collection, fileProjection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                var count = 0
                val limit = if (folderPath == null) 2000 else 1000
                
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Unknown"
                    val sizeBytes = cursor.getLong(sizeCol)
                    val mimeType = cursor.getString(mimeCol) ?: "application/octet-stream"
                    val dateSec = cursor.getLong(dateCol)
                    
                    if (isExcludedMedia(name, mimeType)) continue

                    val uri = ContentUris.withAppendedId(collection, id)
                    val type = getDocumentType(name, mimeType)
                    
                    results.add(DocumentModel(
                        id = id,
                        uri = uri,
                        name = name,
                        size = Formatter.formatFileSize(context, sizeBytes),
                        mimeType = mimeType,
                        date = dateFormat.format(Date(dateSec * 1000)),
                        type = type
                    ))
                    count++
                }
            }
        } catch (e: Exception) { }
        return results
    }

    private fun getDocumentSelection(): String? {
        return null 
    }

    private fun isExcludedMedia(name: String, mimeType: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mediaExtensions = listOf(
            "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "svg", "bmp", "tiff", "tif",
            "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv",
            "mp3", "wav", "aac", "flac", "ogg", "m4a", "amr"
        )
        val appExtensions = listOf("apk", "aab", "ipa", "dmg", "iso")
        
        return ext in mediaExtensions || ext in appExtensions ||
               mimeType.startsWith("image/") || mimeType.startsWith("video/") || 
               mimeType.startsWith("audio/") || mimeType == "application/vnd.android.package-archive"
    }

    private fun getDocumentType(name: String, mimeType: String): DocumentType {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            mimeType == "application/pdf" || ext == "pdf" -> DocumentType.PDF
            ext in listOf("doc", "docx", "odt", "rtf") -> DocumentType.WORD
            ext in listOf("xls", "xlsx", "xlsm", "ods", "csv") -> DocumentType.EXCEL
            ext in listOf("ppt", "pptx", "odp") -> DocumentType.PPT
            ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2") -> DocumentType.ZIP
            ext in listOf("java", "kt", "c", "cpp", "h", "py", "js", "ts", "html", "css", "sql", "sh", "dart", "rs", "go") -> DocumentType.CODE
            ext in listOf("epub", "mobi", "azw", "azw3", "fb2") -> DocumentType.EBOOK
            ext in listOf("cer", "crt", "pem", "p12", "pfx", "key", "pub") -> DocumentType.SECURITY
            ext in listOf("gpx", "kml", "kmz", "geojson") -> DocumentType.LOCATION
            ext in listOf("dwg", "dxf", "stl", "obj", "step", "stp", "iges", "igs", "3mf", "fcstd", "blend") -> DocumentType.CAD
            else -> DocumentType.OTHER
        }
    }

    override fun getSharedDocuments(): Flow<List<DocumentModel>> = flow {
        val docs = messageDao?.getAllSharedDocumentsOnce()?.map { item ->
            val msg = item.message
            val attachment = item.attachments.firstOrNull()
            DocumentModel(
                id = msg.id.hashCode().toLong(),
                uri = (attachment?.storagePath ?: "").toUri(),
                name = attachment?.fileName ?: "Document",
                size = Formatter.formatFileSize(context, attachment?.fileSize ?: 0L),
                mimeType = attachment?.mimeType ?: "", 
                date = dateFormat.format(Date(msg.timestamp)),
                type = getDocumentType(attachment?.fileName ?: "", "")
            )
        } ?: emptyList()
        emit(docs)
    }.flowOn(Dispatchers.IO)

    override suspend fun resolveMetadata(uri: Uri): DocumentModel? {
        return null
    }

    override fun isFullAccessUnlocked(): Boolean {
        return isAllFilesAccessGranted()
    }

    override fun setFullAccessUri(uri: Uri?) {
        // No longer using SAF Tree for full access, using MANAGE_EXTERNAL_STORAGE instead
    }
}
