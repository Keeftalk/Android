package com.keeftalk.chat.ui.screens.editor.signal

import android.os.Parcelable

/**
 * Ported from Signal Android (AGPL-3.0)
 */
interface Renderer : Parcelable {
    fun render(rendererContext: RendererContext)
    fun hitTest(x: Float, y: Float): Boolean
}
