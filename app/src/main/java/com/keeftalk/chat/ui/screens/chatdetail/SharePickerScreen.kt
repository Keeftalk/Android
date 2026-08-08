package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.components.KeeftalkFeatureTopBar

enum class ShareCategory {
    NOTES, EMAILS, VAULT, AGENDA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePickerScreen(
    chatId: String,
    onBack: () -> Unit,
    onShareContent: (ShareCategory) -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = "Share Content",
                onBack = onBack
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "Choose what to share",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            val brandBlue = Color(0xFF2196F3)

            ShareCategoryItem(
                title = "Notes",
                subtitle = "Share your private notes with permissions",
                icon = Icons.Default.Description,
                color = brandBlue,
                onClick = { onShareContent(ShareCategory.NOTES) }
            )

            ShareCategoryItem(
                title = "Emails",
                subtitle = "Share important emails to this chat",
                icon = Icons.Default.Email,
                color = brandBlue,
                onClick = { onShareContent(ShareCategory.EMAILS) }
            )

            ShareCategoryItem(
                title = "Vault Files",
                subtitle = "Securely share files from your vault",
                icon = Icons.Default.Lock,
                color = brandBlue,
                onClick = { onShareContent(ShareCategory.VAULT) }
            )

            ShareCategoryItem(
                title = "Agenda",
                subtitle = "Share events and calendar items",
                icon = Icons.Default.CalendarToday,
                color = brandBlue,
                onClick = { onShareContent(ShareCategory.AGENDA) }
            )
        }
    }
}

@Composable
fun ShareCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}
