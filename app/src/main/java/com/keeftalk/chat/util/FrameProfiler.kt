package com.keeftalk.chat.util

import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import com.keeftalk.chat.BuildConfig

/**
 * Detailed Frame Profiler for Keeftalk.
 * Tracks granular frame stages and detects jank.
 */
object FrameProfiler {
    private val isEnabled = BuildConfig.DEBUG
    private val frameHandler = Handler(Looper.getMainLooper())

    fun attachToWindow(window: Window) {
        if (!isEnabled) return
        
        window.addOnFrameMetricsAvailableListener(object : Window.OnFrameMetricsAvailableListener {
            private var frameCount = 0
            
            override fun onFrameMetricsAvailable(w: Window?, m: FrameMetrics?, dropCount: Int) {
                if (m == null) return
                
                val totalDuration = m.getMetric(FrameMetrics.TOTAL_DURATION)
                val inputDuration = m.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)
                val animDuration = m.getMetric(FrameMetrics.ANIMATION_DURATION)
                val layoutMeasureDuration = m.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
                val drawDuration = m.getMetric(FrameMetrics.DRAW_DURATION)
                val gpuDuration = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    m.getMetric(FrameMetrics.GPU_DURATION)
                } else 0L
                
                frameCount++
                
                val threshold = 16_666_666L // 60 FPS
                if (totalDuration > threshold) {
                    val jankType = when {
                        totalDuration > 100_000_000L -> "FREEZE (>100ms)"
                        totalDuration > 50_000_000L -> "LARGE_JANK (>50ms)"
                        totalDuration > 33_333_333L -> "MEDIUM_JANK (>33ms)"
                        else -> "JANK (>16ms)"
                    }
                    
                    val info = "Total: ${totalDuration / 1_000_000}ms | Input: ${inputDuration / 1_000_000}ms | " +
                               "Anim: ${animDuration / 1_000_000}ms | Layout: ${layoutMeasureDuration / 1_000_000}ms | " +
                               "Draw: ${drawDuration / 1_000_000}ms | GPU: ${gpuDuration / 1_000_000}ms"
                    
                    PerformanceProfiler.logEvent(
                        name = "Frame #$frameCount ($jankType)",
                        info = info,
                        category = PerformanceProfiler.Category.DRAW
                    )
                }
            }
        }, frameHandler)
    }
}
