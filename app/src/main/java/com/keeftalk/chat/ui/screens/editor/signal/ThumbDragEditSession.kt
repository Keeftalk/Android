package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.PointF
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.model.ThumbRenderer

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class ThumbDragEditSession private constructor(
    selected: EditorElement,
    private val controlPoint: ThumbRenderer.ControlPoint,
    inverseMatrix: Matrix,
    private val thumbContainerRelativeMatrix: Matrix
) : ElementEditSession(selected, inverseMatrix) {

    private val oppositeControlPoint = PointF()
    private val oppositeControlPointOnControlParent = FloatArray(2)
    private val oppositeControlPointOnElement = FloatArray(2)

    override fun movePoint(p: Int, point: PointF) {
        setScreenEndPoint(p, point)
        val editorMatrix = selected.editorMatrix
        editorMatrix.reset()

        oppositeControlPointOnControlParent[0] = controlPoint.opposite().x
        oppositeControlPointOnControlParent[1] = controlPoint.opposite().y
        thumbContainerRelativeMatrix.mapPoints(oppositeControlPointOnElement, oppositeControlPointOnControlParent)
        
        val x = oppositeControlPointOnElement[0]
        val y = oppositeControlPointOnElement[1]
        oppositeControlPoint.set(x, y)

        val dx = endPointElement[0].x - startPointElement[0].x
        val dy = endPointElement[0].y - startPointElement[0].y
        val xEnd = controlPoint.x + dx
        val yEnd = controlPoint.y + dy

        if (controlPoint.isScaleAndRotateThumb()) {
            val scale = findScale(oppositeControlPoint, startPointElement[0], endPointElement[0])
            editorMatrix.postTranslate(-oppositeControlPoint.x, -oppositeControlPoint.y)
            editorMatrix.postScale(scale, scale)
            val angle = angle(endPointElement[0], oppositeControlPoint) - angle(startPointElement[0], oppositeControlPoint)
            editorMatrix.postRotate(Math.toDegrees(angle).toFloat())
            editorMatrix.postTranslate(oppositeControlPoint.x, oppositeControlPoint.y)
        } else {
            val aspectLocked = selected.flags.isAspectLocked() && !controlPoint.isCenter()
            val defaultScale = if (aspectLocked) 2f else 1f
            val scaleX = if (controlPoint.isVerticalCenter()) defaultScale else (xEnd - x) / (controlPoint.x - x)
            val scaleY = if (controlPoint.isHorizontalCenter()) defaultScale else (yEnd - y) / (controlPoint.y - y)
            scale(editorMatrix, aspectLocked, scaleX, scaleY, controlPoint.opposite())
        }
    }

    private fun scale(editorMatrix: Matrix, aspectLocked: Boolean, scaleX: Float, scaleY: Float, around: ThumbRenderer.ControlPoint) {
        val x = around.x
        val y = around.y
        editorMatrix.postTranslate(-x, -y)
        if (aspectLocked) {
            val minScale = Math.min(scaleX, scaleY)
            editorMatrix.postScale(minScale, minScale)
        } else {
            editorMatrix.postScale(scaleX, scaleY)
        }
        editorMatrix.postTranslate(x, y)
    }

    override fun newPoint(newInverse: Matrix, point: PointF, p: Int): EditSession? = null
    override fun removePoint(newInverse: Matrix, p: Int): EditSession? = null

    companion object {
        fun startDrag(
            selected: EditorElement,
            inverseViewModelMatrix: Matrix,
            thumbContainerRelativeMatrix: Matrix,
            controlPoint: ThumbRenderer.ControlPoint,
            point: PointF
        ): EditSession? {
            if (!selected.flags.isEditable()) return null
            val session = ThumbDragEditSession(selected, controlPoint, inverseViewModelMatrix, thumbContainerRelativeMatrix)
            session.setScreenStartPoint(0, point)
            session.setScreenEndPoint(0, point)
            return session
        }

        private fun angle(a: PointF, b: PointF): Double = Math.atan2((a.y - b.y).toDouble(), (a.x - b.x).toDouble())

        private fun findScale(anchor: PointF, from: PointF, to: PointF): Float {
            val originalD2 = getDistanceSquared(from, anchor)
            val newD2 = getDistanceSquared(to, anchor)
            return Math.sqrt((newD2 / originalD2).toDouble()).toFloat()
        }

        private fun getDistanceSquared(a: PointF, b: PointF): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            return dx * dx + dy * dy
        }
    }
}
