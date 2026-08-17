package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.graphics.Path
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.EditorResources
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class InverseFillRenderer(private val color: Int) : Renderer {
    private val dst = RectF()
    private val path = Path().apply { fillType = Path.FillType.INVERSE_WINDING }

    override fun render(rendererContext: RendererContext) {
        rendererContext.canvas.save()
        rendererContext.mapRect(dst, Bounds.FULL_BOUNDS)
        rendererContext.canvasMatrix.setToIdentity()
        
        path.reset()
        val radius = EditorResources.dpToPx(18f)
        path.addRoundRect(dst, radius, radius, Path.Direction.CW)
        
        rendererContext.canvas.clipPath(path)
        rendererContext.canvas.drawColor(color)
        rendererContext.canvas.restore()
    }

    override fun hitTest(x: Float, y: Float): Boolean = !Bounds.contains(x, y)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(color)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<InverseFillRenderer> {
            override fun createFromParcel(`in`: Parcel): InverseFillRenderer = InverseFillRenderer(`in`.readInt())
            override fun newArray(size: Int): Array<InverseFillRenderer?> = arrayOfNulls(size)
        }
    }
}
