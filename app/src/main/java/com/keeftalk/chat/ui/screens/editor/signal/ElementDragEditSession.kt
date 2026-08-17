package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.PointF
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class ElementDragEditSession private constructor(
    selected: EditorElement,
    inverseMatrix: Matrix
) : ElementEditSession(selected, inverseMatrix) {

    override fun movePoint(p: Int, point: PointF) {
        setScreenEndPoint(p, point)
        selected.editorMatrix.setTranslate(
            endPointElement[0].x - startPointElement[0].x,
            endPointElement[0].y - startPointElement[0].y
        )
    }

    override fun newPoint(newInverse: Matrix, point: PointF, p: Int): EditSession? {
        return ElementScaleEditSession.startScale(this, newInverse, point, p)
    }

    override fun removePoint(newInverse: Matrix, p: Int): EditSession = this

    companion object {
        fun startDrag(selected: EditorElement, inverseViewModelMatrix: Matrix, point: PointF): ElementDragEditSession? {
            if (!selected.flags.isEditable()) return null
            val session = ElementDragEditSession(selected, inverseViewModelMatrix)
            session.setScreenStartPoint(0, point)
            session.setScreenEndPoint(0, point)
            return session
        }
    }
}
