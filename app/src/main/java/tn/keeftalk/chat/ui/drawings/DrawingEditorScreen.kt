package tn.keeftalk.chat.ui.drawings

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.drawings.engine.CanvasEngine
import tn.keeftalk.chat.ui.drawings.engine.UndoRedoManager
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingEditorScreen(
    drawingId: String,
    viewModel: DrawingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var drawingState by remember { mutableStateOf<Drawing?>(null) }
    var currentLayers by remember { mutableStateOf<List<DrawingLayer>>(emptyList()) }
    var activeLayerId by remember { mutableStateOf("") }
    var activeTool by remember { mutableStateOf(DrawingToolType.PENCIL) }

    var strokeColorHex by remember { mutableStateOf("#000000") }
    var strokeWidthPx by remember { mutableStateOf(8.0f) }
    var strokeOpacity by remember { mutableStateOf(1.0f) }
    var fillColorHex by remember { mutableStateOf("#00000000") }
    var isFilledShape by remember { mutableStateOf(false) }

    // Canvas Transform (Zoom & Pan)
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Active gesture drawing points
    var currentPoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    var shapeStart by remember { mutableStateOf<Offset?>(null) }
    var shapeEnd by remember { mutableStateOf<Offset?>(null) }

    // Sheets & Dialogs
    var showBrushSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayerSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var exportFormatToSave by remember { mutableStateOf<DrawingExportFormat?>(null) }

    val undoRedoManager = remember { UndoRedoManager() }

    LaunchedEffect(drawingId) {
        val loaded = viewModel.uiState.value.let { state ->
            if (state is DrawingsUiState.Success) state.drawings.find { it.id == drawingId } else null
        } ?: viewModel.createNewDrawing("New Drawing")

        drawingState = loaded
        currentLayers = loaded.layers
        activeLayerId = loaded.layers.firstOrNull()?.id ?: ""
        undoRedoManager.clear(loaded.layers)
    }

    val currentDrawing = drawingState ?: return

    // System file picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val format = exportFormatToSave ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.exportDrawing(currentDrawing, format, context, uri) { d ->
                CanvasEngine.renderDrawingToBitmap(d, context = context)
            }
            Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    // Image import launcher
    val imageImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val localPath = viewModel.importImageToLocalStorage(context, uri)
                if (localPath != null && activeLayerId.isNotBlank()) {
                    val newImg = DrawingElement.ImportedImage(
                        layerId = activeLayerId,
                        imagePath = localPath,
                        x = currentDrawing.canvasWidth / 4f,
                        y = currentDrawing.canvasHeight / 4f,
                        width = currentDrawing.canvasWidth / 2f,
                        height = currentDrawing.canvasHeight / 2f
                    )
                    val updatedLayers = currentLayers.map { layer ->
                        if (layer.id == activeLayerId) {
                            layer.copy(elements = layer.elements + newImg)
                        } else layer
                    }
                    currentLayers = updatedLayers
                    undoRedoManager.pushState(updatedLayers)
                }
            }
        }
    }

    fun saveCurrentDrawingState() {
        val updated = currentDrawing.copy(layers = currentLayers)
        drawingState = updated
        val thumb = CanvasEngine.renderDrawingToBitmap(updated, 300, (300f * updated.canvasHeight / updated.canvasWidth).toInt(), context)
        scope.launch {
            viewModel.saveDrawing(updated, thumb)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDrawing.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        saveCurrentDrawingState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            undoRedoManager.undo()?.let { currentLayers = it }
                        },
                        enabled = undoRedoManager.canUndo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = {
                            undoRedoManager.redo()?.let { currentLayers = it }
                        },
                        enabled = undoRedoManager.canRedo
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { showLayerSheet = true }) {
                        Icon(Icons.Default.Layers, contentDescription = "Layers")
                    }
                    IconButton(onClick = {
                        saveCurrentDrawingState()
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    var showExportMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export PNG (Transparent)") },
                            onClick = {
                                showExportMenu = false
                                exportFormatToSave = DrawingExportFormat.PNG
                                exportLauncher.launch("${currentDrawing.title}.png")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export JPEG") },
                            onClick = {
                                showExportMenu = false
                                exportFormatToSave = DrawingExportFormat.JPEG
                                exportLauncher.launch("${currentDrawing.title}.jpg")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export SVG Vector") },
                            onClick = {
                                showExportMenu = false
                                exportFormatToSave = DrawingExportFormat.SVG
                                exportLauncher.launch("${currentDrawing.title}.svg")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Keeftalk Native (.ktd)") },
                            onClick = {
                                showExportMenu = false
                                exportFormatToSave = DrawingExportFormat.KTD
                                exportLauncher.launch("${currentDrawing.title}.ktd")
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column {
                    // Quick Color & Brush size row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Preset Colors Palette
                        val colors = listOf("#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FFFFFF")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(colors) { hex ->
                                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Black }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (strokeColorHex == hex) 3.dp else 1.dp,
                                            color = if (strokeColorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { strokeColorHex = hex }
                                )
                            }
                        }

                        IconButton(onClick = { showColorPicker = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Color Picker", tint = Color(android.graphics.Color.parseColor(strokeColorHex)))
                        }

                        IconButton(onClick = { showBrushSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Brush Settings")
                        }
                    }

                    // Main Tools Bar
                    ScrollableTabRow(
                        selectedTabIndex = DrawingToolType.entries.indexOf(activeTool).coerceAtLeast(0),
                        edgePadding = 8.dp
                    ) {
                        listOf(
                            DrawingToolType.PENCIL to "Pencil",
                            DrawingToolType.PEN to "Pen",
                            DrawingToolType.BRUSH to "Brush",
                            DrawingToolType.MARKER to "Marker",
                            DrawingToolType.HIGHLIGHTER to "Highlighter",
                            DrawingToolType.ERASER to "Eraser",
                            DrawingToolType.LINE to "Line",
                            DrawingToolType.RECTANGLE to "Rectangle",
                            DrawingToolType.FILLED_RECTANGLE to "Fill Rect",
                            DrawingToolType.CIRCLE to "Circle",
                            DrawingToolType.POLYGON to "Polygon",
                            DrawingToolType.TEXT to "Text",
                            DrawingToolType.IMAGE_INSERT to "Image"
                        ).forEach { (tool, label) ->
                            Tab(
                                selected = (activeTool == tool),
                                onClick = {
                                    if (tool == DrawingToolType.IMAGE_INSERT) {
                                        imageImportLauncher.launch("image/*")
                                    } else if (tool == DrawingToolType.TEXT) {
                                        showTextDialog = true
                                    } else {
                                        activeTool = tool
                                    }
                                },
                                text = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF2B2B2B))
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.2f, 5.0f)
                        panOffset += pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Main Drawing Canvas
            Canvas(
                modifier = Modifier
                    .size(currentDrawing.canvasWidth.dp, currentDrawing.canvasHeight.dp)
                    .pointerInput(activeTool, activeLayerId) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val canvasX = (offset.x - panOffset.x) / zoomScale
                                val canvasY = (offset.y - panOffset.y) / zoomScale

                                if (isStrokeTool(activeTool)) {
                                    currentPoints = listOf(DrawingPoint(canvasX, canvasY))
                                } else if (isShapeTool(activeTool)) {
                                    shapeStart = Offset(canvasX, canvasY)
                                    shapeEnd = Offset(canvasX, canvasY)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val canvasX = (change.position.x - panOffset.x) / zoomScale
                                val canvasY = (change.position.y - panOffset.y) / zoomScale

                                if (isStrokeTool(activeTool)) {
                                    currentPoints = currentPoints + DrawingPoint(canvasX, canvasY)
                                } else if (isShapeTool(activeTool)) {
                                    shapeEnd = Offset(canvasX, canvasY)
                                }
                            },
                            onDragEnd = {
                                if (activeLayerId.isNotBlank()) {
                                    var newElement: DrawingElement? = null

                                    if (isStrokeTool(activeTool) && currentPoints.isNotEmpty()) {
                                        newElement = DrawingElement.Stroke(
                                            layerId = activeLayerId,
                                            toolType = activeTool,
                                            points = currentPoints,
                                            colorHex = strokeColorHex,
                                            strokeWidth = strokeWidthPx,
                                            opacity = strokeOpacity
                                        )
                                    } else if (isShapeTool(activeTool) && shapeStart != null && shapeEnd != null) {
                                        newElement = DrawingElement.Shape(
                                            layerId = activeLayerId,
                                            shapeType = activeTool,
                                            startX = shapeStart!!.x,
                                            startY = shapeStart!!.y,
                                            endX = shapeEnd!!.x,
                                            endY = shapeEnd!!.y,
                                            colorHex = strokeColorHex,
                                            strokeWidth = strokeWidthPx,
                                            isFilled = (activeTool == DrawingToolType.FILLED_RECTANGLE || activeTool == DrawingToolType.FILLED_CIRCLE || isFilledShape),
                                            fillColorHex = fillColorHex
                                        )
                                    }

                                    if (newElement != null) {
                                        val updatedLayers = currentLayers.map { layer ->
                                            if (layer.id == activeLayerId) {
                                                layer.copy(elements = layer.elements + newElement)
                                            } else layer
                                        }
                                        currentLayers = updatedLayers
                                        undoRedoManager.pushState(updatedLayers)
                                    }
                                }

                                currentPoints = emptyList()
                                shapeStart = null
                                shapeEnd = null
                            }
                        )
                    }
            ) {
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.translate(panOffset.x, panOffset.y)
                drawContext.canvas.nativeCanvas.scale(zoomScale, zoomScale)

                // Background
                val bgColor = try { android.graphics.Color.parseColor(currentDrawing.backgroundColorHex) } catch (_: Exception) { android.graphics.Color.WHITE }
                drawContext.canvas.nativeCanvas.drawColor(bgColor)

                // Render layers
                for (layer in currentLayers.filter { it.isVisible }) {
                    for (element in layer.elements) {
                        CanvasEngine.drawElementOnCanvas(
                            drawContext.canvas.nativeCanvas,
                            element,
                            layer.opacity,
                            currentDrawing.backgroundColorHex,
                            context
                        )
                    }
                }

                // Render active in-progress stroke/shape
                if (currentPoints.isNotEmpty()) {
                    val activeStroke = DrawingElement.Stroke(
                        layerId = activeLayerId,
                        toolType = activeTool,
                        points = currentPoints,
                        colorHex = strokeColorHex,
                        strokeWidth = strokeWidthPx,
                        opacity = strokeOpacity
                    )
                    CanvasEngine.drawElementOnCanvas(drawContext.canvas.nativeCanvas, activeStroke, 1.0f, currentDrawing.backgroundColorHex, context)
                }

                if (shapeStart != null && shapeEnd != null) {
                    val activeShape = DrawingElement.Shape(
                        layerId = activeLayerId,
                        shapeType = activeTool,
                        startX = shapeStart!!.x,
                        startY = shapeStart!!.y,
                        endX = shapeEnd!!.x,
                        endY = shapeEnd!!.y,
                        colorHex = strokeColorHex,
                        strokeWidth = strokeWidthPx,
                        isFilled = (activeTool == DrawingToolType.FILLED_RECTANGLE || activeTool == DrawingToolType.FILLED_CIRCLE || isFilledShape),
                        fillColorHex = fillColorHex
                    )
                    CanvasEngine.drawElementOnCanvas(drawContext.canvas.nativeCanvas, activeShape, 1.0f, currentDrawing.backgroundColorHex, context)
                }

                drawContext.canvas.nativeCanvas.restore()
            }
        }
    }

    // Brush settings modal sheet
    if (showBrushSheet) {
        ModalBottomSheet(onDismissRequest = { showBrushSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Brush Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Text("Stroke Size: ${strokeWidthPx.toInt()} px")
                Slider(
                    value = strokeWidthPx,
                    onValueChange = { strokeWidthPx = it },
                    valueRange = 1f..100f
                )

                Text("Opacity: ${(strokeOpacity * 100).toInt()}%")
                Slider(
                    value = strokeOpacity,
                    onValueChange = { strokeOpacity = it },
                    valueRange = 0.05f..1.0f
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFilledShape, onCheckedChange = { isFilledShape = it })
                    Text("Fill Shapes", fontSize = 14.sp)
                }

                Button(
                    onClick = { showBrushSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }

    // Layers management sheet
    if (showLayerSheet) {
        ModalBottomSheet(onDismissRequest = { showLayerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layers", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val newLayer = DrawingLayer(name = "Layer ${currentLayers.size + 1}")
                        currentLayers = currentLayers + newLayer
                        activeLayerId = newLayer.id
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Layer")
                    }
                }

                currentLayers.reversed().forEach { layer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (layer.id == activeLayerId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { activeLayerId = layer.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                currentLayers = currentLayers.map { l ->
                                    if (l.id == layer.id) l.copy(isVisible = !l.isVisible) else l
                                }
                            }) {
                                Icon(
                                    if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                            Text(layer.name, fontWeight = if (layer.id == activeLayerId) FontWeight.Bold else FontWeight.Normal)
                        }

                        if (currentLayers.size > 1) {
                            IconButton(onClick = {
                                currentLayers = currentLayers.filter { l -> l.id != layer.id }
                                if (activeLayerId == layer.id) {
                                    activeLayerId = currentLayers.firstOrNull()?.id ?: ""
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Layer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Insert Text Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Insert Text") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Text content") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (textInput.isNotBlank() && activeLayerId.isNotBlank()) {
                            val textEl = DrawingElement.Text(
                                layerId = activeLayerId,
                                x = currentDrawing.canvasWidth / 3f,
                                y = currentDrawing.canvasHeight / 2f,
                                text = textInput,
                                colorHex = strokeColorHex,
                                fontSizeSp = 32.0f
                            )
                            val updatedLayers = currentLayers.map { layer ->
                                if (layer.id == activeLayerId) {
                                    layer.copy(elements = layer.elements + textEl)
                                } else layer
                            }
                            currentLayers = updatedLayers
                            undoRedoManager.pushState(updatedLayers)
                        }
                        textInput = ""
                        showTextDialog = false
                    }
                ) {
                    Text("Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun isStrokeTool(tool: DrawingToolType): Boolean {
    return tool in listOf(
        DrawingToolType.PENCIL,
        DrawingToolType.PEN,
        DrawingToolType.BRUSH,
        DrawingToolType.MARKER,
        DrawingToolType.HIGHLIGHTER,
        DrawingToolType.ERASER
    )
}

private fun isShapeTool(tool: DrawingToolType): Boolean {
    return tool in listOf(
        DrawingToolType.LINE,
        DrawingToolType.RECTANGLE,
        DrawingToolType.FILLED_RECTANGLE,
        DrawingToolType.CIRCLE,
        DrawingToolType.ELLIPSE,
        DrawingToolType.FILLED_CIRCLE,
        DrawingToolType.POLYGON
    )
}
