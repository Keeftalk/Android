package com.keeftalk.chat.ui.screens.chatdetail

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.EditorModel
import com.keeftalk.chat.ui.screens.VideoViewer
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.keeftalk.chat.ui.screens.editor.BrushTools
import com.keeftalk.chat.ui.screens.editor.EditorMode
import com.keeftalk.chat.ui.screens.editor.EditorToolbar
import com.keeftalk.chat.ui.screens.editor.mapMode
import com.keeftalk.chat.ui.screens.editor.signal.ColorableRenderer
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext
import com.keeftalk.chat.ui.screens.editor.signal.SignalImageEditorView
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel
import com.keeftalk.chat.ui.screens.editor.signal.renderers.MultiLineTextRenderer
import com.keeftalk.chat.ui.screens.editor.signal.renderers.UriGlideRenderer
import com.keeftalk.chat.ui.theme.LocalChatTheme

enum class MediaQuality {
    OFF, ON, ORIGINAL
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewOverlay(
    mediaItems: List<EditorModel>,
    onDismiss: () -> Unit,
    qualityMap: Map<Long, MediaQuality>,
    onQualityChange: (Long, MediaQuality) -> Unit,
    onPageChange: (Int) -> Unit = {},
    onUpdateItem: (Int, EditorModel) -> Unit = { _, _ -> }
) {
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val chatTheme = LocalChatTheme.current
    
    var isEditing by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf(EditorMode.NONE) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var strokeWidth by remember { mutableFloatStateOf(0.02f) }
    
    var editorViewRef by remember { mutableStateOf<SignalImageEditorView?>(null) }

    // Maintain SignalEditorModels for each item internally
    val signalModels = remember { mutableStateMapOf<Long, SignalEditorModel>() }
    
    val isTextEditing = editorViewRef?.isTextEditing() == true

    LaunchedEffect(isTextEditing) {
        // Fix warning
    }

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
            userScrollEnabled = !isEditing && currentMode == EditorMode.NONE && !isTextEditing
        ) { page ->
            val editorModel = mediaItems[page]
            val item = editorModel.mediaItem
            val itemId = item?.id ?: 0L
            
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (item?.isVideo == true) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoViewer(uri = item.uri.toString(), onToggleControls = {}, initialPlaybackPosition = 0L)
                        
                        if (isEditing) {
                            val signalModel = signalModels.getOrPut(itemId) {
                                SignalEditorModel(android.graphics.Color.BLACK).apply {
                                    val renderer = UriGlideRenderer(item.uri, decryptable = false, maxWidth = 2048, maxHeight = 2048)
                                    val el = EditorElement(renderer)
                                    el.flags.setSelectable(false).persist()
                                    addElementWithoutPushUndo(el)
                                }
                            }
                            AndroidView(
                                factory = { ctx ->
                                    SignalImageEditorView(ctx).apply {
                                        setModel(signalModel)
                                        setMode(mapMode(currentMode))
                                        setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                    }
                                },
                                update = { view ->
                                    view.setModel(signalModel)
                                    view.setMode(mapMode(currentMode))
                                    view.setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                    if (pagerState.currentPage == page) {
                                        editorViewRef = view
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else if (item?.isImage == true) {
                    if (isEditing) {
                        val signalModel = signalModels.getOrPut(itemId) {
                            SignalEditorModel(android.graphics.Color.BLACK).apply {
                                val renderer = UriGlideRenderer(item.uri, decryptable = false, maxWidth = 2048, maxHeight = 2048)
                                val el = EditorElement(renderer)
                                el.flags.setSelectable(false).persist()
                                addElementWithoutPushUndo(el)
                            }
                        }
                        AndroidView(
                            factory = { ctx ->
                                SignalImageEditorView(ctx).apply {
                                    setModel(signalModel)
                                    setMode(mapMode(currentMode))
                                    setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                    
                                    tapListener = object : SignalImageEditorView.TapListener {
                                        override fun onEntityDown(element: EditorElement?) {}
                                        override fun onEntitySingleTap(element: EditorElement?) {}
                                        override fun onEntityDoubleTap(element: EditorElement) {
                                            startTextEditing(element)
                                        }
                                    }
                                }
                            },
                            update = { view ->
                                view.setModel(signalModel)
                                view.setMode(mapMode(currentMode))
                                view.setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                if (pagerState.currentPage == page) {
                                    editorViewRef = view
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Overlay controls
        val currentItem = mediaItems.getOrNull(pagerState.currentPage)?.mediaItem

        Column(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isEditing) {
                            isEditing = false
                            currentMode = EditorMode.NONE
                        } else onDismiss()
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing) {
                        val currentItemId = currentItem?.id ?: 0L
                        val signalModel = signalModels[currentItemId]
                        IconButton(onClick = { signalModel?.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                        }
                        IconButton(onClick = { signalModel?.redo() }) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = Color.White)
                        }
                    }

                    if (currentItem != null) {
                        IconButton(
                            onClick = { 
                                if (isEditing) {
                                    val currentItemId = currentItem.id
                                    signalModels[currentItemId]?.let { state ->
                                        onUpdateItem(pagerState.currentPage, mediaItems[pagerState.currentPage].copy(signalState = state))
                                    }
                                }
                                isEditing = !isEditing 
                            },
                            modifier = Modifier.padding(horizontal = 4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                    }

                    if (currentItem != null && !isEditing) {
                        val quality = qualityMap[currentItem.id] ?: MediaQuality.OFF
                        var showQualityMenu by remember { mutableStateOf(false) }

                        // HD toggle
                        Box(modifier = Modifier.padding(start = 8.dp)) {
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

            if (isEditing) {
                Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f))) {
                    EditorToolbar(
                        currentMode = currentMode,
                        onModeChange = { 
                            if (it == EditorMode.CROP) {
                                editorViewRef?.startCrop()
                                currentMode = it
                            } else if (currentMode == EditorMode.CROP) {
                                editorViewRef?.doneCrop()
                                currentMode = it
                            } else if (it == EditorMode.TEXT) {
                                editorViewRef?.addText()
                                currentMode = it
                            } else {
                                currentMode = it 
                            }
                        },
                        onAutoBlurFaces = { /* TODO */ },
                        onAddSticker = { /* TODO */ },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    if (currentMode == EditorMode.DRAW || currentMode == EditorMode.BLUR || currentMode == EditorMode.TEXT || isTextEditing) {
                        BrushTools(
                            selectedColor = selectedColor,
                            onColorSelect = { 
                                selectedColor = it 
                                if (currentMode == EditorMode.TEXT || isTextEditing) {
                                    val sel = editorViewRef?.getModel()?.getSelectedElement()
                                    val renderer = sel?.renderer
                                    if (renderer is ColorableRenderer) {
                                        renderer.color = it.toArgb()
                                        editorViewRef?.invalidate()
                                    }
                                }
                            },
                            strokeWidth = if (currentMode == EditorMode.TEXT || isTextEditing) null else strokeWidth,
                            onStrokeWidthChange = { strokeWidth = it },
                            showStyleToggle = currentMode == EditorMode.TEXT || isTextEditing,
                            onStyleToggle = {
                                val sel = editorViewRef?.getModel()?.getSelectedElement()
                                val renderer = sel?.renderer
                                if (renderer is MultiLineTextRenderer) {
                                    renderer.nextMode()
                                    editorViewRef?.invalidate()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
