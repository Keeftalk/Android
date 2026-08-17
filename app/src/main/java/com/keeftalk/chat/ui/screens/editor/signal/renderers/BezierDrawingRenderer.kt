package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import com.keeftalk.chat.ui.screens.editor.signal.ColorableRenderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class BezierDrawingRenderer : InvalidateableRenderer, ColorableRenderer {
    private val paint: Paint = Paint()
    private val bezierLine: AutomaticControlPointBezierLine
    private val cap: Paint.Cap
    private val clipRect: RectF?
    
    override var color: Int = 0
        set(value) {
            if (field != value) {
                field = value
                updatePaint()
                invalidate()
            }
        }
    
    private var thickness: Float = 0f

    private constructor(color: Int, thickness: Float, cap: Paint.Cap, bezierLine: AutomaticControlPointBezierLine?, clipRect: RectF?) {
        this.cap = cap
        this.thickness = thickness
        this.clipRect = clipRect
        this.bezierLine = bezierLine ?: AutomaticControlPointBezierLine()
        this.color = color // Trigger setter after other properties are initialized
    }

    constructor(color: Int, thickness: Float, cap: Paint.Cap, clipRect: RectF?) : this(color, thickness, cap, null, clipRect?.let { RectF(it) })

    fun setThickness(thickness: Float) {
        if (this.thickness != thickness) {
            this.thickness = thickness
            updatePaint()
            invalidate()
        }
    }

    private fun updatePaint() {
        paint.color = color
        paint.strokeWidth = thickness
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        // Safe check for cap initialization
        @Suppress("UNNECESSARY_SAFE_CALL")
        if (cap != null) {
            paint.strokeCap = cap
        } else {
            paint.strokeCap = Paint.Cap.ROUND
        }
    }

    fun setFirstPoint(point: PointF) {
        bezierLine.reset()
        bezierLine.addPoint(point.x, point.y)
        invalidate()
    }

    fun addNewPoint(point: PointF) {
        if (cap != Paint.Cap.ROUND) {
            bezierLine.addPointFiltered(point.x, point.y, thickness * 0.5f)
        } else {
            bezierLine.addPoint(point.x, point.y)
        }
        invalidate()
    }

    override fun render(rendererContext: RendererContext) {
        super.render(rendererContext)
        val canvas = rendererContext.canvas
        canvas.save()
        clipRect?.let { canvas.clipRect(it) }

        val alpha = paint.alpha
        paint.alpha = rendererContext.getAlpha(alpha)
        paint.xfermode = rendererContext.maskPaint?.xfermode

        bezierLine.draw(canvas, paint)

        paint.alpha = alpha
        canvas.restore()
    }

    override fun hitTest(x: Float, y: Float): Boolean = false

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(color)
        dest.writeFloat(thickness)
        dest.writeInt(cap.ordinal)
        dest.writeParcelable(bezierLine, flags)
        dest.writeParcelable(clipRect, flags)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<BezierDrawingRenderer> {
            override fun createFromParcel(`in`: Parcel): BezierDrawingRenderer {
                val color = `in`.readInt()
                val thickness = `in`.readFloat()
                val cap = Paint.Cap.values()[`in`.readInt()]
                val bezierLine = `in`.readParcelable<AutomaticControlPointBezierLine>(AutomaticControlPointBezierLine::class.java.classLoader)
                val clipRect = `in`.readParcelable<RectF>(RectF::class.java.classLoader)
                return BezierDrawingRenderer(color, thickness, cap, bezierLine, clipRect)
            }
            override fun newArray(size: Int): Array<BezierDrawingRenderer?> = arrayOfNulls(size)
        }
    }
}
