package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.SecurityEvent
import com.keeftalk.chat.domain.model.UserSecuritySettings
import com.keeftalk.chat.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    val securitySettings: Flow<UserSecuritySettings?>
    val activeSessions: Flow<List<UserSession>>
    val securityEvents: Flow<List<SecurityEvent>>

    suspend fun enableTwoStepVerification(pin: String, recoveryEmail: String?): Result<Unit>
    suspend fun disableTwoStepVerification(pin: String): Result<Unit>
    suspend fun changePin(oldPin: String, newPin: String): Result<Unit>
    suspend fun updateRecoveryEmail(email: String, pin: String): Result<Unit>
    suspend fun verifyPin(pin: String): Result<Boolean>

    suspend fun getSessions(): Result<List<UserSession>>
    suspend fun logoutSession(sessionId: String): Result<Unit>
    suspend fun logoutAllOtherSessions(): Result<Unit>

    suspend fun getSecurityEvents(limit: Int = 20, offset: Int = 0): Result<List<SecurityEvent>>
    suspend fun reportSuspiciousActivity(eventId: String): Result<Unit>

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    // App Lock
    suspend fun updateAppLockSettings(enabled: Boolean, timeoutSeconds: Int, biometricEnabled: Boolean): Result<Unit>
    suspend fun recordSecurityEvent(eventType: com.keeftalk.chat.domain.model.SecurityEventType, description: String?, status: String = "SUCCESS"): Result<Unit>
    fun shutdown()
}
