package com.keeftalk.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerButton(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick() 
        },
        modifier = Modifier.size(80.dp).clip(CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = digit, 
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 32.sp
                    )
                )
                if (letters.isNotEmpty()) {
                    Text(
                        text = letters, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
