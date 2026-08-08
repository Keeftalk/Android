package com.keeftalk.chat.ui.screens.picker.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.request.CachePolicy
import coil.size.Precision
import com.keeftalk.chat.domain.model.MediaItem
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemThumbnail(
    item: MediaItem,
    isSelected: Boolean,
    selectionIndex: Int,
    onThumbnailClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionModeActive: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "selectionScale"
    )

    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(120, 120)
            .precision(Precision.INEXACT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowHardware(true)
            .crossfade(100)
            .placeholder(android.graphics.drawable.ColorDrawable(0xFFE0E0E0.toInt()))
            .error(android.graphics.drawable.ColorDrawable(android.graphics.Color.GRAY))
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (item.isVideo) {
                    videoFrameMillis(500)
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .scale(scale)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onThumbnailClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = formatDuration(item.duration ?: 0),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // Selection indicator
        if (isSelectionModeActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp) // Reduced size (approx 50% of 24.dp area would be ~17.dp, 16.dp is standard)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary 
                        else Color.Black.copy(alpha = 0.3f)
                    )
                    .then(
                        if (!isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        else Modifier
                    )
                    .clickable { onToggleSelection() },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = (selectionIndex + 1).toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
