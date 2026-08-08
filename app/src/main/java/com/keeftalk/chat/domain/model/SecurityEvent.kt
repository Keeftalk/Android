package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SecurityEvent(
    val id: String,
    val userId: String,
    val eventType: SecurityEventType,
    val description: String?,
    val deviceId: String?,
    val ipAddress: String?,
    val country: String?,
    val city: String?,
    val status: String = "SUCCESS",
    val createdAt: Long
)

enum class SecurityEventType {
    ACCOUNT_CREATED,
    LOGIN,
    LOGOUT,
    PIN_CHANGED,
    TWO_FACTOR_ENABLED,
    TWO_FACTOR_DISABLED,
    RECOVERY_EMAIL_CHANGED,
    PASSWORD_CHANGED,
    EMAIL_CHANGED,
    PHONE_CHANGED,
    FAILED_LOGIN_ATTEMPT,
    SUSPICIOUS_ACTIVITY,
    DEVICE_ADDED,
    DEVICE_REMOVED,
    SESSION_EXPIRED,
    APP_LOCK_ENABLED,
    APP_LOCK_DISABLED,
    BIOMETRIC_ENABLED,
    BIOMETRIC_DISABLED
}
