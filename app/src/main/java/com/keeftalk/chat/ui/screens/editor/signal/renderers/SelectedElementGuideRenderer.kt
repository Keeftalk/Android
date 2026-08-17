package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Parcel
import android.os.Parcelable
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.EditorResources
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class SelectedElementGuideRenderer : Renderer {
    private val allPointsOnScreen = FloatArray(8)
    private val allPointsInLocalCords = floatArrayOf(
        Bounds.LEFT, Bounds.TOP,
        Bounds.RIGHT, Bounds.TOP,
        Bounds.RIGHT, Bounds.BOTTOM,
        Bounds.LEFT, Bounds.BOTTOM
    )

    private val guidePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }

    private val circlePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val path = Path()

    override fun render(rendererContext: RendererContext) {
        val circleRadius = EditorResources.dpToPx(5f)
        guidePaint.strokeWidth = EditorResources.dpToPx(1.5f)
        
        rendererContext.canvasMatrix.mapPoints(allPointsOnScreen, allPointsInLocalCords)
        
        rendererContext.save()
        rendererContext.canvasMatrix.setToIdentity()

        path.reset()
        path.moveTo(allPointsOnScreen[0], allPointsOnScreen[1])
        path.lineTo(allPointsOnScreen[2], allPointsOnScreen[3])
        path.lineTo(allPointsOnScreen[4], allPointsOnScreen[5])
        path.lineTo(allPointsOnScreen[6], allPointsOnScreen[7])
        path.close()

        rendererContext.canvas.drawPath(path, guidePaint)
        rendererContext.canvas.drawCircle(
            (allPointsOnScreen[6] + allPointsOnScreen[0]) / 2f,
            (allPointsOnScreen[7] + allPointsOnScreen[1]) / 2f,
            circleRadius,
            circlePaint
        )
        rendererContext.canvas.drawCircle(
            (allPointsOnScreen[4] + allPointsOnScreen[2]) / 2f,
            (allPointsOnScreen[5] + allPointsOnScreen[3]) / 2f,
            circleRadius,
            circlePaint
        )
        rendererContext.restore()
    }

    override fun hitTest(x: Float, y: Float): Boolean = false

    override fun writeToParcel(parcel: Parcel, flags: Int) {}
    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SelectedElementGuideRenderer> {
            override fun createFromParcel(parcel: Parcel): SelectedElementGuideRenderer = SelectedElementGuideRenderer()
            override fun newArray(size: Int): Array<SelectedElementGuideRenderer?> = arrayOfNulls(size)
        }
    }
}
