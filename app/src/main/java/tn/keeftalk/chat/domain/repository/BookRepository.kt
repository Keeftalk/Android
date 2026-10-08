package tn.keeftalk.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.*

interface BookRepository {
    fun getAllBooksFlow(): Flow<List<Book>>
    suspend fun getAllBooks(): List<Book>
    suspend fun getBookById(id: String): Book?
    fun getBookByIdFlow(id: String): Flow<Book?>
    suspend fun saveBook(book: Book)
    suspend fun deleteBook(id: String)
    suspend fun restoreBook(id: String)
    suspend fun updateCustomCover(bookId: String, customCover: CustomCover?)

    // Page Progress
    suspend fun getPageProgressForBook(bookId: String): List<BookPageProgress>
    fun getPageProgressForBookFlow(bookId: String): Flow<List<BookPageProgress>>
    suspend fun savePageProgress(progress: BookPageProgress)

    // Highlights & Notes
    suspend fun getHighlightsForBook(bookId: String): List<BookHighlight>
    fun getAllHighlightsFlow(): Flow<List<BookHighlight>>
    suspend fun saveHighlight(highlight: BookHighlight)
    suspend fun deleteHighlight(id: String)

    // Bookmarks
    suspend fun getBookmarksForBook(bookId: String): List<BookBookmark>
    fun getAllBookmarksFlow(): Flow<List<BookBookmark>>
    suspend fun saveBookmark(bookmark: BookBookmark)
    suspend fun deleteBookmark(bookId: String, pageNumber: Int)

    // Sessions
    suspend fun getAllSessions(): List<BookReadingSession>
    suspend fun recordReadingSession(session: BookReadingSession)

    // Settings
    fun getBookSettingsFlow(): Flow<BookSettings>
    suspend fun getBookSettings(): BookSettings
    suspend fun updateBookSettings(settings: BookSettings)
}
