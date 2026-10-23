package tn.keeftalk.chat.domain.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.*

interface DocRepository {
    fun getDocSettingsFlow(): Flow<DocSettings>
    suspend fun getDocSettings(): DocSettings
    suspend fun updateDocSettings(settings: DocSettings)

    fun getAllDocsFlow(): Flow<List<Doc>>
    suspend fun getAllDocs(): List<Doc>
    suspend fun getDocById(id: String): Doc?
    fun getDocByIdFlow(id: String): Flow<Doc?>

    suspend fun saveDoc(doc: Doc)
    suspend fun importDocFromUri(context: Context, uri: Uri, customTitle: String? = null): Doc?
    suspend fun importDocsFromUris(context: Context, uris: List<Uri>): List<Doc>
    suspend fun importDocFolderFromUri(context: Context, treeUri: Uri): List<Doc>
    suspend fun deleteDoc(id: String)
    suspend fun restoreDoc(id: String)
    suspend fun updateCustomCover(docId: String, customCover: CustomDocCover?)

    suspend fun getBookmarksForDoc(docId: String): List<DocBookmark>
    fun getBookmarksForDocFlow(docId: String): Flow<List<DocBookmark>>
    suspend fun saveBookmark(bookmark: DocBookmark)
    suspend fun deleteBookmark(docId: String, pageOrPosition: Int)

    suspend fun getHighlightsForDoc(docId: String): List<DocHighlight>
    fun getHighlightsForDocFlow(docId: String): Flow<List<DocHighlight>>
    suspend fun saveHighlight(highlight: DocHighlight)
    suspend fun deleteHighlight(id: String)

    suspend fun syncDocsWithCloud()
}
