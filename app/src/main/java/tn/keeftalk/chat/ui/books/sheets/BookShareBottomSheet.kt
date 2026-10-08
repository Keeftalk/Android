package tn.keeftalk.chat.ui.books.sheets

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import tn.keeftalk.chat.domain.model.Book
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookShareBottomSheet(
    book: Book,
    onDismiss: () -> Unit,
    onDirectChatShare: (Book) -> Unit,
    onEmailShare: (Book) -> Unit
) {
    val context = LocalContext.current

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Book",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Text(text = "✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // 1. Share File through System Share Menu
            ShareOptionRow(
                icon = Icons.Default.Share,
                label = "Share file via System Menu (${book.format})",
                onClick = {
                    val file = File(book.filePath)
                    if (file.exists()) {
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = if (book.format.equals("PDF", true)) "application/pdf" else "application/epub+zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "Sharing book: ${book.title} by ${book.author}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share ${book.title}"))
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "I'm reading “${book.title}” by ${book.author} on Keeftalk")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share ${book.title}"))
                        }
                    } else {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "I'm reading “${book.title}” by ${book.author} on Keeftalk")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share ${book.title}"))
                    }
                    onDismiss()
                }
            )

            // 2. Share Direct Through Chats
            ShareOptionRow(
                icon = Icons.Default.Message,
                label = "Share direct through Chats",
                onClick = {
                    onDirectChatShare(book)
                    onDismiss()
                }
            )

            // 3. Share Through My Emails
            ShareOptionRow(
                icon = Icons.Default.Email,
                label = "Share through My Emails",
                onClick = {
                    onEmailShare(book)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ShareOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
