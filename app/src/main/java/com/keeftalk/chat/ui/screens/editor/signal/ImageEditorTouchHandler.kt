package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel
import com.keeftalk.chat.ui.screens.editor.signal.model.ThumbRenderer
import com.keeftalk.chat.ui.screens.editor.signal.renderers.BezierDrawingRenderer

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class ImageEditorTouchHandler {

    private var drawing: Boolean = false
    private var blur: Boolean = false
    private var drawColor: Int = -0x1000000
    private var drawThickness: Float = 0.02f
    private var drawCap: Paint.Cap = Paint.Cap.ROUND

    private var editSession: EditSession? = null
    private var moreThanOnePointerUsedInSession: Boolean = false

    fun setDrawing(drawing: Boolean, blur: Boolean) {
        this.drawing = drawing
        this.blur = blur
    }

    fun setDrawingBrush(color: Int, thickness: Float, cap: Paint.Cap) {
        drawColor = color
        drawThickness = thickness
        drawCap = cap
    }

    fun onDown(model: SignalEditorModel, viewMatrix: Matrix, point: PointF): EditorElement? {
        val inverse = Matrix()
        val selected = model.findElementAtPoint(point, viewMatrix, inverse)
        moreThanOnePointerUsedInSession = false
        model.pushUndoPoint()
        editSession = startEdit(model, viewMatrix, inverse, point, selected)
        return editSession?.selected
    }

    fun onMove(model: SignalEditorModel, pointers: Array<PointF>) {
        val session = editSession ?: return
        val pointerCount = Math.min(2, pointers.size)
        for (p in 0 until pointerCount) {
            session.movePoint(p, pointers[p])
        }
        model.moving(session.selected)
    }

    fun onSecondPointerDown(model: SignalEditorModel, viewMatrix: Matrix, newPointerPoint: PointF, pointerIndex: Int) {
        val session = editSession ?: return
        moreThanOnePointerUsedInSession = true
        session.commit()
        model.pushUndoPoint()
        val newInverse = model.findElementInverseMatrix(session.selected, viewMatrix)
        editSession = if (newInverse != null) session.newPoint(newInverse, newPointerPoint, pointerIndex) else null
        if (editSession == null) model.dragDropRelease()
    }

    fun onSecondPointerUp(model: SignalEditorModel, viewMatrix: Matrix, releasedIndex: Int) {
        val session = editSession ?: return
        session.commit()
        model.pushUndoPoint()
        model.dragDropRelease()
        val newInverse = model.findElementInverseMatrix(session.selected, viewMatrix)
        editSession = if (newInverse != null) session.removePoint(newInverse, releasedIndex) else null
    }

    fun onUp(model: SignalEditorModel) {
        editSession?.let {
            it.commit()
            model.dragDropRelease()
            editSession = null
        }
        model.postEdit(moreThanOnePointerUsedInSession)
    }

    fun cancel() { editSession = null }
    fun hasActiveSession(): Boolean = editSession != null
    fun getSelected(): EditorElement? = editSession?.selected

    private fun startEdit(model: SignalEditorModel, viewMatrix: Matrix, inverse: Matrix, point: PointF, selected: EditorElement?): EditSession? {
        val session = startMoveAndResizeSession(model, viewMatrix, inverse, point, selected)
        return if (session == null && drawing) startDrawingSession(model, viewMatrix, point) else session
    }

    private fun startDrawingSession(model: SignalEditorModel, viewMatrix: Matrix, point: PointF): EditSession {
        val renderer = BezierDrawingRenderer(drawColor, drawThickness * Bounds.FULL_BOUNDS.width(), drawCap, model.findCropRelativeToRoot())
        val element = EditorElement(renderer, if (blur) -1 else 0) // Z_MASK is -1, Z_DRAWING is 0
        model.addElementCentered(element, 1f)
        val elementInverseMatrix = model.findElementInverseMatrix(element, viewMatrix)!!
        return DrawingSession.start(element, renderer, elementInverseMatrix, point)
    }

    companion object {
        private fun startMoveAndResizeSession(model: SignalEditorModel, viewMatrix: Matrix, inverse: Matrix, point: PointF, selected: EditorElement?): EditSession? {
            if (selected == null) return null
            if (selected.renderer is ThumbRenderer) {
                val thumb = selected.renderer as ThumbRenderer
                val thumbControlledElement = model.findById(thumb.getElementToControl()) ?: return null
                val thumbsParent = model.root.findParent(selected) ?: return null
                val thumbContainerRelativeMatrix = model.findRelativeMatrix(thumbsParent, thumbControlledElement) ?: return null
                val elementInverseMatrix = model.findElementInverseMatrix(thumbControlledElement, viewMatrix)
                return if (elementInverseMatrix != null) {
                    ThumbDragEditSession.startDrag(thumbControlledElement, elementInverseMatrix, thumbContainerRelativeMatrix, thumb.getControlPoint(), point)
                } else null
            }
            return ElementDragEditSession.startDrag(selected, inverse, point)
        }
    }
}
