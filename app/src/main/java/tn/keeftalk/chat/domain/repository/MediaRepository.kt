package tn.keeftalk.chat.domain.repository

import androidx.paging.PagingData
import tn.keeftalk.chat.domain.model.MediaAlbum
import tn.keeftalk.chat.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaPagingData(bucketId: String? = null): Flow<PagingData<MediaItem>>
    fun getAlbums(): Flow<List<MediaAlbum>>
}
