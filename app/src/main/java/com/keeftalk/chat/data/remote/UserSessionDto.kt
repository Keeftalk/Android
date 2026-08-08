package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.UserSession
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSessionDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String?,
    val manufacturer: String? = null,
    val model: String? = null,
    val platform: String?,
    @SerialName("app_version") val appVersion: String?,
    @SerialName("ip_address") val ipAddress: String?,
    val country: String?,
    val city: String? = null,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("last_active") val lastActive: Long = 0,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("is_current") val isCurrent: Boolean = false,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("auth_method") val authMethod: String? = null
) {
    fun toDomain() = UserSession(
        id = id ?: "",
        userId = userId,
        deviceId = deviceId,
        deviceName = deviceName,
        manufacturer = manufacturer,
        model = model,
        platform = platform,
        appVersion = appVersion,
        ipAddress = ipAddress,
        country = country,
        city = city,
        lastActive = lastActive,
        createdAt = createdAt,
        isCurrent = isCurrent,
        fcmToken = fcmToken,
        authMethod = authMethod
    )
}
