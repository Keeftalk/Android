package tn.keeftalk.chat.ui.books.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.ui.books.components.BookCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadNowSheet(
    books: List<Book>,
    lastOpenedBookId: String?,
    dailyLeftMin: Int,
    isDailyGoalDone: Boolean,
    onDismiss: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    val activeBooks = books.filter { !it.isDeleted && !it.isArchived }
    val lastOpened = activeBooks.firstOrNull { it.id == lastOpenedBookId }
        ?: activeBooks.firstOrNull { !it.markedDone && it.percentProgress < 100 }
        ?: activeBooks.firstOrNull()

    val otherBooks = activeBooks.filter { it.id != lastOpened?.id }

    val subtitle = if (isDailyGoalDone) "Daily goal done ✅" else "Goal: $dailyLeftMin min left today"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Read now",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Last opened or Suggested section
                if (lastOpened != null) {
                    item {
                        Text(
                            text = if (lastOpenedBookId != null && lastOpened.id == lastOpenedBookId) "LAST OPENED" else "SUGGESTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BookItemRow(book = lastOpened, onClick = { onBookClick(lastOpened) })
                    }
                }

                // Other books section
                if (otherBooks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "OR PICK ANOTHER BOOK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(otherBooks, key = { it.id }) { book ->
                        BookItemRow(book = book, onClick = { onBookClick(book) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookItemRow(
    book: Book,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini cover (38dp wide, 2:3)
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(57.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                BookCover(book = book, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                val pageText = "p.${book.lastReadPage} / ${book.totalPages}" + if (book.markedDone || book.percentProgress >= 100) " · 🏁 finished" else ""
                Text(
                    text = pageText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { book.percentProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF6C63FF),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "▶",
                fontSize = 16.sp,
                color = Color(0xFF6C63FF)
            )
        }
    }
}
