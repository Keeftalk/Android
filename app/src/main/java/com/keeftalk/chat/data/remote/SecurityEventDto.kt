package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.SecurityEvent
import com.keeftalk.chat.domain.model.SecurityEventType
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecurityEventDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("event_type") val eventType: String,
    val description: String?,
    @SerialName("device_id") val deviceId: String?,
    @SerialName("ip_address") val ipAddress: String?,
    val country: String?,
    val city: String? = null,
    val status: String = "SUCCESS",
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at") val createdAt: Long = 0
) {
    fun toDomain() = SecurityEvent(
        id = id,
        userId = userId,
        eventType = try { SecurityEventType.valueOf(eventType) } catch (e: Exception) { SecurityEventType.SUSPICIOUS_ACTIVITY },
        description = description,
        deviceId = deviceId,
        ipAddress = ipAddress,
        country = country,
        city = city,
        status = status,
        createdAt = createdAt
    )
}
