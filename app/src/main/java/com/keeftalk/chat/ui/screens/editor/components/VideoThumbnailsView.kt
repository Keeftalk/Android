package com.keeftalk.chat.ui.screens.editor.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoThumbnailsView(
    uri: Uri,
    modifier: Modifier = Modifier,
    thumbnailCount: Int = 8
) {
    val context = LocalContext.current
    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                val interval = duration / thumbnailCount
                
                val list = mutableListOf<Bitmap>()
                for (i in 0 until thumbnailCount) {
                    val timeUs = i * interval * 1000
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bitmap?.let { list.add(it) }
                }
                thumbnails = list
            } catch (e: Exception) {
                // Ignore
            } finally {
                retriever.release()
            }
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        thumbnails.forEach { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
