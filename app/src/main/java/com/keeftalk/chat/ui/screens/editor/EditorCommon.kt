package com.keeftalk.chat.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.screens.editor.signal.SignalImageEditorView

enum class EditorMode {
    NONE, DRAW, TEXT, STICKER, BLUR, CROP
}

fun mapMode(mode: EditorMode): SignalImageEditorView.Mode {
    return when (mode) {
        EditorMode.DRAW -> SignalImageEditorView.Mode.Draw
        EditorMode.BLUR -> SignalImageEditorView.Mode.Blur
        else -> SignalImageEditorView.Mode.MoveAndResize
    }
}

@Composable
fun EditorToolbar(
    currentMode: EditorMode,
    onModeChange: (EditorMode) -> Unit,
    onAutoBlurFaces: () -> Unit,
    onAddSticker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(24.dp), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onModeChange(if (currentMode == EditorMode.DRAW) EditorMode.NONE else EditorMode.DRAW) }) {
                Icon(Icons.Default.Edit, contentDescription = "Draw", tint = if (currentMode == EditorMode.DRAW) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = { onModeChange(EditorMode.TEXT) }) {
                Icon(Icons.Default.TextFields, contentDescription = "Text", tint = if (currentMode == EditorMode.TEXT) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = { onModeChange(if (currentMode == EditorMode.BLUR) EditorMode.NONE else EditorMode.BLUR) }) {
                Icon(Icons.Default.BlurOn, contentDescription = "Blur", tint = if (currentMode == EditorMode.BLUR) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = onAutoBlurFaces) {
                Icon(Icons.Default.Face, contentDescription = "Auto Blur Faces", tint = Color.White)
            }
            IconButton(onClick = { onModeChange(if (currentMode == EditorMode.CROP) EditorMode.NONE else EditorMode.CROP) }) {
                Icon(Icons.Default.Crop, contentDescription = "Crop", tint = if (currentMode == EditorMode.CROP) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = onAddSticker) {
                Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = "Stickers", tint = if (currentMode == EditorMode.STICKER) MaterialTheme.colorScheme.primary else Color.White)
            }
        }
    }
}

@Composable
fun BrushTools(
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    strokeWidth: Float? = null,
    onStrokeWidthChange: ((Float) -> Unit)? = null,
    showStyleToggle: Boolean = false,
    onStyleToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.White, Color.Black, Color.Cyan, Color.Magenta)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(if (selectedColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onColorSelect(color) }
                )
            }
            
            if (showStyleToggle) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onStyleToggle?.invoke() }) {
                    Icon(Icons.Default.Title, "Style", tint = Color.White)
                }
            }
        }
        
        if (strokeWidth != null && onStrokeWidthChange != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LineWeight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 0.005f..0.1f, // Relative to FULL_BOUNDS
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
