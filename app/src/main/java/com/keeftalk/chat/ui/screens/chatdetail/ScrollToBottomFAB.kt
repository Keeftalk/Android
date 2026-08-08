package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScrollToBottomFAB(
    visible: Boolean,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    modifier = Modifier.size(28.dp)
                )
            }

            AnimatedVisibility(
                visible = unreadCount > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(com.keeftalk.chat.ui.theme.FabGradient, CircleShape)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                        .defaultMinSize(minWidth = 10.dp, minHeight = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
