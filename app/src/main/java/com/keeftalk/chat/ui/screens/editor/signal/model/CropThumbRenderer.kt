package com.keeftalk.chat.ui.screens.editor.signal.model

import android.graphics.Matrix
import android.os.Parcel
import android.os.Parcelable
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.EditorResources
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class CropThumbRenderer(
    private val controlPoint: ThumbRenderer.ControlPoint,
    private val toControl: UUID
) : Renderer, ThumbRenderer {
    private val centreOnScreen = FloatArray(2)
    private val matrix = Matrix()
    private var size = 0f

    override fun getControlPoint(): ThumbRenderer.ControlPoint = controlPoint
    override fun getElementToControl(): UUID = toControl

    override fun render(rendererContext: RendererContext) {
        rendererContext.canvasMatrix.mapPoints(centreOnScreen, Bounds.CENTRE)
        rendererContext.canvasMatrix.copyTo(matrix)
        size = EditorResources.dpToPx(EditorResources.CROP_AREA_RENDERER_EDGE_SIZE)
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        val hitPointOnScreen = FloatArray(2)
        matrix.mapPoints(hitPointOnScreen, floatArrayOf(x, y))

        val dx = centreOnScreen[0] - hitPointOnScreen[0]
        val dy = centreOnScreen[1] - hitPointOnScreen[1]

        return dx * dx + dy * dy < size * size
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(controlPoint.ordinal)
        ParcelUtils.writeUUID(dest, toControl)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<CropThumbRenderer> {
            override fun createFromParcel(`in`: Parcel): CropThumbRenderer {
                return CropThumbRenderer(ThumbRenderer.ControlPoint.values()[`in`.readInt()], ParcelUtils.readUUID(`in`))
            }
            override fun newArray(size: Int): Array<CropThumbRenderer?> = arrayOfNulls(size)
        }
    }
}
