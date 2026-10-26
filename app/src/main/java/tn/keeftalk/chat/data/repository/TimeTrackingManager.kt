package tn.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import tn.keeftalk.chat.di.AppModule
import java.time.LocalDate

class TimeTrackingManager private constructor(context: Context) {

    private val repository = TimeTrackingRepository(
        context.applicationContext,
        AppModule.provideDatabase(context.applicationContext).timeTrackingDao()
    )

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentFeature: String = "SETTINGS"
    private var lastTickTimestamp: Long = System.currentTimeMillis()
    private var accumulatedSeconds: Long = 0L
    private var isAppInForeground: Boolean = true
    private var tickerJob: Job? = null

    init {
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = managerScope.launch {
            while (isActive) {
                delay(10_000L) // Tick & flush every 10 seconds
                flushCurrentTime()
            }
        }
    }

    @Synchronized
    fun setCurrentFeature(featureKey: String) {
        val normalized = featureKey.uppercase()
        if (currentFeature != normalized) {
            flushCurrentTime()
            currentFeature = normalized
            lastTickTimestamp = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun onAppForegroundStateChanged(isForeground: Boolean) {
        if (isAppInForeground != isForeground) {
            flushCurrentTime()
            isAppInForeground = isForeground
            lastTickTimestamp = System.currentTimeMillis()
        }
    }

    @Synchronized
    fun flushCurrentTime() {
        if (!isAppInForeground) {
            lastTickTimestamp = System.currentTimeMillis()
            return
        }
        val now = System.currentTimeMillis()
        val elapsedMillis = (now - lastTickTimestamp).coerceAtMost(15_000L) // Cap at 15s to prevent sleep/resume spikes (e.g. 25h Friday bug)
        lastTickTimestamp = now
        if (elapsedMillis <= 0) return

        val secondsToAdd = elapsedMillis / 1000L
        if (secondsToAdd > 0) {
            val targetFeature = currentFeature
            managerScope.launch {
                runCatching {
                    repository.addActiveTimeSeconds(targetFeature, secondsToAdd, LocalDate.now())
                }.onFailure { e ->
                    Log.e("TimeTrackingManager", "Error recording time for $targetFeature", e)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TimeTrackingManager? = null

        fun getInstance(context: Context): TimeTrackingManager {
            return instance ?: synchronized(this) {
                instance ?: TimeTrackingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
