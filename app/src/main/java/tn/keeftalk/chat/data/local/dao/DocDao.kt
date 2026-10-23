package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.*

@Dao
interface DocDao {
    @Query("SELECT * FROM docs WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun getAllDocsFlow(): Flow<List<DocEntity>>

    @Query("SELECT * FROM docs WHERE is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getAllDocs(): List<DocEntity>

    @Query("SELECT * FROM docs WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getDocById(id: String): DocEntity?

    @Query("SELECT * FROM docs WHERE id = :id AND is_deleted = 0 LIMIT 1")
    fun getDocByIdFlow(id: String): Flow<DocEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDoc(doc: DocEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDocs(docs: List<DocEntity>)

    @Query("UPDATE docs SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markDocDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE docs SET is_deleted = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun restoreDoc(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM docs WHERE id = :id")
    suspend fun hardDeleteDoc(id: String)

    // Bookmarks
    @Query("SELECT * FROM doc_bookmarks WHERE doc_id = :docId ORDER BY page_or_position ASC")
    suspend fun getBookmarksForDoc(docId: String): List<DocBookmarkEntity>

    @Query("SELECT * FROM doc_bookmarks WHERE doc_id = :docId ORDER BY page_or_position ASC")
    fun getBookmarksForDocFlow(docId: String): Flow<List<DocBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBookmark(bookmark: DocBookmarkEntity)

    @Query("DELETE FROM doc_bookmarks WHERE doc_id = :docId AND page_or_position = :pageOrPosition")
    suspend fun deleteBookmark(docId: String, pageOrPosition: Int)

    // Highlights
    @Query("SELECT * FROM doc_highlights WHERE doc_id = :docId ORDER BY created_at DESC")
    suspend fun getHighlightsForDoc(docId: String): List<DocHighlightEntity>

    @Query("SELECT * FROM doc_highlights WHERE doc_id = :docId ORDER BY created_at DESC")
    fun getHighlightsForDocFlow(docId: String): Flow<List<DocHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHighlight(highlight: DocHighlightEntity)

    @Query("DELETE FROM doc_highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)

    // Sync Queue
    @Query("SELECT * FROM doc_sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingSyncItems(): List<DocSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: DocSyncQueueEntity)

    @Query("DELETE FROM doc_sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: String)
}
