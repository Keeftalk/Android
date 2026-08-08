package com.keeftalk.chat.ui.emoji

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

@Composable
fun AnimatedEmoji(
    model: Any, // URL, Resource ID, etc.
    modifier: Modifier = Modifier.size(64.dp)
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .build(),
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = modifier
    )
}
