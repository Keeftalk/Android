package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.PointF
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.renderers.BezierDrawingRenderer

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class DrawingSession private constructor(
    selected: EditorElement,
    inverseMatrix: Matrix,
    private val renderer: BezierDrawingRenderer
) : ElementEditSession(selected, inverseMatrix) {

    override fun movePoint(p: Int, point: PointF) {
        if (p != 0) return
        setScreenEndPoint(p, point)
        renderer.addNewPoint(endPointElement[0])
    }

    override fun newPoint(newInverse: Matrix, point: PointF, p: Int): EditSession = this
    override fun removePoint(newInverse: Matrix, p: Int): EditSession = this

    companion object {
        fun start(element: EditorElement, renderer: BezierDrawingRenderer, inverseMatrix: Matrix, point: PointF): EditSession {
            val drawingSession = DrawingSession(element, inverseMatrix, renderer)
            drawingSession.setScreenStartPoint(0, point)
            renderer.setFirstPoint(drawingSession.startPointElement[0])
            return drawingSession
        }
    }
}
