package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.PointF
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement

/**
 * Ported from Signal Android (AGPL-3.0)
 */
abstract class ElementEditSession(
    override val selected: EditorElement,
    private val inverseMatrix: Matrix
) : EditSession {

    val startPointElement = newTwoPointArray()
    val endPointElement = newTwoPointArray()
    val startPointScreen = newTwoPointArray()
    val endPointScreen = newTwoPointArray()

    fun setScreenStartPoint(p: Int, point: PointF) {
        startPointScreen[p].set(point)
        mapPoint(startPointElement[p], inverseMatrix, point)
    }

    fun setScreenEndPoint(p: Int, point: PointF) {
        endPointScreen[p].set(point)
        mapPoint(endPointElement[p], inverseMatrix, point)
    }

    override fun commit() {
        selected.commitEditorMatrix()
    }

    companion object {
        private fun newTwoPointArray(): Array<PointF> = Array(2) { PointF() }

        private fun mapPoint(dst: PointF, matrix: Matrix, src: PointF) {
            val out = FloatArray(2)
            matrix.mapPoints(out, floatArrayOf(src.x, src.y))
            dst.set(out[0], out[1])
        }
    }
}
