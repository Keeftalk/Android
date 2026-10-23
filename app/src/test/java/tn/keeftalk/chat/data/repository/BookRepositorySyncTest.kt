package tn.keeftalk.chat.data.repository

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.data.remote.*
import tn.keeftalk.chat.domain.model.*

class BookRepositorySyncTest {

    @Test
    fun testBookSyncDtoMappingPreservesFinishedAtAndFields() {
        val now = System.currentTimeMillis()
        val finishedTime = now - 5000L

        val entity = BookEntity(
            id = "b_test_123",
            title = "Clean Code",
            author = "Robert C. Martin",
            totalPages = 464,
            format = "PDF",
            filePath = "/storage/emulated/0/books/clean_code.pdf",
            percentProgress = 100,
            lastReadPage = 464,
            markedDone = true,
            finishedAt = finishedTime,
            contentHash = "hash_sha256_12345",
            createdAt = now,
            updatedAt = now
        )

        val userId = "user_uuid_789"
        val dto = BookSyncDto.fromEntity(entity, userId)

        assertEquals("b_test_123", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Clean Code", dto.title)
        assertEquals("Robert C. Martin", dto.author)
        assertEquals(464, dto.totalPages)
        assertEquals(100, dto.percentProgress)
        assertEquals(finishedTime, dto.finishedAt)
        assertEquals("hash_sha256_12345", dto.contentHash)

        val mappedBack = dto.toEntity(localFilePath = "/local/path/clean_code.pdf")

        assertEquals("b_test_123", mappedBack.id)
        assertEquals("/local/path/clean_code.pdf", mappedBack.filePath)
        assertEquals(finishedTime, mappedBack.finishedAt)
        assertEquals("hash_sha256_12345", mappedBack.contentHash)
        assertTrue(mappedBack.markedDone)
    }

    @Test
    fun testProgressSyncDtoConversion() {
        val entity = BookPageProgressEntity(
            bookId = "b_100",
            pageNumber = 12,
            isRead = true,
            phraseDockIndex = 2,
            freeX = 100.5f,
            freeY = 200.0f,
            updatedAt = 1000L
        )

        val dto = BookProgressSyncDto.fromEntity(entity, "user_1")
        assertEquals("b_100", dto.bookId)
        assertEquals(12, dto.pageNumber)
        assertTrue(dto.isRead)
        assertEquals(2, dto.phraseDockIndex)

        val restored = dto.toEntity()
        assertEquals("b_100", restored.bookId)
        assertEquals(12, restored.pageNumber)
        assertTrue(restored.isRead)
        assertEquals(100.5f, restored.freeX)
    }

    @Test
    fun testBookmarkAndHighlightSyncDtoConversions() {
        val bookmarkEntity = BookBookmarkEntity(
            bookId = "b_200",
            pageNumber = 42,
            chapterTitle = "Chapter 4",
            excerpt = "The answer is 42",
            isUser = true,
            createdAt = 5000L
        )

        val bookmarkDto = BookBookmarkSyncDto.fromEntity(bookmarkEntity, "u_42")
        assertEquals("b_200", bookmarkDto.bookId)
        assertEquals("Chapter 4", bookmarkDto.chapterTitle)

        val restoredBm = bookmarkDto.toEntity()
        assertEquals("b_200", restoredBm.bookId)
        assertEquals(42, restoredBm.pageNumber)

        val highlightEntity = BookHighlightEntity(
            id = "hl_1",
            bookId = "b_200",
            pageNumber = 42,
            startOffset = 10,
            endOffset = 50,
            selectedText = "Important quote",
            colorHex = "#2ECC71",
            isQuote = true,
            noteText = "Remember this",
            createdAt = 6000L
        )

        val highlightDto = BookHighlightSyncDto.fromEntity(highlightEntity, "u_42")
        assertEquals("hl_1", highlightDto.id)
        assertEquals("Important quote", highlightDto.selectedText)

        val restoredHl = highlightDto.toEntity()
        assertEquals("hl_1", restoredHl.id)
        assertEquals("#2ECC71", restoredHl.colorHex)
        assertTrue(restoredHl.isQuote)
    }

    @Test
    fun testBookSettingsSyncDtoConversion() {
        val domain = BookSettings(
            showReadMarker = true,
            buttonSizePx = 44,
            syncAcrossDevices = true,
            theme = "sepia",
            dailyGoalMinutes = 45,
            yearlyGoalBooks = 30
        )

        val dto = BookSettingsSyncDto.fromDomain(domain, "user_555")
        assertEquals("user_555", dto.userId)
        assertEquals(44, dto.buttonSizePx)
        assertTrue(dto.syncAcrossDevices)
        assertEquals("sepia", dto.theme)

        val restoredDomain = dto.toDomain()
        assertEquals(44, restoredDomain.buttonSizePx)
        assertTrue(restoredDomain.syncAcrossDevices)
        assertEquals("sepia", restoredDomain.theme)
        assertEquals(45, restoredDomain.dailyGoalMinutes)
    }

    @Test
    fun testDeduplicationMatchingLogic() {
        val existingBook = Book(
            id = "b_existing_1",
            title = "The Pragmatic Programmer",
            author = "Andrew Hunt",
            totalPages = 350,
            contentHash = "sha256_pragmatic_prog_hash"
        )

        val allBooks = listOf(existingBook)

        val newImportHash = "sha256_pragmatic_prog_hash"
        val isDuplicateByHash = allBooks.any { !it.isDeleted && (!it.contentHash.isNullOrEmpty() && it.contentHash == newImportHash) }
        assertTrue("Import with identical content hash should be recognized as duplicate", isDuplicateByHash)

        val isDuplicateByTitle = allBooks.any { !it.isDeleted && it.title.equals("The Pragmatic Programmer", ignoreCase = true) }
        assertTrue("Import with identical title should be recognized as duplicate", isDuplicateByTitle)

        val isDifferentBook = allBooks.any { !it.isDeleted && it.title.equals("Clean Code", ignoreCase = true) }
        assertFalse("Distinct book title should not match duplicate logic", isDifferentBook)
    }

    @Test
    fun testProgressCalculationMonotonicNonDecreasing() {
        val totalPages = 100
        val lastReadPage = 50
        val readPagesCount = 20
        val currentPercent = 50

        val posPercent = ((lastReadPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
        val pageReadPercent = ((readPagesCount.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)

        val updatedPercent = maxOf(currentPercent, posPercent, pageReadPercent)

        assertEquals("Progress percent must not decrease when read pages count is smaller than max page position", 50, updatedPercent)
    }
}
