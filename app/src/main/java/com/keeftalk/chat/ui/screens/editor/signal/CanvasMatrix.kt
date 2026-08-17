package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class CanvasMatrix(private val canvas: Canvas) {
    private val canvasMatrix = Matrix()
    private val temp = Matrix()
    private val stack = Array(16) { Matrix() }
    private var stackHeight = 0

    fun concat(matrix: Matrix) {
        canvas.concat(matrix)
        canvasMatrix.preConcat(matrix)
    }

    fun save() {
        canvas.save()
        if (stackHeight == 16) {
            throw AssertionError("Not enough space on stack")
        }
        stack[stackHeight++].set(canvasMatrix)
    }

    fun restore() {
        canvas.restore()
        canvasMatrix.set(stack[--stackHeight])
    }

    fun getCurrent(into: Matrix) {
        into.set(canvasMatrix)
    }

    fun setToIdentity() {
        if (canvasMatrix.invert(temp)) {
            concat(temp)
        }
    }

    fun initial(viewMatrix: Matrix) {
        concat(viewMatrix)
    }

    fun mapRect(dst: RectF, src: RectF): Boolean {
        return canvasMatrix.mapRect(dst, src)
    }

    fun mapPoints(dst: FloatArray, src: FloatArray) {
        canvasMatrix.mapPoints(dst, src)
    }

    fun copyTo(matrix: Matrix) {
        matrix.set(canvasMatrix)
    }
}
