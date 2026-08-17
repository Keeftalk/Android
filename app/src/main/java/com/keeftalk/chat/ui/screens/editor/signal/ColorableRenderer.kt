package com.keeftalk.chat.ui.screens.editor.signal

import androidx.annotation.ColorInt

/**
 * Ported from Signal Android (AGPL-3.0)
 */
interface ColorableRenderer : Renderer {
    @get:ColorInt
    var color: Int
}
