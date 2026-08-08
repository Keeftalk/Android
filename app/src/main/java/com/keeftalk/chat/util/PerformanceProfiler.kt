package com.keeftalk.chat.util

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.util.Log
import com.keeftalk.chat.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Keeftalk Performance Optimization Platform.
 * Instruments frames, coroutines, database, and network to provide deep insights.
 */
object PerformanceProfiler {
    private const val TAG = "PerformanceProfiler"
    private val isEnabled = BuildConfig.DEBUG

    private val processStartTimeNanos = SystemClock.elapsedRealtimeNanos()
    private val processStartTimeMillis = SystemClock.elapsedRealtime()
    
    private val sessions = ConcurrentHashMap<String, ProfilingSession>()
    private val activeStages = ConcurrentHashMap<String, StageStart>()
    private val taskStack = ThreadLocal<Stack<String>>()
    private val currentTraceId = ThreadLocal<String>()
    
    private var appContext: Context? = null
    
    // Global Metrics
    private val compositionMetrics = ConcurrentHashMap<String, CompositionStats>()
    
    enum class Category {
        PROCESS, APP, ANDROID, DI, STORAGE, DATABASE, AUTH, UI, COMPOSE, LAYOUT, DRAW, NETWORK, ENCRYPTION, CHAT, MEDIA, BACKGROUND, GENERAL
    }

    data class ProfilingEvent(
        val name: String,
        val category: Category,
        val startTimeNanos: Long,
        val durationNanos: Long? = null,
        val threadName: String,
        val traceId: String? = null,
        val parentTask: String? = null,
        val info: String? = null,
        val isError: Boolean = false,
        val metadata: Map<String, String>? = null,
    )

    data class CompositionStats(
        val name: String,
        val compositions: AtomicInteger = AtomicInteger(0),
        val recompositions: AtomicInteger = AtomicInteger(0),
        val totalTimeNanos: AtomicLong = AtomicLong(0),
        val skipped: AtomicInteger = AtomicInteger(0)
    )

    class ProfilingSession(val name: String, val category: Category) {
        val events = CopyOnWriteArrayList<ProfilingEvent>()
        val startTimeNanos = SystemClock.elapsedRealtimeNanos()
        var endTimeNanos: Long? = null
        val traceId: String = UUID.randomUUID().toString().take(8)
        
        fun log(event: ProfilingEvent) {
            events.add(event)
        }

        fun stop() {
            endTimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    private data class StageStart(val timeNanos: Long, val cpuMs: Long, val parent: String?, val traceId: String?)

    /**
     * Initialize the profiler.
     */
    fun init(context: Context) {
        if (!isEnabled) return
        this.appContext = context.applicationContext
        startSession("STARTUP", Category.PROCESS)
        
        val processStart = Process.getStartElapsedRealtime()
        val coldStartDiff = processStartTimeMillis - processStart
        val startType = when {
            coldStartDiff < 500 -> "Cold Start"
            coldStartDiff < 2000 -> "Warm Start"
            else -> "Hot Start (Cached Process)"
        }
        logEvent("Start Type Detected", info = startType, category = Category.PROCESS)
    }

    fun startSession(name: String, category: Category) {
        if (!isEnabled) return
        val session = ProfilingSession(name, category)
        sessions[name] = session
        currentTraceId.set(session.traceId)
        logEvent("Session Started: $name", category = category)
    }

    fun endSession(name: String) {
        if (!isEnabled) return
        val session = sessions[name] ?: return
        session.stop()
        printReport(session)
        currentTraceId.set(null)
    }

    fun logEvent(
        name: String,
        info: String? = null,
        category: Category = Category.GENERAL,
        isError: Boolean = false,
        metadata: Map<String, String>? = null
    ) {
        if (!isEnabled) return
        val now = SystemClock.elapsedRealtimeNanos()
        val parent = taskStack.get()?.peekOrNull()
        val event = ProfilingEvent(
            name = name,
            category = category,
            startTimeNanos = now,
            threadName = Thread.currentThread().name,
            traceId = currentTraceId.get(),
            parentTask = parent,
            info = info,
            isError = isError,
            metadata = metadata
        )
        
        // Add to active sessions
        sessions.values.forEach { it.log(event) }
    }

    fun startStage(name: String) {
        if (!isEnabled) return
        val stack = taskStack.get() ?: Stack<String>().also { taskStack.set(it) }
        val parent = stack.peekOrNull()
        
        val now = SystemClock.elapsedRealtimeNanos()
        val cpuStart = SystemClock.currentThreadTimeMillis()
        
        activeStages[getStageKey(name)] = StageStart(now, cpuStart, parent, currentTraceId.get())
        stack.push(name)
    }

    fun endStage(
        name: String,
        info: String? = null,
        isError: Boolean = false,
        category: Category = Category.GENERAL,
        metadata: Map<String, String>? = null
    ) {
        if (!isEnabled) return
        val stack = taskStack.get() ?: return
        
        // Defensive: Check if stack is empty or mismatch
        if (stack.isEmpty()) {
            return // Silently ignore unmatched endStage in production-like environments
        }

        if (stack.peek() != name) {
            // We only pop if it matches, otherwise we wait for the correct closer 
            // or let the session end cleanup. This prevents "Unordered endStage" spam 
            // during complex coroutine suspensions.
            return 
        }
        
        val end = SystemClock.elapsedRealtimeNanos()
        
        val startData = activeStages.remove(getStageKey(name)) ?: return
        stack.pop()

        val event = ProfilingEvent(
            name = name,
            category = category,
            startTimeNanos = startData.timeNanos,
            durationNanos = end - startData.timeNanos,
            threadName = Thread.currentThread().name,
            traceId = startData.traceId,
            parentTask = startData.parent,
            info = info,
            isError = isError,
            metadata = metadata
        )
        
        sessions.values.forEach { it.log(event) }
    }

    fun trackComposition(name: String, durationNanos: Long, isRecomposition: Boolean, isSkipped: Boolean = false) {
        if (!isEnabled) return
        val stats = compositionMetrics.getOrPut(name) { CompositionStats(name) }
        if (isSkipped) {
            stats.skipped.incrementAndGet()
        } else {
            if (isRecomposition) stats.recompositions.incrementAndGet() else stats.compositions.incrementAndGet()
            stats.totalTimeNanos.addAndGet(durationNanos)
        }
    }

    private fun getStageKey(name: String) = "${Thread.currentThread().id}_$name"
    private fun <T> Stack<T>.peekOrNull(): T? = if (isEmpty()) null else peek()

    fun printReport() {
        endSession("STARTUP")
    }

    fun getAllSessions(): List<ProfilingSession> = sessions.values.toList()
    
    fun getSession(name: String): ProfilingSession? = sessions[name]

    fun getCompositionStats(): List<CompositionStats> = compositionMetrics.values.toList()

    fun getReportFiles(): List<File> {
        val dir = File(appContext?.filesDir, "performance_reports")
        return dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun printReport(session: ProfilingSession) {
        val report = ReportGenerator.generate(session, appContext)
        Log.i(TAG, report)
        
        saveReport(session.name, report)
        exportToJson(session)
    }

    private fun saveReport(name: String, report: String) {
        appContext?.let { context ->
            try {
                val dir = File(context.filesDir, "performance_reports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "report_${name}_${System.currentTimeMillis()}.md")
                file.writeText(report)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save report", e)
            }
        }
    }

    private fun exportToJson(session: ProfilingSession) {
        appContext?.let { context ->
            try {
                val root = JSONObject()
                root.put("session", session.name)
                root.put("traceId", session.traceId)
                root.put("duration_ms", ((session.endTimeNanos ?: 0L) - session.startTimeNanos) / 1_000_000.0)
                
                val eventsArray = JSONArray()
                session.events.forEach { event ->
                    val obj = JSONObject()
                    obj.put("name", event.name)
                    obj.put("category", event.category.name)
                    obj.put("duration_ms", (event.durationNanos ?: 0L) / 1_000_000.0)
                    obj.put("thread", event.threadName)
                    eventsArray.put(obj)
                }
                root.put("events", eventsArray)

                val dir = File(context.filesDir, "performance_reports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "report_${session.name}_${System.currentTimeMillis()}.json")
                file.writeText(root.toString(2))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export JSON", e)
            }
        }
    }

    /**
     * Helper for Report Generation.
     */
    private object ReportGenerator {
        fun generate(session: ProfilingSession, context: Context?): String {
            val sb = StringBuilder()
            val totalDurationMs = ((session.endTimeNanos ?: SystemClock.elapsedRealtimeNanos()) - session.startTimeNanos) / 1_000_000.0
            
            sb.append("\n" + "=".repeat(20) + " KEEFTALK PERFORMANCE REPORT: ${session.name} " + "=".repeat(20) + "\n")
            sb.append("TRACE ID: ${session.traceId}\n")
            sb.append("TOTAL DURATION: %.2f ms\n".format(totalDurationMs))
            
            // Comparison
            ComparisonEngine.compareToPrevious(session, context)?.let { diff ->
                sb.append("COMPARISON: $diff\n")
            }

            // Root Cause Analysis
            sb.append("\n[ROOT CAUSE ANALYSIS]\n")
            val categories = session.events.groupBy { it.category }
            val totalTimeByCategory = categories.mapValues { (_, events) ->
                events.sumOf { it.durationNanos ?: 0L } / 1_000_000.0
            }.toList().sortedByDescending { it.second }

            totalTimeByCategory.take(5).forEach { (cat, time) ->
                val percent = (time / totalDurationMs) * 100
                sb.append("%-15s: %6.2f ms (%5.1f%%)\n".format(cat.name, time, percent))
            }

            // Recommendations
            sb.append("\n[RECOMMENDATIONS]\n")
            val slowEvents = session.events.filter { (it.durationNanos ?: 0L) > 50_000_000 } // > 50ms
            if (slowEvents.isNotEmpty()) {
                slowEvents.forEach { sb.append("• Slow Operation: '${it.name}' took %.2f ms. Consider moving to background or optimizing.\n".format(it.durationNanos!! / 1_000_000.0)) }
            }
            
            if (totalTimeByCategory.firstOrNull()?.first == Category.UI) {
                sb.append("• UI Thread is the primary bottleneck. Check for expensive layouts or Main-thread work.\n")
            }
            if (totalTimeByCategory.any { it.first == Category.DATABASE && it.second > 100 }) {
                sb.append("• High Database activity detected. Consider batching queries or adding indexes.\n")
            }

            // Waterfall
            sb.append("\n[EVENT TIMELINE]\n")
            session.events.sortedBy { it.startTimeNanos }.forEach { event ->
                val offset = (event.startTimeNanos - session.startTimeNanos) / 1_000_000.0
                val duration = (event.durationNanos ?: 0L) / 1_000_000.0
                sb.append("%8.2f ms | %-30s | %.2f ms\n".format(offset, event.name.take(30), duration))
            }

            sb.append("\n" + "=".repeat(60 + session.name.length) + "\n")
            return sb.toString()
        }
    }

    private object ComparisonEngine {
        fun compareToPrevious(current: ProfilingSession, context: Context?): String? {
            try {
                val dir = File(context?.filesDir, "performance_reports")
                val files = dir.listFiles { f -> f.name.contains(current.name) && f.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() } ?: return null
                
                if (files.size < 2) return null
                val previousFile = files[1]
                val json = JSONObject(previousFile.readText())
                val prevDuration = json.getDouble("duration_ms")
                val currDuration = ((current.endTimeNanos ?: 0L) - current.startTimeNanos) / 1_000_000.0
                
                val diff = currDuration - prevDuration
                val percent = (diff / prevDuration) * 100
                
                return if (diff > 0) {
                    "Regression: +%.2f ms (+%.1f%%)".format(diff, percent)
                } else {
                    "Improvement: %.2f ms (%.1f%% faster)".format(-diff, -percent)
                }
            } catch (e: Exception) {
                return null
            }
        }
    }
}
