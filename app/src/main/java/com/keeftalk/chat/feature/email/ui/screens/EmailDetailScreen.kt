package com.keeftalk.chat.feature.email.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.feature.email.model.EmailAttachment
import com.keeftalk.chat.feature.email.model.EmailMessage
import com.keeftalk.chat.feature.email.ui.components.EmailHtmlRenderer
import com.keeftalk.chat.feature.email.viewmodel.EmailViewModel
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    messageId: String,
    viewModel: EmailViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onReply: (EmailMessage) -> Unit,
    onForward: (EmailMessage) -> Unit
) {
    val messageFlow = remember(messageId) { viewModel.getMessage(messageId) }
    val message by messageFlow.collectAsState(initial = null)
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleStar(messageId, !(message?.isStarred ?: false)) }) {
                        Icon(
                            imageVector = if (message?.isStarred == true) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star",
                            tint = if (message?.isStarred == true) Color(0xFFFFB300) else LocalContentColor.current,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.archiveMessage(messageId); onBack() }) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive", modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { 
                        viewModel.deleteMessage(messageId)
                        onDelete()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { /* Move */ }) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move", modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        message?.let { msg ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // Subject Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = msg.subject,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        )
                        
                        if (msg.isImportant) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SuggestionChip(
                                onClick = { },
                                label = { Text("Important", style = MaterialTheme.typography.labelSmall) },
                                icon = { Icon(Icons.AutoMirrored.Filled.LabelImportant, null, Modifier.size(16.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = MaterialTheme.colorScheme.primary,
                                    iconContentColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Sender Info Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeeftalkAvatar(
                        avatarUrl = msg.senderProfilePicUrl,
                        initials = com.keeftalk.chat.util.AvatarUtils.getInitials(msg.senderName),
                        seed = msg.senderEmail,
                        size = 56.dp
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = msg.senderName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = msg.senderEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "to me",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(msg.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Content Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (msg.htmlContent != null) {
                        EmailHtmlRenderer(htmlContent = msg.htmlContent)
                    } else {
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Attachments
                if (msg.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attachments (${msg.attachments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        msg.attachments.forEach { attachment ->
                            AttachmentItem(attachment) {
                                viewModel.downloadAttachment(attachment)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Reply/Forward Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { msg.let { onReply(it) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reply")
                    }
                    OutlinedButton(
                        onClick = { msg.let { onForward(it) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Forward")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AttachmentItem(attachment: EmailAttachment, onDownload: () -> Unit) {
    val isDownloaded = attachment.localUri != null
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(attachment.localUri) {
        if (attachment.localUri != null) isDownloading = false
    }

    Surface(
        onClick = { 
            if (isDownloaded) {
                // Handle opening
            } else if (!isDownloading) {
                isDownloading = true
                onDownload()
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = if (isDownloaded) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            1.dp, 
            if (isDownloaded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        attachment.mimeType.startsWith("image/") -> Icons.Default.Image
                        attachment.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = if (isDownloaded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatFileSize(attachment.size)} • ${if (isDownloaded) "Offline" else if (isDownloading) "Downloading..." else "Tap to download"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDownloading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (isDownloaded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
