package tn.keeftalk.chat.ui.books.reader

import android.os.SystemClock

class PageDwellTracker {
    private var pageStartTimeMs: Long = SystemClock.elapsedRealtime()
    private var accumulatedMs: Long = 0L
    private var isTracking: Boolean = true

    fun onPageChanged() {
        pageStartTimeMs = SystemClock.elapsedRealtime()
        accumulatedMs = 0L
        isTracking = true
    }

    fun onPause() {
        if (isTracking) {
            accumulatedMs += (SystemClock.elapsedRealtime() - pageStartTimeMs).coerceAtLeast(0L)
            isTracking = false
        }
    }

    fun onResume() {
        if (!isTracking) {
            pageStartTimeMs = SystemClock.elapsedRealtime()
            isTracking = true
        }
    }

    fun dwellSec(): Double {
        val currentChunk = if (isTracking) (SystemClock.elapsedRealtime() - pageStartTimeMs).coerceAtLeast(0L) else 0L
        val totalMs = accumulatedMs + currentChunk
        return totalMs / 1000.0
    }
}
