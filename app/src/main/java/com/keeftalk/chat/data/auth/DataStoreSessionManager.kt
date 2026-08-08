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
        if (isLoaded) {
            return cachedSession ?: throw IllegalStateException("No session cached")
        }
        
        com.keeftalk.chat.util.PerformanceProfiler.startStage("Auth: Load Session from Disk")
        val data = try {
            kotlinx.coroutines.withTimeout(3000) {
                prefs.userPreferencesFlow.first().sessionData
            }
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
        
        cachedSession = session
        isLoaded = true
        com.keeftalk.chat.util.PerformanceProfiler.endStage("Auth: Load Session from Disk", if (session != null) "Found" else "Not found")
        
        return session ?: throw IllegalStateException("No session found")
    }

    override suspend fun deleteSession() {
        cachedSession = null
        isLoaded = true
        prefs.updateSessionData(null)
    }
}
