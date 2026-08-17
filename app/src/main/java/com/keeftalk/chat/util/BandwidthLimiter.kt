package com.keeftalk.chat.util

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

/**
 * A utility class to throttle data transfer based on a bytes-per-second limit.
 * It uses a simple windowed approach to calculate and apply delays.
 */
class BandwidthLimiter(private val limitBps: Long?) {
    
    private var bytesTransferredInWindow = AtomicLong(0)
    private var windowStartTime = AtomicLong(System.currentTimeMillis())
    
    // We update our measurement window every 500ms for responsiveness
    private val windowSizeMs = 500L

    /**
     * Throttles the current coroutine if the transfer speed exceeds the limit.
     * @param bytesCount The number of bytes just processed/transferred.
     */
    suspend fun throttle(bytesCount: Int) {
        if (limitBps == null || limitBps <= 0) return

        val now = System.currentTimeMillis()
        val currentWindowStart = windowStartTime.get()
        val elapsedTime = now - currentWindowStart
        
        val transferredSoFar = bytesTransferredInWindow.addAndGet(bytesCount.toLong())

        // If we've passed the window size, check if we need to throttle
        if (elapsedTime >= windowSizeMs) {
            val expectedMaxBytes = (limitBps * elapsedTime) / 1000L
            
            if (transferredSoFar > expectedMaxBytes) {
                // Calculate required delay to reach the target Bps
                // Target time = total_bytes / limit_bps
                val targetTimeMs = (transferredSoFar * 1000L) / limitBps
                val delayMs = targetTimeMs - elapsedTime
                
                if (delayMs > 0) {
                    delay(delayMs)
                }
            }
            
            // Reset window for the next period
            windowStartTime.set(System.currentTimeMillis())
            bytesTransferredInWindow.set(0)
        }
    }
}
