package tn.keeftalk.chat.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.data.local.dao.DocDao
import tn.keeftalk.chat.data.local.entities.DocSyncQueueEntity
import tn.keeftalk.chat.data.remote.*
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.DocRepository
import tn.keeftalk.chat.util.KeeftalkStore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DocRepositoryImpl(
    private val docDao: DocDao,
    private val context: Context
) : DocRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val store by lazy { KeeftalkStore.getInstance() }

    private val _docSettingsFlow = MutableStateFlow(loadSettingsFromStore())

    private fun loadSettingsFromStore(): DocSettings {
        val raw = store.getString("doc_settings_json", null)
        return if (!raw.isNullOrEmpty()) {
            runCatching { json.decodeFromString<DocSettings>(raw) }.getOrDefault(DocSettings())
        } else {
            DocSettings()
        }
    }

    private fun saveSettingsToStore(settings: DocSettings) {
        store.putString("doc_settings_json", json.encodeToString(settings))
        _docSettingsFlow.value = settings
    }

    private suspend fun enqueueSync(action: String, entityType: String, payloadJson: String) {
        runCatching {
            val item = DocSyncQueueEntity(
                id = "doc_sync_${System.currentTimeMillis()}_${UUID.randomUUID()}",
                action = action,
                entityType = entityType,
                payloadJson = payloadJson,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            docDao.insertSyncItem(item)
        }
    }

    override fun getDocSettingsFlow(): Flow<DocSettings> = _docSettingsFlow.asStateFlow()

    override suspend fun getDocSettings(): DocSettings = _docSettingsFlow.value

    override suspend fun updateDocSettings(settings: DocSettings) {
        saveSettingsToStore(settings)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "SETTINGS", json.encodeToString(DocSettingsSyncDto.fromDomain(settings, userId)))
    }

    override fun getAllDocsFlow(): Flow<List<Doc>> {
        return docDao.getAllDocsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllDocs(): List<Doc> {
        return docDao.getAllDocs().map { it.toDomain() }
    }

    override suspend fun getDocById(id: String): Doc? {
        return docDao.getDocById(id)?.toDomain()
    }

    override fun getDocByIdFlow(id: String): Flow<Doc?> {
        return docDao.getDocByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun saveDoc(doc: Doc) {
        val entity = doc.toEntity(json)
        docDao.insertOrUpdateDoc(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "DOC", json.encodeToString(DocSyncDto.fromEntity(entity, userId)))
    }

    override suspend fun importDocFromUri(context: Context, uri: Uri, customTitle: String?): Doc? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Document_${System.currentTimeMillis()}"
            var fileSize = 0L
            var mimeType = context.contentResolver.getType(uri) ?: ""

            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) fileName = name
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    fileName = file.name
                    fileSize = file.length()
                }
            }

            val ext = fileName.substringAfterLast('.', "")
            val title = customTitle?.takeIf { it.isNotBlank() }
                ?: if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName

            val docsDir = File(context.filesDir, "docs").apply { if (!exists()) mkdirs() }
            val docId = "doc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            val destFile = File(docsDir, "${docId}_$fileName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (fileSize == 0L && destFile.exists()) {
                fileSize = destFile.length()
            }

            val docFormat = DocFormat.fromExtension(ext, mimeType)

            val newDoc = Doc(
                id = docId,
                title = title,
                authorOrSource = "Local Import",
                totalPagesOrSections = 1,
                format = docFormat,
                filePath = destFile.absolutePath,
                fileSize = fileSize,
                mimeType = mimeType,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            saveDoc(newDoc)
            newDoc
        } catch (e: Exception) {
            android.util.Log.e("DocRepositoryImpl", "Failed to import document from Uri", e)
            null
        }
    }

    override suspend fun importDocsFromUris(context: Context, uris: List<Uri>): List<Doc> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<Doc>()
        for (uri in uris) {
            importDocFromUri(context, uri)?.let { imported.add(it) }
        }
        imported
    }

    override suspend fun importDocFolderFromUri(context: Context, treeUri: Uri): List<Doc> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<Doc>()
        try {
            val rootDocFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            if (rootDocFile != null && rootDocFile.exists()) {
                traverseAndImportFolder(context, rootDocFile, imported)
            }
        } catch (e: Exception) {
            android.util.Log.e("DocRepositoryImpl", "Failed to import folder tree", e)
        }
        imported
    }

    private suspend fun traverseAndImportFolder(context: Context, folder: androidx.documentfile.provider.DocumentFile, results: MutableList<Doc>) {
        val files = folder.listFiles()
        for (docFile in files) {
            if (docFile.isDirectory) {
                traverseAndImportFolder(context, docFile, results)
            } else if (docFile.isFile) {
                val name = docFile.name ?: continue
                val ext = name.substringAfterLast('.', "")
                val supportedExtensions = listOf("pdf", "md", "markdown", "html", "htm", "txt", "log", "conf", "ini", "env", "docx", "doc", "epub", "kt", "java", "py", "js", "ts", "cpp", "c", "h", "cs", "rs", "go", "sql", "sh", "css", "json", "xml", "yaml", "yml")
                if (ext.lowercase() in supportedExtensions) {
                    importDocFromUri(context, docFile.uri)?.let { results.add(it) }
                }
            }
        }
    }

    override suspend fun deleteDoc(id: String) {
        docDao.markDocDeleted(id)
        docDao.getDocById(id)?.let { entity ->
            val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
            enqueueSync("UPSERT", "DOC", json.encodeToString(DocSyncDto.fromEntity(entity, userId)))
        }
    }

    override suspend fun restoreDoc(id: String) {
        docDao.restoreDoc(id)
        docDao.getDocById(id)?.let { entity ->
            val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
            enqueueSync("UPSERT", "DOC", json.encodeToString(DocSyncDto.fromEntity(entity, userId)))
        }
    }

    override suspend fun updateCustomCover(docId: String, customCover: CustomDocCover?) {
        val existing = docDao.getDocById(docId) ?: return
        val customCoverJsonStr = customCover?.let { json.encodeToString(it) }
        val updated = existing.copy(
            customCoverJson = customCoverJsonStr,
            updatedAt = System.currentTimeMillis()
        )
        docDao.insertOrUpdateDoc(updated)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "DOC", json.encodeToString(DocSyncDto.fromEntity(updated, userId)))
    }

    override suspend fun getBookmarksForDoc(docId: String): List<DocBookmark> {
        return docDao.getBookmarksForDoc(docId).map { it.toDomain() }
    }

    override fun getBookmarksForDocFlow(docId: String): Flow<List<DocBookmark>> {
        return docDao.getBookmarksForDocFlow(docId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveBookmark(bookmark: DocBookmark) {
        val entity = bookmark.toEntity()
        docDao.insertOrUpdateBookmark(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "BOOKMARK", json.encodeToString(DocBookmarkSyncDto.fromEntity(entity, userId)))
    }

    override suspend fun deleteBookmark(docId: String, pageOrPosition: Int) {
        docDao.deleteBookmark(docId, pageOrPosition)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("DELETE", "BOOKMARK", json.encodeToString(DocBookmarkSyncDto(userId = userId, docId = docId, pageOrPosition = pageOrPosition)))
    }

    override suspend fun getHighlightsForDoc(docId: String): List<DocHighlight> {
        return docDao.getHighlightsForDoc(docId).map { it.toDomain() }
    }

    override fun getHighlightsForDocFlow(docId: String): Flow<List<DocHighlight>> {
        return docDao.getHighlightsForDocFlow(docId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveHighlight(highlight: DocHighlight) {
        val entity = highlight.toEntity()
        docDao.insertOrUpdateHighlight(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "HIGHLIGHT", json.encodeToString(DocHighlightSyncDto.fromEntity(entity, userId)))
    }

    override suspend fun deleteHighlight(id: String) {
        docDao.deleteHighlight(id)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("DELETE", "HIGHLIGHT", json.encodeToString(DocHighlightSyncDto(id = id, userId = userId, docId = "", pageOrPosition = 0, selectedText = "")))
    }

    override suspend fun syncDocsWithCloud() {
        val currentSettings = getDocSettings()
        if (!currentSettings.syncAcrossDevices) return

        runCatching {
            val supabase = AppModule.provideSupabaseClientAsync(context)
            val userId = supabase.auth.currentUserOrNull()?.id ?: return

            // 1. Flush pending sync queue items
            val pendingItems = docDao.getPendingSyncItems()
            for (item in pendingItems) {
                runCatching {
                    when (item.entityType) {
                        "DOC" -> {
                            val dto = json.decodeFromString<DocSyncDto>(item.payloadJson)
                            supabase.postgrest["user_docs"].upsert(dto.copy(userId = userId))
                        }
                        "BOOKMARK" -> {
                            val dto = json.decodeFromString<DocBookmarkSyncDto>(item.payloadJson)
                            if (item.action == "DELETE") {
                                supabase.postgrest["user_doc_bookmarks"].delete {
                                    filter {
                                        eq("user_id", userId)
                                        eq("doc_id", dto.docId)
                                        eq("page_or_position", dto.pageOrPosition)
                                    }
                                }
                            } else {
                                supabase.postgrest["user_doc_bookmarks"].upsert(dto.copy(userId = userId))
                            }
                        }
                        "HIGHLIGHT" -> {
                            val dto = json.decodeFromString<DocHighlightSyncDto>(item.payloadJson)
                            if (item.action == "DELETE") {
                                supabase.postgrest["user_doc_highlights"].delete {
                                    filter {
                                        eq("user_id", userId)
                                        eq("id", dto.id)
                                    }
                                }
                            } else {
                                supabase.postgrest["user_doc_highlights"].upsert(dto.copy(userId = userId))
                            }
                        }
                        "SETTINGS" -> {
                            val dto = json.decodeFromString<DocSettingsSyncDto>(item.payloadJson)
                            supabase.postgrest["user_doc_settings"].upsert(dto.copy(userId = userId))
                        }
                    }
                    docDao.deleteSyncItem(item.id)
                }
            }

            // 2. Pull remote updates
            val remoteDocs = supabase.postgrest["user_docs"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<DocSyncDto>()

            for (remoteDoc in remoteDocs) {
                val local = docDao.getDocById(remoteDoc.id)
                if (local == null) {
                    docDao.insertOrUpdateDoc(remoteDoc.toEntity())
                } else if (remoteDoc.updatedAt >= local.updatedAt) {
                    docDao.insertOrUpdateDoc(remoteDoc.toEntity(localFilePath = local.filePath))
                }
            }

            val remoteBookmarks = supabase.postgrest["user_doc_bookmarks"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<DocBookmarkSyncDto>()

            for (bm in remoteBookmarks) {
                docDao.insertOrUpdateBookmark(bm.toEntity())
            }

            val remoteHighlights = supabase.postgrest["user_doc_highlights"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<DocHighlightSyncDto>()

            for (hl in remoteHighlights) {
                docDao.insertOrUpdateHighlight(hl.toEntity())
            }

            val remoteSettings = supabase.postgrest["user_doc_settings"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<DocSettingsSyncDto>()
                .firstOrNull()

            if (remoteSettings != null) {
                saveSettingsToStore(remoteSettings.toDomain())
            }
        }
    }
}
