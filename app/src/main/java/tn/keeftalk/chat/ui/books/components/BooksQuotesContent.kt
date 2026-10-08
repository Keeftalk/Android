package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.domain.model.BookBookmark
import tn.keeftalk.chat.domain.model.BookHighlight

@Composable
fun BooksQuotesContent(
    books: List<Book>,
    highlights: List<BookHighlight>,
    bookmarks: List<BookBookmark>,
    activeTab: String, // "q", "n", "h", "b"
    selectedBookFilterId: String?,
    onTabSelect: (String) -> Unit,
    onBookFilterSelect: (String?) -> Unit,
    onHighlightClick: (BookHighlight) -> Unit,
    onBookmarkClick: (BookBookmark) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (BookBookmark) -> Unit,
    currentTab: String = "quo",
    onSelectTab: (String) -> Unit = {},
    isRailExpanded: Boolean = false,
    onOpenInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExportPanelOpen by remember { mutableStateOf(false) }

    val filteredHighlights = highlights.filter { h ->
        selectedBookFilterId == null || h.bookId == selectedBookFilterId
    }
    val filteredBookmarks = bookmarks.filter { b ->
        selectedBookFilterId == null || b.bookId == selectedBookFilterId
    }

    val quotes = filteredHighlights.filter { it.isQuote }
    val notes = filteredHighlights.filter { it.noteText.isNotEmpty() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Hero Row with Quotes & Notes Gradient Hero Card + Section Rail
        HeroRowWithRail(
            currentTab = currentTab,
            onSelectTab = onSelectTab,
            isRailExpanded = isRailExpanded,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 170.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFF7D5CFF),
                                    Color(0xFF00CCCC)
                                )
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Header Row: QUOTES & NOTES caption on left, 26dp export button on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUOTES & NOTES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .clickable { onOpenInfo() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .clickable { isExportPanelOpen = !isExportPanelOpen },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = "Export",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab Pills Row (translucent white; active = solid white with accent text)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "q" to ("Quotes" to quotes.size),
                                "n" to ("Notes" to notes.size),
                                "h" to ("Highlights" to filteredHighlights.size),
                                "b" to ("Bookmarks" to filteredBookmarks.size)
                            ).forEach { (tabKey, data) ->
                                val isSelected = activeTab == tabKey
                                val (title, count) = data
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) Color.White else Color.White.copy(alpha = 0.18f)
                                        )
                                        .clickable { onTabSelect(tabKey) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (tabKey == "h") {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF6C63FF) else Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        } else if (tabKey == "b") {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF6C63FF) else Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        }
                                        Text(
                                            text = "$title · $count",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFF6C63FF) else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Stat Strip: books
                        val nonDeletedBooksCount = books.count { !it.isDeleted }
                        HeroStatChipsRow(
                            chips = listOf(
                                "$nonDeletedBooksCount" to "books"
                            )
                        )
                    }
                }
            }
        }

        // Selected Book Filter Chip (if active)
        if (selectedBookFilterId != null) {
            val book = books.firstOrNull { it.id == selectedBookFilterId }
            if (book != null) {
                FilterChip(
                    selected = true,
                    onClick = { onBookFilterSelect(null) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = book.title, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Filter",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6C63FF),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // Export Panel
        if (isExportPanelOpen) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Markdown (.md)", "PDF quote cards", "CSV", "Share image").forEach { fmt ->
                            SuggestionChip(
                                onClick = { isExportPanelOpen = false },
                                label = { Text(text = fmt, fontSize = 11.sp) },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hint line directly below hero row, above the list
        Text(
            text = "Latest · tap any item to open it in the reader",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Cards List (Takes remaining space and scrolls)
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "q" -> {
                    if (quotes.isEmpty()) EmptyStateText() else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(quotes, key = { it.id }) { quote ->
                                val book = books.firstOrNull { it.id == quote.bookId }
                                QuoteCard(
                                    quoteText = quote.selectedText,
                                    noteText = quote.noteText,
                                    bookTitle = book?.title ?: "",
                                    pageNumber = quote.pageNumber,
                                    borderColorHex = quote.colorHex,
                                    onClick = { onHighlightClick(quote) },
                                    onDelete = { onDeleteHighlight(quote.id) }
                                )
                            }
                        }
                    }
                }
                "n" -> {
                    if (notes.isEmpty()) EmptyStateText() else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                val book = books.firstOrNull { it.id == note.bookId }
                                NoteCard(
                                    noteText = note.noteText,
                                    quotedText = note.selectedText,
                                    bookTitle = book?.title ?: "",
                                    pageNumber = note.pageNumber,
                                    borderColorHex = note.colorHex,
                                    onClick = { onHighlightClick(note) },
                                    onDelete = { onDeleteHighlight(note.id) }
                                )
                            }
                        }
                    }
                }
                "h" -> {
                    if (filteredHighlights.isEmpty()) EmptyStateText() else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredHighlights, key = { it.id }) { h ->
                                val book = books.firstOrNull { it.id == h.bookId }
                                QuoteCard(
                                    quoteText = h.selectedText,
                                    noteText = h.noteText,
                                    bookTitle = book?.title ?: "",
                                    pageNumber = h.pageNumber,
                                    borderColorHex = h.colorHex,
                                    onClick = { onHighlightClick(h) },
                                    onDelete = { onDeleteHighlight(h.id) }
                                )
                            }
                        }
                    }
                }
                "b" -> {
                    if (filteredBookmarks.isEmpty()) EmptyStateText() else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredBookmarks, key = { "${it.bookId}_${it.pageNumber}" }) { bm ->
                                val book = books.firstOrNull { it.id == bm.bookId }
                                BookmarkCard(
                                    chapterTitle = bm.chapterTitle,
                                    excerpt = bm.excerpt,
                                    bookTitle = book?.title ?: "",
                                    pageNumber = bm.pageNumber,
                                    onClick = { onBookmarkClick(bm) },
                                    onDelete = { onDeleteBookmark(bm) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateText() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Nothing here yet for this selection.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuoteCard(
    quoteText: String,
    noteText: String,
    bookTitle: String,
    pageNumber: Int,
    borderColorHex: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = parseHexColor(borderColorHex, Color(0xFF6C63FF))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "“$quoteText”",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                        Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (noteText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = noteText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$bookTitle · p.$pageNumber · tap to jump ›",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    noteText: String,
    quotedText: String,
    bookTitle: String,
    pageNumber: Int,
    borderColorHex: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = parseHexColor(borderColorHex, Color(0xFF00CCCC))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = noteText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                        Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "“$quotedText”",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$bookTitle · p.$pageNumber · tap to jump ›",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BookmarkCard(
    chapterTitle: String,
    excerpt: String,
    bookTitle: String,
    pageNumber: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFF5D73))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmark",
                            tint = Color(0xFFFF5D73),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chapterTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                        Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (excerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "$excerpt…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$bookTitle · p.$pageNumber · tap to jump ›",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
