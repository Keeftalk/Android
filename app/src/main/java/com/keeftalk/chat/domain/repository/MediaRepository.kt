package com.keeftalk.chat.domain.repository

import androidx.paging.PagingData
import com.keeftalk.chat.domain.model.MediaAlbum
import com.keeftalk.chat.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaPagingData(bucketId: String? = null): Flow<PagingData<MediaItem>>
    fun getAlbums(): Flow<List<MediaAlbum>>
}
