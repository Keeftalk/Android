package com.keeftalk.chat.data.auth

import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log

class DataStoreSessionManager(
    private val prefs: UserPreferencesRepository
) : SessionManager {
    private var cachedSession: UserSession? = null
    private var isLoaded = false

    override suspend fun saveSession(session: UserSession) {
        cachedSession = session
        isLoaded = true
        prefs.updateSessionData(Json.encodeToString(session))
    }

    override suspend fun loadSession(): UserSession {
        if (isLoaded && cachedSession != null) {
            return cachedSession!!
        }
        
        com.keeftalk.chat.util.PerformanceProfiler.startStage("Auth: Load Session from Disk")
        
        var success = false
        val data = try {
            val d = kotlinx.coroutines.withTimeout(3000) {
                prefs.userPreferencesFlow.first().sessionData
            }
            success = true
            d
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to read from DataStore (timeout or error)", e)
            null
        }

        val session = if (data != null) {
            try {
                Json.decodeFromString<UserSession>(data)
            } catch (e: Exception) {
                Log.e("SessionManager", "Failed to decode session", e)
                null
            }
        } else null
        
        // Only set isLoaded = true if we actually successfully queried the preference (even if it was empty)
        if (success) {
            cachedSession = session
            isLoaded = true
        }
        
        com.keeftalk.chat.util.PerformanceProfiler.endStage("Auth: Load Session from Disk", if (session != null) "Found" else "Not found")
        
        return session ?: throw IllegalStateException("No session found in storage")
    }

    override suspend fun deleteSession() {
        cachedSession = null
        isLoaded = true
        prefs.updateSessionData(null)
    }
}
