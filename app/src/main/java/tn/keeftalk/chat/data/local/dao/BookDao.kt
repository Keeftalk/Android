package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.*

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id AND is_deleted = 0 LIMIT 1")
    fun getBookByIdFlow(id: String): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBooks(books: List<BookEntity>)

    @Query("UPDATE books SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun markBookDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET is_deleted = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun restoreBook(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun hardDeleteBook(id: String)

    // Page Progress
    @Query("SELECT * FROM book_pages_progress WHERE book_id = :bookId")
    suspend fun getPageProgressForBook(bookId: String): List<BookPageProgressEntity>

    @Query("SELECT * FROM book_pages_progress WHERE book_id = :bookId")
    fun getPageProgressForBookFlow(bookId: String): Flow<List<BookPageProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePageProgress(progress: BookPageProgressEntity)

    // Highlights
    @Query("SELECT * FROM book_highlights WHERE book_id = :bookId ORDER BY created_at DESC")
    suspend fun getHighlightsForBook(bookId: String): List<BookHighlightEntity>

    @Query("SELECT * FROM book_highlights ORDER BY created_at DESC")
    fun getAllHighlightsFlow(): Flow<List<BookHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHighlight(highlight: BookHighlightEntity)

    @Query("DELETE FROM book_highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)

    // Bookmarks
    @Query("SELECT * FROM book_bookmarks WHERE book_id = :bookId")
    suspend fun getBookmarksForBook(bookId: String): List<BookBookmarkEntity>

    @Query("SELECT * FROM book_bookmarks ORDER BY created_at DESC")
    fun getAllBookmarksFlow(): Flow<List<BookBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBookmark(bookmark: BookBookmarkEntity)

    @Query("DELETE FROM book_bookmarks WHERE book_id = :bookId AND page_number = :pageNumber")
    suspend fun deleteBookmark(bookId: String, pageNumber: Int)

    // Reading Sessions
    @Query("SELECT * FROM book_reading_sessions ORDER BY created_at DESC")
    suspend fun getAllSessions(): List<BookReadingSessionEntity>

    @Query("SELECT * FROM book_reading_sessions WHERE book_id = :bookId ORDER BY created_at DESC")
    suspend fun getSessionsForBook(bookId: String): List<BookReadingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BookReadingSessionEntity)

    // Sync Queue
    @Query("SELECT * FROM book_sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingSyncItems(): List<BookSyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: BookSyncQueueEntity)

    @Query("DELETE FROM book_sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: String)
}
