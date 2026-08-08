package com.keeftalk.chat.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val duration: Long? = null, // For videos
    val bucketId: String,
    val bucketName: String
) : Parcelable {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

data class MediaAlbum(
    val id: String,
    val name: String,
    val thumbnailUri: Uri,
    val itemCount: Int
)
