package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.RectF

/**
 * Ported from Signal Android (AGPL-3.0)
 */
interface SelectableRenderer : Renderer {
    fun onSelected(selected: Boolean)
    fun getSelectionBounds(bounds: RectF)
}
