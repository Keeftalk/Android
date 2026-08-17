package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix

/**
 * Ported from Signal Android (AGPL-3.0)
 */
object MatrixUtils {
    private val tempMatrixValues = ThreadLocal<FloatArray>()

    private fun getTempMatrixValues(): FloatArray {
        var floats = tempMatrixValues.get()
        if (floats == null) {
            floats = FloatArray(9)
            tempMatrixValues.set(floats)
        }
        return floats
    }

    fun getRotationAngle(matrix: Matrix): Float {
        val matrixValues = getTempMatrixValues()
        matrix.getValues(matrixValues)
        return (-Math.atan2(matrixValues[Matrix.MSKEW_X].toDouble(), matrixValues[Matrix.MSCALE_X].toDouble())).toFloat()
    }

    fun getScaleX(matrix: Matrix): Float {
        val matrixValues = getTempMatrixValues()
        matrix.getValues(matrixValues)
        val scaleX = matrixValues[Matrix.MSCALE_X]
        val skewX = matrixValues[Matrix.MSKEW_X]
        return Math.sqrt((scaleX * scaleX + skewX * skewX).toDouble()).toFloat()
    }
}
