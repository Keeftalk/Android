package com.keeftalk.chat.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class BandwidthLimiterTest {

    @Test
    fun testThrottling() = runBlocking {
        // Limit: 1 MB/s = 1,048,576 Bps
        val limitBps = 1024L * 1024L
        val limiter = BandwidthLimiter(limitBps)
        
        // Transfer 2 MB
        val dataSize = 2 * 1024 * 1024
        val chunkSize = 100 * 1024 // 100 KB chunks
        
        val timeTaken = measureTimeMillis {
            var bytesSent = 0
            while (bytesSent < dataSize) {
                limiter.throttle(chunkSize)
                bytesSent += chunkSize
            }
        }
        
        // Expected time: ~2 seconds (2 MB / 1 MB/s)
        // Allowing some margin for overhead and windowing
        println("Time taken for 2MB with 1MB/s limit: ${timeTaken}ms")
        assertTrue("Throttling should take at least 1500ms, got ${timeTaken}ms", timeTaken >= 1500)
        assertTrue("Throttling should take no more than 3000ms, got ${timeTaken}ms", timeTaken <= 3000)
    }

    @Test
    fun testUnlimited() = runBlocking {
        val limiter = BandwidthLimiter(null)
        
        val dataSize = 5 * 1024 * 1024
        val chunkSize = 512 * 1024
        
        val timeTaken = measureTimeMillis {
            var bytesSent = 0
            while (bytesSent < dataSize) {
                limiter.throttle(chunkSize)
                bytesSent += chunkSize
            }
        }
        
        // Should be almost instantaneous
        println("Time taken for 5MB with Unlimited: ${timeTaken}ms")
        assertTrue("Unlimited should be fast, got ${timeTaken}ms", timeTaken < 500)
    }
}
