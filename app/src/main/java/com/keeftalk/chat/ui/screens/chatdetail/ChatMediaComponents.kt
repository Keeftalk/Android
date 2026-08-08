package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun InlineVideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    onFullScreen: (Long) -> Unit
) {
    val context = LocalContext.current
    var videoRatio by remember { mutableFloatStateOf(16f / 9f) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }

    val playVideo = {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                volume = 0f
                playWhenReady = true
            }
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
            exoPlayer = player
        } else {
            exoPlayer?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }
    }

    DisposableEffect(uri) {
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Box(
        modifier = modifier
            .widthIn(max = 280.dp)
            .aspectRatio(videoRatio.coerceIn(0.5f, 2f)) 
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = {
                    androidx.media3.ui.PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize().clickable { onFullScreen(exoPlayer?.currentPosition ?: 0L) }
            )
        } else {
            // Placeholder/Thumbnail
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
        }

        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable {
                        playVideo()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isPlaying) {
                IconButton(
                    onClick = {
                        isPlaying = false
                        exoPlayer?.pause()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    isMuted = !isMuted
                    exoPlayer?.volume = if (isMuted) 0f else 1f
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun VoicePreviewPlayer(
    duration: Long,
    onDelete: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Voice Message",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            formatDurationInternal(duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun VoiceWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        amplitudes.forEach { amplitude ->
            val heightFactor by animateFloatAsState(
                targetValue = amplitude.coerceIn(0.1f, 1f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "waveHeight"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp * heightFactor)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
