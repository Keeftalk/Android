package com.keeftalk.chat.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * A high-performance persistent logger for Keeftalk.
 * Uses a dedicated single-thread executor to ensure non-blocking I/O.
 */
object PersistentLogger {
    private const val TAG = "PersistentLogger"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB
    private val executor = Executors.newSingleThreadExecutor()
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        PerformanceProfiler.startStage("PersistentLogger Initialization")
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) logDir.mkdirs()
        logFile = File(logDir, "keeftalk_startup.log")

        executor.execute {
            if (logFile?.length() ?: 0 > MAX_FILE_SIZE) {
                logFile?.delete()
            }
            log("--- KEEFTALK STARTUP LOG ---")
            PerformanceProfiler.endStage("PersistentLogger Initialization", category = PerformanceProfiler.Category.STORAGE)
        }
    }

    fun log(message: String, category: String = "GENERAL") {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] [$category] $message\n"
        
        // Non-blocking write
        executor.execute {
            try {
                logFile?.let { file ->
                    FileOutputStream(file, true).use { stream ->
                        stream.write(logLine.toByteArray())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to persistent log", e)
            }
        }
    }
}
