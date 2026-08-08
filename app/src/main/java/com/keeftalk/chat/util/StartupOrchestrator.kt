package com.keeftalk.chat.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Orchestrates the three-tier startup system for Keeftalk.
 */
object StartupOrchestrator {
    private const val TAG = "StartupOrchestrator"
    private const val FAILSAFE_RENDER_TIME = 2500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val postRenderHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isStarted = false
    private var outstandingCriticalRenderEvents = 0

    enum class Tier {
        TIER_1_BLOCKING,     // Critical Path (Main Thread)
        TIER_2_BACKGROUND,   // Immediate Background
        TIER_3_POST_RENDER   // After UI is interactive
    }

    private val tierTasks = mutableMapOf<Tier, CopyOnWriteArrayList<suspend () -> Unit>>()
    private val completedTiers = mutableSetOf<Tier>()

    init {
        Tier.entries.forEach { tierTasks[it] = CopyOnWriteArrayList() }
    }

    fun onApplicationCreate() {
        PerformanceProfiler.startStage("Tier 1: Application Initialization")
        executeTierSync(Tier.TIER_1_BLOCKING)
        PerformanceProfiler.endStage("Tier 1: Application Initialization", category = PerformanceProfiler.Category.PROCESS)
    }

    fun startTier2() {
        scope.launch(KeeftalkExecutors.BOUNDED_IO.asCoroutineDispatcher()) {
            PerformanceProfiler.startStage("Tier 2: Background Hydration")
            executeTier(Tier.TIER_2_BACKGROUND)
            PerformanceProfiler.endStage("Tier 2: Background Hydration", category = PerformanceProfiler.Category.BACKGROUND)
        }
    }

    fun onCriticalRenderEventStart() {
        if (outstandingCriticalRenderEvents == 0 && !completedTiers.contains(Tier.TIER_3_POST_RENDER)) {
            PerformanceProfiler.logEvent("Render Start", category = PerformanceProfiler.Category.UI)
            
            postRenderHandler.removeCallbacksAndMessages(null)
            postRenderHandler.postDelayed({
                Log.w(TAG, "Reached failsafe for post-render! App start taking too long or event missed.")
                executePostRender()
            }, FAILSAFE_RENDER_TIME)
        }
        outstandingCriticalRenderEvents++
    }

    fun onCriticalRenderEventEnd() {
        outstandingCriticalRenderEvents = (outstandingCriticalRenderEvents - 1).coerceAtLeast(0)

        if (outstandingCriticalRenderEvents == 0 && !completedTiers.contains(Tier.TIER_3_POST_RENDER)) {
            PerformanceProfiler.logEvent("First Render Finished", category = PerformanceProfiler.Category.UI)
            
            postRenderHandler.removeCallbacksAndMessages(null)
            executePostRender()
        }
    }

    private fun executePostRender() {
        if (isStarted) return
        isStarted = true
        scope.launch(KeeftalkExecutors.BOUNDED.asCoroutineDispatcher()) {
            PerformanceProfiler.startStage("Tier 3: Post-Render Tasks")
            executeTier(Tier.TIER_3_POST_RENDER)
            PerformanceProfiler.endStage("Tier 3: Post-Render Tasks", category = PerformanceProfiler.Category.BACKGROUND)
            
            // Mark startup as interactive after Tier 3 starts
            PerformanceProfiler.logEvent("App Interactive", category = PerformanceProfiler.Category.UI)
            PerformanceProfiler.printReport()
        }
    }

    private fun executeTierSync(tier: Tier) {
        val tasks = tierTasks[tier] ?: return
        while (tasks.isNotEmpty()) {
            val task = try { tasks.removeAt(0) } catch (e: Exception) { null } ?: break
            // Execute non-blocking tasks. Suspend tasks in Tier 1 should be rare and are launched on Main.immediate
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    task()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Tier ${tier.name} (Async-Sync)", e)
                }
            }
        }
        completedTiers.add(tier)
    }

    private suspend fun executeTier(tier: Tier) {
        val tasks = tierTasks[tier] ?: return
        Log.d(TAG, "Executing Tier ${tier.name} with ${tasks.size} tasks")
        coroutineScope {
            while (tasks.isNotEmpty()) {
                val task = try { tasks.removeAt(0) } catch (e: Exception) { null } ?: break
                launch {
                    try {
                        task()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in Tier ${tier.name}", e)
                    }
                }
            }
        }
        completedTiers.add(tier)
        Log.d(TAG, "Tier ${tier.name} execution completed")
    }

    fun enqueue(tier: Tier = Tier.TIER_2_BACKGROUND, task: suspend () -> Unit) {
        if (completedTiers.contains(tier)) {
            val dispatcher = when(tier) {
                Tier.TIER_1_BLOCKING -> Dispatchers.Main
                Tier.TIER_2_BACKGROUND -> KeeftalkExecutors.BOUNDED_IO.asCoroutineDispatcher()
                Tier.TIER_3_POST_RENDER -> KeeftalkExecutors.BOUNDED.asCoroutineDispatcher()
            }
            scope.launch(dispatcher) { task() }
        } else {
            tierTasks[tier]?.add(task)
        }
    }
}
