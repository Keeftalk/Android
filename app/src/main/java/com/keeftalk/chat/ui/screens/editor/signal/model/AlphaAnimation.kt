package com.keeftalk.chat.ui.screens.editor.signal.model

import android.animation.ValueAnimator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

/**
 * Ported from Signal Android (AGPL-3.0)
 */
internal class AlphaAnimation private constructor(
    private val from: Float,
    private val to: Float,
    private val invalidate: Runnable?
) {
    private val canAnimate: Boolean = invalidate != null
    private var animatedFraction: Float = from

    init {
        if (canAnimate) {
            animatedFraction = from
        }
    }

    private constructor(fixed: Float) : this(fixed, fixed, null)

    fun start() {
        if (canAnimate && invalidate != null) {
            val animator = ValueAnimator.ofFloat(from, to)
            animator.duration = 200
            animator.interpolator = interpolator
            animator.addUpdateListener { animation ->
                animatedFraction = animation.animatedValue as Float
                invalidate.run()
            }
            animator.start()
        }
    }

    fun getValue(): Float {
        return if (!canAnimate) to else animatedFraction
    }

    companion object {
        private val interpolator: Interpolator = LinearInterpolator()
        val NULL_1 = AlphaAnimation(1f)

        fun animate(from: Float, to: Float, invalidate: Runnable?): AlphaAnimation {
            if (invalidate == null) return AlphaAnimation(to)

            return if (from != to) {
                val animation = AlphaAnimation(from, to, invalidate)
                animation.start()
                animation
            } else {
                AlphaAnimation(to)
            }
        }
    }
}
