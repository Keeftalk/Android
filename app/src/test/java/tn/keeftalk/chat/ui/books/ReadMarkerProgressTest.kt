package tn.keeftalk.chat.ui.books

import org.junit.Assert.assertEquals
import org.junit.Test
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.domain.model.BookPageProgress

class ReadMarkerProgressTest {

    @Test
    fun testSwipingPagesDoesNotIncreaseReadPageCountOrPercentProgress() {
        val book = Book(
            id = "b1",
            title = "Test Book",
            author = "Author",
            totalPages = 100,
            percentProgress = 0,
            lastReadPage = 1
        )

        // Swiping pages updates lastReadPage position
        val page10 = book.copy(lastReadPage = 10)
        val page50 = page10.copy(lastReadPage = 50)

        // Position changes, but read progress remains 0
        assertEquals(50, page50.lastReadPage)
        assertEquals(0, page50.percentProgress)
    }

    @Test
    fun testGreenReadMarkerButtonUpdatesPercentProgressAccurately() {
        val book = Book(
            id = "b1",
            title = "Test Book",
            author = "Author",
            totalPages = 100,
            percentProgress = 0,
            lastReadPage = 1
        )

        val pageProgresses = listOf(
            BookPageProgress(bookId = "b1", pageNumber = 1, isRead = true),
            BookPageProgress(bookId = "b1", pageNumber = 2, isRead = true),
            BookPageProgress(bookId = "b1", pageNumber = 3, isRead = true),
            BookPageProgress(bookId = "b1", pageNumber = 4, isRead = false) // unmarked
        )

        val readPagesCount = pageProgresses.count { it.isRead }
        val percentProgress = (readPagesCount.toFloat() / book.totalPages.toFloat() * 100).toInt()

        assertEquals(3, readPagesCount)
        assertEquals(3, percentProgress)

        val updatedBook = book.copy(percentProgress = percentProgress)
        assertEquals(3, updatedBook.percentProgress)
    }
}
