package com.keeftalk.chat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabPill(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pill_content"
    )
    val shadowElevation by animateDpAsState(if (selected) 4.dp else 0.dp, label = "pill_shadow")

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = shadowElevation
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = contentColor)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}
