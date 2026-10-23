package tn.keeftalk.chat.data.repository

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.data.remote.*
import tn.keeftalk.chat.domain.model.*

class DocRepositorySyncTest {

    @Test
    fun testDocSyncDtoMappingPreservesFields() {
        val now = System.currentTimeMillis()

        val entity = DocEntity(
            id = "doc_test_123",
            title = "Kotlin Documentation",
            authorOrSource = "JetBrains",
            totalPagesOrSections = 120,
            format = "CODE",
            filePath = "/storage/docs/kotlin_doc.kt",
            fileSize = 45000L,
            mimeType = "text/x-kotlin",
            percentProgress = 45,
            lastReadPage = 54,
            isFavorite = true,
            contentHash = "hash_doc_kotlin_999",
            createdAt = now,
            updatedAt = now
        )

        val userId = "user_uuid_456"
        val dto = DocSyncDto.fromEntity(entity, userId)

        assertEquals("doc_test_123", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Kotlin Documentation", dto.title)
        assertEquals("JetBrains", dto.authorOrSource)
        assertEquals("CODE", dto.format)
        assertEquals(45, dto.percentProgress)
        assertEquals(54, dto.lastReadPage)
        assertTrue(dto.isFavorite)
        assertEquals("hash_doc_kotlin_999", dto.contentHash)

        val mappedBack = dto.toEntity(localFilePath = "/local/path/kotlin_doc.kt")

        assertEquals("doc_test_123", mappedBack.id)
        assertEquals("/local/path/kotlin_doc.kt", mappedBack.filePath)
        assertEquals(45, mappedBack.percentProgress)
        assertEquals(54, mappedBack.lastReadPage)
        assertTrue(mappedBack.isFavorite)
    }

    @Test
    fun testDocBookmarkAndHighlightSyncDtoConversions() {
        val bookmarkEntity = DocBookmarkEntity(
            docId = "doc_200",
            pageOrPosition = 15,
            chapterTitle = "Architecture Overview",
            excerpt = "Unidirectional Data Flow",
            createdAt = 5000L
        )

        val bookmarkDto = DocBookmarkSyncDto.fromEntity(bookmarkEntity, "u_777")
        assertEquals("doc_200", bookmarkDto.docId)
        assertEquals("Architecture Overview", bookmarkDto.chapterTitle)

        val restoredBm = bookmarkDto.toEntity()
        assertEquals("doc_200", restoredBm.docId)
        assertEquals(15, restoredBm.pageOrPosition)

        val highlightEntity = DocHighlightEntity(
            id = "doc_hl_1",
            docId = "doc_200",
            pageOrPosition = 15,
            startOffset = 10,
            endOffset = 50,
            selectedText = "Coroutines simplify async code",
            colorHex = "#2ECC71",
            noteText = "Study structured concurrency",
            createdAt = 6000L
        )

        val highlightDto = DocHighlightSyncDto.fromEntity(highlightEntity, "u_777")
        assertEquals("doc_hl_1", highlightDto.id)
        assertEquals("Coroutines simplify async code", highlightDto.selectedText)

        val restoredHl = highlightDto.toEntity()
        assertEquals("doc_hl_1", restoredHl.id)
        assertEquals("#2ECC71", restoredHl.colorHex)
        assertEquals("Study structured concurrency", restoredHl.noteText)
    }

    @Test
    fun testDocSettingsSyncDtoConversion() {
        val domain = DocSettings(
            theme = "sepia",
            fontSizeSp = 18,
            keepScreenOn = true,
            showLineNumbers = false,
            wordWrap = true,
            syncAcrossDevices = true
        )

        val dto = DocSettingsSyncDto.fromDomain(domain, "user_888")
        assertEquals("user_888", dto.userId)
        assertEquals("sepia", dto.theme)
        assertEquals(18, dto.fontSizeSp)
        assertFalse(dto.showLineNumbers)
        assertTrue(dto.syncAcrossDevices)

        val restoredDomain = dto.toDomain()
        assertEquals("sepia", restoredDomain.theme)
        assertEquals(18, restoredDomain.fontSizeSp)
        assertFalse(restoredDomain.showLineNumbers)
        assertTrue(restoredDomain.syncAcrossDevices)
    }

    @Test
    fun testDocFormatDetectionFromExtensionAndMimeType() {
        assertEquals(DocFormat.PDF, DocFormat.fromExtension("pdf"))
        assertEquals(DocFormat.MARKDOWN, DocFormat.fromExtension("md"))
        assertEquals(DocFormat.MARKDOWN, DocFormat.fromExtension("markdown"))
        assertEquals(DocFormat.HTML, DocFormat.fromExtension("html"))
        assertEquals(DocFormat.TXT, DocFormat.fromExtension("txt"))
        assertEquals(DocFormat.DOCX, DocFormat.fromExtension("docx"))
        assertEquals(DocFormat.EPUB, DocFormat.fromExtension("epub"))
        assertEquals(DocFormat.CODE, DocFormat.fromExtension("kt"))
        assertEquals(DocFormat.CODE, DocFormat.fromExtension("py"))
        assertEquals(DocFormat.CODE, DocFormat.fromExtension("sql"))
    }
}
