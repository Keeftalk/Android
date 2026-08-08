package com.keeftalk.chat.ui.screens.editor.graphics

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke

class BezierDrawingRenderer(
    val color: Color,
    val strokeWidth: Float,
    val path: Path,
    val isEraser: Boolean = false
) : Renderer {
    override fun draw(canvas: Canvas) {
        val paint = Paint().apply {
            this.color = if (isEraser) Color.Transparent else this@BezierDrawingRenderer.color
            this.strokeWidth = this@BezierDrawingRenderer.strokeWidth
            this.style = PaintingStyle.Stroke
            this.strokeCap = StrokeCap.Round
            this.strokeJoin = StrokeJoin.Round
            if (isEraser) {
                this.blendMode = BlendMode.Clear
            }
        }
        canvas.drawPath(path, paint)
    }
}

class TextRenderer(
    val text: String,
    val color: Color,
    val fontSize: Float
) : Renderer {
    override fun draw(canvas: Canvas) {
        canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                this.color = this@TextRenderer.color.toArgb()
                this.textSize = this@TextRenderer.fontSize
                this.isAntiAlias = true
                this.textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText(text, 0f, 0f, paint)
        }
    }
}

class BlurRenderer(
    val path: Path
) : Renderer {
    override fun draw(canvas: Canvas) {
        // Preview: Semi-transparent frosted look
        val paint = Paint().apply {
            this.color = Color.Black.copy(alpha = 0.4f)
            this.style = PaintingStyle.Fill
        }
        canvas.drawPath(path, paint)
    }
}

class StickerRenderer(
    val bitmap: android.graphics.Bitmap
) : Renderer {
    override fun draw(canvas: Canvas) {
        canvas.nativeCanvas.drawBitmap(bitmap, -bitmap.width/2f, -bitmap.height/2f, null)
    }
}
