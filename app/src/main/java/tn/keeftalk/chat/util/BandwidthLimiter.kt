package tn.keeftalk.chat.util

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

/**
 * A utility class to throttle data transfer based on a bytes-per-second limit.
 * It uses a simple windowed approach to calculate and apply delays.
 */
class BandwidthLimiter(private val limitBps: Long?) {
    
    private var bytesTransferredInWindow = AtomicLong(0)
    private var windowStartTime = AtomicLong(System.currentTimeMillis())

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

        val targetTimeMs = (transferredSoFar * 1000L) / limitBps
        if (elapsedTime < targetTimeMs) {
            val delayMs = targetTimeMs - elapsedTime
            if (delayMs > 0) {
                delay(delayMs)
            }
        }
    }
}
