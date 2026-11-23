package tn.keeftalk.chat.ui.drawings

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.model.CanvasSettings
import tn.keeftalk.chat.domain.model.Drawing
import tn.keeftalk.chat.ui.drawings.engine.CanvasEngine
import tn.keeftalk.chat.util.KeeftalkStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingsScreen(
    viewModel: DrawingsViewModel,
    onOpenDrawing: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { runCatching { KeeftalkStore.getInstance() }.getOrNull() }

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var drawingToRename by remember { mutableStateOf<Drawing?>(null) }
    var drawingToDelete by remember { mutableStateOf<Drawing?>(null) }
    var drawingToPreview by remember { mutableStateOf<Drawing?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun handleCreateDrawingClick() {
        val useDefault = store?.getBoolean("drawings_use_default_canvas", false) ?: false
        if (useDefault) {
            val width = store?.getInt("drawings_def_width", 1080) ?: 1080
            val height = store?.getInt("drawings_def_height", 1920) ?: 1920
            val bg = store?.getString("drawings_def_bg", "#FFFFFF") ?: "#FFFFFF"
            val transparent = store?.getBoolean("drawings_def_transparent", false) ?: false

            scope.launch {
                val created = viewModel.createNewDrawing(
                    title = "Untitled Drawing",
                    settings = CanvasSettings(width = width, height = height, backgroundColorHex = bg, isTransparent = transparent)
                )
                onOpenDrawing(created.id)
            }
        } else {
            showCreateDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Drawings") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest first") },
                            onClick = {
                                viewModel.setSortOption(DrawingsSortOption.MODIFIED_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest first") },
                            onClick = {
                                viewModel.setSortOption(DrawingsSortOption.MODIFIED_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title A-Z") },
                            onClick = {
                                viewModel.setSortOption(DrawingsSortOption.TITLE_ASC)
                                showSortMenu = false
                            }
                        )
                        HorizontalDivider()
                        val isDefaultSet = store?.getBoolean("drawings_use_default_canvas", false) ?: false
                        if (isDefaultSet) {
                            DropdownMenuItem(
                                text = { Text("Reset Default Canvas Prompt") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    store?.putBoolean("drawings_use_default_canvas", false)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { handleCreateDrawingClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Drawing")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search drawings...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            when (val state = uiState) {
                is DrawingsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DrawingsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is DrawingsUiState.Success -> {
                    if (state.drawings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Brush,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No drawings yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Tap + to create your first canvas",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.drawings, key = { it.id }) { drawing ->
                                DrawingCard(
                                    drawing = drawing,
                                    onClick = { onOpenDrawing(drawing.id) },
                                    onPreview = { drawingToPreview = drawing },
                                    onRename = { drawingToRename = drawing },
                                    onDuplicate = { viewModel.duplicateDrawing(drawing.id) },
                                    onDelete = { drawingToDelete = drawing }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Preview Dialog
    drawingToPreview?.let { drawing ->
        DrawingPreviewDialog(
            drawing = drawing,
            onEdit = {
                drawingToPreview = null
                onOpenDrawing(drawing.id)
            },
            onDismiss = { drawingToPreview = null }
        )
    }

    // Create New Drawing Dialog
    if (showCreateDialog) {
        CreateDrawingDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, settings, setAsDefault ->
                showCreateDialog = false
                if (setAsDefault) {
                    store?.putBoolean("drawings_use_default_canvas", true)
                    store?.putInt("drawings_def_width", settings.width)
                    store?.putInt("drawings_def_height", settings.height)
                    store?.putString("drawings_def_bg", settings.backgroundColorHex)
                    store?.putBoolean("drawings_def_transparent", settings.isTransparent)
                }
                scope.launch {
                    val created = viewModel.createNewDrawing(title, settings)
                    onOpenDrawing(created.id)
                }
            }
        )
    }

    // Rename Dialog
    drawingToRename?.let { drawing ->
        var newTitle by remember { mutableStateOf(drawing.title) }
        AlertDialog(
            onDismissRequest = { drawingToRename = null },
            title = { Text("Rename Drawing") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toSave = drawing.copy(title = newTitle.ifBlank { "Untitled" })
                        scope.launch { viewModel.saveDrawing(toSave) }
                        drawingToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { drawingToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    drawingToDelete?.let { drawing ->
        AlertDialog(
            onDismissRequest = { drawingToDelete = null },
            title = { Text("Delete Drawing?") },
            text = { Text("Are you sure you want to delete '${drawing.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDrawing(drawing.id)
                        drawingToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { drawingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DrawingCard(
    drawing: Drawing,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail container - click to Quick Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = File(drawing.thumbnailPath)
                if (thumbFile.exists()) {
                    val bitmap = remember(drawing.thumbnailPath, drawing.updatedAt) {
                        BitmapFactory.decodeFile(thumbFile.absolutePath)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = drawing.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Brush, contentDescription = null, tint = Color.Gray)
                    }
                } else {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = Color.Gray)
                }

                // Quick Preview badge icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Quick Preview",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Info bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drawing.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    Text(
                        text = "${drawing.canvasWidth}x${drawing.canvasHeight} • ${dateFormat.format(Date(drawing.updatedAt))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Quick Preview") },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            onClick = { showMenu = false; onPreview() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit / Open") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                            onClick = { showMenu = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = { showMenu = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawingPreviewDialog(
    drawing: Drawing,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val renderedBitmap = remember(drawing.id, drawing.updatedAt) {
        val thumbFile = File(drawing.thumbnailPath)
        if (thumbFile.exists()) {
            BitmapFactory.decodeFile(thumbFile.absolutePath)
        } else {
            CanvasEngine.renderDrawingToBitmap(drawing, targetWidth = 600, targetHeight = (600f * drawing.canvasHeight / drawing.canvasWidth).toInt(), context = context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(drawing.title, fontWeight = FontWeight.Bold)
                    Text("${drawing.canvasWidth} x ${drawing.canvasHeight} px", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2B2B2B))
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 5.0f)
                            panOffset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (renderedBitmap != null) {
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = drawing.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )
                    )
                } else {
                    Text("Unable to load preview", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit in Paint")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CreateDrawingDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, settings: CanvasSettings, setAsDefault: Boolean) -> Unit
) {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val devWidth = displayMetrics.widthPixels
    val devHeight = displayMetrics.heightPixels

    val devicePresetLabel = "Device Screen (${devWidth}x${devHeight})"

    var title by remember { mutableStateOf("Untitled Drawing") }
    var preset by remember { mutableStateOf(devicePresetLabel) }
    var width by remember { mutableStateOf(devWidth.toString()) }
    var height by remember { mutableStateOf(devHeight.toString()) }
    var bgColorHex by remember { mutableStateOf("#FFFFFF") }
    var isTransparent by remember { mutableStateOf(false) }
    var makeDefault by remember { mutableStateOf(false) }

    val presets = listOf(
        devicePresetLabel to (devWidth to devHeight),
        "Phone Portrait (1080x1920)" to (1080 to 1920),
        "Square (1080x1080)" to (1080 to 1080),
        "Landscape (1920x1080)" to (1920 to 1080),
        "HD (1280x720)" to (1280 to 720),
        "Super Canvas 10K (10000x10000)" to (10000 to 10000),
        "Custom" to (0 to 0)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Drawing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Canvas Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column {
                    presets.forEach { (name, size) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preset = name
                                    if (size.first > 0) {
                                        width = size.first.toString()
                                        height = size.second.toString()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (preset == name),
                                onClick = {
                                    preset = name
                                    if (size.first > 0) {
                                        width = size.first.toString()
                                        height = size.second.toString()
                                    }
                                }
                            )
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = if (name == devicePresetLabel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (preset == "Custom") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it.filter { c -> c.isDigit() } },
                            label = { Text("Width (max 10000)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it.filter { c -> c.isDigit() } },
                            label = { Text("Height (max 10000)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isTransparent,
                        onCheckedChange = { isTransparent = it }
                    )
                    Text("Transparent Background", fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = makeDefault,
                        onCheckedChange = { makeDefault = it }
                    )
                    Text("Make this my default canvas (enter directly next time)", fontSize = 13.sp, lineHeight = 16.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = (width.toIntOrNull() ?: devWidth).coerceIn(1, 10000)
                    val h = (height.toIntOrNull() ?: devHeight).coerceIn(1, 10000)
                    onCreate(title, CanvasSettings(width = w, height = h, backgroundColorHex = bgColorHex, isTransparent = isTransparent), makeDefault)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
