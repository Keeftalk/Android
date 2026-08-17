package com.keeftalk.chat.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Stable
class CameraState(
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    initialZoom: Float = 1.0f
) {
    var offsetX by mutableFloatStateOf(initialOffsetX)
    var offsetY by mutableFloatStateOf(initialOffsetY)
    var zoom by mutableFloatStateOf(initialZoom)

    fun reset() {
        offsetX = 0f
        offsetY = 0f
        zoom = 1.0f
    }
}

/**
 * Super-Premium 3D Social Graph with depth-of-field and parallax effects.
 */
@Composable
fun ConnectionGraph3D(
    graphData: GraphData,
    cameraState: CameraState,
    onNodeSelected: (GraphNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodes = graphData.nodes
    val edges = graphData.edges
    val particles = graphData.backgroundParticles
    val density = LocalDensity.current

    // Animated particle flow for primary path
    val infiniteTransition = rememberInfiniteTransition(label = "energy")
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    cameraState.zoom = (cameraState.zoom * zoom).coerceIn(0.15f, 4f)
                    cameraState.offsetX += pan.x / cameraState.zoom
                    cameraState.offsetY += pan.y / cameraState.zoom
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. Background Particles (Atmospheric Depth)
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
                particles.forEach { particle ->
                    // Parallax factor based on Z
                    val parallax = 1f + (particle.z / 2000f)
                    val px = centerX + (particle.x + cameraState.offsetX * parallax) * cameraState.zoom
                    val py = centerY + (particle.y + cameraState.offsetY * parallax) * cameraState.zoom
                    
                    if (px in 0f..width && py in 0f..height) {
                        drawCircle(
                            color = Color.White.copy(alpha = (0.2f + (particle.z + 1500) / 3000f).coerceIn(0.1f, 0.5f)),
                            radius = (1.5.dp.toPx() * cameraState.zoom * parallax).coerceAtLeast(1f),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // 2. Draw Edges
            Canvas(modifier = Modifier.fillMaxSize()) {
                edges.sortedBy { it.highlightType }.forEach { edge ->
                    val from = nodes.find { it.id == edge.fromId }
                    val to = nodes.find { it.id == edge.toId }
                    
                    if (from != null && to != null) {
                        val type = edge.highlightType
                        val isBest = type == PathHighlightType.BEST_PATH
                        
                        // Z-based depth calculations
                        val avgZ = (from.position.z + to.position.z) / 2f
                        val depthAlpha = (1f - (avgZ.coerceAtMost(0f) / -2000f)).coerceIn(0.1f, 1f)
                        
                        val color = when (type) {
                            PathHighlightType.BEST_PATH -> Color(0xFFFFD600)
                            PathHighlightType.PATH -> Color(0xFF00E5FF)
                            else -> Color.White.copy(alpha = 0.1f * depthAlpha)
                        }
                        
                        val stroke = when (type) {
                            PathHighlightType.BEST_PATH -> 8.dp.toPx()
                            PathHighlightType.PATH -> 3.dp.toPx()
                            else -> 1.dp.toPx()
                        } * cameraState.zoom

                        // Project positions with parallax
                        val pFrom = project(from, centerX, centerY, cameraState)
                        val pTo = project(to, centerX, centerY, cameraState)

                        // Main connection line
                        drawLine(
                            brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.4f))),
                            start = pFrom,
                            end = pTo,
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )

                        // Animated light particle for best path
                        if (isBest) {
                            val particlePos = Offset(
                                x = pFrom.x + (pTo.x - pFrom.x) * flowProgress,
                                y = pFrom.y + (pTo.y - pFrom.y) * flowProgress
                            )
                            drawCircle(
                                color = Color.White,
                                radius = stroke * 0.8f,
                                center = particlePos,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)),
                                radius = stroke * 3f,
                                center = particlePos
                            )
                        }
                    }
                }
            }

            // 3. Draw Nodes (Sorted by Z for correct layering)
            nodes.sortedBy { it.position.z }.forEach { node ->
                val projected = project(node, centerX, centerY, cameraState)
                
                // Depth Visual System
                val depthFactor = (1f - (node.position.z.coerceAtMost(0f) / -2000f)).coerceIn(0.2f, 1.2f)
                val isPrimary = node.highlightType == PathHighlightType.BEST_PATH
                val isPath = node.highlightType == PathHighlightType.PATH
                
                val scale = when {
                    node.type == NodeType.YOU || node.type == NodeType.TARGET -> 1.4f
                    isPrimary -> 1.2f
                    isPath -> 1.0f
                    else -> 0.7f
                } * cameraState.zoom * depthFactor
                
                val opacity = when {
                    (isPrimary || node.type == NodeType.YOU || node.type == NodeType.TARGET) -> 1f
                    isPath -> 0.8f
                    node.degree <= 2 -> 0.6f
                    node.degree <= 4 -> 0.4f
                    else -> 0.2f
                } * depthFactor.coerceIn(0.5f, 1f)
                
                val blurAmount = when {
                    isPrimary || node.degree <= 1 -> 0.dp
                    node.degree == 2 -> 1.dp
                    node.degree == 3 -> 3.dp
                    else -> 6.dp
                }

                val nodeSize = 64.dp * scale

                Box(
                    modifier = Modifier
                        .offset(
                            x = (projected.x / density.density).dp - (nodeSize / 2),
                            y = (projected.y / density.density).dp - (nodeSize / 2)
                        )
                        .size(nodeSize + 60.dp)
                        .graphicsLayer {
                            alpha = opacity
                        }
                        .blur(blurAmount)
                        .pointerInput(node.id) {
                            detectTapGestures { onNodeSelected(node) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NodeRendererPremium(node = node, size = 64.dp, scale = scale)
                        
                        if (opacity > 0.5f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = node.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isPrimary) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = (12f * scale).coerceIn(8f, 16f).sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun project(node: GraphNode, centerX: Float, centerY: Float, cameraState: CameraState): Offset {
    // Basic perspective/parallax projection
    // Nodes further back (smaller Z) move less with pan
    val parallax = 1f + (node.position.z / 2500f)
    return Offset(
        x = centerX + (node.position.x + cameraState.offsetX * parallax) * cameraState.zoom,
        y = centerY + (node.position.y + cameraState.offsetY * parallax) * cameraState.zoom
    )
}

@Composable
private fun NodeRendererPremium(node: GraphNode, size: androidx.compose.ui.unit.Dp, scale: Float) {
    val isPrimary = node.highlightType == PathHighlightType.BEST_PATH
    val glowColor = when {
        node.type == NodeType.YOU -> MaterialTheme.colorScheme.primary
        node.type == NodeType.TARGET -> Color(0xFFFF3D00)
        isPrimary -> Color(0xFFFFD600)
        else -> Color.White.copy(alpha = 0.5f)
    }

    Box(modifier = Modifier.size(size * scale), contentAlignment = Alignment.Center) {
        // High-End Radial Glow
        if (isPrimary || node.type == NodeType.TARGET || node.type == NodeType.YOU) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { 
                        scaleX = 2.8f
                        scaleY = 2.8f 
                    }
                    .background(Brush.radialGradient(listOf(glowColor.copy(alpha = 0.35f), Color.Transparent)), CircleShape)
            )
            
            // Animated Pulse for Primary Path
            if (isPrimary) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                        .border(1.5.dp * scale, glowColor.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        // Avatar Container
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Black,
            border = BorderStroke(
                width = (if (isPrimary) 2.5.dp else 1.dp) * scale,
                brush = Brush.sweepGradient(listOf(glowColor, glowColor.copy(alpha = 0.2f), glowColor))
            ),
            tonalElevation = 12.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                KeeftalkAvatar(
                    avatarUrl = node.avatarUrl,
                    initials = if (node.type == NodeType.YOU) "ME" else AvatarUtils.getInitials(node.name),
                    seed = node.id,
                    size = size,
                    modifier = Modifier.fillMaxSize().padding(2.dp).clip(CircleShape)
                )
                
                // Status Indicators
                if (node.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(12.dp * scale)
                            .background(Color(0xFF00C853), CircleShape)
                            .border(1.dp * scale, Color.Black, CircleShape)
                    )
                }
                
                if (node.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(14.dp * scale)
                            .background(Color.White, CircleShape),
                    )
                }
            }
        }
    }
}
