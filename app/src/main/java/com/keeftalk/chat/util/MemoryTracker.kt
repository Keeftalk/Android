package com.keeftalk.chat.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.keeftalk.chat.BuildConfig

/**
 * Tracks memory allocations and GC events.
 */
object MemoryTracker {
    private val isEnabled = BuildConfig.DEBUG

    fun logHeapSnapshot(context: Context, tag: String) {
        if (!isEnabled) return
        
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        
        val info = "Used: ${usedMemory}MB | Max: ${maxMemory}MB | Available: ${memInfo.availMem / 1024 / 1024}MB | LowMem: ${memInfo.lowMemory}"
        
        PerformanceProfiler.logEvent(
            name = "Memory Snapshot: $tag",
            info = info,
            category = PerformanceProfiler.Category.PROCESS
        )
    }

    fun captureLargeAllocations() {
        if (!isEnabled) return
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024 / 1024
        if (nativeHeap > 50) { 
             PerformanceProfiler.logEvent("High Native Allocation", info = "${nativeHeap}MB", category = PerformanceProfiler.Category.PROCESS)
        }
    }
}
