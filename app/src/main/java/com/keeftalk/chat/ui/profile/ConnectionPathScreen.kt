package com.keeftalk.chat.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Visibility

/**
 * Cinematic Redesign of Social Connection Graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionPathScreen(
    viewModel: ConnectionPathViewModel,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val graphData by viewModel.graphData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val cameraState = remember { CameraState() }
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var showFullNetwork by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // 1. Cinematic Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to Color(0xFF001219),
                            0.6f to Color(0xFF00080B),
                            1.0f to Color.Black,
                            center = androidx.compose.ui.geometry.Offset(500f, 500f)
                        )
                    )
            )

            // 2. Main Graph
            if (graphData.nodes.isNotEmpty()) {
                ConnectionGraph3D(
                    graphData = if (showFullNetwork) graphData else graphData.copy(
                        nodes = graphData.nodes.filter { it.highlightType != PathHighlightType.NONE }
                    ),
                    cameraState = cameraState,
                    onNodeSelected = { selectedNode = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (selectedNode != null) 20.dp else 0.dp)
                )
            }

            // 3. Floating Header & Path Summary
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                CinematicHeader(onBack = onBack)
                
                if (graphData.nodes.isNotEmpty()) {
                    PathSummaryPanel(
                        nodes = graphData.nodes.filter { it.highlightType == PathHighlightType.BEST_PATH }
                            .sortedBy { it.degree }
                    )
                }
            }

            // 4. Premium Control Bar (Bottom Floating)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                GraphControls(
                    isNetworkVisible = showFullNetwork,
                    onToggleNetwork = { showFullNetwork = !showFullNetwork },
                    onReset = { cameraState.reset() }
                )
            }

            // 5. Legend
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 24.dp)
            ) {
                GraphLegend()
            }

            // 6. Selected Node Detail
            AnimatedVisibility(
                visible = selectedNode != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.fillMaxSize()
            ) {
                selectedNode?.let { node ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { selectedNode = null }
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        SelectedNodeCard(
                            node = node,
                            onViewProfile = { 
                                onProfileClick(node.id)
                                selectedNode = null
                            }
                        )
                    }
                }
            }

            // 7. Loading / Empty States
            if (isLoading) {
                ConstellationLoader(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(error!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else if (graphData.nodes.isEmpty()) {
                EmptyConnectionState(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CinematicHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column {
            Text(
                text = "Connection Path",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Text(
                text = "SOCIAL UNIVERSE EXPLORER",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF00E5FF).copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PathSummaryPanel(nodes: List<GraphNode>) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            nodes.forEachIndexed { index, node ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KeeftalkAvatar(
                        avatarUrl = node.avatarUrl,
                        initials = if (node.type == NodeType.YOU) "ME" else AvatarUtils.getInitials(node.name),
                        seed = node.id,
                        size = 32.dp,
                        modifier = Modifier.clip(CircleShape)
                    )
                }
                if (index < nodes.size - 1) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${nodes.size - 1} degrees",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFD600)
            )
        }
    }
}

@Composable
fun GraphControls(
    isNetworkVisible: Boolean,
    onToggleNetwork: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        tonalElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlItem(
                icon = Icons.Default.Visibility,
                active = isNetworkVisible,
                onClick = onToggleNetwork
            )
            ControlItem(
                icon = Icons.Default.Refresh,
                active = false,
                onClick = onReset
            )
            ControlItem(
                icon = Icons.Default.Map,
                active = true,
                onClick = {}
            )
        }
    }
}

@Composable
fun ControlItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(if (active) Color.White.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
    ) {
        Icon(icon, null, tint = if (active) Color.White else Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun GraphLegend() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendItem(Color(0xFFFFD600), "Primary Path")
            LegendItem(Color(0xFF00E5FF), "Connections")
            LegendItem(Color.White.copy(alpha = 0.3f), "Background")
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun EmptyConnectionState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PrivacyTip,
            contentDescription = null,
            tint = Color(0xFF00E5FF).copy(alpha = 0.2f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Social Path Found",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "You don't currently have a visible social connection to this person. Try connecting with more people to expand your network.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SelectedNodeCard(
    node: GraphNode,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(IntrinsicSize.Min)
            .clickable(enabled = false) {}
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0A0A),
            shape = RoundedCornerShape(32.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            tonalElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val glowColor = when(node.highlightType) {
                        PathHighlightType.BEST_PATH -> Color(0xFFFFD600)
                        PathHighlightType.PATH -> Color(0xFF00E5FF)
                        else -> Color.White
                    }
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.2f), Color.Transparent)), CircleShape)
                    )
                    
                    KeeftalkAvatar(
                        avatarUrl = node.avatarUrl,
                        initials = if (node.type == NodeType.YOU) "ME" else AvatarUtils.getInitials(node.name),
                        seed = node.id,
                        size = 88.dp,
                        modifier = Modifier.clip(CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                
                node.relationshipInfo?.let { info ->
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFFD600).copy(alpha = 0.8f)
                    )
                }

                if (node.isClickable && node.type != NodeType.YOU) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onViewProfile,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("View Profile", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ConstellationLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(modifier = modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            val radius = size.minDimension / 2
            val count = 6
            for (i in 0 until count) {
                val angle = (i * 360f / count).toDouble() * Math.PI / 180f
                val x = center.x + cos(angle).toFloat() * radius * 0.7f
                val y = center.y + sin(angle).toFloat() * radius * 0.7f
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            strokeWidth = 2.dp,
            color = Color(0xFF00E5FF).copy(alpha = 0.4f)
        )
    }
}
