package com.keeftalk.chat.ui.call

import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.services.GsmCallManager
import com.keeftalk.chat.ui.theme.KeeftalkTheme
import com.keeftalk.chat.ui.theme.FabCyan
import com.keeftalk.chat.ui.theme.FabPurple
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class GsmCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeeftalkTheme {
                GsmCallScreen(onDismiss = { finish() })
            }
        }
    }
}

@Composable
fun GsmCallScreen(onDismiss: () -> Unit) {
    var call by remember { mutableStateOf(GsmCallManager.getCurrentCall()) }
    var callState by remember { mutableStateOf(call?.state ?: Call.STATE_DISCONNECTED) }
    var audioState by remember { mutableStateOf(GsmCallManager.getAudioState()) }
    
    val isMuted = audioState?.isMuted ?: false
    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER

    var callDurationSeconds by remember { mutableLongStateOf(0L) }

    val callback = remember {
        object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                callState = state
                if (state == Call.STATE_DISCONNECTED) {
                    onDismiss()
                }
            }
        }
    }

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            val startTime = System.currentTimeMillis()
            while (callState == Call.STATE_ACTIVE) {
                callDurationSeconds = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }

    DisposableEffect(Unit) {
        GsmCallManager.setListener { newCall ->
            call?.unregisterCallback(callback)
            call = newCall
            callState = newCall?.state ?: Call.STATE_DISCONNECTED
            newCall?.registerCallback(callback)
            if (newCall == null) onDismiss()
        }
        GsmCallManager.setAudioStateListener { state ->
            audioState = state
        }
        onDispose {
            call?.unregisterCallback(callback)
        }
    }

    val number = call?.details?.handle?.schemeSpecificPart ?: "Unknown"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FabCyan,
                        FabPurple,
                        Color(0xFF0E0E12)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Header
            Text(
                text = if (callState == Call.STATE_RINGING) "Incoming call" else "Ongoing call",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Text(
                text = if (callState == Call.STATE_ACTIVE) formatDuration(callDurationSeconds) else getCallStateString(callState),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Pulsing Avatar
            PulsingAvatar()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = number,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            // Feature Grid (3x2)
            FeatureGrid(
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                onToggleMute = { GsmCallManager.setMuted(!isMuted) },
                onToggleSpeaker = { 
                    val newRoute = if (isSpeakerOn) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
                    GsmCallManager.setAudioRoute(newRoute)
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            // Bottom Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (callState == Call.STATE_RINGING) {
                    ControlCircleButton(
                        icon = Icons.Default.CallEnd,
                        color = Color.Red,
                        onClick = { call?.disconnect() },
                        size = 80.dp
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                    ControlCircleButton(
                        icon = Icons.Default.Call,
                        color = Color.Green,
                        onClick = { call?.answer(VideoProfile.STATE_AUDIO_ONLY) },
                        size = 80.dp
                    )
                } else {
                    ControlCircleButton(
                        icon = Icons.Default.CallEnd,
                        color = Color.Red,
                        onClick = { call?.disconnect() },
                        size = 80.dp
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingAvatar() {
    val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(contentAlignment = Alignment.Center) {
        // Pulse Rings
        Box(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer { scaleX = scale1; scaleY = scale1; alpha = alpha1 }
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer { scaleX = scale2; scaleY = scale2; alpha = alpha2 }
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
        )
        
        // Main Avatar Placeholder
        Surface(
            modifier = Modifier.size(130.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(32.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun FeatureGrid(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    val context = LocalContext.current
    
    val items = listOf(
        FeatureItem(Icons.Default.MicOff, "Mute", isMuted, onToggleMute),
        FeatureItem(Icons.Default.Dialpad, "Keypad", false, { /* Show Keypad overlay */ }),
        FeatureItem(Icons.Default.VolumeUp, "Speaker", isSpeakerOn, onToggleSpeaker),
        FeatureItem(Icons.Default.Add, "Add call", false, { /* Open dialer or contact picker */ }),
        FeatureItem(Icons.Default.Videocam, "Video call", false, { /* Upgrade to video */ }),
        FeatureItem(Icons.Default.Contacts, "Contacts", false, {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            context.startActivity(intent)
        })
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(220.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(items) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { item.onClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (item.isActive) Color.White else Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (item.isActive) Color(0xFF00796B) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.label,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

data class FeatureItem(
    val icon: ImageVector,
    val label: String,
    val isActive: Boolean,
    val onClick: () -> Unit
)

@Composable
fun ControlCircleButton(
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

private fun getCallStateString(state: Int): String {
    return when (state) {
        Call.STATE_ACTIVE -> "Active"
        Call.STATE_RINGING -> "Ringing"
        Call.STATE_DIALING -> "Dialing"
        Call.STATE_CONNECTING -> "Connecting"
        Call.STATE_DISCONNECTED -> "Ended"
        Call.STATE_DISCONNECTING -> "Ending"
        Call.STATE_HOLDING -> "On Hold"
        else -> "Connecting"
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = TimeUnit.SECONDS.toMinutes(seconds)
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
