package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.PointF
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement

/**
 * Ported from Signal Android (AGPL-3.0)
 */
interface EditSession {
    fun movePoint(p: Int, point: PointF)
    val selected: EditorElement
    fun newPoint(newInverse: Matrix, point: PointF, p: Int): EditSession?
    fun removePoint(newInverse: Matrix, p: Int): EditSession?
    fun commit()
}
