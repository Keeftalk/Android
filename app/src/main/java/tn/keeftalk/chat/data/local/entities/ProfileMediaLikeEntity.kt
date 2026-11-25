package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "profile_media_likes",
    primaryKeys = ["media_id", "user_id"]
)
data class ProfileMediaLikeEntity(
    @ColumnInfo(name = "media_id") val mediaId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
