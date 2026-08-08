package com.keeftalk.chat.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchProfiled(
    name: String,
    category: PerformanceProfiler.Category = PerformanceProfiler.Category.BACKGROUND,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val scheduledTime = System.nanoTime()
    return launch(context, start) {
        val startTime = System.nanoTime()
        val delay = (startTime - scheduledTime) / 1_000_000.0
        
        PerformanceProfiler.logEvent("$name Scheduled", info = "Queue Delay: %.2f ms".format(delay), category = category)
        
        PerformanceProfiler.startStage(name)
        try {
            block()
        } finally {
            PerformanceProfiler.endStage(name, category = category)
        }
    }
}
