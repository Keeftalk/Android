package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.EditorResources
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class OvalGuideRenderer(@ColorRes private val ovalGuideColor: Int) : Renderer {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val dst = RectF()

    override fun render(rendererContext: RendererContext) {
        rendererContext.save()
        val context = rendererContext.context
        val stroke = EditorResources.dpToPx(EditorResources.OVAL_GUIDE_STROKE_WIDTH)
        val halfStroke = stroke / 2f

        paint.strokeWidth = stroke
        paint.color = ContextCompat.getColor(context, ovalGuideColor)

        rendererContext.mapRect(dst, Bounds.FULL_BOUNDS)
        dst.set(dst.left + halfStroke, dst.top + halfStroke, dst.right - halfStroke, dst.bottom - halfStroke)

        rendererContext.canvasMatrix.setToIdentity()
        rendererContext.canvas.drawOval(dst, paint)
        rendererContext.restore()
    }

    override fun hitTest(x: Float, y: Float): Boolean = !Bounds.contains(x, y)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ovalGuideColor)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<OvalGuideRenderer> {
            override fun createFromParcel(`in`: Parcel): OvalGuideRenderer = OvalGuideRenderer(`in`.readInt())
            override fun newArray(size: Int): Array<OvalGuideRenderer?> = arrayOfNulls(size)
        }
    }
}
