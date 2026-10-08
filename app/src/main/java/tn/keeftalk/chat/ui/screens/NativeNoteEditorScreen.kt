package tn.keeftalk.chat.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import tn.keeftalk.chat.util.NotesUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.components.KeeftalkAvatar
import tn.keeftalk.chat.ui.screens.editor.NotesDesign
import tn.keeftalk.chat.ui.screens.editor.LocalNotesPalette
import tn.keeftalk.chat.ui.screens.editor.EditIcons
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
    val viewModel: NoteEditorViewModel = viewModel {
        NoteEditorViewModel(
            AppModule.provideNoteRepository(context),
            AppModule.provideAuthRepository(context),
            AppModule.provideChatRepository(context)
        )
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    val basePalette = NotesDesign.colors()
    var isEditorDark by remember { mutableStateOf(basePalette.isDark) }
    
    val palette = remember(isEditorDark) {
        if (isEditorDark) {
            tn.keeftalk.chat.ui.screens.editor.NotesPalette(
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
            tn.keeftalk.chat.ui.screens.editor.NotesPalette(
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
    }
    val note by viewModel.note.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val savingState by viewModel.savingState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isToolbarPopupVisible by viewModel.isToolbarPopupVisible.collectAsState()
    
    var lastInteractedBlockIndex by remember { mutableIntStateOf(-1) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadImage(it.toString()) }
    }

    CompositionLocalProvider(LocalNotesPalette provides palette) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            containerColor = palette.bg,
            topBar = {
                val firstLine = remember(blocks) { NotesUtils.getFirstLineOfNote(blocks) }
                val customTitle = note?.title ?: ""

                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                                value = customTitle,
                                onValueChange = { viewModel.updateTitle(it) },
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.text
                                ),
                                cursorBrush = SolidColor(NotesDesign.BrandColor),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (customTitle.isEmpty()) {
                                        Text(
                                            text = firstLine,
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

                            if (note?.sharedUsers?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                                ) {
                                    note!!.sharedUsers.take(3).forEach { share ->
                                        KeeftalkAvatar(
                                            avatarUrl = share.user.avatarUrl,
                                            initials = share.user.initials,
                                            seed = share.user.id,
                                            size = 28.dp,
                                            modifier = Modifier.border(2.dp, palette.bg, CircleShape)
                                        )
                                    }
                                    if (note!!.sharedUsers.size > 3) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(palette.surface)
                                                .border(2.dp, palette.bg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+${note!!.sharedUsers.size - 3}", fontSize = 10.sp, color = palette.textSecondary)
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
                            IconButton(onClick = { showInfoDialog = true }) {
                                Icon(Icons.Outlined.Info, "Note Info", tint = palette.textSecondary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = if (canUndo) palette.textSecondary else palette.textMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null, tint = if (canRedo) palette.textSecondary else palette.textMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { isEditorDark = !isEditorDark }) {
                                Icon(if (palette.isDark) Icons.Default.LightMode else Icons.Default.Nightlight, null, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
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
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Content Blocks
                    itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                        when (block) {
                            is NoteBlock.Text -> {
                                TextBlockEditor(
                                    block = block,
                                    onContentChange = {
                                        lastInteractedBlockIndex = index
                                        viewModel.updateBlock(index, block.copy(richText = it)) 
                                    },
                                    onFocusChanged = { focused, selection ->
                                        if (focused) {
                                            lastInteractedBlockIndex = index
                                            viewModel.setActiveBlock(index, selection)
                                        }
                                    },
                                    onMentionQueryChange = { viewModel.setMentionQuery(it) }
                                )
                            }
                            is NoteBlock.Image -> {
                                val imageUrl by viewModel.getMediaUrl(block.mediaId).collectAsState()
                                ImageBlockEditor(
                                    block = block,
                                    imageUrl = imageUrl,
                                    onCaptionChange = { viewModel.updateBlock(index, block.copy(caption = it)) },
                                    onRemove = { viewModel.removeBlock(index) }
                                )
                            }
                            is NoteBlock.Table -> {
                                TableBlockEditor(
                                    block = block,
                                    onTableChange = { viewModel.updateBlock(index, block.copy(rows = it)) },
                                    onRemove = { viewModel.removeBlock(index) }
                                )
                            }
                            is NoteBlock.Checklist -> {
                                ChecklistBlockEditor(
                                    block = block,
                                    onChecklistChange = { viewModel.updateBlock(index, block.copy(items = it)) },
                                    onRemove = { viewModel.removeBlock(index) }
                                )
                            }
                            is NoteBlock.Code -> {
                                CodeBlockEditor(
                                    block = block,
                                    onCodeChange = { viewModel.updateBlock(index, block.copy(code = it)) },
                                    onRemove = { viewModel.removeBlock(index) }
                                )
                            }
                            is NoteBlock.Math -> {
                                MathBlockEditor(
                                    block = block,
                                    onMathChange = { viewModel.updateBlock(index, block.copy(latex = it)) },
                                    onRemove = { viewModel.removeBlock(index) }
                                )
                            }
                            else -> {}
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // --- POPUP TOOLBAR TRIGGER ---
        val haptic = LocalHapticFeedback.current
        var fabOffset by remember { mutableStateOf(IntOffset.Zero) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
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
                                when (action) {
                                    ToolbarAction.InsertImage -> imagePicker.launch("image/*")
                                    ToolbarAction.InsertTable -> viewModel.addBlock(NoteBlock.Table(rows = listOf(TableRow(listOf("", "")), TableRow(listOf("", "")))))
                                    ToolbarAction.InsertCode -> viewModel.addBlock(NoteBlock.Code())
                                    ToolbarAction.InsertMath -> viewModel.addBlock(NoteBlock.Math())
                                    ToolbarAction.Checklist -> viewModel.addBlock(NoteBlock.Checklist(items = listOf(ChecklistItem())))
                                    else -> viewModel.applyFormatting(action)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Mentions Popup
        val showMentionPopup by viewModel.showMentionPopup.collectAsState()
        val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()
        
        if (showMentionPopup && mentionSuggestions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = { viewModel.setShowMentionPopup(false) })
                    .padding(horizontal = 24.dp, vertical = 80.dp),
                contentAlignment = Alignment.Center
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
                        items(mentionSuggestions) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (lastInteractedBlockIndex != -1) {
                                            viewModel.insertMention(user, lastInteractedBlockIndex)
                                        }
                                        viewModel.setShowMentionPopup(false)
                                    }
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
@Composable
fun MetaItem(icon: ImageVector, text: String) {
    val basePalette = NotesDesign.colors()
    var isEditorDark by remember { mutableStateOf(basePalette.isDark) }
    
    val palette = remember(isEditorDark) {
        if (isEditorDark) {
            tn.keeftalk.chat.ui.screens.editor.NotesPalette(
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
            tn.keeftalk.chat.ui.screens.editor.NotesPalette(
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
    }
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
