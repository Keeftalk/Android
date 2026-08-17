package com.keeftalk.chat.ui.screens.editor.signal

import android.graphics.Matrix
import android.graphics.RectF

/**
 * Ported from Signal Android (AGPL-3.0)
 */
object Bounds {
    const val LEFT = -1000f
    const val RIGHT = 1000f
    const val TOP = -1000f
    const val BOTTOM = 1000f

    const val CENTRE_X = (LEFT + RIGHT) / 2f
    const val CENTRE_Y = (TOP + BOTTOM) / 2f

    val CENTRE = floatArrayOf(CENTRE_X, CENTRE_Y)

    private val POINTS = floatArrayOf(
        LEFT, TOP,
        RIGHT, TOP,
        RIGHT, BOTTOM,
        LEFT, BOTTOM
    )

    fun newFullBounds(): RectF = RectF(LEFT, TOP, RIGHT, BOTTOM)

    val FULL_BOUNDS: RectF = newFullBounds()

    fun contains(x: Float, y: Float): Boolean {
        return x >= FULL_BOUNDS.left && x <= FULL_BOUNDS.right &&
                y >= FULL_BOUNDS.top && y <= FULL_BOUNDS.bottom
    }

    fun boundsRemainInBounds(matrix: Matrix?): Boolean {
        if (matrix == null) return true
        val dst = FloatArray(POINTS.size)
        matrix.mapPoints(dst, POINTS)
        return allWithinBounds(dst)
    }

    private fun allWithinBounds(points: FloatArray): Boolean {
        for (i in 0 until points.size / 2) {
            val x = points[2 * i]
            val y = points[2 * i + 1]
            if (!contains(x, y)) return false
        }
        return true
    }
}
