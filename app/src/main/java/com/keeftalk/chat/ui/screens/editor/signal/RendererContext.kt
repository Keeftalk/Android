package com.keeftalk.chat.ui.screens.editor.signal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Typeface
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class RendererContext(
    val context: Context,
    val canvas: Canvas,
    val rendererReady: Ready,
    val invalidate: Invalidate,
    val typefaceProvider: TypefaceProvider
) {
    val canvasMatrix: CanvasMatrix = CanvasMatrix(canvas)
    
    var isBlockingLoad: Boolean = false
    var isEditing: Boolean = true
    private var fade: Float = 1f
    var children: List<EditorElement> = emptyList<EditorElement>()
    var maskPaint: Paint? = null

    fun mapRect(dst: RectF, src: RectF): Boolean = canvasMatrix.mapRect(dst, src)

    fun getAlpha(alpha: Int): Int = (fade * alpha).toInt().coerceIn(0, 255)

    fun setFade(fade: Float) {
        this.fade = fade
    }

    fun save() = canvasMatrix.save()
    fun restore() = canvasMatrix.restore()
    fun getCurrent(into: Matrix) = canvasMatrix.getCurrent(into)

    interface TypefaceProvider {
        fun getSelectedTypeface(context: Context, renderer: Renderer, invalidate: Invalidate): Typeface
    }

    interface Ready {
        fun onReady(renderer: Renderer, cropMatrix: Matrix?, size: Point?)

        companion object {
            val NULL = object : Ready {
                override fun onReady(renderer: Renderer, cropMatrix: Matrix?, size: Point?) {}
            }
        }
    }

    interface Invalidate {
        fun onInvalidate(renderer: Renderer)

        companion object {
            val NULL = object : Invalidate {
                override fun onInvalidate(renderer: Renderer) {}
            }
        }
    }
}
