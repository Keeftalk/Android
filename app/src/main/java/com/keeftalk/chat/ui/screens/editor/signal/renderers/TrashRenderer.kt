package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.view.animation.Interpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.EditorResources
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext

/**
 * Ported from Signal Android (AGPL-3.0)
 */
internal class TrashRenderer : InvalidateableRenderer(), Renderer, Parcelable {
    private val outlinePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.STROKE
    }

    private val shadePaint = Paint().apply {
        isAntiAlias = true
        color = 0x99000000.toInt()
        style = Paint.Style.FILL
    }

    private val bounds = RectF()
    private val interpolator: Interpolator = FastOutSlowInInterpolator()
    private var startTime = 0L
    private var isExpanding = false
    private val buttonCenter = FloatArray(2)
    private var diameterSmall = 0f
    private var diameterLarge = 0f

    override fun render(rendererContext: RendererContext) {
        super.render(rendererContext)
        val context = rendererContext.context
        outlinePaint.strokeWidth = EditorResources.dpToPx(1.5f)
        diameterSmall = EditorResources.dpToPx(41f)
        diameterLarge = EditorResources.dpToPx(54f)
        val trashSize = EditorResources.dpToPx(24f).toInt()
        val padBottom = EditorResources.dpToPx(16f)

        val frameRenderTime = System.currentTimeMillis()
        
        val trash = AppCompatResources.getDrawable(context, android.R.drawable.ic_menu_delete)
        trash?.setBounds(0, 0, trashSize, trashSize)

        val diameter = getInterpolatedDiameter(frameRenderTime - startTime)

        rendererContext.canvas.save()
        rendererContext.mapRect(bounds, Bounds.FULL_BOUNDS)

        buttonCenter[0] = bounds.centerX()
        buttonCenter[1] = bounds.bottom - diameterLarge / 2f - padBottom

        rendererContext.canvasMatrix.setToIdentity()

        rendererContext.canvas.drawCircle(buttonCenter[0], buttonCenter[1], diameter / 2f, shadePaint)
        rendererContext.canvas.drawCircle(buttonCenter[0], buttonCenter[1], diameter / 2f, outlinePaint)
        
        rendererContext.canvas.save()
        rendererContext.canvas.translate(buttonCenter[0], buttonCenter[1])
        rendererContext.canvas.translate(-(trashSize / 2f), -(trashSize / 2f))
        trash?.draw(rendererContext.canvas)
        rendererContext.canvas.restore()
        
        rendererContext.canvas.restore()

        if (frameRenderTime - DURATION < startTime) {
            invalidate()
        }
    }

    private fun getInterpolatedDiameter(timeElapsed: Long): Float {
        return if (timeElapsed >= DURATION) {
            if (isExpanding) diameterLarge else diameterSmall
        } else {
            val interpolatedFraction = interpolator.getInterpolation(timeElapsed / DURATION.toFloat())
            if (isExpanding) {
                diameterSmall + (diameterLarge - diameterSmall) * interpolatedFraction
            } else {
                diameterSmall + (diameterLarge - diameterSmall) * (1 - interpolatedFraction)
            }
        }
    }

    fun expand() {
        if (isExpanding) return
        isExpanding = true
        startTime = System.currentTimeMillis()
        invalidate()
    }

    fun shrink() {
        if (!isExpanding) return
        isExpanding = false
        startTime = System.currentTimeMillis()
        invalidate()
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        val dx = x - buttonCenter[0]
        val dy = y - buttonCenter[1]
        val radius = diameterLarge / 2
        return dx * dx + dy * dy < radius * radius
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}

    companion object {
        private const val DURATION = 150L

        @JvmField
        val CREATOR = object : Parcelable.Creator<TrashRenderer> {
            override fun createFromParcel(`in`: Parcel): TrashRenderer = TrashRenderer()
            override fun newArray(size: Int): Array<TrashRenderer?> = arrayOfNulls(size)
        }
    }
}
