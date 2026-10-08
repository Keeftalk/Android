package tn.keeftalk.chat.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.data.local.dao.BookDao
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.BookRepository
import tn.keeftalk.chat.util.KeeftalkStore

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val context: Context,
    private val integrationManagerProvider: (() -> tn.keeftalk.chat.domain.engine.BookHabitIntegrationManager)? = null
) : BookRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val store by lazy { KeeftalkStore.getInstance() }

    private val _bookSettingsFlow = MutableStateFlow(loadSettingsFromStore())

    private fun loadSettingsFromStore(): BookSettings {
        val raw = store.getString("book_settings_json", null)
        return if (!raw.isNullOrEmpty()) {
            try {
                json.decodeFromString(raw)
            } catch (e: Exception) {
                BookSettings()
            }
        } else {
            BookSettings()
        }
    }

    private fun saveSettingsToStore(settings: BookSettings) {
        store.putString("book_settings_json", json.encodeToString(settings))
        _bookSettingsFlow.value = settings
    }

    override fun getBookSettingsFlow(): Flow<BookSettings> = _bookSettingsFlow.asStateFlow()

    override suspend fun getBookSettings(): BookSettings = _bookSettingsFlow.value

    override suspend fun updateBookSettings(settings: BookSettings) {
        saveSettingsToStore(settings)
    }

    override fun getAllBooksFlow(): Flow<List<Book>> {
        return bookDao.getAllBooksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllBooks(): List<Book> {
        return bookDao.getAllBooks().map { it.toDomain() }
    }

    override suspend fun getBookById(id: String): Book? {
        return bookDao.getBookById(id)?.toDomain()
    }

    override fun getBookByIdFlow(id: String): Flow<Book?> {
        return bookDao.getBookByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun saveBook(book: Book) {
        bookDao.insertOrUpdateBook(book.toEntity(json))
    }

    override suspend fun deleteBook(id: String) {
        bookDao.markBookDeleted(id)
    }

    override suspend fun restoreBook(id: String) {
        bookDao.restoreBook(id)
    }

    override suspend fun updateCustomCover(bookId: String, customCover: CustomCover?) {
        val existing = bookDao.getBookById(bookId) ?: return
        val customCoverJsonStr = customCover?.let { json.encodeToString(it) }
        val updated = existing.copy(
            customCoverJson = customCoverJsonStr,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.insertOrUpdateBook(updated)
    }

    override suspend fun getPageProgressForBook(bookId: String): List<BookPageProgress> {
        return bookDao.getPageProgressForBook(bookId).map { it.toDomain() }
    }

    override fun getPageProgressForBookFlow(bookId: String): Flow<List<BookPageProgress>> {
        return bookDao.getPageProgressForBookFlow(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun savePageProgress(progress: BookPageProgress) {
        bookDao.insertOrUpdatePageProgress(progress.toEntity())
    }

    override suspend fun getHighlightsForBook(bookId: String): List<BookHighlight> {
        return bookDao.getHighlightsForBook(bookId).map { it.toDomain() }
    }

    override fun getAllHighlightsFlow(): Flow<List<BookHighlight>> {
        return bookDao.getAllHighlightsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveHighlight(highlight: BookHighlight) {
        bookDao.insertOrUpdateHighlight(highlight.toEntity())
    }

    override suspend fun deleteHighlight(id: String) {
        bookDao.deleteHighlight(id)
    }

    override suspend fun getBookmarksForBook(bookId: String): List<BookBookmark> {
        return bookDao.getBookmarksForBook(bookId).map { it.toDomain() }
    }

    override fun getAllBookmarksFlow(): Flow<List<BookBookmark>> {
        return bookDao.getAllBookmarksFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveBookmark(bookmark: BookBookmark) {
        bookDao.insertOrUpdateBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(bookId: String, pageNumber: Int) {
        bookDao.deleteBookmark(bookId, pageNumber)
    }

    override suspend fun getAllSessions(): List<BookReadingSession> {
        return bookDao.getAllSessions().map { it.toDomain() }
    }

    override suspend fun recordReadingSession(session: BookReadingSession) {
        bookDao.insertSession(session.toEntity())
        runCatching {
            val manager = integrationManagerProvider?.invoke()
                ?: tn.keeftalk.chat.di.AppModule.provideBookHabitIntegrationManager(context)
            manager.evaluateReadingHabitsForSession(session)
        }
    }

    // Mapper extensions
    private fun BookEntity.toDomain(): Book {
        val cover = customCoverJson?.let {
            try { json.decodeFromString<CustomCover>(it) } catch (e: Exception) { null }
        }
        return Book(
            id = id,
            title = title,
            author = author,
            totalPages = totalPages,
            format = format,
            filePath = filePath,
            coverUrl = coverUrl,
            customCover = cover,
            percentProgress = percentProgress,
            lastReadPage = lastReadPage,
            hoursRead = hoursRead,
            pagesPerDay = pagesPerDay,
            speedPagesPerHour = speedPagesPerHour,
            insightText = insightText,
            isArchived = isArchived,
            isDeleted = isDeleted,
            isFavorite = isFavorite,
            markedDone = markedDone,
            originalProgress = originalProgress,
            originalInsight = originalInsight,
            createdAt = createdAt,
            updatedAt = updatedAt,
            contentHash = contentHash
        )
    }

    private fun Book.toEntity(jsonSerializer: Json): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            author = author,
            totalPages = totalPages,
            format = format,
            filePath = filePath,
            coverUrl = coverUrl,
            customCoverJson = customCover?.let { jsonSerializer.encodeToString(it) },
            percentProgress = percentProgress,
            lastReadPage = lastReadPage,
            hoursRead = hoursRead,
            pagesPerDay = pagesPerDay,
            speedPagesPerHour = speedPagesPerHour,
            insightText = insightText,
            isArchived = isArchived,
            isDeleted = isDeleted,
            isFavorite = isFavorite,
            markedDone = markedDone,
            originalProgress = originalProgress,
            originalInsight = originalInsight,
            createdAt = createdAt,
            updatedAt = updatedAt,
            contentHash = contentHash
        )
    }

    private fun BookPageProgressEntity.toDomain() = BookPageProgress(
        bookId = bookId,
        pageNumber = pageNumber,
        isRead = isRead,
        phraseDockIndex = phraseDockIndex,
        freeX = freeX,
        freeY = freeY,
        updatedAt = updatedAt
    )

    private fun BookPageProgress.toEntity() = BookPageProgressEntity(
        bookId = bookId,
        pageNumber = pageNumber,
        isRead = isRead,
        phraseDockIndex = phraseDockIndex,
        freeX = freeX,
        freeY = freeY,
        updatedAt = updatedAt
    )

    private fun BookHighlightEntity.toDomain() = BookHighlight(
        id = id,
        bookId = bookId,
        pageNumber = pageNumber,
        startOffset = startOffset,
        endOffset = endOffset,
        selectedText = selectedText,
        colorHex = colorHex,
        isQuote = isQuote,
        noteText = noteText,
        createdAt = createdAt
    )

    private fun BookHighlight.toEntity() = BookHighlightEntity(
        id = id,
        bookId = bookId,
        pageNumber = pageNumber,
        startOffset = startOffset,
        endOffset = endOffset,
        selectedText = selectedText,
        colorHex = colorHex,
        isQuote = isQuote,
        noteText = noteText,
        createdAt = createdAt
    )

    private fun BookBookmarkEntity.toDomain() = BookBookmark(
        bookId = bookId,
        pageNumber = pageNumber,
        chapterTitle = chapterTitle,
        excerpt = excerpt,
        isUser = isUser,
        createdAt = createdAt
    )

    private fun BookBookmark.toEntity() = BookBookmarkEntity(
        bookId = bookId,
        pageNumber = pageNumber,
        chapterTitle = chapterTitle,
        excerpt = excerpt,
        isUser = isUser,
        createdAt = createdAt
    )

    private fun BookReadingSessionEntity.toDomain() = BookReadingSession(
        id = id,
        bookId = bookId,
        startTime = startTime,
        endTime = endTime,
        pagesRead = pagesRead,
        durationSeconds = durationSeconds,
        createdAt = createdAt
    )

    private fun BookReadingSession.toEntity() = BookReadingSessionEntity(
        id = id,
        bookId = bookId,
        startTime = startTime,
        endTime = endTime,
        pagesRead = pagesRead,
        durationSeconds = durationSeconds,
        createdAt = createdAt
    )
}
