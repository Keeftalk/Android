package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.theme.LocalAppIcons

import androidx.compose.foundation.lazy.items
import com.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState(initial = emptyList())
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Blocked Users", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (blockedUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        icons.block,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No restricted contacts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingsSection(title = "Blacklist") {
                        blockedUsers.forEachIndexed { index, user ->
                            BlockedUserItem(
                                user = user,
                                onUnblock = { viewModel.unblockUser(user.id) },
                                showDivider = index != blockedUsers.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUserItem(user: User, onUnblock: () -> Unit, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeeftalkAvatar(
                avatarUrl = user.avatarUrl,
                initials = AvatarUtils.getInitials(user.name),
                seed = user.id,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                user.username?.let {
                    Text(
                        text = "@$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onUnblock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Unblock", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

