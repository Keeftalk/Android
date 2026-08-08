package com.keeftalk.chat.domain.model

import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String = "",
    @SerialName("full_name")
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val bio: String? = null,
    val country: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("join_date")
    val joinDate: Long = 0L,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("last_seen")
    val lastSeen: Long = 0L,
    val privacy: PrivacySettings = PrivacySettings(),
    @SerialName("notification_settings")
    val notifications: com.keeftalk.chat.data.prefs.NotificationPreferences = com.keeftalk.chat.data.prefs.NotificationPreferences()
)

@Serializable
data class PrivacySettings(
    @SerialName("avatar_visibility")
    val avatarVisibility: String = "EVERYONE",
    @SerialName("cover_visibility")
    val coverVisibility: String = "EVERYONE",
    @SerialName("phone_visibility")
    val phoneVisibility: String = "EVERYONE",
    @SerialName("email_visibility")
    val emailVisibility: String = "EVERYONE",
    @SerialName("bio_visibility")
    val bioVisibility: String = "EVERYONE",
    @SerialName("last_seen_visibility")
    val lastSeenVisibility: String = "EVERYONE",
    @SerialName("online_status_visibility")
    val onlineStatusVisibility: String = "EVERYONE"
)
