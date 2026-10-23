package tn.keeftalk.chat.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.components.KeeftalkAvatar
import tn.keeftalk.chat.ui.screens.editor.EditIcons
import tn.keeftalk.chat.ui.screens.editor.LocalNotesPalette
import tn.keeftalk.chat.ui.screens.editor.NotesDesign
import tn.keeftalk.chat.ui.screens.editor.NotesPalette
import tn.keeftalk.chat.ui.screens.editor.components.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeNoteEditorScreen(
    noteId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // key = noteId: a different note can never inherit another note's ViewModel (and overwrite it on save).
    val viewModel: NoteEditorViewModel = viewModel(key = noteId) {
        NoteEditorViewModel(
            AppModule.provideNoteRepository(context),
            AppModule.provideAuthRepository(context),
            AppModule.provideChatRepository(context)
        )
    }

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    // Persist on every way out: leaving the app, opening the picker, composable disposal, back press.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                coroutineScope.launch { viewModel.flushNow() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            coroutineScope.launch { viewModel.flushNow() }
        }
    }
    val leave: () -> Unit = {
        coroutineScope.launch { 
            viewModel.flushNow()
            onBack()
        }
    }
    BackHandler(onBack = leave)

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val basePalette = NotesDesign.colors()
    var isEditorDark by remember { mutableStateOf(basePalette.isDark) }
    val palette = remember(isEditorDark) { editorPalette(isEditorDark) }

    val note by viewModel.note.collectAsState()
    val title by viewModel.title.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val savingState by viewModel.savingState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isToolbarPopupVisible by viewModel.isToolbarPopupVisible.collectAsState()
    val pendingStyles by viewModel.pendingStyles.collectAsState()
    val focusRequest by viewModel.focusRequest.collectAsState()
    // Not delegated on purpose: read inside the LazyColumn scope so only the list recomposes per keystroke.
    val editorState = viewModel.editorState.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadImage(it.toString()) }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(focusRequest) {
        val req = focusRequest ?: return@LaunchedEffect
        val idx = editorState.value.blocks.indexOfFirst { it.id == req.blockId }
        if (idx >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == idx }) {
            listState.animateScrollToItem(idx)
        }
    }

    CompositionLocalProvider(LocalNotesPalette provides palette) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = palette.bg,
                topBar = {
                    // The title field owns its text. External changes (auto-title from the first line) are applied
                    // only while the field is NOT focused, so typing is never overwritten by a stale echo.
                    var titleField by remember { mutableStateOf(TextFieldValue(title)) }
                    var titleFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(title, titleFocused) {
                        if (!titleFocused && title != titleField.text) {
                            titleField = TextFieldValue(title, TextRange(title.length))
                        }
                    }

                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = leave) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = palette.text
                                )
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BasicTextField(
                                    value = titleField,
                                    onValueChange = {
                                        titleField = it
                                        viewModel.updateTitle(it.text)
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.text
                                    ),
                                    cursorBrush = SolidColor(NotesDesign.BrandColor),
                                    singleLine = true,
                                    readOnly = loadError,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { titleFocused = it.isFocused },
                                    decorationBox = { innerTextField ->
                                        if (titleField.text.isEmpty()) {
                                            Text(
                                                text = "Untitled",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.textSecondary
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                val shared = note?.sharedUsers.orEmpty()
                                if (shared.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                                        shared.take(3).forEach { share ->
                                            KeeftalkAvatar(
                                                avatarUrl = share.user.avatarUrl,
                                                initials = share.user.initials,
                                                seed = share.user.id,
                                                size = 28.dp,
                                                modifier = Modifier.border(2.dp, palette.bg, CircleShape)
                                            )
                                        }
                                        if (shared.size > 3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.surface)
                                                    .border(2.dp, palette.bg, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+${shared.size - 3}", fontSize = 10.sp, color = palette.textSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            var showInfoDialog by remember { mutableStateOf(false) }

                            if (showInfoDialog) {
                                AlertDialog(
                                    onDismissRequest = { showInfoDialog = false },
                                    title = { Text("Note Info", color = palette.text) },
                                    containerColor = palette.surface,
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Created: ${note?.let { formatDate(it.createdAt) } ?: "—"}", color = palette.text)
                                            Text("Last Updated: ${note?.let { formatDate(it.updatedAt) } ?: "—"}", color = palette.text)
                                            Text("Status: ${if (note?.archived == true) "Archived" else "Active"}", color = palette.text)
                                            Text("Container: ${note?.container ?: "All"}", color = palette.text)
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = { showInfoDialog = false }) { Text("Close") }
                                    }
                                )
                            }

                            Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                val savingLabel = when (savingState) {
                                    SavingState.Saving -> "Saving…"
                                    SavingState.Saved -> "Saved"
                                    else -> null
                                }
                                if (savingLabel != null) {
                                    Text(savingLabel, fontSize = 11.sp, color = palette.textMuted)
                                }
                                IconButton(onClick = { showInfoDialog = true }) {
                                    Icon(Icons.Outlined.Info, "Note Info", tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.undo() }, enabled = canUndo && !loadError) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (canUndo) palette.textSecondary else palette.textMuted, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.redo() }, enabled = canRedo && !loadError) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (canRedo) palette.textSecondary else palette.textMuted, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { isEditorDark = !isEditorDark }) {
                                    Icon(if (palette.isDark) Icons.Default.LightMode else Icons.Default.Nightlight, "Toggle theme", tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(palette.bg)
                        .imePadding()
                ) {
                    if (loadError) {
                        Surface(
                            color = NotesDesign.DangerColor.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "This note could not be read, so editing is disabled to protect your data.",
                                color = NotesDesign.DangerColor,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val state = editorState.value
                        itemsIndexed(state.blocks, key = { _, block -> block.id }) { _, block ->
                            val setActive = { viewModel.setActiveBlock(block.id) }
                            val remove = { viewModel.removeBlock(block.id) }
                            val handleFocus = viewModel::consumeFocusRequest

                            when (block) {
                                is NoteBlock.Text -> TextBlockEditor(
                                    block = block,
                                    revision = state.revision,
                                    readOnly = loadError,
                                    pendingStyles = pendingStyles,
                                    focusRequest = focusRequest,
                                    onFocusRequestHandled = handleFocus,
                                    onContentChange = { viewModel.onTextChanged(block.id, it) },
                                    onSelectionChange = { viewModel.onTextSelection(block.id, it) },
                                    onFocusGained = setActive,
                                    onPendingStylesConsumed = viewModel::onPendingStylesConsumed,
                                    onMentionQueryChange = viewModel::setMentionQuery
                                )

                                is NoteBlock.Image -> {
                                    // Cached in the ViewModel. The old code created a new flow + download per recomposition.
                                    val imageUrl by produceState<String?>(initialValue = null, block.mediaId) {
                                        value = viewModel.resolveMediaUrl(block.mediaId)
                                    }
                                    ImageBlockEditor(
                                        block = block,
                                        imageUrl = imageUrl,
                                        onCaptionChange = { viewModel.setCaption(block.id, it) },
                                        onRemove = remove,
                                        readOnly = loadError,
                                        modifier = Modifier.blockFocus(block.id, focusRequest, handleFocus, setActive)
                                    )
                                }

                                is NoteBlock.Video -> UnsupportedBlockNotice("Video attachment (not shown in the editor yet)")

                                is NoteBlock.Table -> TableBlockEditor(
                                    block = block,
                                    onTableChange = { viewModel.setTableRows(block.id, it) },
                                    onRemove = remove,
                                    readOnly = loadError,
                                    modifier = Modifier.blockFocus(block.id, focusRequest, handleFocus, setActive)
                                )

                                is NoteBlock.Checklist -> ChecklistBlockEditor(
                                    block = block,
                                    onChecklistChange = { viewModel.setChecklistItems(block.id, it) },
                                    onRemove = remove,
                                    readOnly = loadError,
                                    requestFocus = focusRequest?.blockId == block.id,
                                    onFocusRequestHandled = handleFocus,
                                    modifier = Modifier.blockFocus(block.id, null, handleFocus, setActive)
                                )

                                is NoteBlock.Code -> CodeBlockEditor(
                                    block = block,
                                    onCodeChange = { viewModel.setCode(block.id, it) },
                                    onRemove = remove,
                                    readOnly = loadError,
                                    modifier = Modifier.blockFocus(block.id, focusRequest, handleFocus, setActive)
                                )

                                is NoteBlock.Math -> MathBlockEditor(
                                    block = block,
                                    onMathChange = { viewModel.setLatex(block.id, it) },
                                    onRemove = remove,
                                    readOnly = loadError,
                                    modifier = Modifier.blockFocus(block.id, focusRequest, handleFocus, setActive)
                                )
                            }
                        }

                        // Tap on the empty area below the content: focus the last paragraph, caret at the end.
                        item(key = "tail") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        enabled = !loadError
                                    ) { viewModel.focusEnd() }
                            )
                        }
                    }
                }
            }

            // --- POPUP TOOLBAR TRIGGER ---
            val haptic = LocalHapticFeedback.current
            var fabOffset by remember { mutableStateOf(IntOffset.Zero) }

            if (!loadError) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .imePadding() // keep the FAB above the keyboard
                        .offset { fabOffset }
                        .padding(bottom = 100.dp, end = 24.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    fabOffset = IntOffset(
                                        (fabOffset.x + dragAmount.x).roundToInt(),
                                        (fabOffset.y + dragAmount.y).roundToInt()
                                    )
                                }
                            )
                        }
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.setToolbarPopupVisible(!isToolbarPopupVisible) },
                        containerColor = if (palette.isDark) Color(0xFF1E1E1E) else Color.White,
                        contentColor = if (palette.isDark) Color.White else Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = EditIcons.Edit2,
                            contentDescription = "Editing Tools",
                            tint = if (palette.isDark) Color.White else Color.Black
                        )
                    }

                    if (isToolbarPopupVisible) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            onDismissRequest = { viewModel.setToolbarPopupVisible(false) },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 24.dp, bottom = 16.dp)
                                    .width(320.dp)
                                    .shadow(16.dp, RoundedCornerShape(12.dp))
                            ) {
                                EditorToolbar(
                                    isExpanded = true,
                                    onToggle = { viewModel.setToolbarPopupVisible(false) },
                                    onAction = { action ->
                                        // Inserts close the popup so the new block can take focus; formatting keeps it open.
                                        when (action) {
                                            ToolbarAction.InsertImage -> {
                                                viewModel.setToolbarPopupVisible(false)
                                                imagePicker.launch("image/*")
                                            }
                                            ToolbarAction.InsertTable -> {
                                                viewModel.setToolbarPopupVisible(false)
                                                viewModel.addBlock(
                                                    NoteBlock.Table(rows = listOf(TableRow(listOf("", "")), TableRow(listOf("", ""))))
                                                )
                                            }
                                            ToolbarAction.InsertCode -> {
                                                viewModel.setToolbarPopupVisible(false)
                                                viewModel.addBlock(NoteBlock.Code())
                                            }
                                            ToolbarAction.InsertMath -> {
                                                viewModel.setToolbarPopupVisible(false)
                                                viewModel.addBlock(NoteBlock.Math())
                                            }
                                            ToolbarAction.Checklist -> {
                                                viewModel.setToolbarPopupVisible(false)
                                                viewModel.addBlock(NoteBlock.Checklist(items = listOf(ChecklistItem())))
                                            }
                                            else -> viewModel.applyFormatting(action)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Mentions: anchored above the keyboard, no scrim, so the user can keep typing.
            val mentionQuery by viewModel.mentionQuery.collectAsState()
            val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()

            if (!loadError && mentionQuery != null && mentionSuggestions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier
                            .width(280.dp)
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        LazyColumn {
                            items(mentionSuggestions, key = { it.id }) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.insertMention(user) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KeeftalkAvatar(
                                        avatarUrl = user.avatarUrl,
                                        initials = user.initials,
                                        seed = user.id,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(user.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.text)
                                        Text("@${user.username}", fontSize = 12.sp, color = palette.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Focus plumbing for non-text blocks: reports "this block has focus" (so inserts land after it) and,
 * when [request] targets this block, moves focus to its first focusable child.
 */
@Composable
private fun Modifier.blockFocus(
    blockId: String,
    request: FocusRequest?,
    onRequestHandled: () -> Unit,
    onFocused: () -> Unit
): Modifier {
    val requester = remember { FocusRequester() }
    val wantsFocus = request?.blockId == blockId
    LaunchedEffect(wantsFocus) {
        if (wantsFocus) {
            withFrameNanos { }
            try {
                requester.requestFocus()
            } catch (_: IllegalStateException) {
                // not attached yet
            }
            onRequestHandled()
        }
    }
    return this
        .focusRequester(requester)
        .focusGroup()
        .onFocusChanged { if (it.hasFocus) onFocused() }
}

@Composable
private fun UnsupportedBlockNotice(text: String) {
    val palette = LocalNotesPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text, fontSize = 12.sp, color = palette.textMuted)
    }
}

private fun editorPalette(dark: Boolean): NotesPalette =
    if (dark) {
        NotesPalette(
            bg = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            sidebar = Color(0xFF2A2A2A),
            border = Color(0xFF333333),
            text = Color.White,
            textSecondary = Color(0xFFB0B0B0),
            textMuted = Color(0xFF808080),
            isDark = true
        )
    } else {
        NotesPalette(
            bg = Color.White,
            surface = Color(0xFFF9FAFB),
            sidebar = Color(0xFFF3F4F6),
            border = Color(0xFFE5E7EB),
            text = Color(0xFF111827),
            textSecondary = Color(0xFF4B5563),
            textMuted = Color(0xFF9CA3AF),
            isDark = false
        )
    }

@Composable
fun MetaItem(icon: ImageVector, text: String) {
    val palette = LocalNotesPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = palette.textMuted, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = palette.textMuted)
    }
}

@Composable
fun EditorActionButton(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatDate(ts: Long): String {
    return SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}