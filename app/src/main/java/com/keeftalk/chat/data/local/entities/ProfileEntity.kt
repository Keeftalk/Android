package com.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keeftalk.chat.domain.model.PrivacySettings
import com.keeftalk.chat.domain.model.Profile

@Entity(
    tableName = "profiles"
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "full_name") val fullName: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "bio") val bio: String?,
    @ColumnInfo(name = "country") val country: String?,
    @ColumnInfo(name = "country_code") val countryCode: String?,
    @ColumnInfo(name = "join_date") val joinDate: Long,
    @ColumnInfo(name = "is_verified") val isVerified: Boolean,
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
    @ColumnInfo(name = "views_count") val viewsCount: Long,
    @ColumnInfo(name = "plan_type") val planType: String,
    @ColumnInfo(name = "storage_limit") val storageLimit: Long,
    @ColumnInfo(name = "storage_used") val storageUsed: Long,
    @ColumnInfo(name = "is_family_owner") val isFamilyOwner: Boolean,
    @ColumnInfo(name = "family_id") val familyId: String?,
    // Privacy settings flattened
    @ColumnInfo(name = "avatar_visibility") val avatarVisibility: String,
    @ColumnInfo(name = "cover_visibility") val coverVisibility: String,
    @ColumnInfo(name = "phone_visibility") val phoneVisibility: String,
    @ColumnInfo(name = "email_visibility") val emailVisibility: String,
    @ColumnInfo(name = "bio_visibility") val bioVisibility: String,
    @ColumnInfo(name = "last_seen_visibility") val lastSeenVisibility: String,
    @ColumnInfo(name = "online_status_visibility") val onlineStatusVisibility: String,
    @ColumnInfo(name = "notification_settings_json") val notificationSettingsJson: String
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
    viewsCount = viewsCount,
    planType = try { com.keeftalk.chat.domain.model.SubscriptionPlan.valueOf(planType) } catch (e: Exception) { com.keeftalk.chat.domain.model.SubscriptionPlan.FREE },
    storageLimit = storageLimit,
    storageUsed = storageUsed,
    isFamilyOwner = isFamilyOwner,
    familyId = familyId,
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
    viewsCount = viewsCount,
    planType = planType.name,
    storageLimit = storageLimit,
    storageUsed = storageUsed,
    isFamilyOwner = isFamilyOwner,
    familyId = familyId,
    avatarVisibility = privacy.avatarVisibility,
    coverVisibility = privacy.coverVisibility,
    phoneVisibility = privacy.phoneVisibility,
    emailVisibility = privacy.emailVisibility,
    bioVisibility = privacy.bioVisibility,
    lastSeenVisibility = privacy.lastSeenVisibility,
    onlineStatusVisibility = privacy.onlineStatusVisibility,
    notificationSettingsJson = kotlinx.serialization.json.Json.encodeToString(com.keeftalk.chat.data.prefs.NotificationPreferences.serializer(), notifications)
)
