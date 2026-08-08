package com.keeftalk.chat.util

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isSpecified
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.keeftalk.chat.R
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageData
import com.mohamedrejeb.richeditor.model.ImageLoader
import java.util.UUID

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.net.Uri

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.keeftalk.chat.domain.repository.FileRepository
import androidx.compose.runtime.produceState

val LocalFileRepository = staticCompositionLocalOf<FileRepository?> { null }

@OptIn(ExperimentalRichTextApi::class)
class NotesImageLoader : ImageLoader {
    @Composable
    override fun load(model: Any): ImageData {
        val context = LocalContext.current
        val repo = LocalFileRepository.current
        val modelString = model.toString()
        
        // Protocol Resolver: keeftalk-media://image/[id]
        val resolvedUrl = if (modelString.startsWith("keeftalk-media://")) {
            val id = remember(modelString) { Uri.parse(modelString).lastPathSegment ?: "" }
            val state = produceState<String?>(initialValue = null, repo, id) {
                value = repo?.getFileById(id)?.storagePath
            }
            state.value
        } else if (modelString.startsWith("keeftalk://table/")) {
            null // Let the editor handle tables
        } else modelString

        if (resolvedUrl == null) {
            // Return a blank ImageData while resolving or if it's a special block handled elsewhere
            return ImageData(
                painter = painterResource(id = R.drawable.placeholder),
                contentDescription = "Loading...",
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(0.dp) // Hide it
            )
        }

        // Parse metadata from URL if present
        val uri = Uri.parse(resolvedUrl)
        val originalWidth = uri.getQueryParameter("w")?.toIntOrNull() ?: 0
        val originalHeight = uri.getQueryParameter("h")?.toIntOrNull() ?: 0
        
        // Clean URL (remove query params for Coil)
        val cleanUrl = if (resolvedUrl.contains("?")) resolvedUrl.substringBefore("?") else resolvedUrl
        
        val traceId = remember(cleanUrl) { UUID.randomUUID().toString().take(8) }
        NotesLogger.v("MEDIA_DEBUG", "[traceId=$traceId] Image Loader request started. URL: $cleanUrl")
        
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(cleanUrl)
                .crossfade(enable = true)
                .build(),
            placeholder = painterResource(id = R.drawable.placeholder),
            error = rememberVectorPainter(Icons.Default.BrokenImage),
        )

        val isVideo = remember(cleanUrl) {
            cleanUrl.contains("/video/", ignoreCase = true) ||
            cleanUrl.contains(".mp4", ignoreCase = true) || 
            cleanUrl.contains(".mov", ignoreCase = true) ||
            cleanUrl.contains(".webm", ignoreCase = true)
        }

        val ratio = if (originalWidth > 0 && originalHeight > 0) {
            originalWidth.toFloat() / originalHeight.toFloat()
        } else {
            // Try to get from painter if loaded
            val size = painter.intrinsicSize
            if (size.isSpecified && size.width > 0 && size.height > 0) {
                size.width / size.height
            } else {
                1.6f // Default 16:10 ratio for unknown images
            }
        }

        return ImageData(
            painter = if (isVideo) VideoPainter(painter, rememberVectorPainter(Icons.Default.PlayCircle), traceId) else painter,
            contentDescription = if (isVideo) "Video Note" else "Image Note",
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.8f) // Always default to 80% as requested
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(8.dp)), // Match HTML preview corners
        )
    }
}


class VideoPainter(
    private val imagePainter: Painter,
    private val playIconPainter: Painter,
    private val traceId: String
) : Painter() {
    override val intrinsicSize: androidx.compose.ui.geometry.Size 
        get() = if (imagePainter.intrinsicSize.isSpecified) imagePainter.intrinsicSize 
                else androidx.compose.ui.geometry.Size(100f, 100f)

    override fun DrawScope.onDraw() {
        // Draw the base image
        val targetSize = if (size.width > 0 && size.height > 0) size 
                         else androidx.compose.ui.geometry.Size(100f, 100f)
        
        with(imagePainter) {
            draw(targetSize)
        }
        
        // Draw play icon in center
        val iconSize = (targetSize.minDimension * 0.3f).coerceAtLeast(24f)
        val left = (targetSize.width - iconSize) / 2f
        val top = (targetSize.height - iconSize) / 2f
        
        translate(left, top) {
            with(playIconPainter) {
                draw(androidx.compose.ui.geometry.Size(iconSize, iconSize), alpha = 0.8f, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White))
            }
        }
    }
}
