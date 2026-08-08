package com.keeftalk.chat.util

import android.content.Context
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AppLockManager(
    private val context: Context,
    private val securityRepository: SecurityRepository
) {
    private val sharedPrefs = context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE)
    
    fun setLastExitTime() {
        sharedPrefs.edit().putLong("last_exit_time", System.currentTimeMillis()).apply()
    }

    suspend fun shouldLock(): Boolean {
        // Try to get cached settings first to avoid network call
        val settings = securityRepository.securitySettings.first() ?: return false
        if (!settings.appLockEnabled) return false

        val lastExit = sharedPrefs.getLong("last_exit_time", 0L)
        
        // If it's the first time or we don't have a last exit time, lock it
        if (lastExit == 0L) return true

        val timeoutMillis = settings.appLockTimeoutSeconds * 1000L
        val elapsed = System.currentTimeMillis() - lastExit
        
        android.util.Log.d("AppLock", "Checking lock: elapsed=$elapsed, timeout=$timeoutMillis")
        return elapsed >= timeoutMillis
    }

    fun setUnlocked() {
        // Clear last exit time so it doesn't immediately lock again
        sharedPrefs.edit().putLong("last_exit_time", System.currentTimeMillis()).apply()
    }
}
