package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.theme.LocalChatThemeExtra
import com.keeftalk.chat.util.AvatarUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun TypingIndicator(typingNames: List<String>) {
    if (typingNames.isEmpty()) return

    val chatTheme = LocalChatTheme.current
    val typingText = when {
        typingNames.size == 1 -> "${typingNames[0]} is typing"
        typingNames.size == 2 -> "${typingNames[0]} and ${typingNames[1]} are typing"
        else -> "${typingNames[0]} and ${typingNames.size - 1} others are typing"
    }

    if (chatTheme.id == "rosa") {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "TypingDots")

            val colors = listOf(Color(0xFFFF6B9D), Color(0xFFD44AD6), Color(0xFFFF8AAE))
            repeat(3) { index ->
                val waveAnim by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1400
                            0f at 0
                            1f at 300
                            0f at 600
                            0f at 1400
                        },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(index * 200)
                    ), label = "waveAnim"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            translationY = -waveAnim * 8.dp.toPx()
                            scaleX = 1f + (waveAnim * 0.2f)
                            scaleY = 1f + (waveAnim * 0.2f)
                            alpha = 0.5f + (waveAnim * 0.5f)
                        }
                        .background(colors[index % colors.size], CircleShape)
                )
            }
            Text(
                text = "$typingText...",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    } else {
        val themeExtra = LocalChatThemeExtra.current
        val containerColor = when(chatTheme.id) {
            "alpha" -> Color.White.copy(alpha = 0.05f)
            else -> themeExtra.msgThem
        }

        val borderColor = when(chatTheme.id) {
            "alpha" -> Color.White.copy(alpha = 0.08f)
            else -> Color.Transparent
        }

        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = containerColor,
            border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    typingText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (chatTheme.id == "alpha") Color.White else MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { index ->
                        val infiniteTransition = rememberInfiniteTransition(label = "SomeoneTyping")
                        val translationY by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = keyframes {
                                    durationMillis = 1200
                                    0f at 0
                                    -4f at 300
                                    0f at 600
                                    0f at 1200
                                },
                                initialStartOffset = StartOffset(index * 200)
                            ), label = "translationY"
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .offset(y = translationY.dp)
                                .clip(CircleShape)
                                .background(
                                    color = when(chatTheme.id) {
                                        "alpha" -> Color(0xFFC9A84C)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GhostModeIndicator(timer: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier
            .padding(end = 8.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            LocalAppIcons.current.clock, 
            null, 
            modifier = Modifier.size(14.dp), 
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            formatAutoDeleteInternal(timer), 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ReadReceiptOverlay(
    receipts: Map<String, Pair<String, Long>>,
    bubblePositions: Map<String, Pair<Offset, IntSize>>,
    peerAvatarUrl: String?,
    peerName: String,
    peerId: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val animatingReceipts = remember { mutableStateMapOf<String, Pair<Offset, String>>() }
    var prevReceipts by remember { mutableStateOf(receipts) }

    LaunchedEffect(receipts) {
        receipts.forEach { (userId, pointer) ->
            val messageId = pointer.first
            if (prevReceipts[userId]?.first != messageId) {
                val oldMessageId = prevReceipts[userId]?.first
                val startPosInfo = bubblePositions[oldMessageId]
                val startOffset = startPosInfo?.let { (offset, size) ->
                    Offset(offset.x + size.width.toFloat() + with(density) { 6.dp.toPx() }, offset.y + size.height.toFloat() - with(density) { 18.dp.toPx() })
                }
                if (startOffset != null) {
                    animatingReceipts[userId] = startOffset to messageId
                }
            }
        }
        prevReceipts = receipts
    }

    Box(modifier = modifier) {
        animatingReceipts.forEach { (userId, data) ->
            val (startOffset, targetMessageId) = data
            val posInfo = bubblePositions[targetMessageId]
            if (posInfo != null) {
                val (offset, size) = posInfo
                val targetX = offset.x + size.width.toFloat() + with(density) { 6.dp.toPx() }
                val targetY = offset.y + size.height.toFloat() - with(density) { 18.dp.toPx() }

                key(userId + targetMessageId) {
                    PhysicsMovingAvatar(
                        startOffset = startOffset,
                        targetOffset = Offset(targetX, targetY),
                        avatarUrl = peerAvatarUrl,
                        initials = AvatarUtils.getInitials(peerName),
                        seed = peerId,
                        onAnimationFinished = {
                            animatingReceipts.remove(userId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PhysicsMovingAvatar(
    startOffset: Offset,
    targetOffset: Offset,
    avatarUrl: String?,
    initials: String,
    seed: String,
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    val animX = remember { Animatable(startOffset.x) }
    val animY = remember { Animatable(startOffset.y) }
    val scale = remember { Animatable(1f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(targetOffset) {
        launch {
            scale.animateTo(1.2f, tween(100))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }

        val xAnim = launch {
            animX.animateTo(
                targetOffset.x,
                spring(dampingRatio = 0.8f, stiffness = 300f)
            )
        }
        val yAnim = launch {
            animY.animateTo(
                targetOffset.y,
                spring(dampingRatio = 0.7f, stiffness = 350f)
            )
        }

        xAnim.join()
        yAnim.join()
        onAnimationFinished()
    }

    Box(
        modifier = modifier
            .offset { IntOffset(animX.value.roundToInt(), animY.value.roundToInt()) }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .size(16.dp)
            .shadow(3.dp, CircleShape)
            .border(1.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .background(AvatarUtils.getAvatarColor(seed))
    ) {
        KeeftalkAvatar(
            avatarUrl = avatarUrl,
            initials = initials,
            seed = seed,
            size = 16.dp,
            isOnline = false
        )
    }
}
