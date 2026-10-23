package tn.keeftalk.chat.data.repository

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.data.local.dao.BookDao
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.data.remote.*
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.BookRepository
import tn.keeftalk.chat.util.KeeftalkStore
import java.util.UUID

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

    private suspend fun enqueueSync(action: String, entityType: String, payloadJson: String) {
        runCatching {
            val item = BookSyncQueueEntity(
                id = "sync_${System.currentTimeMillis()}_${UUID.randomUUID()}",
                action = action,
                entityType = entityType,
                payloadJson = payloadJson,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            bookDao.insertSyncItem(item)
        }
    }

    override fun getBookSettingsFlow(): Flow<BookSettings> = _bookSettingsFlow.asStateFlow()

    override suspend fun getBookSettings(): BookSettings = _bookSettingsFlow.value

    override suspend fun updateBookSettings(settings: BookSettings) {
        saveSettingsToStore(settings)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "SETTINGS", json.encodeToString(BookSettingsSyncDto.fromDomain(settings, userId)))
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
        val entity = book.toEntity(json)
        bookDao.insertOrUpdateBook(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "BOOK", json.encodeToString(BookSyncDto.fromEntity(entity, userId)))
    }

    override suspend fun deleteBook(id: String) {
        bookDao.markBookDeleted(id)
        bookDao.getBookById(id)?.let { entity ->
            val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
            enqueueSync("UPSERT", "BOOK", json.encodeToString(BookSyncDto.fromEntity(entity, userId)))
        }
    }

    override suspend fun restoreBook(id: String) {
        bookDao.restoreBook(id)
        bookDao.getBookById(id)?.let { entity ->
            val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
            enqueueSync("UPSERT", "BOOK", json.encodeToString(BookSyncDto.fromEntity(entity, userId)))
        }
    }

    override suspend fun updateCustomCover(bookId: String, customCover: CustomCover?) {
        val existing = bookDao.getBookById(bookId) ?: return
        val customCoverJsonStr = customCover?.let { json.encodeToString(it) }
        val updated = existing.copy(
            customCoverJson = customCoverJsonStr,
            updatedAt = System.currentTimeMillis()
        )
        bookDao.insertOrUpdateBook(updated)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "BOOK", json.encodeToString(BookSyncDto.fromEntity(updated, userId)))
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
        val entity = progress.toEntity()
        bookDao.insertOrUpdatePageProgress(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "PROGRESS", json.encodeToString(BookProgressSyncDto.fromEntity(entity, userId)))
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
        val entity = highlight.toEntity()
        bookDao.insertOrUpdateHighlight(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "HIGHLIGHT", json.encodeToString(BookHighlightSyncDto.fromEntity(entity, userId)))
    }

    override suspend fun deleteHighlight(id: String) {
        bookDao.deleteHighlight(id)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("DELETE", "HIGHLIGHT", json.encodeToString(BookHighlightSyncDto(id = id, userId = userId, bookId = "", pageNumber = 0, startOffset = 0, endOffset = 0, selectedText = "", colorHex = "")))
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
        val entity = bookmark.toEntity()
        bookDao.insertOrUpdateBookmark(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "BOOKMARK", json.encodeToString(BookBookmarkSyncDto.fromEntity(entity, userId)))
    }

    override suspend fun deleteBookmark(bookId: String, pageNumber: Int) {
        bookDao.deleteBookmark(bookId, pageNumber)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("DELETE", "BOOKMARK", json.encodeToString(BookBookmarkSyncDto(userId = userId, bookId = bookId, pageNumber = pageNumber)))
    }

    override suspend fun getAllSessions(): List<BookReadingSession> {
        return bookDao.getAllSessions().map { it.toDomain() }
    }

    override suspend fun recordReadingSession(session: BookReadingSession) {
        val entity = session.toEntity()
        bookDao.insertSession(entity)
        val userId = runCatching { AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id }.getOrNull() ?: "local"
        enqueueSync("UPSERT", "SESSION", json.encodeToString(BookSessionSyncDto.fromEntity(entity, userId)))
        runCatching {
            val manager = integrationManagerProvider?.invoke()
                ?: tn.keeftalk.chat.di.AppModule.provideBookHabitIntegrationManager(context)
            manager.evaluateReadingHabitsForSession(session)
        }
    }

    override suspend fun syncBooksWithCloud() {
        val currentSettings = getBookSettings()
        if (!currentSettings.syncAcrossDevices) return

        runCatching {
            val supabase = AppModule.provideSupabaseClientAsync(context)
            val userId = supabase.auth.currentUserOrNull()?.id ?: return

            // 1. Flush pending sync queue items
            val pendingItems = bookDao.getPendingSyncItems()
            for (item in pendingItems) {
                runCatching {
                    when (item.entityType) {
                        "BOOK" -> {
                            val dto = json.decodeFromString<BookSyncDto>(item.payloadJson)
                            supabase.postgrest["user_books"].upsert(dto.copy(userId = userId))
                        }
                        "PROGRESS" -> {
                            val dto = json.decodeFromString<BookProgressSyncDto>(item.payloadJson)
                            supabase.postgrest["user_book_progress"].upsert(dto.copy(userId = userId))
                        }
                        "BOOKMARK" -> {
                            val dto = json.decodeFromString<BookBookmarkSyncDto>(item.payloadJson)
                            if (item.action == "DELETE") {
                                supabase.postgrest["user_book_bookmarks"].delete {
                                    filter {
                                        eq("user_id", userId)
                                        eq("book_id", dto.bookId)
                                        eq("page_number", dto.pageNumber)
                                    }
                                }
                            } else {
                                supabase.postgrest["user_book_bookmarks"].upsert(dto.copy(userId = userId))
                            }
                        }
                        "HIGHLIGHT" -> {
                            val dto = json.decodeFromString<BookHighlightSyncDto>(item.payloadJson)
                            if (item.action == "DELETE") {
                                supabase.postgrest["user_book_highlights"].delete {
                                    filter {
                                        eq("user_id", userId)
                                        eq("id", dto.id)
                                    }
                                }
                            } else {
                                supabase.postgrest["user_book_highlights"].upsert(dto.copy(userId = userId))
                            }
                        }
                        "SESSION" -> {
                            val dto = json.decodeFromString<BookSessionSyncDto>(item.payloadJson)
                            supabase.postgrest["user_book_sessions"].upsert(dto.copy(userId = userId))
                        }
                        "SETTINGS" -> {
                            val dto = json.decodeFromString<BookSettingsSyncDto>(item.payloadJson)
                            supabase.postgrest["user_book_settings"].upsert(dto.copy(userId = userId))
                        }
                    }
                    bookDao.deleteSyncItem(item.id)
                }
            }

            // 2. Pull remote updates
            val remoteBooks = supabase.postgrest["user_books"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<BookSyncDto>()

            for (remoteBook in remoteBooks) {
                val local = bookDao.getBookById(remoteBook.id)
                if (local == null) {
                    bookDao.insertOrUpdateBook(remoteBook.toEntity())
                } else if (remoteBook.updatedAt >= local.updatedAt) {
                    bookDao.insertOrUpdateBook(remoteBook.toEntity(localFilePath = local.filePath))
                }
            }

            val remoteProgress = supabase.postgrest["user_book_progress"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<BookProgressSyncDto>()

            for (p in remoteProgress) {
                bookDao.insertOrUpdatePageProgress(p.toEntity())
            }

            val remoteBookmarks = supabase.postgrest["user_book_bookmarks"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<BookBookmarkSyncDto>()

            for (bm in remoteBookmarks) {
                bookDao.insertOrUpdateBookmark(bm.toEntity())
            }

            val remoteHighlights = supabase.postgrest["user_book_highlights"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<BookHighlightSyncDto>()

            for (hl in remoteHighlights) {
                bookDao.insertOrUpdateHighlight(hl.toEntity())
            }

            val remoteSessions = supabase.postgrest["user_book_sessions"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<BookSessionSyncDto>()

            for (s in remoteSessions) {
                bookDao.insertSession(s.toEntity())
            }

            val remoteSettings = supabase.postgrest["user_book_settings"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<BookSettingsSyncDto>()
                .firstOrNull()

            if (remoteSettings != null) {
                saveSettingsToStore(remoteSettings.toDomain())
            }
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
            finishedAt = finishedAt,
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
            finishedAt = finishedAt,
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
