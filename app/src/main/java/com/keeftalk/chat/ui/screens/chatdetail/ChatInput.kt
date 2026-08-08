package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.domain.model.MediaAlbum
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.ui.screens.VoiceRecordingState
import com.keeftalk.chat.ui.screens.picker.components.MediaItemThumbnail
import androidx.compose.ui.res.painterResource
import com.keeftalk.chat.R
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.LocalChatSettings
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.util.LanguageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class AttachmentType {
    PICTURE, SHARE, VOICE_FILE, FILE, APK, LOCATION, CONTACT, POLL
}

data class AttachmentItem(val type: AttachmentType, val label: String, val icon: ImageVector, val color: Color)

@Composable
fun AttachmentIconButton(item: AttachmentItem, onClick: (AttachmentType) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item.type) }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(item.color.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, item.color.copy(alpha = 0.2f), CircleShape), 
            contentAlignment = Alignment.Center
        ) { 
            Icon(item.icon, null, tint = item.color, modifier = Modifier.size(28.dp)) 
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            item.label, 
            style = MaterialTheme.typography.labelMedium, 
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AttachmentMenu(
    onTypeClick: (AttachmentType) -> Unit,
    mediaItems: LazyPagingItems<MediaItem>? = null,
    onMediaClick: (MediaItem) -> Unit = {},
    albums: List<MediaAlbum> = emptyList(),
    selectedAlbum: MediaAlbum? = null,
    onAlbumClick: (MediaAlbum?) -> Unit = {},
    selectedItems: List<MediaItem> = emptyList(),
    onToggleSelection: (MediaItem) -> Unit = {},
    onNextClick: () -> Unit = {},
    onGooglePhotosClick: () -> Unit = {},
    onGooglePhotosAppClick: () -> Unit = {}
) {
    val icons = LocalAppIcons.current
    val documentColor = Color(0xFF2196F3)
    val attachmentItems = remember(icons) {
        listOf(
            AttachmentItem(AttachmentType.PICTURE, "Gallery", icons.image, documentColor),
            AttachmentItem(AttachmentType.SHARE, "Share", Icons.Default.Share, documentColor),
            AttachmentItem(AttachmentType.VOICE_FILE, "Audio", icons.mic, documentColor),
            AttachmentItem(AttachmentType.FILE, "Document", icons.attach, documentColor),
            AttachmentItem(AttachmentType.APK, "App", Icons.Default.Android, documentColor),
            AttachmentItem(AttachmentType.LOCATION, "Location", Icons.Default.Place, documentColor),
            AttachmentItem(AttachmentType.CONTACT, "Contact", icons.user, documentColor),
            AttachmentItem(AttachmentType.POLL, "Poll", Icons.Default.Poll, documentColor)
        )
    }

    var showAlbumDropdown by remember { mutableStateOf(false) }
    
    // Pre-chunk icon rows to avoid recomposition work
    val iconRows = remember(attachmentItems) { attachmentItems.chunked(4) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight() // Allow expansion up to top bar
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, 
                end = 16.dp, 
                top = 16.dp, 
                bottom = if (selectedItems.isNotEmpty()) 100.dp else 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(contentType = "title") {
                Text(
                    "Share Content", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(iconRows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in rowItems) {
                        Box(modifier = Modifier.weight(1f)) {
                            AttachmentIconButton(item, onTypeClick)
                        }
                    }
                }
            }

            item(contentType = "gallery_header") {
                Row(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Gallery", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onGooglePhotosClick, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Cloud, 
                                contentDescription = "Google Photos Cloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onGooglePhotosAppClick, modifier = Modifier.size(24.dp)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_google_photos),
                                contentDescription = "Google Photos App",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Box {
                        Surface(
                            onClick = { showAlbumDropdown = true },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedAlbum?.name ?: "Recent",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showAlbumDropdown,
                            onDismissRequest = { showAlbumDropdown = false },
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recent") },
                                onClick = { onAlbumClick(null); showAlbumDropdown = false }
                            )
                            albums.forEach { album ->
                                DropdownMenuItem(
                                    text = { Text(album.name) },
                                    onClick = { onAlbumClick(album); showAlbumDropdown = false }
                                )
                            }
                        }
                    }
                }
            }

            if (mediaItems != null) {
                val mediaCount = mediaItems.itemCount
                val columns = 6 
                val rowCount = (mediaCount + columns - 1) / columns
                
                items(rowCount) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (colIndex in 0 until columns) {
                            val itemIndex = rowIndex * columns + colIndex
                            Box(modifier = Modifier.weight(1f)) {
                                if (itemIndex < mediaCount) {
                                    val item = mediaItems[itemIndex]
                                    if (item != null) {
                                        val selectionIndex = selectedItems.indexOfFirst { it.id == item.id }
                                        MediaItemThumbnail(
                                            item = item,
                                            isSelected = selectionIndex != -1,
                                            selectionIndex = selectionIndex,
                                            isSelectionModeActive = selectedItems.isNotEmpty(),
                                            onThumbnailClick = { 
                                                if (selectedItems.isNotEmpty()) {
                                                    onToggleSelection(item)
                                                } else {
                                                    onMediaClick(item)
                                                }
                                            },
                                            onLongClick = { onToggleSelection(item) },
                                            onToggleSelection = { onToggleSelection(item) }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                            startY = 0f,
                            endY = 50f
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Next (${selectedItems.size})")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
fun ChatInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCameraClick: () -> Unit,
    onCameraLongClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onMicStart: () -> Unit,
    onMicStop: (Boolean) -> Unit,
    onMicCancel: () -> Unit,
    autoDeleteTimer: Long?,
    focusRequester: FocusRequester,
    replyingToMessage: Message? = null,
    onCancelReply: () -> Unit = {},
    recordingState: VoiceRecordingState = VoiceRecordingState.IDLE,
    recordingDuration: Long = 0L,
    amplitudeHistory: List<Float> = emptyList(),
    onSendVoice: () -> Unit = {},
    hasMicPermission: Boolean = false,
    onRequestMicPermission: () -> Unit = {},
    isCaptioning: Boolean = false
) {
    val icons = LocalAppIcons.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentRecordingState by rememberUpdatedState(recordingState)
    val currentMessageText by rememberUpdatedState(messageText)
    val currentIsCaptioning by rememberUpdatedState(isCaptioning)

    Column {
        if (replyingToMessage != null && recordingState == VoiceRecordingState.IDLE) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Replying to", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            replyingToMessage.content, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis, 
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Surface(
            tonalElevation = 0.dp,
            color = Color.Transparent,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).union(WindowInsets.ime))
                .padding(bottom = if (LocalChatTheme.current.id == "rosa") 12.dp else 0.dp),
            shape = RectangleShape
        ) {
            val chatTheme = LocalChatTheme.current
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recordingState == VoiceRecordingState.RECORDING || recordingState == VoiceRecordingState.PREVIEW) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (recordingState == VoiceRecordingState.RECORDING) {
                            val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "pulseAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .graphicsLayer { alpha = pulseAlpha }
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "REC",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                formatDurationInternal(recordingDuration),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            VoiceWaveform(
                                amplitudes = amplitudeHistory,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (dragOffset < -100f) "Release to cancel" else "Slide to cancel <",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dragOffset < -100f) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            VoicePreviewPlayer(
                                duration = recordingDuration,
                                onDelete = onMicCancel,
                                onSend = onSendVoice
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(
                                color = if (chatTheme.id == "rosa") Color.White.copy(alpha = 0.15f)
                                        else if (chatTheme.id == "alpha") Color.White.copy(alpha = 0.06f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .then(
                                if (chatTheme.id == "rosa") Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                else if (chatTheme.id == "alpha") Modifier.border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                                else Modifier
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onEmojiClick) {
                            Icon(icons.emoji, contentDescription = "Emoji", tint = if (chatTheme.id == "rosa") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        val chatSettingsLocal = LocalChatSettings.current
                        val isTextRtl = remember(messageText) { LanguageUtils.isRtl(messageText) }

                        CompositionLocalProvider(LocalLayoutDirection provides (if (isTextRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                            BasicTextField(
                                value = messageText,
                                onValueChange = onMessageChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                                            if (chatSettingsLocal.enterKeyBehavior == "SEND" && messageText.isNotBlank()) {
                                                onSendMessage()
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (chatTheme.id == "rosa" || chatTheme.id == "alpha") Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    textAlign = if (isTextRtl) androidx.compose.ui.text.style.TextAlign.Right else androidx.compose.ui.text.style.TextAlign.Left
                                ),
                                cursorBrush = Brush.verticalGradient(listOf(if (chatTheme.id == "rosa") Color(0xFFD44AD6) else MaterialTheme.colorScheme.primary, if (chatTheme.id == "rosa") Color(0xFFFF6B9D) else MaterialTheme.colorScheme.primary)),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Default,
                                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                                ),
                                decorationBox = {
                                    if (messageText.isEmpty()) {
                                        Text(
                                            "Type something...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (chatTheme.id == "rosa" || chatTheme.id == "alpha") Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    it()
                                }
                            )
                        }

                        if (autoDeleteTimer != null) GhostModeIndicator(timer = autoDeleteTimer)

                        AnimatedVisibility(
                            visible = messageText.isEmpty(),
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (messageText.isEmpty() && !isCaptioning) {
                                    IconButton(onClick = onAttachmentClick) { Icon(Icons.Default.Add, null, tint = if (chatTheme.id == "rosa") Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                if (!isCaptioning) {
                                    IconButton(onClick = onCameraClick, modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onCameraClick() }, onLongPress = { onCameraLongClick() }) }) { Icon(Icons.Default.CameraAlt, null, tint = if (chatTheme.id == "rosa") Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                
                                if (isCaptioning) {
                                    IconButton(onClick = onAttachmentClick) { Icon(Icons.Default.Add, null, tint = if (chatTheme.id == "rosa") Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        awaitFirstDown()
                                                        if (!hasMicPermission) {
                                                            onRequestMicPermission()
                                                        } else {
                                                            var isLongPress = false
                                                            val longPressJob = scope.launch {
                                                                delay(400.milliseconds)
                                                                isLongPress = true
                                                                if (currentRecordingState == VoiceRecordingState.IDLE) {
                                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                    onMicStart()
                                                                }
                                                            }

                                                            var dragX = 0f
                                                            while (true) {
                                                                val event = awaitPointerEvent()
                                                                if (event.type == PointerEventType.Move) {
                                                                    val change = event.changes.first()
                                                                    dragX += change.position.x - change.previousPosition.x
                                                                    dragOffset = dragX
                                                                } else if (event.type == PointerEventType.Release) {
                                                                    longPressJob.cancel()
                                                                    if (isLongPress) {
                                                                        if (currentRecordingState == VoiceRecordingState.RECORDING) {
                                                                            if (dragOffset < -150f) onMicCancel() else onMicStop(true)
                                                                        }
                                                                    } else {
                                                                        if (currentRecordingState == VoiceRecordingState.IDLE) {
                                                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                            onMicStart()
                                                                        } else if (currentRecordingState == VoiceRecordingState.RECORDING) {
                                                                            onMicStop(false)
                                                                        }
                                                                    }
                                                                    dragOffset = 0f
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Mic, null, tint = if (chatTheme.id == "rosa") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (chatTheme.id == "rosa") Modifier.shadow(20.dp, CircleShape, spotColor = Color(0xFFFF6B9D))
                                else if (chatTheme.id == "alpha") Modifier.border(1.dp, Color(0xFFC8B464).copy(alpha = 0.15f), CircleShape)
                                else Modifier.shadow(28.dp, CircleShape, spotColor = Color(0x4D00CCCC), ambientColor = Color(0x99000000))
                            )
                            .clip(CircleShape)
                            .background(
                                brush = if (recordingState == VoiceRecordingState.RECORDING) SolidColor(Color.Red)
                                else if (chatTheme.id == "rosa") Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFD44AD6)))
                                else if (chatTheme.id == "alpha") SolidColor(Color(0xFFC9A84C).copy(alpha = 0.15f))
                                else Brush.linearGradient(
                                    colors = listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF)),
                                    start = Offset(0f, 0f),
                                    end = Offset.Infinite
                                )
                            )
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitFirstDown()
                                        if (currentIsCaptioning || currentMessageText.isNotEmpty()) {
                                            val up = waitForUpOrCancellation()
                                            if (up != null) {
                                                onSendMessage()
                                            }
                                        } else {
                                            if (!hasMicPermission) {
                                                onRequestMicPermission()
                                            } else {
                                                var isLongPress = false
                                                val longPressJob = scope.launch {
                                                    delay(400.milliseconds)
                                                    isLongPress = true
                                                    if (currentRecordingState == VoiceRecordingState.IDLE) {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        onMicStart()
                                                    }
                                                }

                                                var dragX = 0f
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    if (event.type == PointerEventType.Move) {
                                                        val change = event.changes.first()
                                                        dragX += change.position.x - change.previousPosition.x
                                                        dragOffset = dragX
                                                    } else if (event.type == PointerEventType.Release) {
                                                        longPressJob.cancel()
                                                        if (isLongPress) {
                                                            if (currentRecordingState == VoiceRecordingState.RECORDING) {
                                                                if (dragOffset < -150f) onMicCancel() else onMicStop(true)
                                                            }
                                                        } else {
                                                            if (currentRecordingState == VoiceRecordingState.IDLE) {
                                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                                onMicStart()
                                                            } else if (currentRecordingState == VoiceRecordingState.RECORDING) {
                                                                onMicStop(false)
                                                            } else if (currentRecordingState == VoiceRecordingState.PREVIEW) {
                                                                onSendVoice()
                                                            }
                                                        }
                                                        dragOffset = 0f
                                                        break
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isCaptioning -> Icons.AutoMirrored.Filled.Send
                                messageText.isNotEmpty() -> Icons.AutoMirrored.Filled.Send
                                recordingState == VoiceRecordingState.RECORDING -> Icons.Default.Stop
                                recordingState == VoiceRecordingState.PREVIEW -> Icons.AutoMirrored.Filled.Send
                                else -> Icons.Default.Mic
                            },
                            contentDescription = null,
                            tint = if (chatTheme.id == "rosa") Color.White else if (chatTheme.id == "alpha") Color(0xFFC9A84C) else Color.White
                        )
                    }
                }
            }
        }
    }
}
