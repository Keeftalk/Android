package com.keeftalk.chat.ui.screens.editor.signal.model

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.view.animation.CycleInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import com.keeftalk.chat.ui.screens.editor.signal.CanvasMatrix

/**
 * Animation Matrix provides a matrix that animates over time down to the identity matrix.
 * Ported from Signal Android (AGPL-3.0)
 */
internal class AnimationMatrix {

    private val invalidate: Runnable?
    private val canAnimate: Boolean
    private val undoValues = FloatArray(9)
    private val temp = Matrix()
    private val tempValues = FloatArray(9)
    private var animator: ValueAnimator? = null
    private var animatedFraction: Float = 0f

    private constructor(undo: Matrix, invalidate: Runnable) {
        this.invalidate = invalidate
        this.canAnimate = true
        undo.getValues(undoValues)
    }

    private constructor() {
        canAnimate = false
        invalidate = null
    }

    fun start(interpolator: Interpolator) {
        if (canAnimate && invalidate != null) {
            animator = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 250
                setInterpolator(interpolator)
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    this@AnimationMatrix.animatedFraction = value
                    invalidate.run()
                }
                start()
            }
        }
    }

    fun stop() {
        animator?.cancel()
    }

    fun preConcatValueTo(onTo: Matrix) {
        if (!canAnimate) return
        onTo.preConcat(buildTemp())
    }

    fun preConcatValueTo(canvasMatrix: CanvasMatrix) {
        if (!canAnimate) return
        canvasMatrix.concat(buildTemp())
    }

    private fun buildTemp(): Matrix {
        if (!canAnimate) {
            temp.reset()
            return temp
        }

        val fractionCompliment = 1f - animatedFraction
        for (i in 0 until 9) {
            tempValues[i] = fractionCompliment * iValues[i] + animatedFraction * undoValues[i]
        }

        temp.setValues(tempValues)
        return temp
    }

    companion object {
        private val iValues = FloatArray(9).apply { Matrix().getValues(this) }
        private val interpolator: Interpolator = DecelerateInterpolator()
        private val pulseInterpolator: Interpolator = inverse(CycleInterpolator(0.5f))

        val NULL = AnimationMatrix()

        fun animate(from: Matrix, to: Matrix, invalidate: Runnable?): AnimationMatrix {
            if (invalidate == null) return NULL

            val undo = Matrix()
            val inverted = to.invert(undo)
            if (inverted) {
                undo.preConcat(from)
            }
            return if (inverted && !undo.isIdentity) {
                val animationMatrix = AnimationMatrix(undo, invalidate)
                animationMatrix.start(interpolator)
                animationMatrix
            } else {
                NULL
            }
        }

        fun singlePulse(pulse: Matrix, invalidate: Runnable?): AnimationMatrix {
            if (invalidate == null) return NULL
            val animationMatrix = AnimationMatrix(pulse, invalidate)
            animationMatrix.start(pulseInterpolator)
            return animationMatrix
        }

        private fun inverse(interpolator: Interpolator): Interpolator {
            return Interpolator { input -> 1f - interpolator.getInterpolation(input) }
        }
    }
}
