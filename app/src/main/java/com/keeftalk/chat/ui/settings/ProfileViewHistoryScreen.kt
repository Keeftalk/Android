package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.repository.ProfileView
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewHistoryScreen(
    viewModel: ProfileViewHistoryViewModel,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val views by viewModel.views.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Profile Views", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (views.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        icons.user,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No profile views recorded",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingsSection(title = "Recent Visitors") {
                        views.forEachIndexed { index, view ->
                            ProfileViewItem(
                                view = view,
                                onClick = { if (view.viewerId.isNotEmpty()) onProfileClick(view.viewerId) },
                                showDivider = index != views.size - 1
                            )
                        }
                    }
                }
                
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileViewItem(view: ProfileView, onClick: () -> Unit, showDivider: Boolean) {
    val isHidden = view.viewerId.isEmpty()
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeeftalkAvatar(
                avatarUrl = if (isHidden) null else view.viewerAvatarUrl,
                initials = if (isHidden) "?" else AvatarUtils.getInitials(view.viewerName),
                seed = view.viewerId,
                size = 50.dp,
                modifier = if (isHidden) Modifier.blur(10.dp) else Modifier
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHidden) "Hidden User" else (view.viewerName ?: "Someone"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = com.keeftalk.chat.ui.settings.formatTimestamp(view.viewedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isHidden) {
                Icon(
                    imageVector = com.keeftalk.chat.ui.theme.LocalAppIcons.current.back,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = 180f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

