package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keeftalk.chat.domain.model.PrivacySettings
import com.keeftalk.chat.domain.model.Profile

@Entity(
    tableName = "profiles"
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val avatarUrl: String?,
    val coverUrl: String?,
    val bio: String?,
    val country: String?,
    val countryCode: String?,
    val joinDate: Long,
    val isVerified: Boolean,
    val lastSeen: Long,
    // Privacy settings flattened
    val avatarVisibility: String,
    val coverVisibility: String,
    val phoneVisibility: String,
    val emailVisibility: String,
    val bioVisibility: String,
    val lastSeenVisibility: String,
    val onlineStatusVisibility: String,
    val notificationSettingsJson: String
)

fun ProfileEntity.toDomain() = Profile(
    id = id,
    username = username,
    fullName = fullName,
    email = email,
    phone = phone,
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
    bio = bio,
    country = country,
    countryCode = countryCode,
    joinDate = joinDate,
    isVerified = isVerified,
    lastSeen = lastSeen,
    privacy = PrivacySettings(
        avatarVisibility = avatarVisibility,
        coverVisibility = coverVisibility,
        phoneVisibility = phoneVisibility,
        emailVisibility = emailVisibility,
        bioVisibility = bioVisibility,
        lastSeenVisibility = lastSeenVisibility,
        onlineStatusVisibility = onlineStatusVisibility
    ),
    notifications = try {
        kotlinx.serialization.json.Json.decodeFromString(notificationSettingsJson)
    } catch (e: Exception) {
        com.keeftalk.chat.data.prefs.NotificationPreferences()
    }
)

fun Profile.toEntity() = ProfileEntity(
    id = id,
    username = username,
    fullName = fullName,
    email = email,
    phone = phone,
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
    bio = bio,
    country = country,
    countryCode = countryCode,
    joinDate = joinDate,
    isVerified = isVerified,
    lastSeen = lastSeen,
    avatarVisibility = privacy.avatarVisibility,
    coverVisibility = privacy.coverVisibility,
    phoneVisibility = privacy.phoneVisibility,
    emailVisibility = privacy.emailVisibility,
    bioVisibility = privacy.bioVisibility,
    lastSeenVisibility = privacy.lastSeenVisibility,
    onlineStatusVisibility = privacy.onlineStatusVisibility,
    notificationSettingsJson = kotlinx.serialization.json.Json.encodeToString(com.keeftalk.chat.data.prefs.NotificationPreferences.serializer(), notifications)
)
