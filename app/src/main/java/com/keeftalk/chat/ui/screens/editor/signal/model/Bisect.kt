package com.keeftalk.chat.ui.screens.editor.signal.model

import android.graphics.Matrix

/**
 * Ported from Signal Android (AGPL-3.0)
 */
object Bisect {
    const val ACCURACY = 0.001f
    private const val MAX_ITERATIONS = 16

    fun interface Predicate {
        fun test(): Boolean
    }

    fun interface ModifyElement {
        fun applyFactor(matrix: Matrix, factor: Float)
    }

    fun bisectToTest(
        element: EditorElement,
        outOfBoundsValue: Float,
        atMost: Float,
        predicate: Predicate,
        modifyElement: ModifyElement,
        invalidate: Runnable
    ): Boolean {
        val closestSuccessful = bisectToTest(element, outOfBoundsValue, atMost, predicate, modifyElement)
        return if (closestSuccessful != null) {
            element.animateLocalTo(closestSuccessful, invalidate)
            true
        } else {
            false
        }
    }

    fun bisectToTest(
        element: EditorElement,
        outOfBoundsValue: Float,
        atMost: Float,
        predicate: Predicate,
        modifyElement: ModifyElement
    ): Matrix? {
        var mutableOutOfBoundsValue = outOfBoundsValue
        val elementMatrix = element.localMatrix
        val original = Matrix(elementMatrix)
        val closestSuccessful = Matrix()
        var haveResult = false
        var attempt = 0
        var successValue = 0f
        var inBoundsValue = atMost
        var nextValueToTry = inBoundsValue

        do {
            attempt++
            modifyElement.applyFactor(elementMatrix, nextValueToTry)
            try {
                if (predicate.test()) {
                    inBoundsValue = nextValueToTry
                    if (!haveResult || Math.abs(nextValueToTry - mutableOutOfBoundsValue) < Math.abs(successValue - mutableOutOfBoundsValue)) {
                        haveResult = true
                        successValue = nextValueToTry
                        closestSuccessful.set(elementMatrix)
                    }
                } else {
                    if (attempt == 1) return null
                    mutableOutOfBoundsValue = nextValueToTry
                }
            } finally {
                elementMatrix.set(original)
            }
            nextValueToTry = (inBoundsValue + mutableOutOfBoundsValue) / 2f
        } while (attempt < MAX_ITERATIONS && Math.abs(inBoundsValue - mutableOutOfBoundsValue) > ACCURACY)

        return if (haveResult) closestSuccessful else null
    }
}
