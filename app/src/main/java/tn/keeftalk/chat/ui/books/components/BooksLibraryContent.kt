package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Book

@Composable
fun BooksLibraryContent(
    books: List<Book>,
    viewMode: String, // "list", "g3", "g2"
    selectedFilter: String, // "all", "rd", "fin", "fav", "arc"
    archivedCount: Int,
    onFilterSelect: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onBookClick: (Book) -> Unit,
    onContinueReadingClick: (Book) -> Unit,
    currentTab: String = "lib",
    onSelectTab: (String) -> Unit = {},
    isRailExpanded: Boolean = false,
    onOpenTour: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val continueReadingBook = books.firstOrNull { !it.isArchived && !it.isDeleted && it.percentProgress < 100 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // 1. Hero Row (Continue Reading Hero Card + Section Rail)
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
                    .clickable {
                        if (continueReadingBook != null) {
                            onContinueReadingClick(continueReadingBook)
                        }
                    }
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
                    // "?" button in top-right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onOpenTour() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (continueReadingBook != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "CONTINUE READING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BookCover(
                                    book = continueReadingBook,
                                    modifier = Modifier.width(46.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = continueReadingBook.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Page ${continueReadingBook.lastReadPage} of ${continueReadingBook.totalPages}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { continueReadingBook.percentProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Stat Strip / Reading speed fallback message
                            if (continueReadingBook.percentProgress < 100) {
                                val pagesReadCount = (continueReadingBook.totalPages * continueReadingBook.percentProgress / 100)
                                val pagesLeft = (continueReadingBook.totalPages - pagesReadCount).coerceAtLeast(0)
                                if (continueReadingBook.speedPagesPerHour > 0) {
                                    val hoursToFinish = maxOf(0.1f, pagesLeft / continueReadingBook.speedPagesPerHour.toFloat())
                                    val hoursStr = if (hoursToFinish >= 10) "~${hoursToFinish.toInt()}h" else "~${String.format("%.1f", hoursToFinish)}h"

                                    HeroStatChipsRow(
                                        chips = listOf(
                                            "$pagesLeft" to "pages left",
                                            hoursStr to "~hours to finish"
                                        )
                                    )
                                } else {
                                    Text(
                                        text = "The average adult reads about 0.8 to 1 page per minute (or roughly 40 to 60 pages per hour) for a standard paperback book",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "MY LIBRARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active book",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Select or import a book to start reading",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }



        // 3. Filter Pills Row + View Toggle Button (Horizontally Scrollable Pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "all" to "All",
                    "rd" to "Reading",
                    "fin" to "Finished",
                    "fav" to "Favorites",
                    "arc" to "Archived${if (archivedCount > 0) " ($archivedCount)" else ""}"
                ).forEach { (filterKey, label) ->
                    val isSelected = selectedFilter == filterKey
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onFilterSelect(filterKey) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (filterKey == "fav") {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = if (isSelected) Color.White else Color(0xFFFFB800),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            } else if (filterKey == "arc") {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onToggleViewMode,
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = when (viewMode) {
                        "g3" -> Icons.Default.GridView
                        "g2" -> Icons.Default.ViewAgenda
                        else -> Icons.Default.List
                    },
                    contentDescription = "Toggle View Mode",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 4. Book List / Grid View
        val filteredBooks = books.filter { b ->
            if (selectedFilter == "arc") {
                b.isArchived && !b.isDeleted
            } else {
                !b.isArchived && !b.isDeleted && when (selectedFilter) {
                    "rd" -> b.percentProgress < 100
                    "fin" -> b.percentProgress >= 100
                    "fav" -> b.isFavorite
                    else -> true
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedFilter == "arc") "No archived books." else "No books in library.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                when (viewMode) {
                    "g3" -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredBooks, key = { it.id }) { book ->
                                BookGridTile(book = book, isThreeCol = true, onClick = { onBookClick(book) })
                            }
                        }
                    }
                    "g2" -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredBooks, key = { it.id }) { book ->
                                BookGridTile(book = book, isThreeCol = false, onClick = { onBookClick(book) })
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredBooks, key = { it.id }) { book ->
                                BookListRow(book = book, onClick = { onBookClick(book) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStatChipsRow(
    chips: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (value, caption) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(vertical = 5.dp, horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = caption,
                        fontSize = 9.5.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BookListRow(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(book = book, modifier = Modifier.width(54.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.author} · ${book.totalPages} pages",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (book.percentProgress >= 100) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Finished",
                            tint = Color(0xFF00CCCC),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Finished",
                            fontSize = 12.sp,
                            color = Color(0xFF00CCCC)
                        )
                    } else {
                        Text(
                            text = "Page ${book.lastReadPage} · ${book.percentProgress}%",
                            fontSize = 12.sp,
                            color = Color(0xFF00CCCC)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { book.percentProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF6C63FF),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (book.insightText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF6C63FF).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = book.insightText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookGridTile(
    book: Book,
    isThreeCol: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            BookCover(book = book, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (book.percentProgress >= 100) "Finished" else "${book.percentProgress}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00CCCC)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { book.percentProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF6C63FF),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (!isThreeCol && book.insightText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF6C63FF).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = book.insightText,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
