package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.graphics.Canvas
import android.graphics.Paint
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
class CropAreaRenderer(
    private val color: Int,
    private val renderCenterThumbs: Boolean
) : Renderer {
    private val cropClipPath = Path().apply {
        fillType = Path.FillType.INVERSE_WINDING
        moveTo(Bounds.LEFT, Bounds.TOP)
        lineTo(Bounds.RIGHT, Bounds.TOP)
        lineTo(Bounds.RIGHT, Bounds.BOTTOM)
        lineTo(Bounds.LEFT, Bounds.BOTTOM)
        close()
    }
    private val screenClipPath = Path().apply { fillType = Path.FillType.INVERSE_WINDING }
    private val dst = RectF()
    private val paint = Paint()

    override fun render(rendererContext: RendererContext) {
        rendererContext.save()
        val canvas = rendererContext.canvas
        val context = rendererContext.context

        canvas.clipPath(cropClipPath)
        canvas.drawColor(color)

        rendererContext.mapRect(dst, Bounds.FULL_BOUNDS)

        val thickness = EditorResources.dpToPx(EditorResources.CROP_AREA_RENDERER_EDGE_THICKNESS)
        val edgeSize = EditorResources.dpToPx(EditorResources.CROP_AREA_RENDERER_EDGE_SIZE)
        val size = Math.min(edgeSize, Math.min(dst.width(), dst.height()) / 3f - 10)

        paint.color = EditorResources.CROP_AREA_RENDERER_EDGE_COLOR

        rendererContext.canvasMatrix.setToIdentity()
        screenClipPath.reset()
        screenClipPath.moveTo(dst.left, dst.top)
        screenClipPath.lineTo(dst.right, dst.top)
        screenClipPath.lineTo(dst.right, dst.bottom)
        screenClipPath.lineTo(dst.left, dst.bottom)
        screenClipPath.close()
        canvas.clipPath(screenClipPath)
        canvas.translate(dst.left, dst.top)

        val halfDx = (dst.right - dst.left - size + thickness) / 2
        val halfDy = (dst.bottom - dst.top - size + thickness) / 2

        canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(0f, halfDy)
        if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(0f, halfDy)
        canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(halfDx, 0f)
        if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(halfDx, 0f)
        canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(0f, -halfDy)
        if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(0f, -halfDy)
        canvas.drawRect(-thickness, -thickness, size, size, paint)

        canvas.translate(-halfDx, 0f)
        if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint)

        rendererContext.restore()
    }

    override fun hitTest(x: Float, y: Float): Boolean = !Bounds.contains(x, y)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(color)
        dest.writeByte(if (renderCenterThumbs) 1 else 0)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<CropAreaRenderer> {
            override fun createFromParcel(`in`: Parcel): CropAreaRenderer {
                return CropAreaRenderer(`in`.readInt(), `in`.readByte().toInt() == 1)
            }
            override fun newArray(size: Int): Array<CropAreaRenderer?> = arrayOfNulls(size)
        }
    }
}
