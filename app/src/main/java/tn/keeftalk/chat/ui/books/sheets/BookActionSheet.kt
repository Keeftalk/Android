package tn.keeftalk.chat.ui.books.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.ui.books.components.BookCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookActionSheet(
    book: Book,
    onDismiss: () -> Unit,
    onRead: (Book) -> Unit,
    onStats: (Book) -> Unit,
    onShare: (Book) -> Unit,
    onChangeCoverClick: (Book) -> Unit,
    onToggleDone: (Book) -> Unit,
    onToggleArchive: (Book) -> Unit,
    onDeleteConfirm: (Book) -> Unit
) {
    var isDeleteConfirmState by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(36.dp)) {
                    BookCover(book = book, isMini = true)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${book.author} · ${book.percentProgress}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Text(text = "✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (isDeleteConfirmState) {
                // Delete Confirmation
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5D73),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Delete “${book.title}”?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "It disappears from your library, stats, and quotes. You can undo right after.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isDeleteConfirmState = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel")
                        }
                        Button(
                            onClick = {
                                onDeleteConfirm(book)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5D73)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Delete", color = Color.White)
                        }
                    }
                }
            } else {
                // Main Menu Rows
                ActionSheetRow(icon = Icons.Default.Book, label = "Read", onClick = { onRead(book); onDismiss() })
                ActionSheetRow(icon = Icons.Default.BarChart, label = "Stats", onClick = { onStats(book); onDismiss() })
                ActionSheetRow(icon = Icons.Default.Share, label = "Share", onClick = { onShare(book); onDismiss() })
                ActionSheetRow(icon = Icons.Default.Image, label = "Change cover", hasChevron = true, onClick = { onChangeCoverClick(book) })
                ActionSheetRow(
                    icon = if (book.markedDone) Icons.Default.RotateLeft else Icons.Default.CheckCircle,
                    label = if (book.markedDone) "Mark as not finished" else "Mark as done",
                    onClick = { onToggleDone(book); onDismiss() }
                )
                ActionSheetRow(
                    icon = Icons.Default.Archive,
                    label = if (book.isArchived) "Unarchive" else "Archive",
                    onClick = { onToggleArchive(book); onDismiss() }
                )
                ActionSheetRow(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    isDanger = true,
                    onClick = { isDeleteConfirmState = true }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    hasChevron: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDanger) Color(0xFFFF5D73) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDanger) Color(0xFFFF5D73) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (hasChevron) {
                Text(text = "›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
