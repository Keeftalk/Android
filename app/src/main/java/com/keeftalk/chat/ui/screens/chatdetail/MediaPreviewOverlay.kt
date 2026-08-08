package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.ui.screens.ImageViewer
import com.keeftalk.chat.ui.screens.VideoViewer

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.keeftalk.chat.ui.theme.LocalChatTheme

enum class MediaQuality {
    OFF, ON, ORIGINAL
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewOverlay(
    mediaItems: List<com.keeftalk.chat.domain.model.EditorModel>,
    onDismiss: () -> Unit,
    qualityMap: Map<Long, MediaQuality>,
    onQualityChange: (Long, MediaQuality) -> Unit,
    onExecuteCommand: (Int, com.keeftalk.chat.domain.model.EditorCommand) -> Unit,
    onPageChange: (Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val chatTheme = LocalChatTheme.current
    var currentMode by remember { mutableStateOf(com.keeftalk.chat.ui.screens.editor.EditorMode.NONE) }

    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) { } // Consume clicks
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = currentMode == com.keeftalk.chat.ui.screens.editor.EditorMode.NONE
        ) { page ->
            val editorModel = mediaItems[page]
            val item = editorModel.mediaItem
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (item?.isVideo == true) {
                    VideoViewer(uri = item.uri.toString(), onToggleControls = {}, initialPlaybackPosition = 0L)
                } else if (item?.isImage == true) {
                    com.keeftalk.chat.ui.screens.editor.ImageEditorView(
                        state = editorModel,
                        onExecute = { onExecuteCommand(page, it) },
                        currentMode = currentMode,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top controls
        val currentEditorModel = mediaItems.getOrNull(pagerState.currentPage)
        val currentItem = currentEditorModel?.mediaItem

        Column(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                if (currentItem != null) {
                    val quality = qualityMap[currentItem.id] ?: MediaQuality.OFF
                    var showQualityMenu by remember { mutableStateOf(false) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // HD toggle
                        Box {
                            val backgroundBrush = remember(chatTheme.id) {
                                if (chatTheme.id == "rosa") Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFD44AD6)))
                                else if (chatTheme.id == "alpha") Brush.linearGradient(listOf(Color(0xFFC9A84C), Color(0xFF8B732A)))
                                else Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF)))
                            }

                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .then(
                                        if (quality != MediaQuality.OFF) Modifier.background(backgroundBrush, RoundedCornerShape(20.dp))
                                        else Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onQualityChange(currentItem.id, if (quality == MediaQuality.OFF) MediaQuality.ON else MediaQuality.OFF)
                                        },
                                        onLongClick = {
                                            showQualityMenu = true
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Hd, contentDescription = null, tint = if (chatTheme.id == "alpha" && quality != MediaQuality.OFF) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when(quality) {
                                            MediaQuality.OFF -> "HD OFF"
                                            MediaQuality.ON -> "HD ON"
                                            MediaQuality.ORIGINAL -> "Original"
                                        },
                                        color = if (chatTheme.id == "alpha" && quality != MediaQuality.OFF) Color.Black else Color.White,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("HD Off") },
                                    onClick = { onQualityChange(currentItem.id, MediaQuality.OFF); showQualityMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("HD On") },
                                    onClick = { onQualityChange(currentItem.id, MediaQuality.ON); showQualityMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Original") },
                                    onClick = { onQualityChange(currentItem.id, MediaQuality.ORIGINAL); showQualityMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // Editor Toolbar
            com.keeftalk.chat.ui.screens.editor.EditorToolbar(
                currentMode = currentMode,
                onModeChange = { currentMode = it },
                onAutoBlurFaces = { /* TODO: Support in overlay */ },
                onAddSticker = { /* TODO: Support in overlay */ }
            )
        }
    }
}
