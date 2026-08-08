package com.keeftalk.chat.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.ui.components.FixedRichTextEditor
import com.keeftalk.chat.ui.screens.chatdetail.ChatInput
import com.keeftalk.chat.ui.emoji.EmojiPicker
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaReviewScreen(
    initialMedia: List<MediaItem>,
    initialDocs: List<DocumentModel>,
    onBack: () -> Unit,
    onSend: (List<EditorModel>) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: MediaReviewViewModel = viewModel()
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialMedia, initialDocs) {
        viewModel.setMixedItems(initialMedia, initialDocs)
    }

    val items by viewModel.reviewItems.collectAsState()
    val editorStates by viewModel.editorStates.collectAsState()
    
    if (items.isEmpty()) return

    val pagerState = rememberPagerState { items.size }
    val currentItem = items.getOrNull(pagerState.currentPage) ?: return
    val currentItemId = currentItem.mediaItem?.id ?: currentItem.documentItem?.id ?: 0L
    val currentState = editorStates[currentItemId] ?: currentItem

    var currentMode by remember { mutableStateOf(EditorMode.NONE) }
    var isExporting by remember { mutableStateOf(value = false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            Surface(color = Color.Black.copy(alpha = 0.6f)) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Column {
                            val name = currentItem.mediaItem?.displayName ?: currentItem.documentItem?.name ?: ""
                            Text(name, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
                        IconButton(onClick = { viewModel.undo(currentItemId) }, enabled = !isExporting) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { viewModel.redo(currentItemId) }, enabled = !isExporting) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                if (showEmojiPicker) {
                    EmojiPicker(
                        onEmojiSelected = { viewModel.updateCaption(currentItemId, currentState.caption + it) },
                        onDismiss = { showEmojiPicker = false },
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                }
                
                ChatInput(
                    messageText = currentState.caption,
                    onMessageChange = { viewModel.updateCaption(currentItemId, it) },
                    onSendMessage = { 
                        isExporting = true
                        onSend(editorStates.values.toList()) 
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
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // IMPORTANT: HorizontalPager swipe is DISABLED for PDF items to allow for 
            // internal manual paging via buttons and to prevent accidental navigation.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isExporting && currentMode == EditorMode.NONE && currentItem.documentItem?.type != DocumentType.PDF
            ) { page ->
                val model = items[page]
                val id = model.mediaItem?.id ?: model.documentItem?.id ?: 0L
                val state = editorStates[id] ?: model
                
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.mediaItem?.isImage == true -> {
                            ImageEditorView(state = state, onExecute = { viewModel.executeCommand(id, it) }, currentMode = currentMode)
                        }
                        state.documentItem?.type == DocumentType.PDF -> {
                            PdfEditorView(state = state, onExecute = { viewModel.executeCommand(id, it) }, currentMode = currentMode)
                        }
                        state.documentItem != null && isTextDocument(state.documentItem.mimeType, state.documentItem.name) -> {
                            TextDocumentEditor(state = state, onTextChange = { viewModel.updateEditedText(id, it) })
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No preview available", color = Color.White)
                                }
                            }
                        }
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

@Composable
fun PdfEditorView(
    state: EditorModel,
    onExecute: (EditorCommand) -> Unit,
    currentMode: EditorMode
) {
    val context = LocalContext.current
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(state.documentItem?.uri, currentPage) {
        state.documentItem?.uri?.let { uri ->
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        val renderer = PdfRenderer(pfd)
                        pageCount = renderer.pageCount
                        if (currentPage < pageCount) {
                            val page = renderer.openPage(currentPage)
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pdfBitmap = bitmap
                            page.close()
                        }
                        renderer.close()
                        pfd.close()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PDF_PREVIEW", "Failed to render PDF page $currentPage", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (pdfBitmap != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageEditorView(
                    state = state.copy(mediaItem = MediaItem(0, Uri.EMPTY, "", "", 0, 0, pdfBitmap!!.width, pdfBitmap!!.height, null, "", "")),
                    onExecute = onExecute,
                    currentMode = currentMode,
                    overrideBitmap = pdfBitmap
                )
                
                if (pageCount > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp) // Above ChatInput
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                            }
                            
                            Text(
                                text = "Page ${currentPage + 1} / $pageCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = { if (currentPage < pageCount - 1) currentPage++ },
                                enabled = currentPage < pageCount - 1
                            ) {
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

enum class EditorMode {
    NONE, DRAW, TEXT, STICKER, BLUR, CROP
}

@Composable
fun EditorToolbar(
    currentMode: EditorMode,
    onModeChange: (EditorMode) -> Unit,
    onAutoBlurFaces: () -> Unit,
    onAddSticker: () -> Unit,
) {
    Surface(color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onModeChange(EditorMode.DRAW) }) {
                Icon(Icons.Default.Edit, contentDescription = "Draw", tint = if (currentMode == EditorMode.DRAW) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = { onModeChange(EditorMode.TEXT) }) {
                Icon(Icons.Default.TextFields, contentDescription = "Text", tint = if (currentMode == EditorMode.TEXT) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = { onModeChange(EditorMode.BLUR) }) {
                Icon(Icons.Default.BlurOn, contentDescription = "Blur", tint = if (currentMode == EditorMode.BLUR) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = onAutoBlurFaces) {
                Icon(Icons.Default.Face, contentDescription = "Auto Blur Faces", tint = Color.White)
            }
            IconButton(onClick = { onModeChange(EditorMode.CROP) }) {
                Icon(Icons.Default.Crop, contentDescription = "Crop", tint = if (currentMode == EditorMode.CROP) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = onAddSticker) {
                Icon(Icons.Default.StickyNote2, contentDescription = "Stickers", tint = if (currentMode == EditorMode.STICKER) MaterialTheme.colorScheme.primary else Color.White)
            }
        }
    }
}

@Composable
fun TextDocumentEditor(
    state: EditorModel,
    onTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    val richTextState = remember { RichTextState() }
    
    LaunchedEffect(state.documentItem?.uri) {
        state.documentItem?.uri?.let { uri ->
            withContext(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    withContext(Dispatchers.Main) {
                        richTextState.setText(content)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TEXT_EDITOR", "Failed to read text file", e)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        FixedRichTextEditor(
            state = richTextState,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp)
        )
        
        LaunchedEffect(richTextState.toText()) {
            onTextChange(richTextState.toText())
        }
    }
}

fun isTextDocument(mimeType: String, name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return mimeType.startsWith("text/") || ext in listOf("txt", "xml", "html", "md", "json", "yaml", "yml", "toml", "kt", "java", "py", "c", "cpp", "js", "ts", "sql")
}
