package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.model.MessageType
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MediaViewerViewModel(
    application: Application,
    private val repository: ChatRepository,
    private val chatId: String,
    initialMessageId: String
) : AndroidViewModel(application) {

    private val _mediaIds = MutableStateFlow<List<String>>(emptyList())
    val mediaIds: StateFlow<List<String>> = _mediaIds.asStateFlow()

    private val _initialIndex = MutableStateFlow(0)
    val initialIndex: StateFlow<Int> = _initialIndex.asStateFlow()

    init {
        viewModelScope.launch {
            val ids = repository.getMediaMessageIds(chatId)
            _mediaIds.value = ids
            val index = ids.indexOf(initialMessageId)
            if (index != -1) {
                _initialIndex.value = index
            }
        }
    }

    fun getMessage(messageId: String): Flow<Message?> = repository.getMessage(messageId)

    fun toggleLock(messageId: String, locked: Boolean) {
        viewModelScope.launch {
            repository.toggleMediaLock(messageId, locked)
        }
    }

    fun downloadMedia(message: Message) {
        viewModelScope.launch {
            repository.downloadMedia(message).onSuccess { result ->
                val typeName = when(message.type) {
                    MessageType.IMAGE -> "Pic"
                    MessageType.VIDEO -> "Video"
                    else -> "Media"
                }
                val subFolder = when(message.type) {
                    MessageType.IMAGE -> "Picture"
                    MessageType.VIDEO -> "Video"
                    else -> "files"
                }
                val msg = if (result.alreadyExisted) {
                    "$typeName already saved in Keeftalk/$subFolder"
                } else {
                    "$typeName downloaded to Keeftalk/$subFolder"
                }
                android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                android.widget.Toast.makeText(getApplication(), e.message ?: "Failed to download", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun ensureLocal(message: Message) {
        viewModelScope.launch {
            repository.ensureMediaLocal(message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    viewModel: MediaViewerViewModel,
    onBack: () -> Unit,
    currentUserId: String,
    initialPlaybackPosition: Long = 0L
) {
    val mediaIds by viewModel.mediaIds.collectAsState()
    val initialIndex by viewModel.initialIndex.collectAsState()
    
    if (mediaIds.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaIds.size })
    var showControls by remember { mutableStateOf(true) }

    val currentMessageId = if (pagerState.currentPage < mediaIds.size) mediaIds[pagerState.currentPage] else null
    val currentMessage by remember(currentMessageId) {
        if (currentMessageId != null) viewModel.getMessage(currentMessageId) else flowOf(null)
    }.collectAsState(initial = null)

    LaunchedEffect(currentMessage) {
        currentMessage?.let { viewModel.ensureLocal(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { pageIndex ->
            val messageId = mediaIds[pageIndex]
            val message by viewModel.getMessage(messageId).collectAsState(initial = null)
            
            if (message != null) {
                MediaItemView(
                    message = message!!,
                    currentUserId = currentUserId,
                    onToggleControls = { showControls = !showControls },
                    initialPlaybackPosition = if (pageIndex == initialIndex) initialPlaybackPosition else 0L
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // Top Bar
        AnimatedVisibility(
            visible = showControls && currentMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val message = currentMessage!!
            val isMe = message.senderId == currentUserId
            
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isMe) "You" else "Peer", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            text = android.text.format.DateFormat.format("MMM d, yyyy HH:mm", message.timestamp).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isMe) {
                        IconButton(onClick = { viewModel.toggleLock(message.id, !message.mediaLocked) }) {
                            Icon(
                                imageVector = if (message.mediaLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (message.mediaLocked) Color.Red else Color.White
                            )
                        }
                    }
                    if (isMe || !message.mediaLocked) {
                        IconButton(onClick = { /* Forward logic */ }) {
                            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "Forward", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.downloadMedia(message) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White
                )
            )
        }

        // Lock Overlay Info
        if (currentMessage?.mediaLocked == true && currentMessage?.senderId != currentUserId) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sender has locked this media. Saving/Sharing disabled.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun MediaItemView(
    message: Message,
    @Suppress("UNUSED_PARAMETER") currentUserId: String,
    onToggleControls: () -> Unit,
    initialPlaybackPosition: Long = 0L
) {
    val mediaUri = message.localFilePath ?: message.fileUrl ?: return
    
    when (message.type) {
        MessageType.IMAGE -> ImageViewer(uri = mediaUri, onToggleControls = onToggleControls)
        MessageType.VIDEO -> VideoViewer(uri = mediaUri, onToggleControls = onToggleControls, initialPlaybackPosition = initialPlaybackPosition)
        MessageType.VOICE -> AudioViewer(uri = mediaUri)
        MessageType.FILE -> FileViewer(message = message)
        MessageType.SHARED_VAULT_FILE -> {
            when (message.effectiveFileType) {
                com.keeftalk.chat.domain.model.FileType.IMAGE -> ImageViewer(uri = mediaUri, onToggleControls = onToggleControls)
                com.keeftalk.chat.domain.model.FileType.VIDEO -> VideoViewer(uri = mediaUri, onToggleControls = onToggleControls, initialPlaybackPosition = initialPlaybackPosition)
                else -> FileViewer(message = message)
            }
        }
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unsupported media type", color = Color.White)
        }
    }
}

@Composable
fun ImageViewer(uri: String, onToggleControls: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offset += pan
                    } else {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    }
                }
            }
            .clickable { onToggleControls() }
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoViewer(uri: String, onToggleControls: () -> Unit, initialPlaybackPosition: Long = 0L) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            if (initialPlaybackPosition > 0) {
                seekTo(initialPlaybackPosition)
            }
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onToggleControls() }) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AudioViewer(uri: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val duration = exoPlayer.duration
            if (duration > 0) {
                progress = exoPlayer.currentPosition.toFloat() / duration.toFloat()
            }
            kotlinx.coroutines.delay(100)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        IconButton(
            onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
            modifier = Modifier.size(64.dp).background(Color.White, CircleShape)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
        }
    }
}

@Composable
fun FileViewer(message: Message) {
    val context = LocalContext.current
    val isLocal = message.localFilePath != null && java.io.File(message.localFilePath).exists()

    // Auto-open once downloaded
    LaunchedEffect(isLocal) {
        if (isLocal) {
            val localPath = message.localFilePath
            if (localPath != null) {
                val file = java.io.File(localPath)
                if (file.exists()) {
                    com.keeftalk.chat.util.FileUtils.openFile(context, file)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(message.fileName ?: "Document", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val size = message.fileSize
        Text(
            if (size != null) "${size / 1024} KB" else "",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isLocal) {
            Button(
                onClick = { 
                    com.keeftalk.chat.util.FileUtils.openFile(context, java.io.File(message.localFilePath))
                },
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Document")
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Downloading...", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
