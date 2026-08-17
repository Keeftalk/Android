package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.os.Parcel
import android.os.Parcelable
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class FaceBlurRenderer : Renderer {
    override fun render(rendererContext: RendererContext) {
        rendererContext.canvas.drawRect(Bounds.FULL_BOUNDS, rendererContext.maskPaint!!)
    }

    override fun hitTest(x: Float, y: Float): Boolean = Bounds.contains(x, y)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {}

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<FaceBlurRenderer> {
            override fun createFromParcel(`in`: Parcel): FaceBlurRenderer = FaceBlurRenderer()
            override fun newArray(size: Int): Array<FaceBlurRenderer?> = arrayOfNulls(size)
        }
    }
}
