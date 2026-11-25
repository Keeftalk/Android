package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.ProfileMediaEntity
import tn.keeftalk.chat.data.local.entities.ProfileMediaLikeEntity

@Dao
interface ProfileMediaDao {
    @Query("SELECT * FROM profile_media WHERE user_id = :userId AND media_type = :mediaType AND status != 'DETENTION' ORDER BY activated_at DESC")
    fun getMediaHistory(userId: String, mediaType: String): Flow<List<ProfileMediaEntity>>

    @Query("SELECT * FROM profile_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): ProfileMediaEntity?

    @Query("SELECT * FROM profile_media WHERE user_id = :userId AND media_type = :mediaType AND status != 'DETENTION' ORDER BY activated_at DESC LIMIT 1")
    suspend fun getActiveMedia(userId: String, mediaType: String): ProfileMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMedia(mediaList: List<ProfileMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMedia(media: ProfileMediaEntity)

    @Query("UPDATE profile_media SET is_active = CASE WHEN id = :mediaId THEN 1 ELSE 0 END WHERE user_id = :userId AND media_type = :mediaType")
    suspend fun setActiveMedia(userId: String, mediaType: String, mediaId: String)

    @Query("UPDATE profile_media SET status = 'DETENTION', is_active = 0, deleted_at = :deletedAt WHERE id = :mediaId")
    suspend fun markAsDetained(mediaId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE profile_media SET status = 'INACTIVE', deleted_at = NULL WHERE id = :mediaId")
    suspend fun markAsRestored(mediaId: String)

    @Query("DELETE FROM profile_media WHERE id = :mediaId")
    suspend fun deleteMedia(mediaId: String)

    @Query("UPDATE profile_media SET likes_count = :count, is_liked_by_me = :isLiked WHERE id = :mediaId")
    suspend fun updateLikesInfo(mediaId: String, count: Int, isLiked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: ProfileMediaLikeEntity)

    @Query("DELETE FROM profile_media_likes WHERE media_id = :mediaId AND user_id = :userId")
    suspend fun deleteLike(mediaId: String, userId: String)

    @Query("SELECT COUNT(*) FROM profile_media_likes WHERE media_id = :mediaId")
    suspend fun getLikesCount(mediaId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM profile_media_likes WHERE media_id = :mediaId AND user_id = :userId)")
    suspend fun isLikedByUser(mediaId: String, userId: String): Boolean
}
