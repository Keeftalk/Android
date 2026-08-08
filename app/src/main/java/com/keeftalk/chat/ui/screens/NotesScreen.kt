package com.keeftalk.chat.ui.screens

import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.Note
import com.keeftalk.chat.domain.model.NoteShare
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.components.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.keeftalk.chat.ui.components.FixedRichTextEditor
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.KeeftalkLogo
import com.keeftalk.chat.util.LocalFileRepository
import com.keeftalk.chat.util.NotesImageLoader
import com.keeftalk.chat.util.MediaUtils
import com.keeftalk.chat.util.NotesLogger
import com.mohamedrejeb.richeditor.model.*
import com.mohamedrejeb.richeditor.model.LocalImageLoader
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val BrandColor = Color(0xFF6C63FF)

// HTML Preview Theme Palettes
private val LightBg = Color(0xFFF0F2F5)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSidebar = Color(0xFFF8FAFC)
private val LightText = Color(0xFF1E293B)
private val LightTextGray = Color(0xFF64748B)

private val DarkBg = Color(0xFF000000)
private val DarkSurface = Color(0xFF121212)
private val DarkSidebar = Color(0xFF121212)
private val DarkText = Color(0xFFE2E8F0)
private val DarkTextGray = Color(0xFF94A3B8)

private val BorderDark = Color(0xFF334155)
private val BorderLight = Color(0xFFEEF2F6)

@Composable
fun notesColors(): NotesColorPalette {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        NotesColorPalette(
            bg = DarkBg, surface = DarkSurface, sidebar = DarkSidebar, 
            text = DarkText, textGray = DarkTextGray, border = BorderDark
        )
    } else {
        NotesColorPalette(
            bg = LightBg, surface = LightSurface, sidebar = LightSidebar, 
            text = LightText, textGray = LightTextGray, border = BorderLight
        )
    }
}

data class NotesColorPalette(
    val bg: Color,
    val surface: Color,
    val sidebar: Color,
    val text: Color,
    val textGray: Color,
    val border: Color
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    initialNoteId: String? = null,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<com.keeftalk.chat.ui.components.FabActionType>? = null,
    onDetailVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: UnifiedNotesViewModel = viewModel {
        UnifiedNotesViewModel(
            AppModule.provideNoteRepository(context),
            AppModule.provideAuthRepository(context),
            AppModule.provideChatRepository(context),
            AppModule.provideFileRepository(context),
            com.keeftalk.chat.data.prefs.UserPreferencesRepository(context)
        )
    }

    LaunchedEffect(fabActionFlow) {
        NotesLogger.d("UI", "NotesScreen: Started collecting fabActionFlow")
        fabActionFlow?.collect { action ->
            NotesLogger.d("UI", "NotesScreen: Received FAB action: $action")
            if (action == com.keeftalk.chat.ui.components.FabActionType.NEW_NOTE) {
                viewModel.createNote()
            }
        }
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val selectedNoteId by viewModel.selectedNoteId.collectAsState()
    
    LaunchedEffect(initialNoteId) {
        if ((initialNoteId != null) && (selectedNoteId == null)) {
            viewModel.selectNote(initialNoteId)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncNotes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedNoteId) {
        onDetailVisibilityChange(selectedNoteId != null)
        if (selectedNoteId != null) {
            onNoteClick(selectedNoteId!!)
            viewModel.selectNote(null) // Reset selection so it can be re-triggered
        }
    }

    val palette = notesColors()

    Box(modifier = Modifier.fillMaxSize().background(palette.bg)) {
        SidebarPane(
            viewModel = viewModel,
            onBack = onBack,
            onSettingsClick = onSettingsClick
        )

        // New Note Creation Overlay
        val isCreatingNote by viewModel.isCreatingNote.collectAsState()
        if (isCreatingNote) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandColor, strokeWidth = 3.dp)
                        Text("Preparing your note...", color = palette.text, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    val showShareModal by viewModel.showShareModal.collectAsState()
    if (showShareModal) {
        ShareModal(viewModel = viewModel)
    }
}

@Composable
fun ShareModal(viewModel: UnifiedNotesViewModel) {
    val palette = notesColors()
    val note by viewModel.selectedNote.collectAsState()
    val shareSearchQuery by viewModel.shareSearchQuery.collectAsState()
    val shareSuggestions by viewModel.shareSuggestions.collectAsState()
    
    Dialog(onDismissRequest = { viewModel.setShowShareModal(false) }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = palette.surface
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text("Share Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.text)
                Text("Manage who has access to this note", style = MaterialTheme.typography.bodyMedium, color = palette.textGray)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = shareSearchQuery,
                    onValueChange = { viewModel.setShareSearchQuery(it) },
                    placeholder = { Text("Search people...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        focusedContainerColor = palette.bg,
                        unfocusedContainerColor = palette.bg
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(shareSuggestions) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeeftalkAvatar(
                                avatarUrl = contact.avatarUrl, 
                                initials = contact.initials, 
                                seed = contact.id,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.text)
                                Text("@${contact.username}", fontSize = 12.sp, color = palette.textGray)
                            }
                            IconButton(onClick = { viewModel.addShare(contact.id, "read") }) {
                                Icon(Icons.Default.Add, null, tint = BrandColor)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.setShowShareModal(false) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarPane(
    viewModel: UnifiedNotesViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val palette = notesColors()
    val notes by viewModel.notes.collectAsState()
    val activeContainer by viewModel.activeContainer.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customContainers by viewModel.customContainers.collectAsState()
    val selectedNoteId by viewModel.selectedNoteId.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val counts by viewModel.counts.collectAsState()
    val icons = LocalAppIcons.current
    
    Scaffold(
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search notes...", color = palette.textGray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = palette.textGray, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Close, null, tint = palette.textGray) } }
                    } else null,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedBorderColor = BrandColor,
                        unfocusedBorderColor = palette.border,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        focusedPlaceholderColor = palette.textGray,
                        unfocusedPlaceholderColor = palette.textGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${notes.size} notes", color = palette.textGray, fontSize = 12.sp)
                    if (searchQuery.isNotEmpty()) {
                        Text("Clear", color = BrandColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.setSearchQuery("") })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                val containers = listOf("All", "Shared", "Archived") + customContainers
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(containers) { container ->
                        val isActive = activeContainer == container
                        val count = counts[container] ?: 0
                        Surface(
                            onClick = { viewModel.setContainer(container) },
                            color = if (isActive) BrandColor else palette.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = if (isActive) null else BorderStroke(1.dp, palette.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = container,
                                    color = if (isActive) Color.White else palette.textGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isActive) Color.White.copy(alpha = 0.2f) else palette.bg.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(count.toString(), color = if (isActive) Color.White else palette.textGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        IconButton(onClick = { viewModel.createNote() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, null, tint = BrandColor)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isSyncing,
                onRefresh = { viewModel.syncNotes() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (notes.isEmpty()) {
                    EmptyState(activeContainer, searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = 80.dp // Space for FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NotePremiumItem(
                                note = note,
                                isActive = selectedNoteId == note.id,
                                onClick = { viewModel.selectNote(note.id) },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotePremiumItem(
    note: Note,
    isActive: Boolean,
    onClick: () -> Unit,
    viewModel: UnifiedNotesViewModel
) {
    val palette = notesColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")
    
    val noteColor = try { Color(note.color.toColorInt()) } catch (_: Exception) { BrandColor }
    val isLight = isLightColor(noteColor)
    val textColor = if (isLight) LightText else Color.White
    val secondaryTextColor = if (isLight) LightTextGray else Color.White.copy(alpha = 0.7f)
    val dotBorder = if (isLight) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.4f)

    var showContextMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true },
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(24.dp),
        color = noteColor,
        border = if (isActive) BorderStroke(2.dp, if(isLight) Color.Black else Color.White) else null,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.sharedUsers.isNotEmpty()) {
                    Icon(Icons.Default.Groups, null, tint = if (isLight) BrandColor else Color.White, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
                if (note.pinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.PushPin, null, tint = if (isLight) Color(0xFFF59E0B) else Color.White, modifier = Modifier.size(16.dp))
                }
            }
            
            Text(
                text = stripHtml(note.content).ifEmpty { "Empty note" },
                color = secondaryTextColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(noteColor)
                            .border(1.5.dp, dotBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatTime(note.updatedAt), color = secondaryTextColor, fontSize = 11.sp)
                }
                
                if (note.sharedUsers.isNotEmpty()) {
                    AvatarStack(users = note.sharedUsers.map { it.user }, size = 24.dp)
                }
            }
        }

        if (showContextMenu) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.background(palette.surface).border(1.dp, palette.border, RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = { Text(if (note.pinned) "Unpin" else "Pin", color = palette.text) },
                    onClick = { viewModel.togglePin(); showContextMenu = false },
                    leadingIcon = { Icon(Icons.Default.PushPin, null, tint = palette.textGray) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.archived) "Unarchive" else "Archive", color = palette.text) },
                    onClick = { viewModel.toggleArchive(); showContextMenu = false },
                    leadingIcon = { Icon(Icons.Default.Archive, null, tint = palette.textGray) }
                )
                DropdownMenuItem(
                    text = { Text("Delete Permanently", color = Color.Red) },
                    onClick = { viewModel.deletePermanently(); showContextMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                )
                HorizontalDivider(color = palette.border)
                // Color Picker in Menu
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("#6C63FF", "#3b82f6", "#22c55e", "#f59e0b", "#ef4444").forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(hex.toColorInt()))
                                .border(1.dp, if (note.color == hex) Color.White else Color.Transparent, CircleShape)
                                .clickable { viewModel.changeColor(hex); showContextMenu = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class)
@Composable
fun EditorPane(
    viewModel: UnifiedNotesViewModel,
    onBack: () -> Unit
) {
    val palette = notesColors()
    val note by viewModel.selectedNote.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()
    val richTextState = viewModel.richTextState
    val savingState by viewModel.savingState.collectAsState()
    val icons = LocalAppIcons.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorFocusRequester = remember { FocusRequester() }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }
    var linkText by remember { mutableStateOf("") }

    var showTableDialog by remember { mutableStateOf(false) }
    var tableRows by remember { mutableStateOf("3") }
    var tableCols by remember { mutableStateOf("3") }

    var showVideoDialog by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf("") }

    var showMathDialog by remember { mutableStateOf(false) }
    var mathLatex by remember { mutableStateOf("") }

    val insertMediaAction = { uri: String, typeHint: String ->
        scope.launch {
            val dimensions = MediaUtils.getDimensions(context, uri)
            val id = UUID.randomUUID().toString()
            val type = context.contentResolver.getType(uri.toUri()) ?: typeHint
            val abstractUri = "keeftalk-media://${if (type.contains("video")) "video" else "image"}/$id"
            
            val file = com.keeftalk.chat.domain.model.File(
                id = id,
                ownerId = note!!.ownerId,
                storagePath = uri,
                fileHash = null,
                fileName = null,
                mimeType = type,
                fileSize = null,
                fileType = if (type.contains("video")) com.keeftalk.chat.domain.model.FileType.VIDEO else com.keeftalk.chat.domain.model.FileType.IMAGE,
                sourceType = com.keeftalk.chat.domain.model.SourceType.NOTE,
                width = dimensions?.width,
                height = dimensions?.height,
                duration = dimensions?.duration?.toInt(),
                thumbnailPath = null,
                encryptionMetadata = null,
                referenceCount = 1,
                status = com.keeftalk.chat.domain.model.FileStatus.ACTIVE,
                securityMetadata = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null
            )
            
            if (viewModel.saveFile(file)) {
                viewModel.addAttachment(file)
                val srcWithMetadata = "$abstractUri?w=${dimensions?.width ?: 0}&h=${dimensions?.height ?: 0}&id=$id"
                // Default to 80% width
                val html = """<img src="$srcWithMetadata" data-id="$id" style="max-width:80%; width:80%; border-radius:8px; height:auto; display:block; margin: 12px auto;" />"""
                val pos = if (richTextState.selection.end >= 0) richTextState.selection.end else richTextState.annotatedString.length
                richTextState.insertHtml("<br/>$html<br/>", pos)
                viewModel.onContentChange()
                
                // Background upload
                scope.launch {
                    val uploadedFileId = viewModel.uploadMedia(uri)
                    if (uploadedFileId != null) {
                        // Update the media ID in HTML to the permanent one
                        val newUri = "keeftalk-media://${if (type.contains("video")) "video" else "image"}/$uploadedFileId"
                        viewModel.updateMediaInHtml(id, newUri, "UPLOADED")
                    }
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { insertMediaAction(it.toString(), "image") }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Insert Link", color = palette.text) },
            containerColor = palette.surface,
            text = {
                Column {
                    OutlinedTextField(value = linkText, onValueChange = { linkText = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    richTextState.addLink(text = if (linkText.isBlank()) linkUrl else linkText, url = linkUrl)
                    viewModel.onContentChange()
                    showLinkDialog = false
                }) { Text("Insert") }
            }
        )
    }

    if (showTableDialog) {
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text("Insert Table", color = palette.text) },
            containerColor = palette.surface,
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tableRows, onValueChange = { tableRows = it }, label = { Text("Rows") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = tableCols, onValueChange = { tableCols = it }, label = { Text("Cols") }, modifier = Modifier.weight(1f))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val rows = tableRows.toIntOrNull() ?: 3
                    val cols = tableCols.toIntOrNull() ?: 3
                    val id = UUID.randomUUID().toString()
                    // Use a placeholder image that the library handles well for inline content
                    // The alt text carries the signal that this is a table
                    val html = """<img src="keeftalk://table/$id" alt="table:{}" data-id="$id" width="300" height="200" />"""
                    
                    val pos = if (richTextState.selection.end >= 0) richTextState.selection.end else richTextState.annotatedString.length
                    richTextState.insertHtml(html, pos)
                    viewModel.onContentChange()
                    showTableDialog = false
                }) { Text("Insert") }
            }
        )
    }

    if (showVideoDialog) {
        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            title = { Text("Insert Video", color = palette.text) },
            containerColor = palette.surface,
            text = {
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Video URL (YouTube/Direct)") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (videoUrl.isNotBlank()) {
                        val id = UUID.randomUUID().toString()
                        val html = """<div class="editor-video-wrap" data-id="$id"><video src="$videoUrl" controls style="max-width:100%; border-radius:8px;"></video></div>"""
                        val pos = if (richTextState.selection.end >= 0) richTextState.selection.end else richTextState.annotatedString.length
                        richTextState.insertHtml(html, pos)
                        viewModel.onContentChange()
                    }
                    showVideoDialog = false
                }) { Text("Insert") }
            }
        )
    }

    if (showMathDialog) {
        AlertDialog(
            onDismissRequest = { showMathDialog = false },
            title = { Text("Insert Math", color = palette.text) },
            containerColor = palette.surface,
            text = {
                OutlinedTextField(value = mathLatex, onValueChange = { mathLatex = it }, label = { Text("LaTeX Expression") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    if (mathLatex.isNotBlank()) {
                        val id = UUID.randomUUID().toString()
                        val html = """<div class="math-block" data-id="$id" style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; padding:12px; font-family:monospace;">\[ $mathLatex \]</div>"""
                        val pos = if (richTextState.selection.end >= 0) richTextState.selection.end else richTextState.annotatedString.length
                        richTextState.insertHtml(html, pos)
                        viewModel.onContentChange()
                    }
                    showMathDialog = false
                }) { Text("Insert") }
            }
        )
    }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize().background(palette.bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.StickyNote2, null, modifier = Modifier.size(64.dp), tint = palette.surface)
                Text("Select a note to begin", color = palette.textGray)
            }
        }
        return
    }

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            Surface(color = Color.Transparent) {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = note?.title ?: "",
                                    onValueChange = { if (canEdit) viewModel.updateTitle(it) },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = palette.text
                                    ),
                                    cursorBrush = SolidColor(BrandColor),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (note?.title?.isEmpty() == true) Text("Title", color = palette.textGray)
                                        innerTextField()
                                    }
                                )
                                if (note?.sharedUsers?.isNotEmpty() == true) {
                                    AvatarStack(users = note!!.sharedUsers.map { it.user }, size = 28.dp)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(icons.back, null, tint = palette.text) }
                        },
                        actions = {
                            if (savingState == SavingState.Saving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BrandColor)
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            IconButton(onClick = { viewModel.setShowShareModal(true) }) {
                                Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF3B82F6))
                            }
                            IconButton(onClick = { /* More */ }) {
                                Icon(icons.more, null, tint = palette.textGray)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    if (canEdit) {
                        val isToolbarExpanded by viewModel.isToolbarExpanded.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(palette.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setToolbarExpanded(!isToolbarExpanded) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, null, tint = BrandColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Formatting Tools", color = palette.textGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Icon(
                                    if (isToolbarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    tint = palette.textGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            AnimatedVisibility(visible = isToolbarExpanded) {
                                EditorToolbarRedesign(
                                    state = richTextState,
                                    viewModel = viewModel,
                                    canEdit = canEdit,
                                    onAddImage = { imagePicker.launch("image/*") },
                                    onAddLink = { showLinkDialog = true },
                                    onAddTable = { showTableDialog = true },
                                    onAddVideo = { showVideoDialog = true },
                                    onAddMath = { showMathDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (note != null) {
                Surface(
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, tint = palette.textGray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Updated ${formatTime(note!!.updatedAt)}",
                                    color = palette.textGray,
                                    fontSize = 10.sp
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.onContentChange() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.togglePin() },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (note!!.pinned) Color(0xFFF59E0B) else palette.border),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (note!!.pinned) Color(0xFFF59E0B) else palette.textGray
                                    )
                                ) {
                                    Icon(Icons.Default.PushPin, null, modifier = Modifier.size(12.dp))
                                    if (note!!.pinned) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pinned", fontSize = 10.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.toggleArchive() },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, palette.border),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textGray)
                                ) {
                                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalImageLoader provides NotesImageLoader(),
                    com.keeftalk.chat.util.LocalFileRepository provides AppModule.provideFileRepository(context)
                ) {
                    FixedRichTextEditor(
                        state = richTextState,
                        readOnly = !canEdit,
                        modifier = Modifier.fillMaxSize().padding(16.dp).focusRequester(editorFocusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text, lineHeight = 28.sp),
                        placeholder = { Text("Start typing your thoughts...", color = palette.textGray) }
                    )
                }

                // Mentions Popup
                val showMentionPopup by viewModel.showMentionPopup.collectAsState()
                val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()
                if (showMentionPopup && mentionSuggestions.isNotEmpty() && canEdit) {
                    Popup(alignment = Alignment.TopStart, offset = IntOffset(16, 0)) {
                        Surface(
                            modifier = Modifier.width(240.dp).shadow(16.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surface,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                items(mentionSuggestions) { contact ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val current = richTextState.toHtml()
                                            val mention = "<span style=\"color:#2563EB;background:#DBEAFE;padding:2px 4px;border-radius:4px;font-weight:bold;\">@${contact.name}</span>&nbsp;"
                                            val query = viewModel.mentionQuery.value
                                            richTextState.setHtml(if (query.isNotEmpty()) current.replaceFirst("@$query", mention) else current + mention)
                                            viewModel.setShowMentionPopup(false)
                                        }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        KeeftalkAvatar(
                                            avatarUrl = contact.avatarUrl, 
                                            initials = com.keeftalk.chat.util.AvatarUtils.getInitials(contact.name), 
                                            seed = contact.id,
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(contact.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.text)
                                            Text("@${contact.username}", fontSize = 12.sp, color = palette.textGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Meta Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = palette.textGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Created: ${formatFullTime(note!!.createdAt)}", color = palette.textGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Schedule, null, tint = palette.textGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Updated: ${formatFullTime(note!!.updatedAt)}", color = palette.textGray, fontSize = 11.sp)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = if (note!!.archived) "Archived" else "Active",
                    color = if (note!!.archived) palette.textGray else Color(0xFF22C55E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalRichTextApi::class, ExperimentalLayoutApi::class)
@Composable
fun EditorToolbarRedesign(
    state: RichTextState,
    viewModel: UnifiedNotesViewModel,
    canEdit: Boolean,
    onAddImage: () -> Unit,
    onAddLink: () -> Unit,
    onAddTable: () -> Unit,
    onAddVideo: () -> Unit,
    onAddMath: () -> Unit
) {
    if (!canEdit) return
    val palette = notesColors()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Undo/Redo
        ToolbarIconButton(Icons.AutoMirrored.Filled.Undo, false, enabled = canUndo) { viewModel.undo() }
        ToolbarIconButton(Icons.AutoMirrored.Filled.Redo, false, enabled = canRedo) { viewModel.redo() }
        
        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)
        
        // Font Styles
        ToolbarIconButton(Icons.Default.FormatBold, state.currentSpanStyle.fontWeight == FontWeight.Bold) {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
        ToolbarIconButton(Icons.Default.FormatItalic, state.currentSpanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic) {
            state.toggleSpanStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
        }
        ToolbarIconButton(Icons.Default.FormatUnderlined, state.currentSpanStyle.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true) {
            state.toggleSpanStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
        }
        ToolbarIconButton(Icons.Default.FormatStrikethrough, state.currentSpanStyle.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.LineThrough) == true) {
            state.toggleSpanStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
        }

        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)

        // Font Size
        HeadingButton("A-", false) { 
            val current = state.currentSpanStyle.fontSize.value.takeIf { it > 0 } ?: 16f
            state.toggleSpanStyle(SpanStyle(fontSize = (current - 2).sp))
        }
        HeadingButton("A+", false) { 
            val current = state.currentSpanStyle.fontSize.value.takeIf { it > 0 } ?: 16f
            state.toggleSpanStyle(SpanStyle(fontSize = (current + 2).sp))
        }

        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)

        // Lists
        ToolbarIconButton(Icons.AutoMirrored.Filled.FormatListBulleted, false) { state.toggleUnorderedList() }
        ToolbarIconButton(Icons.Default.FormatListNumbered, false) { state.toggleOrderedList() }
        ToolbarIconButton(Icons.Default.CheckBox, false) { 
            state.insertHtml("<ul class=\"checklist\"><li><input type=\"checkbox\" /> New task</li></ul>", state.selection.end)
        }

        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)

        // Alignment
        ToolbarIconButton(Icons.AutoMirrored.Filled.FormatAlignLeft, state.currentParagraphStyle.textAlign == TextAlign.Left) {
            state.toggleParagraphStyle(androidx.compose.ui.text.ParagraphStyle(textAlign = TextAlign.Left))
        }
        ToolbarIconButton(Icons.Default.FormatAlignCenter, state.currentParagraphStyle.textAlign == TextAlign.Center) {
            state.toggleParagraphStyle(androidx.compose.ui.text.ParagraphStyle(textAlign = TextAlign.Center))
        }
        ToolbarIconButton(Icons.AutoMirrored.Filled.FormatAlignRight, state.currentParagraphStyle.textAlign == TextAlign.Right) {
            state.toggleParagraphStyle(androidx.compose.ui.text.ParagraphStyle(textAlign = TextAlign.Right))
        }

        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)

        // Media & Inserts
        ToolbarIconButton(Icons.Default.Image, false) { onAddImage() }
        ToolbarIconButton(Icons.Default.VideoLibrary, false) { onAddVideo() }
        ToolbarIconButton(Icons.Default.Link, false) { onAddLink() }
        ToolbarIconButton(Icons.Default.TableChart, false) { onAddTable() }
        ToolbarIconButton(Icons.Default.Code, false) {
            state.toggleSpanStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = palette.bg))
        }
        ToolbarIconButton(Icons.Default.Superscript, false) { onAddMath() }

        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)

        // Blocks
        ToolbarIconButton(Icons.Default.FormatQuote, false) {
            state.toggleSpanStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, background = palette.bg))
        }
        ToolbarIconButton(Icons.Default.AlternateEmail, false) { 
             val pos = if (state.selection.end >= 0) state.selection.end else state.annotatedString.length
             state.insertHtml(" @", pos) 
        }
        
        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = palette.border)

        // Colors
        val fontColors = listOf(Color.White, Color.Red, Color.Cyan, Color.Green, Color.Yellow)
        fontColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, if(state.currentSpanStyle.color == color) BrandColor else Color.Transparent, CircleShape)
                    .clickable { state.toggleSpanStyle(SpanStyle(color = color)) }
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun HeadingButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    val palette = notesColors()
    val textColor = if (isActive) BrandColor else palette.text
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.Black, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun ToolbarIconButton(icon: ImageVector, isActive: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = notesColors()
    val tintColor = if (isActive) BrandColor else if (enabled) palette.textGray else palette.textGray.copy(alpha = 0.3f)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .background(if (isActive) BrandColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
    ) {
        Icon(icon, null, tint = tintColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EmptyState(container: String, isSearch: Boolean) {
    val palette = notesColors()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.AutoMirrored.Filled.StickyNote2, null, modifier = Modifier.size(80.dp), tint = palette.surface)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isSearch) "No matches found" else "No notes in $container",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text
        )
        Text("Click the + button to create a new one", color = palette.textGray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AvatarStack(users: List<User>, size: Dp) {
    val palette = notesColors()
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        users.take(3).forEach { user ->
            KeeftalkAvatar(
                avatarUrl = user.avatarUrl, 
                initials = user.initials, 
                seed = user.id,
                size = size, 
                modifier = Modifier.border(2.dp, palette.bg, CircleShape)
            )
        }
    }
}

fun formatTime(ts: Long) = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ts))
fun formatFullTime(ts: Long) = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(ts))
fun stripHtml(html: String) = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString().trim()

fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}
