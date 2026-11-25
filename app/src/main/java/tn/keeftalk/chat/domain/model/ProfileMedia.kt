package tn.keeftalk.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    AVATAR,
    WALLPAPER
}

@Serializable
enum class MediaStatus {
    ACTIVE,
    INACTIVE,
    DETENTION
}

@Serializable
data class ProfileMedia(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_type") val mediaType: MediaType,
    @SerialName("original_object_key") val originalObjectKey: String,
    @SerialName("compressed_object_key") val compressedObjectKey: String? = null,
    @SerialName("original_cdn_url") val originalCdnUrl: String,
    @SerialName("compressed_cdn_url") val compressedCdnUrl: String? = null,
    val status: MediaStatus = MediaStatus.ACTIVE,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("activated_at") val activatedAt: Long = System.currentTimeMillis(),
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("scheduled_purge_at") val scheduledPurgeAt: Long? = null,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val likers: List<MediaLiker> = emptyList()
)

@Serializable
data class MediaLiker(
    @SerialName("user_id") val userId: String,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val isContact: Boolean = false
)
