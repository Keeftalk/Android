package com.keeftalk.chat.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntSize
import com.keeftalk.chat.ui.screens.editor.graphics.EditorElement

data class EditorModel(
    val mediaItem: MediaItem? = null,
    val documentItem: DocumentModel? = null,
    val rootElement: EditorElement? = null,
    val drawingPaths: List<DrawingPath> = emptyList(),
    val textElements: List<TextElement> = emptyList(),
    val stickers: List<StickerElement> = emptyList(),
    val blurRegions: List<BlurRegion> = emptyList(),
    val cropRect: android.graphics.RectF? = null,
    val rotation: Float = 0f,
    val videoTrimRange: LongRange? = null,
    val editedText: String? = null,
    val caption: String = ""
)

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val referenceSize: IntSize? = null, // The size of the canvas when this path was drawn
    val isEraser: Boolean = false
)

data class TextElement(
    val id: String,
    val text: String,
    val color: Color,
    val fontSize: Float,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

data class StickerElement(
    val id: String,
    val stickerUri: String,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

data class BlurRegion(
    val path: Path,
    val isFace: Boolean = false
)
