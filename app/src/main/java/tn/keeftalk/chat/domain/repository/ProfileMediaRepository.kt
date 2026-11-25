package tn.keeftalk.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.MediaLiker
import tn.keeftalk.chat.domain.model.MediaType
import tn.keeftalk.chat.domain.model.ProfileMedia

interface ProfileMediaRepository {
    fun getMediaHistory(userId: String, mediaType: MediaType): Flow<List<ProfileMedia>>
    suspend fun fetchMediaHistoryFromRemote(userId: String, mediaType: MediaType): Result<List<ProfileMedia>>
    suspend fun likeMedia(mediaId: String): Result<Unit>
    suspend fun unlikeMedia(mediaId: String): Result<Unit>
    suspend fun deleteMediaToDetention(mediaId: String): Result<Unit>
    suspend fun restoreMediaFromDetention(mediaId: String): Result<Unit>
    suspend fun setActiveMedia(userId: String, mediaId: String, mediaType: MediaType): Result<Unit>
    suspend fun getMediaLikers(mediaId: String): Result<List<MediaLiker>>
    suspend fun registerProfileMedia(
        userId: String,
        mediaType: MediaType,
        originalObjectKey: String,
        compressedObjectKey: String?,
        originalCdnUrl: String,
        compressedCdnUrl: String?
    ): Result<ProfileMedia>
}
