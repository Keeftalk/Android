package com.keeftalk.chat.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.ui.screens.chatdetail.ChatInput
import com.keeftalk.chat.ui.emoji.EmojiPicker
import com.keeftalk.chat.ui.screens.editor.signal.renderers.MultiLineTextRenderer
import com.keeftalk.chat.ui.screens.editor.signal.ColorableRenderer
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext
import com.keeftalk.chat.ui.screens.editor.signal.SignalImageEditorView
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel
import com.keeftalk.chat.ui.screens.editor.MediaReviewViewModel.ReviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaReviewScreen(
    initialMedia: List<MediaItem>,
    initialDocs: List<DocumentModel>,
    onBack: () -> Unit,
    onSend: (List<com.keeftalk.chat.domain.model.EditorModel>) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: MediaReviewViewModel = viewModel()
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialMedia, initialDocs) {
        viewModel.setMixedItems(initialMedia, initialDocs)
    }

    val items by viewModel.reviewItems.collectAsState()
    val editorStates by viewModel.editorStates.collectAsState()
    val captions by viewModel.captions.collectAsState()
    val videoTrimRanges by viewModel.videoTrimRanges.collectAsState()
    
    val pagerState = rememberPagerState { items.size }
    val currentItem = items.getOrNull(pagerState.currentPage)
    val currentItemId = currentItem?.id ?: 0L
    val currentState = editorStates[currentItemId]
    val currentCaption = captions[currentItemId] ?: ""
    val isReady = items.isNotEmpty() && currentState != null

    var currentMode by remember { mutableStateOf(EditorMode.NONE) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var strokeWidth by remember { mutableFloatStateOf(0.02f) }
    var editorViewRef by remember { mutableStateOf<SignalImageEditorView?>(null) }
    var isExporting by remember { mutableStateOf(value = false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val isTextEditing = editorViewRef?.isTextEditing() == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            Column(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f)).statusBarsPadding()) {
                val itemForHeader = items.getOrNull(pagerState.currentPage)
                TopAppBar(
                    title = {
                        Column {
                            Text(itemForHeader?.displayName ?: "", color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text("${pagerState.currentPage + 1} of ${items.size}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isExporting) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.undo(currentItemId) }, enabled = !isExporting && isReady) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { viewModel.redo(currentItemId) }, enabled = !isExporting && isReady) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                    }
                )
                
                if (isReady) {
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
                        onAutoBlurFaces = { 
                            itemForHeader?.let { viewModel.detectAndBlurFaces(it.id, context) }
                        },
                        onAddSticker = { /* TODO */ },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
        },
        bottomBar = {
            if (isReady) {
                Column {
                    if (showEmojiPicker) {
                        EmojiPicker(
                            onEmojiSelected = { viewModel.updateCaption(currentItemId, currentCaption + it) },
                            onDismiss = { showEmojiPicker = false },
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                    }
                    
                    ChatInput(
                        messageText = currentCaption,
                        onMessageChange = { viewModel.updateCaption(currentItemId, it) },
                        onSendMessage = { 
                            isExporting = true
                            scope.launch {
                                val results = withContext(Dispatchers.IO) {
                                    items.map { item ->
                                        val caption = captions[item.id] ?: ""
                                        val trimRange = videoTrimRanges[item.id]
                                        
                                        when (item) {
                                            is ReviewItem.Media -> {
                                                val signalModel = editorStates[item.id]
                                                if (item.item.isImage) {
                                                    com.keeftalk.chat.domain.model.EditorModel(
                                                        mediaItem = item.item,
                                                        caption = caption,
                                                        signalState = signalModel
                                                    )
                                                } else {
                                                    com.keeftalk.chat.domain.model.EditorModel(
                                                        mediaItem = item.item,
                                                        caption = caption,
                                                        videoTrimRange = trimRange,
                                                        signalState = signalModel
                                                    )
                                                }
                                            }
                                            is ReviewItem.Doc -> {
                                                com.keeftalk.chat.domain.model.EditorModel(
                                                    documentItem = item.item,
                                                    caption = caption
                                                )
                                            }
                                        }
                                    }
                                }
                                onSend(results)
                            }
                        },
                        onCameraClick = {},
                        onCameraLongClick = {},
                        onAttachmentClick = {},
                        onEmojiClick = { showEmojiPicker = !showEmojiPicker },
                        onMicStart = {},
                        onMicStop = {},
                        onMicCancel = {},
                        autoDeleteTimer = null,
                        focusRequester = focusRequester,
                        isCaptioning = true
                    )
                }
            }
        }
    ) { padding ->
        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isExporting && currentMode == EditorMode.NONE && !isTextEditing
                ) { page ->
                    val item = items[page]
                    val state = editorStates[item.id] ?: return@HorizontalPager
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        val isVideo = item is ReviewItem.Media && item.item.isVideo
                        val isImage = item is ReviewItem.Media && item.item.isImage
                        
                        if (isVideo) {
                            val mediaItem = (item as ReviewItem.Media).item
                            Box(modifier = Modifier.fillMaxSize()) {
                                com.keeftalk.chat.ui.screens.VideoViewer(
                                    uri = mediaItem.uri.toString(),
                                    onToggleControls = {},
                                    initialPlaybackPosition = 0L
                                )
                                
                                AndroidView(
                                    factory = { ctx ->
                                        SignalImageEditorView(ctx).apply {
                                            setModel(state)
                                            setMode(mapMode(currentMode))
                                            setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                            
                                            tapListener = object : SignalImageEditorView.TapListener {
                                                override fun onEntityDown(element: com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement?) {}
                                                override fun onEntitySingleTap(element: com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement?) {}
                                                override fun onEntityDoubleTap(element: com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement) {
                                                    startTextEditing(element)
                                                }
                                            }
                                        }
                                    },
                                    update = { view ->
                                        view.setModel(state)
                                        view.setMode(mapMode(currentMode))
                                        view.setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                        if (pagerState.currentPage == page) {
                                            editorViewRef = view
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else if (isImage) {
                            AndroidView(
                                factory = { ctx ->
                                    SignalImageEditorView(ctx).apply {
                                        setModel(state)
                                        setMode(mapMode(currentMode))
                                        setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                        
                                        tapListener = object : SignalImageEditorView.TapListener {
                                            override fun onEntityDown(element: com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement?) {}
                                            override fun onEntitySingleTap(element: com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement?) {}
                                            override fun onEntityDoubleTap(element: com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement) {
                                                startTextEditing(element)
                                            }
                                        }
                                    }
                                },
                                update = { view ->
                                    view.setModel(state)
                                    view.setMode(mapMode(currentMode))
                                    view.setDrawingBrush(selectedColor.toArgb(), strokeWidth, android.graphics.Paint.Cap.ROUND)
                                    if (pagerState.currentPage == page) {
                                        editorViewRef = view
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Document preview
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(item.displayName, color = Color.White)
                                    Text("No editor for this file type", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Tools OVER the pager
                if (isReady) {
                    val item = currentItem!!
                    val isVideo = item is ReviewItem.Media && item.item.isVideo
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isVideo && currentMode == EditorMode.NONE) {
                            val mediaItem = (item as ReviewItem.Media).item
                            val videoTrimRange = videoTrimRanges[item.id] ?: (0L..(mediaItem.duration ?: 0L))
                            
                            com.keeftalk.chat.ui.screens.editor.components.VideoThumbnailsRangeSelectorView(
                                uri = mediaItem.uri,
                                totalDuration = mediaItem.duration ?: 0L,
                                selectedRange = videoTrimRange,
                                onRangeChange = { viewModel.updateVideoTrim(item.id, it.first, it.last) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Trim Video", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (isExporting) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.5f)) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Preparing files...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun saveBitmapToTempFile(context: Context, bitmap: Bitmap): Uri {
    return withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "edited_${UUID.randomUUID()}.jpg")
        file.outputStream().use { 
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        Uri.fromFile(file)
    }
}
