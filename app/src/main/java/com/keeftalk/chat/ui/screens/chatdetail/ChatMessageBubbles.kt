package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.keeftalk.chat.ui.components.EncryptedThumbnail
import com.keeftalk.chat.domain.model.FileType
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.model.MessageStatus
import com.keeftalk.chat.domain.model.MessageType
import com.keeftalk.chat.domain.model.MessageReaction
import com.keeftalk.chat.domain.model.UserChatSettings
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.PdfPreviewContent
import com.keeftalk.chat.ui.theme.LucideIcons
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.theme.LocalChatThemeExtra
import com.keeftalk.chat.util.AvatarUtils
import com.keeftalk.chat.util.LanguageUtils
import com.keeftalk.chat.ui.emoji.AnimatedEmoji
import com.keeftalk.chat.ui.emoji.EmojiUtils
import com.keeftalk.chat.ui.components.MessageReactions
import com.keeftalk.chat.ui.components.ReactionsPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.layout.onGloballyPositioned
import java.util.Locale
import com.keeftalk.chat.domain.model.DecryptionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun DecryptionLoadingBubble(contentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "decryption_shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp).alpha(alpha)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Decrypting...",
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun VoiceMessageBubble(
    message: Message,
    contentColor: Color
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    // Lazy initialization of ExoPlayer
    val playVoice = {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context).build().apply {
                val localPath = message.localFilePath
                val uri = if (localPath != null && java.io.File(localPath).exists()) {
                    localPath
                } else {
                    message.fileUrl
                }

                if (uri != null) {
                    setMediaItem(MediaItem.fromUri(uri))
                    setPlaybackSpeed(playbackSpeed)
                    prepare()
                }
                playWhenReady = true
            }
            
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                    playbackSpeed = playbackParameters.speed
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        player.seekTo(0)
                        player.pause()
                    }
                }
            })
            exoPlayer = player
        } else {
            exoPlayer?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            exoPlayer?.let {
                val duration = it.duration
                if (duration > 0) {
                    progress = it.currentPosition.toFloat() / duration.toFloat()
                }
            }
            delay(100.milliseconds)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { playVoice() },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = contentColor
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                val current = exoPlayer?.currentPosition ?: 0L
                Text(
                    text = "${formatDurationInternal(current)} / ${formatDurationInternal(duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1f, 1.5f, 2f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) contentColor else Color.Transparent)
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer?.setPlaybackSpeed(speed)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${if (speed == 1f) "1" else speed}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    if (contentColor.luminance() > 0.5f) Color.Black else Color.White
                                } else {
                                    contentColor
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileMessageContent(
    message: Message,
    isMe: Boolean,
    contentColor: Color,
    onFileClick: () -> Unit,
    onPdfClick: () -> Unit = {},
    onCodeClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggleMediaLock: () -> Unit = {}
) {
    val fileName = message.fileName?.lowercase() ?: ""
    val isPdf = fileName.endsWith(".pdf")
    val isCode = fileName.endsWith(".java") || fileName.endsWith(".kt") || fileName.endsWith(".txt") ||
                 fileName.endsWith(".py") || fileName.endsWith(".js") || fileName.endsWith(".html") ||
                 fileName.endsWith(".css") || fileName.endsWith(".json") || fileName.endsWith(".xml") ||
                 fileName.endsWith(".log") || fileName.endsWith(".c") || fileName.endsWith(".cpp") ||
                 fileName.endsWith(".h") || fileName.endsWith(".sh") || fileName.endsWith(".ts") ||
                 fileName.endsWith(".sql") || fileName.endsWith(".md") || fileName.endsWith(".yaml") ||
                 fileName.endsWith(".yml") || fileName.endsWith(".toml")

    val isLocal = remember(message.localFilePath) {
        val path = message.localFilePath
        path != null && java.io.File(path).exists()
    }

    if (isCode) {
        CodeFilePreviewContent(
            message = message,
            isMe = isMe,
            contentColor = contentColor,
            onExpand = onCodeClick,
            onToggleLock = onToggleMediaLock
        )
    } else {
        Surface(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .pointerInput(message.id) {
                    detectTapGestures(
                        onTap = { 
                            if (isPdf) onPdfClick() else onFileClick()
                        },
                        onLongPress = { onLongClick() }
                    )
                }
                .padding(vertical = 2.dp),
            color = contentColor.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(contentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isLocal && !isMe) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.fileName ?: "Document",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor
                    )
                    Text(
                        text = if (isLocal) formatFileSize(message.fileSize) else "Tap to download",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeFilePreviewContent(
    message: Message,
    isMe: Boolean,
    contentColor: Color,
    onExpand: () -> Unit,
    onToggleLock: () -> Unit = {}
) {
    var previewText by remember { mutableStateOf<AnnotatedString?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val extension = message.fileName?.substringAfterLast(".", "") ?: ""
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(message.localFilePath, message.fileUrl) {
        if (message.localFilePath == null && message.fileUrl == null) return@LaunchedEffect

        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val file = message.localFilePath?.let { java.io.File(it) }
                val content = if (file != null && file.exists()) {
                    file.useLines { it.take(15).joinToString("\n") }
                } else {
                    null
                }

                if (content != null) {
                    previewText = com.keeftalk.chat.util.SyntaxHighlighter.highlight(content, extension, isDark)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.05f)),
        color = Color.Transparent
    ) {
        Box {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Code,
                        null,
                        tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.fileName ?: "Code File",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (isMe) {
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (message.mediaLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (message.mediaLocked) Color.Red else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (previewText != null) {
                    Text(
                        text = previewText!!,
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        "Tap to preview code",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                            startY = 300f
                        )
                    )
                    .clickable { onExpand() },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "EXPAND",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmbeddedCodeBlock(
    code: String,
    language: String
) {
    val isDark = isSystemInDarkTheme()
    var isExpanded by remember { mutableStateOf(false) }
    val codeLines = remember(code) { code.lines() }
    val isLong = codeLines.size > 9

    val codeColor = if (isDark) Color.White else Color.Black
    val secondaryColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    val isRtl = remember(code) { LanguageUtils.isRtl(code) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = if (isDark) Color(0xFF0D1117) else Color(0xFFF6F8FA),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (language.isNotBlank()) {
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryColor
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (isLong) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = secondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (language.isNotBlank() || isLong) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val displayCode = if (isLong && !isExpanded) {
                        codeLines.take(8).joinToString("\n") + "\n..."
                    } else {
                        code
                    }

                    val displayHighlighted = remember(displayCode, language, isDark) {
                        com.keeftalk.chat.util.SyntaxHighlighter.highlight(displayCode, language, isDark)
                    }

                    Text(
                        text = displayHighlighted,
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = codeColor,
                            textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TextMessageContent(
    content: String,
    searchQuery: String,
    contentColor: Color,
    isMe: Boolean,
    chatThemeId: String,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val textLines = remember(content) { content.lines() }
    val isTextLong = textLines.size > 9
    var isTextExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        val parts = remember(content) { content.split("'''") }

        if (parts.size >= 3) {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    val codeLines = part.trim().lines()
                    val firstLine = codeLines.firstOrNull() ?: ""
                    val language = if (firstLine.isNotBlank() && !firstLine.contains(" ")) firstLine else ""
                    val code = if (language.isNotBlank()) codeLines.drop(1).joinToString("\n") else part.trim()
                    
                    EmbeddedCodeBlock(code = code, language = language)
                } else if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isMe && chatThemeId == "rosa") FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                        )
                    )
                }
            }
        } else {
            val displayText = if (isTextLong && !isTextExpanded && searchQuery.isEmpty()) {
                textLines.take(8).joinToString("\n") + "..."
            } else {
                content
            }

            val finalAnnotatedText = remember(displayText, searchQuery) {
                if (searchQuery.isBlank() || !displayText.contains(searchQuery, ignoreCase = true)) {
                    AnnotatedString(displayText)
                } else {
                    androidx.compose.ui.text.buildAnnotatedString {
                        var start = 0
                        while (start < displayText.length) {
                            val index = displayText.indexOf(searchQuery, start, ignoreCase = true)
                            if (index == -1) {
                                append(displayText.substring(start))
                                break
                            }
                            append(displayText.substring(start, index))
                            withStyle(androidx.compose.ui.text.SpanStyle(background = Color(0xFFFFDD57), color = Color.Black)) {
                                append(displayText.substring(index, index + searchQuery.length))
                            }
                            start = index + searchQuery.length
                        }
                    }
                }
            }

            Text(
                text = finalAnnotatedText, 
                color = contentColor, 
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isMe && chatThemeId == "rosa") FontWeight.SemiBold else FontWeight.Medium,
                    shadow = if (!isMe && (chatThemeId == "rosa")) androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.1f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    ) else null,
                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                )
            )
            
            if (isTextLong) {
                Text(
                    text = if (isTextExpanded) "See Less" else "See More",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { isTextExpanded = !isTextExpanded }
                )
            }
        }
    }
}

@Composable
fun ImageMessageContent(
    message: Message,
    mediaUri: String,
    isMe: Boolean,
    transferProgress: Float?,
    onMediaClick: (String, Long) -> Unit,
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onToggleMediaLock: (String, Boolean) -> Unit
) {
    val isHD = remember(message.width, message.height, message.fileSize) {
        val resolution = (message.width ?: 0) * (message.height ?: 0)
        resolution >= 1920 * 1080 || (message.fileSize ?: 0L) > 3 * 1024 * 1024
    }
    
    // OPTIMIZATION: Use fixed aspect ratio to prevent list jumping when images load
    val aspectRatio = remember(message.width, message.height) {
        val w = message.width?.toFloat() ?: 16f
        val h = message.height?.toFloat() ?: 9f
        (w / h).coerceIn(0.5f, 2.0f)
    }

    // Check if we should show a shimmer while waiting for local decrypted image
    val showShimmer = remember(mediaUri, message.localFilePath, message.cryptoVersion) {
        message.cryptoVersion > 0 && message.localFilePath == null
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(max = 280.dp)
            .aspectRatio(aspectRatio)
            .pointerInput(message.id) {
                detectTapGestures(
                    onTap = { if (!showShimmer && mediaUri.isNotBlank()) onMediaClick(message.id, 0L) }, 
                    onLongPress = { onLongClick() }, 
                    onDoubleTap = { onDoubleTap() }
                )
            }
    ) {
        if (showShimmer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preparing image...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            AsyncImage(
                model = mediaUri, 
                contentDescription = null, 
                modifier = Modifier.fillMaxSize(), 
                contentScale = ContentScale.Fit
            )
        }
        
        if (isHD && !showShimmer) HDBadge(modifier = Modifier.align(if (isMe) Alignment.TopStart else Alignment.TopEnd).padding(8.dp))
        if (message.status == MessageStatus.SENDING) {
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { transferProgress ?: 0.1f }, modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = Color.White)
                if (transferProgress != null) {
                    Text("${(transferProgress * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (isMe) {
            IconButton(
                onClick = { onToggleMediaLock(message.id, !message.mediaLocked) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(imageVector = if (message.mediaLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = if (message.mediaLocked) Color.Red else Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun VideoMessageContent(
    message: Message,
    mediaUri: String,
    isMe: Boolean,
    transferProgress: Float?,
    onMediaClick: (String, Long) -> Unit,
    onDownloadClick: () -> Unit,
    onToggleMediaLock: (String, Boolean) -> Unit
) {
    val isHD = remember(message.width, message.height, message.fileSize) {
        val resolution = (message.width ?: 0) * (message.height ?: 0)
        resolution >= 1920 * 1080 || (message.fileSize ?: 0L) > 5 * 1024 * 1024
    }
    Box(modifier = Modifier.widthIn(max = 280.dp)) {
        val isLocal = message.localFilePath != null && java.io.File(message.localFilePath!!).exists()
        if (isLocal) {
            InlineVideoPlayer(uri = mediaUri, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), onFullScreen = { pos: Long -> onMediaClick(message.id, pos) })
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black.copy(alpha = 0.1f))
                    .clickable { if (transferProgress == null) onDownloadClick() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = message.thumbnailUrl ?: mediaUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                if (transferProgress != null) {
                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { transferProgress }, color = Color.White, modifier = Modifier.size(48.dp))
                        Text("${(transferProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp).shadow(4.dp, CircleShape))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatFileSize(message.fileSize), color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        }
        if (isHD) HDBadge(modifier = Modifier.align(if (isMe) Alignment.TopStart else Alignment.TopEnd).padding(8.dp))
        if (isMe) {
            IconButton(
                onClick = { onToggleMediaLock(message.id, !message.mediaLocked) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(imageVector = if (message.mediaLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = if (message.mediaLocked) Color.Red else Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MediaMessageContent(
    message: Message,
    isMe: Boolean,
    contentColor: Color,
    transferProgress: Float?,
    onMediaClick: (String, Long) -> Unit,
    onPdfClick: (String) -> Unit,
    onEmailClick: (String) -> Unit,
    onVaultClick: (String) -> Unit,
    onAgendaClick: (String) -> Unit,
    onCodeClick: (String) -> Unit = {},
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onToggleMediaLock: (String, Boolean) -> Unit,
    onDownloadClick: () -> Unit,
    checkNoteAvailability: suspend (String) -> Boolean
) {
    // mediaUri is only non-null if we have a local decrypted file OR it's not E2EE
    val mediaUri = message.localFilePath ?: if (message.cryptoVersion == 0) message.fileUrl else null
    
    when (message.type) {
        MessageType.SHARED_NOTE -> {
            val noteId = message.fileUrl ?: ""
            SharedNoteBubbleContent(
                content = message.content,
                noteId = noteId,
                isMe = isMe,
                contentColor = contentColor,
                onClick = { onPdfClick(noteId) }, // reusing onPdfClick as generic item click or I should add onNoteClick
                checkAvailability = checkNoteAvailability
            )
        }
        MessageType.SHARED_EMAIL -> {
            SharedEmailBubbleContent(
                message = message,
                isMe = isMe,
                contentColor = contentColor,
                onClick = { onEmailClick(message.fileUrl ?: "") }
            )
        }
        MessageType.SHARED_VAULT_FILE -> {
            if (message.effectiveFileType == null) {
                // Attachments are still being fetched (common for Realtime)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Preparing Vault file...", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
                    }
                }
            } else {
                when (message.effectiveFileType) {
                    FileType.IMAGE -> {
                        ImageMessageContent(message, mediaUri ?: message.fileUrl ?: "", isMe, transferProgress, onMediaClick, onLongClick, onDoubleTap, onToggleMediaLock)
                    }
                    FileType.VIDEO -> {
                        VideoMessageContent(message, mediaUri ?: message.fileUrl ?: "", isMe, transferProgress, onMediaClick, onDownloadClick, onToggleMediaLock)
                    }
                    FileType.DOCUMENT -> {
                        SharedVaultFileBubbleContent(
                            message = message,
                            isMe = isMe,
                            contentColor = contentColor,
                            onVaultClick = onVaultClick,
                            onMediaClick = onMediaClick,
                            onPdfClick = onPdfClick,
                            onCodeClick = onCodeClick
                        )
                    }
                    else -> {
                        SharedVaultFileBubbleContent(
                            message = message,
                            isMe = isMe,
                            contentColor = contentColor,
                            onVaultClick = onVaultClick,
                            onMediaClick = onMediaClick,
                            onPdfClick = onPdfClick,
                            onCodeClick = onCodeClick
                        )
                    }
                }
            }
        }
        MessageType.SHARED_AGENDA -> {
            SharedAgendaBubbleContent(
                message = message,
                isMe = isMe,
                contentColor = contentColor,
                onClick = { onAgendaClick(message.fileUrl ?: "") }
            )
        }
        MessageType.PDF -> {
            if (mediaUri == null && message.thumbnailUrl == null && message.cryptoVersion > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preparing PDF...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfPreviewContent(
                        uri = mediaUri ?: message.fileUrl ?: "",
                        thumbnailUri = message.thumbnailUrl,
                        modifier = Modifier.fillMaxWidth(),
                        isLocked = message.mediaLocked,
                        onClick = { onPdfClick(message.id) }
                    )
                    if (isMe) {
                        IconButton(
                            onClick = { onToggleMediaLock(message.id, !message.mediaLocked) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(imageVector = if (message.mediaLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = if (message.mediaLocked) Color.Red else Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        MessageType.VIDEO -> {
            VideoMessageContent(message, mediaUri ?: message.fileUrl ?: "", isMe, transferProgress, onMediaClick, onDownloadClick, onToggleMediaLock)
        }
        MessageType.IMAGE -> {
            ImageMessageContent(message, mediaUri ?: message.fileUrl ?: "", isMe, transferProgress, onMediaClick, onLongClick, onDoubleTap, onToggleMediaLock)
        }
        else -> {}
    }
}

@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    chatSettings: UserChatSettings = UserChatSettings(""),
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onReactionClick: (String) -> Unit,
    showReactionsPopup: Boolean = false,
    onDismissReactions: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onReactionSelect: (String) -> Unit = {},
    onReplyPreviewClick: (String) -> Unit = {},
    onReactionBubbleClick: (List<MessageReaction>) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    checkNoteAvailability: suspend (String) -> Boolean = { true },
    isLastSeenByPeer: Boolean = false,
    seenUsers: List<com.keeftalk.chat.domain.model.User> = emptyList(),
    seenCount: Int = 0,
    peerAvatarUrl: String? = null,
    peerName: String = "",
    peerId: String = "",
    onMediaClick: (String, Long) -> Unit = { _, _ -> },
    onPdfClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onVaultClick: (String) -> Unit = {},
    onAgendaClick: (String) -> Unit = {},
    onCodeClick: (String) -> Unit = {},
    onToggleMediaLock: (String, Boolean) -> Unit = { _, _ -> },
    isStackRoot: Boolean = false,
    stackMessages: List<Message> = emptyList(),
    onReplySwipe: () -> Unit = {},
    transferProgress: Float? = null,
    onDownloadClick: () -> Unit = {},
    isLatest: Boolean = false,
    isTimestampExpanded: Boolean = false,
    onToggleTimestamp: () -> Unit = {},
    isGroup: Boolean = false,
    senderProfile: com.keeftalk.chat.domain.model.User? = null,
    isSms: Boolean = false
) {
    val themeExtra = LocalChatThemeExtra.current
    val chatTheme = LocalChatTheme.current
    val haptic = LocalHapticFeedback.current
    
    val isMe = remember(message.senderId, currentUserId) { message.senderId == currentUserId }
    
    val containerColor = remember(isMe, chatTheme, themeExtra) {
        if (isMe) {
            if (chatTheme.id != "default") chatTheme.msgMe else themeExtra.msgMe
        } else {
            if (chatTheme.id != "default") chatTheme.msgThem else themeExtra.msgThem
        }
    }
    
    val defaultOnBackground = MaterialTheme.colorScheme.onBackground
    val contentColor = remember(isMe, chatTheme, defaultOnBackground) {
        if (isMe) {
            if (chatTheme.id == "rosa") Color(0xFF4A2A3A) 
            else if (chatTheme.id == "alpha") Color(0xFFE8E0D0) 
            else Color.White
        } else {
            chatTheme.textColor ?: defaultOnBackground
        }
    }
    
    val radius = chatSettings.bubbleRadius.dp
    val shape = remember(chatSettings.bubbleStyle, radius, isMe, chatTheme.id) {
        when (chatSettings.bubbleStyle) {
            "ROUNDED" -> RoundedCornerShape(radius)
            "COMPACT" -> RoundedCornerShape(4.dp)
            else -> {
                if (chatTheme.id == "rosa") {
                    if (isMe) RoundedCornerShape(26.dp, 26.dp, 6.dp, 26.dp)
                    else RoundedCornerShape(26.dp, 26.dp, 26.dp, 6.dp)
                } else if (chatTheme.id == "alpha") {
                    if (isMe) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                    else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                } else {
                    if (isMe) RoundedCornerShape(radius, radius, 4.dp, radius)
                    else RoundedCornerShape(radius, radius, radius, 4.dp)
                }
            }
        }
    }

    var showOriginal by remember { mutableStateOf(value = false) }
    val displayContent = if (showOriginal || message.translatedContent == null) message.content else message.translatedContent
    
    val spacing = remember(chatSettings.chatDensity, chatTheme.id) {
        when (chatSettings.chatDensity) {
            "COMPACT" -> 1.dp
            "SPACIOUS" -> 14.dp
            else -> if (chatTheme.id == "rosa") 7.dp else 4.dp
        }
    }

    val isMediaMessage = remember(message.type, message.effectiveFileType, isStackRoot) {
        message.type == MessageType.IMAGE || 
        message.type == MessageType.VIDEO || 
        (message.type == MessageType.SHARED_VAULT_FILE && (message.effectiveFileType == FileType.IMAGE || message.effectiveFileType == FileType.VIDEO)) ||
        isStackRoot
    }

    val layoutDirection = LocalLayoutDirection.current
    val basePadding = remember(chatTheme.id) {
        when (chatTheme.id) {
            "rosa" -> PaddingValues(horizontal = 20.dp, vertical = 14.dp)
            "alpha" -> PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            else -> PaddingValues(10.dp)
        }
    }
    
    val bubblePadding = remember(isMediaMessage, basePadding) {
        if (isMediaMessage) {
            PaddingValues(0.dp)
        } else {
            basePadding
        }
    }

    val updatedOnToggleTimestamp by rememberUpdatedState(onToggleTimestamp)

    val horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val maxSwipe = 80.dp
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val newOffset = swipeOffset + delta
                        if (isMe) {
                            if (newOffset <= 0) swipeOffset = newOffset.coerceIn(-maxSwipe.value * density.density, 0f)
                        } else {
                            if (newOffset >= 0) swipeOffset = newOffset.coerceIn(0f, maxSwipe.value * density.density)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (kotlin.math.abs(swipeOffset) > (maxSwipe.value * density.density) * 0.7f) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onReplySwipe()
                        }
                        swipeOffset = 0f
                    }
                )
                .graphicsLayer { translationX = swipeOffset }
        ) {
            if (kotlin.math.abs(swipeOffset) > 20f) {
                Box(
                    modifier = Modifier
                        .align(if (isMe) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        null,
                        tint = contentColor.copy(alpha = (kotlin.math.abs(swipeOffset) / (maxSwipe.value * density.density)).coerceIn(0f, 1f)),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing)
                    .padding(start = 12.dp, end = if (isMe) 2.dp else 12.dp),
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = Alignment.Bottom
            ) {
                if (!isMe && isGroup) {
                    KeeftalkAvatar(
                        avatarUrl = senderProfile?.avatarUrl,
                        initials = senderProfile?.initials ?: "?",
                        seed = message.senderId,
                        size = 32.dp,
                        modifier = Modifier.padding(bottom = 4.dp, end = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                ) {
                    Column(
                        modifier = modifier
                            .clip(shape)
                            .then(
                                if (isMe && (chatTheme.id == "rosa")) {
                                    Modifier
                                        .background(Brush.linearGradient(
                                            0.0f to Color(0xFFFF9A9E),
                                            0.5f to Color(0xFFFECFEF),
                                            1.0f to Color(0xFFFDFCFB),
                                            start = Offset(0f, 0f),
                                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        ))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), shape)
                                } else if (isMe && (chatTheme.id == "alpha")) {
                                    Modifier.background(Color(0xFFC8B464).copy(alpha = 0.12f)).border(1.dp, Color(0xFFC8B464).copy(alpha = 0.2f), shape)
                                } else if (!isMe && (chatTheme.id == "rosa")) {
                                    Modifier
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
                                } else if (!isMe && (chatTheme.id == "alpha")) {
                                    Modifier.background(Color.White.copy(alpha = 0.1f)).border(0.5.dp, Color.White.copy(alpha = 0.1f), shape)
                                } else {
                                    Modifier.background(containerColor)
                                }
                            )
                            .shadow(if (chatTheme.id == "rosa" && isMe) 20.dp else if (chatTheme.id != "default") 4.dp else 0.dp, shape, spotColor = if (isMe && (chatTheme.id == "rosa")) Color(0xFFFF6B9D).copy(alpha = 0.25f) else Color.Black)
                            .onGloballyPositioned { 
                                android.util.Log.d("UI_MESSAGE_BIND", "id=${message.id} decryptionState=${message.decryptionState} displayTextAvailable=${message.content.isNotBlank()}")
                            }
                            .pointerInput(message.id) {
                                detectTapGestures(
                                    onTap = { 
                                        if (message.type == MessageType.TEXT) updatedOnToggleTimestamp()
                                    },
                                    onDoubleTap = { onDoubleTap() },
                                    onLongPress = { onLongClick() }
                                )
                            }
                            .padding(bubblePadding)
                    ) {
                        if (!isMe && isGroup) {
                            Text(
                                text = senderProfile?.name ?: "Unknown",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .then(if (isMediaMessage) Modifier.padding(basePadding.calculateStartPadding(layoutDirection), basePadding.calculateTopPadding(), basePadding.calculateEndPadding(layoutDirection), 4.dp) else Modifier)
                                    .padding(bottom = 4.dp)
                            )
                        }

                        if (isSms && !isMe) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically, 
                                modifier = Modifier
                                    .then(if (isMediaMessage) Modifier.padding(basePadding.calculateStartPadding(layoutDirection), basePadding.calculateTopPadding(), basePadding.calculateEndPadding(layoutDirection), 4.dp) else Modifier)
                                    .padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = contentColor.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "SMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (message.decryptionState == DecryptionState.PENDING || message.decryptionState == DecryptionState.RETRY_REQUIRED) {
                            val isRecent = remember(message.timestamp) { System.currentTimeMillis() - message.timestamp < 10000 }
                            if (isRecent) {
                                DecryptionLoadingBubble(contentColor = contentColor)
                            } else {
                                Text(
                                    text = "Message encrypted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        } else if (isStackRoot && stackMessages.isNotEmpty()) {
                            MediaStackBubble(
                                messages = stackMessages,
                                currentUserId = currentUserId,
                                onMediaClick = onMediaClick,
                                onLongClick = onLongClick,
                                onDoubleTap = onDoubleTap,
                                onToggleMediaLock = onToggleMediaLock
                            )
                        } else {
                            val replyTo = message.replyTo
                            if (replyTo != null) {
                                val isReplyRtl = remember(replyTo.content) { LanguageUtils.isRtl(replyTo.content) }
                                CompositionLocalProvider(LocalLayoutDirection provides (if (isReplyRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(if (isMediaMessage) Modifier.padding(basePadding.calculateStartPadding(layoutDirection), basePadding.calculateTopPadding(), basePadding.calculateEndPadding(layoutDirection), 8.dp) else Modifier)
                                            .padding(bottom = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onReplyPreviewClick(replyTo.id) },
                                        color = contentColor.copy(alpha = 0.1f)
                                    ) {
                                        Box {
                                            Box(modifier = Modifier.matchParentSize().width(4.dp).background(if (isMe) Color.White else MaterialTheme.colorScheme.primary))
                                            Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)) {
                                                Text(
                                                    text = if (replyTo.senderId == currentUserId) "You" else "Peer",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                                    textAlign = if (isReplyRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    text = replyTo.content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = contentColor.copy(alpha = 0.8f),
                                                    textAlign = if (isReplyRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            when (message.type) {
                                MessageType.SHARED_NOTE -> {
                                    val noteId = message.fileUrl ?: ""
                                    SharedNoteBubbleContent(
                                        content = message.content,
                                        noteId = noteId,
                                        isMe = isMe,
                                        contentColor = contentColor,
                                        onClick = { onNoteClick(noteId) },
                                        checkAvailability = checkNoteAvailability
                                    )
                                }
                                MessageType.VOICE -> {
                                    VoiceMessageBubble(
                                        message = message,
                                        contentColor = contentColor
                                    )
                                }
                                MessageType.FILE -> {
                                    FileMessageContent(
                                        message = message,
                                        isMe = isMe,
                                        contentColor = contentColor,
                                        onFileClick = { onMediaClick(message.id, 0L) },
                                        onPdfClick = { onPdfClick(message.id) },
                                        onCodeClick = { onCodeClick(message.id) },
                                        onLongClick = onLongClick,
                                        onToggleMediaLock = { onToggleMediaLock(message.id, !message.mediaLocked) }
                                    )
                                }
                                MessageType.PDF, MessageType.VIDEO, MessageType.IMAGE, 
                                MessageType.SHARED_NOTE, MessageType.SHARED_EMAIL, 
                                MessageType.SHARED_VAULT_FILE, MessageType.SHARED_AGENDA -> {
                                    MediaMessageContent(
                                        message = message,
                                        isMe = isMe,
                                        contentColor = contentColor,
                                        transferProgress = transferProgress,
                                        onMediaClick = onMediaClick,
                                        onPdfClick = onPdfClick,
                                        onEmailClick = onEmailClick,
                                        onVaultClick = onVaultClick,
                                        onAgendaClick = onAgendaClick,
                                        onLongClick = onLongClick,
                                        onDoubleTap = onDoubleTap,
                                        onToggleMediaLock = onToggleMediaLock,
                                        onDownloadClick = onDownloadClick,
                                        checkNoteAvailability = checkNoteAvailability
                                    )
                                }
                                MessageType.LOCATION -> {
                                    LocationMessageBubble(
                                        message = message,
                                        contentColor = contentColor,
                                        onLocationClick = { _, _ ->
                                            onMediaClick(message.id, 0L)
                                        }
                                    )
                                }
                                else -> {}
                            }

                            val isPlaceholderContent = message.content == "[${message.type.name}]"
                            val isFilenameContent = remember(message.content, message.fileName, isMediaMessage) {
                                if (!isMediaMessage) return@remember false
                                val fname = message.fileName?.lowercase() ?: ""
                                val content = message.content.lowercase().trim()
                                
                                // Catch common filename patterns to avoid displaying them as captions
                                content == fname || 
                                content.matches(Regex("^[0-9_]+\\.[a-z0-9]+$")) || 
                                content.matches(Regex("^img[-_][0-9]+[-_]wa[0-9]+\\.[a-z0-9]+$")) || // WhatsApp style
                                content.matches(Regex("^vid[-_][0-9]+[-_]wa[0-9]+\\.[a-z0-9]+$")) ||
                                (fname.isNotBlank() && content.contains(fname))
                            }
                            
                            if (message.content.isNotBlank() && !isPlaceholderContent && !isFilenameContent) {
                                if (EmojiUtils.isSingleEmoji(message.content)) {
                                    Box(
                                        modifier = Modifier.padding(
                                            if (isMediaMessage) basePadding else PaddingValues(vertical = 4.dp)
                                        )
                                    ) {
                                        AnimatedEmoji(
                                            model = EmojiUtils.getAnimatedEmojiUrl(message.content),
                                            modifier = Modifier.size(chatSettings.emojiSize.dp * 2.5f)
                                        )
                                    }
                                } else {
                                    val isRtl = remember(displayContent) { LanguageUtils.isRtl(displayContent) }
                                    
                                    CompositionLocalProvider(LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                                        TextMessageContent(
                                            content = displayContent,
                                            searchQuery = searchQuery,
                                            contentColor = contentColor,
                                            isMe = isMe,
                                            chatThemeId = chatTheme.id,
                                            isRtl = isRtl,
                                            modifier = if (isMediaMessage) Modifier.padding(basePadding) else Modifier
                                        )
                                    }
                                }
                            }
                        }

                        if (message.translatedContent != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .then(if (isMediaMessage) Modifier.padding(basePadding.calculateStartPadding(layoutDirection), 0.dp, basePadding.calculateEndPadding(layoutDirection), basePadding.calculateBottomPadding()) else Modifier)
                                    .clickable { showOriginal = !showOriginal }
                            ) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = contentColor.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showOriginal) "Show Translation" else "Show Original",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.5f),
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }

                    if (message.reactions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(if (isMe) Alignment.TopStart else Alignment.TopEnd)
                                .offset(x = if (isMe) (-12).dp else 12.dp, y = (-8).dp)
                        ) {
                            MessageReactions(
                                reactions = message.reactions,
                                onReactionClick = { emoji ->
                                    onReactionClick(emoji)
                                },
                                onReactionBubbleClick = {
                                    onReactionBubbleClick(message.reactions)
                                }
                            )
                        }
                    }
                }

                if (showReactionsPopup) {
                    ReactionsPopup(
                        onDismiss = onDismissReactions,
                        onReactionSelect = onReactionSelect,
                        onMoreClick = onMoreClick,
                        currentReaction = message.reactions.find { it.userId == currentUserId }?.emoji,
                        isMe = isMe
                    )
                }
            }
        }
        
        // Timestamp and Status outside the bubble (Messenger style)
        if (isLatest || isTimestampExpanded) {
            Row(
                modifier = Modifier
                    .align(if (isMe) Alignment.End else Alignment.Start)
                    .padding(horizontal = 14.dp)
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTimestampInternal(message.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )

                if (isMe) {
                    if (seenUsers.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-4).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            seenUsers.take(5).forEach { user ->
                                KeeftalkAvatar(
                                    avatarUrl = user.avatarUrl,
                                    initials = user.initials,
                                    seed = user.id,
                                    size = 14.dp,
                                    isOnline = false
                                )
                            }
                            if (seenUsers.size > 5) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        .border(1.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${seenUsers.size - 5}",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (isLastSeenByPeer) {
                        KeeftalkAvatar(
                            avatarUrl = peerAvatarUrl,
                            initials = AvatarUtils.getInitials(peerName),
                            seed = peerId,
                            size = 12.dp,
                            isOnline = false
                        )
                    } else if (isLatest) {
                        MessageStatusIcon(message.status, contentColor, transferProgress, isGroup, seenCount)
                    }
                }
            }
        }
    }
}

@Composable
fun MediaStackBubble(
    messages: List<Message>,
    currentUserId: String,
    onMediaClick: (String, Long) -> Unit,
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onToggleMediaLock: (String, Boolean) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val message = messages.getOrNull(currentIndex) ?: return
    val isMe = message.senderId == currentUserId
    val mediaUri = message.localFilePath ?: message.fileUrl ?: ""
    val isHD = (message.fileSize ?: 0L) > 2 * 1024 * 1024

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(message.id) {
                detectTapGestures(
                    onTap = { onMediaClick(message.id, 0L) },
                    onLongPress = { onLongClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        messages.forEachIndexed { index, m ->
            if (index != currentIndex && index > currentIndex && index < currentIndex + 3) {
                val rotation = (index - currentIndex) * 5f
                val offset = ((index - currentIndex) * 8).dp
                
                AsyncImage(
                    model = m.localFilePath ?: m.fileUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                            translationX = offset.toPx()
                            translationY = offset.toPx()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .alpha(0.3f),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (isHD) {
                HDBadge(modifier = Modifier.align(if (isMe) Alignment.TopStart else Alignment.TopEnd).padding(8.dp))
            }

            if (messages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { if (currentIndex < messages.size - 1) currentIndex++ },
                        enabled = currentIndex < messages.size - 1,
                        modifier = Modifier.padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                
                Text(
                    text = "${currentIndex + 1} / ${messages.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (isMe) {
                IconButton(
                    onClick = { onToggleMediaLock(message.id, !message.mediaLocked) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (message.mediaLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Toggle Lock",
                        tint = if (message.mediaLocked) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HDBadge(modifier: Modifier = Modifier) {
    val chatTheme = LocalChatTheme.current
    
    val backgroundBrush = remember(chatTheme.id) {
        if (chatTheme.id == "rosa") Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFD44AD6)))
        else if (chatTheme.id == "alpha") Brush.linearGradient(listOf(Color(0xFFC9A84C), Color(0xFF8B732A)))
        else Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF)))
    }

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                "HD",
                color = if (chatTheme.id == "alpha") Color.Black else Color.White,
                fontSize = 11.sp, 
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    contentColor: Color,
    progress: Float? = null,
    isGroup: Boolean = false,
    seenCount: Int = 0
) {
    val chatTheme = LocalChatTheme.current
    
    if (status == MessageStatus.SENDING) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress ?: 0.1f },
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF007AFF)
            )
            if (progress != null && progress > 0f) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 6.sp,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    if (isGroup && seenCount > 0) {
        Text(
            text = "Seen by $seenCount",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 2.dp)
        )
        return
    }

    when (status) {
        MessageStatus.SENT -> {
            Icon(LucideIcons.EyeClosed, null, modifier = Modifier.size(12.dp), tint = contentColor.copy(alpha = 0.6f))
        }
        MessageStatus.DELIVERED, MessageStatus.FCM_RECEIVED -> {
            Icon(LucideIcons.EyeOff, null, modifier = Modifier.size(12.dp), tint = contentColor.copy(alpha = 0.6f))
        }
        MessageStatus.SEEN -> {
            // Usually SEEN is handled by showing the avatar, but if we fall back here:
            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(12.dp), tint = if (chatTheme.id == "rosa") Color(0xFFD44AD6) else Color(0xFF00BFFF))
        }
        MessageStatus.FAILED -> {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(12.dp), tint = Color.Red)
        }
        MessageStatus.SENDING -> {}
    }
}

@Composable
fun LocationMessageBubble(
    message: Message,
    contentColor: Color,
    onLocationClick: (Double, Double) -> Unit
) {
    val geoUri = message.fileUrl ?: ""
    val (lat, lon) = remember(geoUri) {
        try {
            val coords = geoUri.removePrefix("geo:").substringBefore("?").split(",")
            coords[0].toDouble() to coords[1].toDouble()
        } catch (_: Exception) {
            0.0 to 0.0
        }
    }

    val location = LatLng(lat, lon)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }

    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clickable { onLocationClick(lat, lon) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false
                )
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(position = location)
                )
            }
            // Overlay to capture clicks and prevent map interaction inside the bubble
            Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isLive = geoUri.contains("live=true")
            Icon(
                if (isLive) Icons.Default.MyLocation else Icons.Default.Place,
                null,
                tint = if (isLive) Color(0xFF25D366) else contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLive) "Live Location" else "Location",
                style = MaterialTheme.typography.labelMedium,
                color = if (isLive) Color(0xFF25D366) else contentColor,
                fontWeight = FontWeight.Bold
            )
        }
        if (message.content.isNotBlank()) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SharedEmailBubbleContent(
    message: Message,
    isMe: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = contentColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, null, tint = Color(0xFF2196F3), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Shared Email", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                Text(message.content, style = MaterialTheme.typography.bodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SharedVaultFileBubbleContent(
    message: Message,
    isMe: Boolean,
    contentColor: Color,
    onVaultClick: (String) -> Unit,
    onMediaClick: (String, Long) -> Unit,
    onPdfClick: (String) -> Unit,
    onCodeClick: (String) -> Unit = {}
) {
    val file = message.attachments.firstOrNull()
    
    val onClick = {
        val f = message.attachments.firstOrNull()
        val fname = f?.fileName?.lowercase() ?: ""
        val isPdf = fname.endsWith(".pdf")
        val isCode = fname.endsWith(".java") || fname.endsWith(".kt") || fname.endsWith(".txt") ||
                     fname.endsWith(".py") || fname.endsWith(".js") || fname.endsWith(".html") ||
                     fname.endsWith(".css") || fname.endsWith(".json") || fname.endsWith(".xml") ||
                     fname.endsWith(".log") || fname.endsWith(".c") || fname.endsWith(".cpp") ||
                     fname.endsWith(".h") || fname.endsWith(".sh") || fname.endsWith(".ts") ||
                     fname.endsWith(".sql") || fname.endsWith(".md") || fname.endsWith(".yaml") ||
                     fname.endsWith(".yml") || fname.endsWith(".toml")

        when {
            f?.fileType == FileType.IMAGE || f?.fileType == FileType.VIDEO -> onMediaClick(message.id, 0L)
            isPdf -> onPdfClick(message.id)
            isCode -> onCodeClick(message.id)
            else -> onVaultClick(message.fileUrl ?: "")
        }
    }

    Surface(
        onClick = onClick,
        color = contentColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EncryptedThumbnail(
                file = file,
                modifier = Modifier.size(52.dp),
                chatId = message.chatId
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.content, 
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                    color = contentColor, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudQueue, 
                        contentDescription = null, 
                        tint = contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vault • ${file?.fileSize?.let { formatSize(it) } ?: "E2EE"}", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight, 
                contentDescription = null, 
                tint = contentColor.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(kb)
}

@Composable
fun SharedAgendaBubbleContent(
    message: Message,
    isMe: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = contentColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFFE91E63), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Shared Agenda", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                Text(message.content, style = MaterialTheme.typography.bodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
@Composable
fun SharedNoteBubbleContent(
    content: String,
    noteId: String,
    isMe: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    checkAvailability: suspend (String) -> Boolean
) {
    var isAvailable by remember { mutableStateOf(value = true) }

    LaunchedEffect(noteId) {
        isAvailable = checkAvailability(noteId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAvailable) { onClick() }
            .padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                contentDescription = null,
                tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Shared Note",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isAvailable) content else "This shared note is no longer available.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isAvailable) contentColor.copy(alpha = 0.9f) else Color.Red.copy(alpha = 0.8f)
        )
        if (isAvailable) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap to open",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

