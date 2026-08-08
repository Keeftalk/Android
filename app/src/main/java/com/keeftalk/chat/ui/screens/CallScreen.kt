package com.keeftalk.chat.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.keeftalk.chat.domain.model.CallState
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils
import com.keeftalk.chat.ui.theme.LocalAppIcons
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onDismiss: () -> Unit
) {
    val session by viewModel.callSession.collectAsState()
    val peerUser by viewModel.peerUser.collectAsState()
    val localTrack by viewModel.localVideoTrack.collectAsState()
    val remoteTrack by viewModel.remoteVideoTrack.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val icons = LocalAppIcons.current

    val permissions = remember(session?.type) {
        mutableListOf(android.Manifest.permission.RECORD_AUDIO).apply {
            if (session?.type == "VIDEO") add(android.Manifest.permission.CAMERA)
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.onPermissionsGranted()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    if (session?.state == CallState.ENDED) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background Blurry Avatar
        AsyncImage(
            model = peerUser?.avatarUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(80.dp).graphicsLayer { alpha = 0.4f },
            contentScale = ContentScale.Crop
        )

        AnimatedContent(
            targetState = session?.state,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            label = "CallStateTransition"
        ) { state ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Video Streams
                if (session?.type == "VIDEO") {
                    if (remoteTrack != null && isVideoEnabled) {
                        WebRTCVideoRenderer(videoTrack = remoteTrack!!, modifier = Modifier.fillMaxSize())
                    }
                    if (localTrack != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .size(100.dp, 150.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp
                        ) {
                            WebRTCVideoRenderer(videoTrack = localTrack!!, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Overlay UI
                Column(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(100.dp))
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        if (state == CallState.INCOMING_RINGING || state == CallState.OUTGOING_RINGING) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 1.5f - pulse }
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                        
                        KeeftalkAvatar(
                            avatarUrl = peerUser?.avatarUrl,
                            initials = AvatarUtils.getInitials(peerUser?.name),
                            seed = peerUser?.id,
                            size = 130.dp,
                            modifier = Modifier.border(4.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = peerUser?.name ?: "Connecting...",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(
                            text = if (session?.type == "VIDEO") "VIDEO CALL" else "VOICE CALL",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = state?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Controls with Glassmorphism
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(40.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        CallControls(
                            state = state ?: CallState.IDLE,
                            type = session?.type ?: "VOICE",
                            isMuted = isMuted,
                            isVideoEnabled = isVideoEnabled,
                            onAccept = viewModel::acceptCall,
                            onReject = viewModel::rejectCall,
                            onEnd = viewModel::endCall,
                            onToggleMic = viewModel::toggleMic,
                            onToggleVideo = viewModel::toggleVideo
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallControls(
    state: CallState,
    type: String,
    isMuted: Boolean,
    isVideoEnabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEnd: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleVideo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state == CallState.INCOMING_RINGING) {
            ControlCircleButton(
                icon = Icons.Default.CallEnd,
                color = Color(0xFFEF4444),
                onClick = onReject,
                size = 72.dp
            )
            ControlCircleButton(
                icon = Icons.Default.Call,
                color = Color(0xFF22C55E),
                onClick = onAccept,
                size = 72.dp
            )
        } else {
            ControlCircleButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                color = if (isMuted) Color.White else Color.White.copy(alpha = 0.15f),
                tint = if (isMuted) Color.Black else Color.White,
                onClick = onToggleMic
            )
            
            ControlCircleButton(
                icon = Icons.Default.CallEnd,
                color = Color(0xFFEF4444),
                onClick = onEnd,
                size = 80.dp
            )
            
            if (type == "VIDEO") {
                ControlCircleButton(
                    icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    color = if (!isVideoEnabled) Color.White else Color.White.copy(alpha = 0.15f),
                    tint = if (!isVideoEnabled) Color.Black else Color.White,
                    onClick = onToggleVideo
                )
            }
        }
    }
}

@Composable
fun ControlCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
fun WebRTCVideoRenderer(videoTrack: VideoTrack, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(org.webrtc.EglBase.create().eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(true)
            }
        },
        update = { view ->
            videoTrack.addSink(view)
        },
        modifier = modifier
    )
}
