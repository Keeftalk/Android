package tn.keeftalk.chat.ui.drawings.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import tn.keeftalk.chat.domain.model.Drawing
import tn.keeftalk.chat.domain.model.DrawingElement
import tn.keeftalk.chat.domain.model.DrawingLayer
import tn.keeftalk.chat.domain.model.DrawingToolType
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class UndoRedoManager(private val maxHistorySize: Int = 50) {
    private val history = mutableListOf<List<DrawingLayer>>()
    private var currentIndex = -1

    val canUndo: Boolean get() = currentIndex > 0
    val canRedo: Boolean get() = currentIndex < history.lastIndex

    fun pushState(layers: List<DrawingLayer>) {
        if (currentIndex < history.lastIndex) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(layers.map { layer -> layer.copy(elements = layer.elements.toList()) })
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        } else {
            currentIndex++
        }
    }

    fun undo(): List<DrawingLayer>? {
        if (!canUndo) return null
        currentIndex--
        return history[currentIndex].map { layer -> layer.copy(elements = layer.elements.toList()) }
    }

    fun redo(): List<DrawingLayer>? {
        if (!canRedo) return null
        currentIndex++
        return history[currentIndex].map { layer -> layer.copy(elements = layer.elements.toList()) }
    }

    fun clear(initialLayers: List<DrawingLayer>) {
        history.clear()
        currentIndex = -1
        pushState(initialLayers)
    }
}

object CanvasEngine {

    fun renderDrawingToBitmap(
        drawing: Drawing,
        targetWidth: Int = drawing.canvasWidth,
        targetHeight: Int = drawing.canvasHeight,
        context: Context? = null
    ): Bitmap {
        val width = maxOf(1, targetWidth)
        val height = maxOf(1, targetHeight)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scaleX = width.toFloat() / drawing.canvasWidth.toFloat()
        val scaleY = height.toFloat() / drawing.canvasHeight.toFloat()
        canvas.scale(scaleX, scaleY)

        // Draw background
        val bgColor = try { Color.parseColor(drawing.backgroundColorHex) } catch (_: Exception) { Color.WHITE }
        canvas.drawColor(bgColor)

        // Draw visible layers
        for (layer in drawing.layers.filter { it.isVisible }) {
            for (element in layer.elements) {
                drawElementOnCanvas(canvas, element, layer.opacity, drawing.backgroundColorHex, context)
            }
        }

        return bitmap
    }

    fun drawElementOnCanvas(
        canvas: Canvas,
        element: DrawingElement,
        layerOpacity: Float = 1.0f,
        backgroundColorHex: String = "#FFFFFF",
        context: Context? = null
    ) {
        when (element) {
            is DrawingElement.Stroke -> drawStroke(canvas, element, layerOpacity, backgroundColorHex)
            is DrawingElement.Shape -> drawShape(canvas, element, layerOpacity)
            is DrawingElement.Text -> drawText(canvas, element, layerOpacity)
            is DrawingElement.ImportedImage -> drawImportedImage(canvas, element, layerOpacity, context)
        }
    }

    private fun drawStroke(
        canvas: Canvas,
        stroke: DrawingElement.Stroke,
        layerOpacity: Float,
        backgroundColorHex: String
    ) {
        if (stroke.points.isEmpty()) return

        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = stroke.strokeWidth
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val baseColor = try { Color.parseColor(stroke.colorHex) } catch (_: Exception) { Color.BLACK }
        val alpha = (Color.alpha(baseColor) * stroke.opacity * layerOpacity).toInt().coerceIn(0, 255)
        paint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

        when (stroke.toolType) {
            DrawingToolType.ERASER -> {
                val bgCol = try { Color.parseColor(backgroundColorHex) } catch (_: Exception) { Color.WHITE }
                paint.color = bgCol
            }
            DrawingToolType.HIGHLIGHTER -> {
                paint.alpha = (alpha * 0.4f).toInt().coerceIn(0, 255)
                paint.strokeCap = Paint.Cap.SQUARE
            }
            DrawingToolType.MARKER -> {
                paint.alpha = (alpha * 0.7f).toInt().coerceIn(0, 255)
            }
            DrawingToolType.PENCIL -> {
                paint.strokeWidth = stroke.strokeWidth * 0.8f
                paint.alpha = (alpha * 0.9f).toInt().coerceIn(0, 255)
            }
            else -> {}
        }

        if (stroke.points.size == 1) {
            val p = stroke.points.first()
            canvas.drawPoint(p.x, p.y, paint)

        } else {
            val path = Path()
            path.moveTo(stroke.points.first().x, stroke.points.first().y)
            for (i in 1 until stroke.points.size) {
                val p1 = stroke.points[i - 1]
                val p2 = stroke.points[i]
                path.quadTo(p1.x, p1.y, (p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawShape(canvas: Canvas, shape: DrawingElement.Shape, layerOpacity: Float) {
        val baseColor = try { Color.parseColor(shape.colorHex) } catch (_: Exception) { Color.BLACK }
        val alpha = (Color.alpha(baseColor) * layerOpacity).toInt().coerceIn(0, 255)

        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = shape.strokeWidth
            color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            val fillColor = try { Color.parseColor(shape.fillColorHex) } catch (_: Exception) { Color.TRANSPARENT }
            val fillAlpha = (Color.alpha(fillColor) * layerOpacity).toInt().coerceIn(0, 255)
            color = Color.argb(fillAlpha, Color.red(fillColor), Color.green(fillColor), Color.blue(fillColor))
        }

        val left = minOf(shape.startX, shape.endX)
        val top = minOf(shape.startY, shape.endY)
        val right = maxOf(shape.startX, shape.endX)
        val bottom = maxOf(shape.startY, shape.endY)
        val rect = RectF(left, top, right, bottom)

        when (shape.shapeType) {
            DrawingToolType.LINE -> {
                canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, strokePaint)
            }
            DrawingToolType.RECTANGLE, DrawingToolType.FILLED_RECTANGLE -> {
                if (shape.isFilled || shape.shapeType == DrawingToolType.FILLED_RECTANGLE) {
                    canvas.drawRect(rect, fillPaint)
                }
                canvas.drawRect(rect, strokePaint)
            }
            DrawingToolType.CIRCLE, DrawingToolType.ELLIPSE, DrawingToolType.FILLED_CIRCLE -> {
                if (shape.isFilled || shape.shapeType == DrawingToolType.FILLED_CIRCLE) {
                    canvas.drawOval(rect, fillPaint)
                }
                canvas.drawOval(rect, strokePaint)
            }
            DrawingToolType.POLYGON -> {
                val sides = shape.polygonSides.coerceAtLeast(3)
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val rx = kotlin.math.abs(right - left) / 2f
                val ry = kotlin.math.abs(bottom - top) / 2f

                val path = Path()
                for (i in 0 until sides) {
                    val angle = (2 * Math.PI * i / sides) - (Math.PI / 2)
                    val px = cx + (rx * cos(angle)).toFloat()
                    val py = cy + (ry * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()

                if (shape.isFilled) canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            else -> {}
        }
    }

    private fun drawText(canvas: Canvas, textElement: DrawingElement.Text, layerOpacity: Float) {
        val baseColor = try { Color.parseColor(textElement.colorHex) } catch (_: Exception) { Color.BLACK }
        val alpha = (Color.alpha(baseColor) * layerOpacity).toInt().coerceIn(0, 255)

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = textElement.fontSizeSp * 2f // Scale to pixel density
            color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        }

        canvas.drawText(textElement.text, textElement.x, textElement.y, paint)
    }

    private fun drawImportedImage(
        canvas: Canvas,
        imgElement: DrawingElement.ImportedImage,
        layerOpacity: Float,
        context: Context?
    ) {
        val file = File(imgElement.imagePath)
        if (!file.exists()) return

        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
        }

        val dstRect = RectF(
            imgElement.x,
            imgElement.y,
            imgElement.x + imgElement.width,
            imgElement.y + imgElement.height
        )

        canvas.drawBitmap(bitmap, null, dstRect, paint)
    }
}
