package com.keeftalk.chat.ui.screens

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.screens.editor.NotesDesign
import com.keeftalk.chat.ui.screens.editor.EditIcons
import com.keeftalk.chat.ui.screens.editor.components.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeNoteEditorScreen(
    noteId: String,
    onBack: () -> Unit,
    onThemeToggle: () -> Unit = {}
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

    val palette = NotesDesign.colors()
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = palette.bg,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.StickyNote2, null, tint = NotesDesign.BrandColor, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Keeftalk Notes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.text
                            )
                            if (note?.title?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    note!!.title,
                                    fontSize = 16.sp,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Notes", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    },
                    actions = {
                        Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = if (canUndo) palette.textSecondary else palette.textMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null, tint = if (canRedo) palette.textSecondary else palette.textMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onThemeToggle) {
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
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title Field
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = note?.title ?: "",
                                onValueChange = { viewModel.updateTitle(it) },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = palette.text
                                ),
                                cursorBrush = SolidColor(NotesDesign.BrandColor),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (note?.title?.isEmpty() == true) {
                                        Text("Note title...", style = MaterialTheme.typography.headlineMedium.copy(color = palette.textMuted))
                                    }
                                    innerTextField()
                                }
                            )
                            
                            // Avatars
                            if (note?.sharedUsers?.isNotEmpty() == true) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp),
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }

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

                // Bottom Info & Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Meta Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MetaItem(Icons.Outlined.CalendarToday, "Created: ${note?.let { formatDate(it.createdAt) } ?: "—"}")
                        MetaItem(Icons.Outlined.Schedule, "Updated: ${note?.let { formatDate(it.updatedAt) } ?: "—"}")
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (note?.archived == true) "Archived" else "Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (note?.archived == true) palette.textSecondary else NotesDesign.SuccessColor
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = palette.textMuted, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(note?.container ?: "All", fontSize = 12.sp, color = palette.text)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = palette.border)

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorActionButton(
                            icon = Icons.Default.Save,
                            text = "Save",
                            containerColor = NotesDesign.BrandColor,
                            contentColor = Color.White,
                            onClick = { viewModel.updateTitle(note?.title ?: "") }
                        )
                        
                        EditorActionButton(
                            icon = Icons.Default.PushPin,
                            text = if (note?.pinned == true) "Unpin" else "Pin",
                            containerColor = if (note?.pinned == true) palette.bg else Color(0xFFFEF3C7),
                            contentColor = if (note?.pinned == true) palette.textSecondary else Color(0xFFD97706),
                            onClick = { viewModel.togglePin() }
                        )

                        EditorActionButton(
                            icon = Icons.Default.Archive,
                            text = if (note?.archived == true) "Unarchive" else "Archive",
                            containerColor = if (note?.archived == true) Color(0xFFDBEAFE) else Color(0xFFFEF3C7),
                            contentColor = if (note?.archived == true) Color(0xFF2563EB) else Color(0xFFD97706),
                            onClick = { viewModel.toggleArchive() }
                        )

                        AnimatedVisibility(visible = savingState != SavingState.Idle) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                if (savingState == SavingState.Saving) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NotesDesign.BrandColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saving...", fontSize = 12.sp, color = NotesDesign.BrandColor)
                                } else {
                                    Icon(Icons.Default.CheckCircle, null, tint = NotesDesign.SuccessColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saved", fontSize = 12.sp, color = NotesDesign.SuccessColor)
                                }
                            }
                        }
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
@Composable
fun MetaItem(icon: ImageVector, text: String) {
    val palette = NotesDesign.colors()
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
