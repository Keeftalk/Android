package com.keeftalk.chat.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.ui.screens.editor.graphics.BezierDrawingRenderer
import com.keeftalk.chat.ui.screens.editor.graphics.TextRenderer
import java.util.*

@Composable
fun ImageEditorView(
    state: EditorModel,
    onExecute: (EditorCommand) -> Unit,
    currentMode: EditorMode,
    modifier: Modifier = Modifier,
    overrideBitmap: android.graphics.Bitmap? = null
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableFloatStateOf(10f) }

    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White, Color.Black, Color.Cyan, Color.Magenta)

    var selectedElementId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { canvasSize = it.size }
    ) {
        if (overrideBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = overrideBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            AsyncImage(
                model = state.mediaItem?.uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canvasSize, currentMode) {
                    if (canvasSize == IntSize.Zero) return@pointerInput
                    
                    when (currentMode) {
                        EditorMode.DRAW -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    currentPath?.let {
                                        val drawingPath = DrawingPath(it, currentColor, strokeWidth, referenceSize = canvasSize)
                                        onExecute(EditorCommand.AddDrawingPath(drawingPath))
                                    }
                                    currentPath = null
                                }
                            )
                        }
                        EditorMode.TEXT, EditorMode.STICKER, EditorMode.NONE -> {
                            detectTapGestures { offset ->
                                val hit = state.textElements.find {
                                    val x = it.x * canvasSize.width
                                    val y = it.y * canvasSize.height
                                    (offset.x in (x - 150)..(x + 150)) && (offset.y in (y - 100)..(y + 100))
                                }
                                selectedElementId = hit?.id
                            }
                        }
                        else -> {}
                    }
                }
                .pointerInput(selectedElementId, canvasSize) {
                    if (selectedElementId == null || canvasSize == IntSize.Zero) return@pointerInput
                    
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val element = state.textElements.find { it.id == selectedElementId } ?: return@detectTransformGestures
                        val newX = element.x + (pan.x / canvasSize.width)
                        val newY = element.y + (pan.y / canvasSize.height)
                        val newScale = (element.scale * zoom).coerceIn(0.5f, 5f)
                        val newRotation = element.rotation + rotation
                        
                        onExecute(EditorCommand.UpdateTextTransform(element.id, newX, newY, newScale, newRotation))
                    }
                }
        ) {
            drawContext.canvas.nativeCanvas.apply {
                // Layer 1: Blur
                state.blurRegions.forEach { region ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(150, 0, 0, 0)
                        style = android.graphics.Paint.Style.FILL
                    }
                    save()
                    scale(canvasSize.width.toFloat(), canvasSize.height.toFloat())
                    drawPath(region.path.asAndroidPath(), paint)
                    restore()
                }

                // Layer 2: Drawing
                state.drawingPaths.forEach { dp ->
                    val renderer = BezierDrawingRenderer(dp.color, dp.strokeWidth, dp.path, dp.isEraser)
                    renderer.draw(drawContext.canvas)
                }
                
                currentPath?.let {
                    val renderer = BezierDrawingRenderer(currentColor, strokeWidth, it, isEraser = false)
                    renderer.draw(drawContext.canvas)
                }

                // Layer 3: Text
                state.textElements.forEach { text ->
                    save()
                    translate(text.x * canvasSize.width, text.y * canvasSize.height)
                    rotate(text.rotation)
                    scale(text.scale, text.scale)
                    
                    val renderer = TextRenderer(text.text, text.color, text.fontSize)
                    renderer.draw(drawContext.canvas)
                    
                    if (text.id == selectedElementId) {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 2f
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                        }
                        drawRect(-150f, -50f, 150f, 50f, paint)
                    }
                    restore()
                }

                // Layer 4: Stickers
                state.stickers.forEach { sticker ->
                    save()
                    translate(sticker.x * canvasSize.width, sticker.y * canvasSize.height)
                    rotate(sticker.rotation)
                    scale(sticker.scale, sticker.scale)
                    
                    // Simple Emoji Sticker for now
                    val paint = android.graphics.Paint().apply {
                        textSize = 100f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText("😊", 0f, 0f, paint)
                    restore()
                }
            }
        }

        // Tool Controls (Color & Size)
        if (currentMode == EditorMode.DRAW || currentMode == EditorMode.TEXT) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp) // Leave space for the actual bottom bar toolbar
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(if (currentColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { currentColor = color }
                        )
                    }
                }
                
                if (currentMode == EditorMode.DRAW) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 2f..50f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Overlay UI for selected element
        selectedElementId?.let { id ->
            val element = state.textElements.find { it.id == id } ?: state.stickers.find { it.id == id }
            if (element != null && canvasSize != IntSize.Zero) {
                val x = when (element) {
                    is TextElement -> element.x
                    is StickerElement -> element.x
                    else -> 0.5f
                }
                val y = when (element) {
                    is TextElement -> element.y
                    is StickerElement -> element.y
                    else -> 0.5f
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = (x * canvasSize.width / LocalDensity.current.density).dp - 20.dp,
                            y = (y * canvasSize.height / LocalDensity.current.density).dp - 80.dp
                        )
                ) {
                    IconButton(
                        onClick = { 
                            onExecute(EditorCommand.DeleteElement(id))
                            selectedElementId = null 
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        }
    }
}
