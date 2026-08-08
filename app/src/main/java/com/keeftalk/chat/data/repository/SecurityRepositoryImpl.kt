package com.keeftalk.chat.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.data.remote.SecurityEventDto
import com.keeftalk.chat.data.remote.UserSecuritySettingsDto
import com.keeftalk.chat.data.remote.UserSessionDto
import com.keeftalk.chat.domain.model.SecurityEvent
import com.keeftalk.chat.domain.model.SecurityEventType
import com.keeftalk.chat.domain.model.UserSecuritySettings
import com.keeftalk.chat.domain.model.UserSession
import com.keeftalk.chat.domain.repository.SecurityRepository
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import java.security.MessageDigest
import java.util.*

class SecurityRepositoryImpl(
    private val context: Context,
    private val prefs: UserPreferencesRepository,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SecurityRepository {

    private val _securitySettings = MutableStateFlow<UserSecuritySettings?>(null)
    override val securitySettings: Flow<UserSecuritySettings?> = _securitySettings.asStateFlow()

    private val _activeSessions = MutableStateFlow<List<UserSession>>(emptyList())
    override val activeSessions: Flow<List<UserSession>> = _activeSessions.asStateFlow()

    private val _securityEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    override val securityEvents: Flow<List<SecurityEvent>> = _securityEvents.asStateFlow()

    private var sessionStatusChannel: RealtimeChannel? = null
    private var securityDataChannel: RealtimeChannel? = null

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    init {
        // LOAD FROM FAST CACHE IMMEDIATELY (Synchronous-like behavior)
        val cachedBiometric = prefs.isAppLockEnabledFast()
        val cachedTimeout = prefs.getAppLockTimeoutFast()
        _securitySettings.value = UserSecuritySettings(
            userId = "",
            appLockEnabled = cachedBiometric,
            biometricUnlockEnabled = cachedBiometric,
            appLockTimeoutSeconds = cachedTimeout
        )

        com.keeftalk.chat.util.StartupOrchestrator.enqueue(com.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            externalScope.launch {
                val supabase = getSupabase()
                supabase.auth.sessionStatus.collect { status ->
                    if (status is SessionStatus.Authenticated) {
                        val userId = status.session.user?.id ?: return@collect
                        observeSecurityData(userId)
                        registerCurrentSession(userId)
                        observeSessionStatus(userId)
                    }
                }
            }
        }
    }

    private fun observeSessionStatus(userId: String) {
        val deviceId = getDeviceId()
        
        externalScope.launch {
            val supabase = getSupabase()
            sessionStatusChannel?.let {
                try { it.unsubscribe() } catch (_: Exception) {}
                supabase.realtime.removeChannel(it)
            }
            
            val channel = supabase.realtime.channel("session_check_$deviceId")
            sessionStatusChannel = channel
            
            channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "user_sessions"
            }.onEach { _ ->
                // Note: PostgresAction.Delete might not have the full record depending on replica identity
                // But we can check the filter if available or just re-verify session
                Log.w("SecurityRepo", "A session was deleted. Verifying current session...")
                verifyCurrentSession(userId, deviceId)
            }.launchIn(externalScope)

            try {
                channel.subscribe()
            } catch (e: Exception) {
                Log.e("SecurityRepo", "Failed to subscribe to session status", e)
            }
        }
    }

    private suspend fun verifyCurrentSession(userId: String, deviceId: String) {
        try {
            val supabase = getSupabase()
            val session = supabase.postgrest["user_sessions"]
                .select { 
                    filter { 
                        eq("user_id", userId) 
                        eq("device_id", deviceId)
                    }
                }.decodeSingleOrNull<UserSessionDto>()
            
            if (session == null) {
                Log.w("SecurityRepo", "Current device session not found on server. Logging out...")
                supabase.auth.signOut()
            }
        } catch (e: Exception) {
            Log.e("SecurityRepo", "Failed to verify current session", e)
        }
    }

    private suspend fun observeSecurityData(userId: String) {
        val supabase = getSupabase()
        // Initial fetch
        try {
            val settings = supabase.postgrest["user_security_settings"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<UserSecuritySettingsDto>()?.toDomain()
            
            if (settings != null) {
                _securitySettings.value = settings
                // UPDATE CACHE
                prefs.updatePrivacySetting("biometricLockEnabled", settings.biometricUnlockEnabled)
                prefs.updatePrivacySetting("biometricTimeout", settings.appLockTimeoutSeconds)
            }
        } catch (e: Exception) {
            Log.e("SecurityRepo", "Failed to fetch security settings", e)
        }

        // Realtime updates
        try {
            securityDataChannel?.let {
                try { it.unsubscribe() } catch (e: Exception) {}
                supabase.realtime.removeChannel(it)
            }
            
            val channel = supabase.realtime.channel("security_settings_sync")
            securityDataChannel = channel

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "user_security_settings"
            }.onEach { action ->
                val dto = action.decodeRecord<UserSecuritySettingsDto>()
                if (dto.userId == userId) {
                    _securitySettings.value = dto.toDomain()
                }
            }.launchIn(externalScope)
            
            channel.subscribe()
        } catch (e: Exception) {
            Log.e("SecurityRepo", "Realtime security sync error", e)
        }

        getSessions()
        getSecurityEvents()
    }

    private suspend fun registerCurrentSession(userId: String) {
        try {
            val deviceId = getDeviceId()
            val supabase = getSupabase()
            
            // Check if session already exists
            val existing = supabase.postgrest["user_sessions"]
                .select { 
                    filter { 
                        eq("user_id", userId) 
                        eq("device_id", deviceId)
                    }
                }.decodeSingleOrNull<UserSessionDto>()

            val session = UserSessionDto(
                id = existing?.id,
                userId = userId,
                deviceId = deviceId,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                platform = "Android ${Build.VERSION.RELEASE}",
                appVersion = getAppVersion(),
                ipAddress = null,
                country = null,
                lastActive = System.currentTimeMillis(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                isCurrent = true,
                fcmToken = null,
                authMethod = "PASSWORD"
            )
            
            supabase.postgrest["user_sessions"].upsert(session) {
                onConflict = "device_id, user_id"
            }

            if (existing == null) {
                recordSecurityEvent(SecurityEventType.LOGIN, "New device logged in: ${session.deviceName}")
            }
        } catch (e: Exception) {
            Log.e("SecurityRepo", "Failed to register current session", e)
        }
    }

    override suspend fun enableTwoStepVerification(pin: String, recoveryEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            val pinHash = hashPin(pin)
            
            val dto = UserSecuritySettingsDto(
                userId = userId,
                twoFactorEnabled = true,
                pinHash = pinHash,
                recoveryEmail = recoveryEmail,
                updatedAt = System.currentTimeMillis()
            )
            
            supabase.postgrest["user_security_settings"].upsert(dto)
            
            recordSecurityEvent(SecurityEventType.TWO_FACTOR_ENABLED, "Two-step verification enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disableTwoStepVerification(pin: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            
            if (!verifyPin(pin).getOrDefault(false)) {
                return@withContext Result.failure(Exception("Invalid PIN"))
            }

            supabase.postgrest["user_security_settings"].update({
                set("two_factor_enabled", false)
                set<String?>("pin_hash", null)
            }) {
                filter { eq("user_id", userId) }
            }
            
            recordSecurityEvent(SecurityEventType.TWO_FACTOR_DISABLED, "Two-step verification disabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePin(oldPin: String, newPin: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            
            if (!verifyPin(oldPin).getOrDefault(false)) {
                return@withContext Result.failure(Exception("Invalid current PIN"))
            }

            val newPinHash = hashPin(newPin)
            supabase.postgrest["user_security_settings"].update({
                set("pin_hash", newPinHash)
            }) {
                filter { eq("user_id", userId) }
            }
            
            recordSecurityEvent(SecurityEventType.PIN_CHANGED, "Two-step verification PIN changed")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecoveryEmail(email: String, pin: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            
            if (!verifyPin(pin).getOrDefault(false)) {
                return@withContext Result.failure(Exception("Invalid PIN"))
            }

            supabase.postgrest["user_security_settings"].update({
                set("recovery_email", email)
                set("recovery_email_verified", false)
            }) {
                filter { eq("user_id", userId) }
            }
            
            recordSecurityEvent(SecurityEventType.RECOVERY_EMAIL_CHANGED, "Recovery email updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyPin(pin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            val response = supabase.postgrest["user_security_settings"]
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("pin_hash")) {
                    filter { eq("user_id", userId) }
                }.decodeSingleOrNull<Map<String, String>>()
            
            val storedHash = response?.get("pin_hash")
            val result = storedHash == hashPin(pin)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSessions(): Result<List<UserSession>> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            val currentDeviceId = getDeviceId()
            val sessions = supabase.postgrest["user_sessions"]
                .select { 
                    filter { eq("user_id", userId) }
                    order("last_active", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<UserSessionDto>()
                .map { it.toDomain().copy(isCurrent = it.deviceId == currentDeviceId) }
            
            _activeSessions.value = sessions
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logoutSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            // First get the session to know if it's current
            val session = _activeSessions.value.find { it.id == sessionId }
            
            supabase.postgrest["user_sessions"].delete {
                filter { eq("id", sessionId) }
            }
            
            if (session?.isCurrent == true) {
                supabase.auth.signOut()
            }
            
            getSessions()
            recordSecurityEvent(SecurityEventType.DEVICE_REMOVED, "Logged out from device: ${session?.deviceName ?: "Unknown"}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logoutAllOtherSessions(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            val currentDeviceId = getDeviceId()

            supabase.postgrest["user_sessions"].delete {
                filter {
                    eq("user_id", userId)
                    neq("device_id", currentDeviceId)
                }
            }
            getSessions()
            recordSecurityEvent(SecurityEventType.LOGOUT, "Logged out from all other devices")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSecurityEvents(limit: Int, offset: Int): Result<List<SecurityEvent>> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            val events = supabase.postgrest["security_events"]
                .select { 
                    filter { eq("user_id", userId) }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    range(offset.toLong(), (offset + limit).toLong())
                }
                .decodeList<SecurityEventDto>()
                .map { it.toDomain() }
            
            if (offset == 0) _securityEvents.value = events
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportSuspiciousActivity(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            recordSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, "User reported suspicious activity for event $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            getSupabase().auth.updateUser {
                password = newPassword
            }
            recordSecurityEvent(SecurityEventType.PASSWORD_CHANGED, "Password changed")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAppLockSettings(enabled: Boolean, timeoutSeconds: Int, biometricEnabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            supabase.postgrest["user_security_settings"].update({
                set("app_lock_enabled", enabled)
                set("app_lock_timeout_seconds", timeoutSeconds)
                set("biometric_unlock_enabled", biometricEnabled)
            }) {
                filter { eq("user_id", userId) }
            }
            
            val eventType = if (enabled) SecurityEventType.APP_LOCK_ENABLED else SecurityEventType.APP_LOCK_DISABLED
            recordSecurityEvent(eventType, "App lock ${if (enabled) "enabled" else "disabled"}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordSecurityEvent(eventType: SecurityEventType, description: String?, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val event = SecurityEventDto(
                id = "",
                userId = userId,
                eventType = eventType.name,
                description = description,
                deviceId = getDeviceId(),
                ipAddress = null,
                country = null,
                city = null,
                status = status,
                createdAt = System.currentTimeMillis()
            )
            supabase.postgrest["security_events"].insert(event)
            getSecurityEvents()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SecurityRepo", "Failed to record security event", e)
            Result.failure(e)
        }
    }

    override fun shutdown() {
        Log.i("SecurityRepo", "Shutting down SecurityRepository")
        externalScope.cancel()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
