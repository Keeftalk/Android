package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_media")
data class ProfileMediaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "media_type") val mediaType: String, // "AVATAR" or "WALLPAPER"
    @ColumnInfo(name = "original_object_key") val originalObjectKey: String,
    @ColumnInfo(name = "compressed_object_key") val compressedObjectKey: String? = null,
    @ColumnInfo(name = "original_cdn_url") val originalCdnUrl: String,
    @ColumnInfo(name = "compressed_cdn_url") val compressedCdnUrl: String? = null,
    @ColumnInfo(name = "status") val status: String = "ACTIVE", // "ACTIVE", "INACTIVE", "DETENTION"
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "activated_at") val activatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "scheduled_purge_at") val scheduledPurgeAt: Long? = null,
    @ColumnInfo(name = "likes_count") val likesCount: Int = 0,
    @ColumnInfo(name = "is_liked_by_me") val isLikedByMe: Boolean = false
)
