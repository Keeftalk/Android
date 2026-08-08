package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val id: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String?,
    val manufacturer: String?,
    val model: String?,
    val platform: String?,
    val appVersion: String?,
    val ipAddress: String?,
    val country: String?,
    val city: String?,
    val lastActive: Long,
    val createdAt: Long,
    val isCurrent: Boolean,
    val fcmToken: String?,
    val authMethod: String?
)
