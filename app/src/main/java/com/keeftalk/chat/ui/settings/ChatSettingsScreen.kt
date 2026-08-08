package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.UserChatSettings
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit,
    onAppearanceClick: () -> Unit,
    onTextAccessibilityClick: () -> Unit,
    onMediaDownloadsClick: () -> Unit,
    onChatBehaviorClick: () -> Unit,
    onStorageCacheClick: () -> Unit,
    onChatCleanupClick: () -> Unit
) {
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Chats", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Personalization") {
                    SettingsItem(
                        icon = icons.palette,
                        title = "Chat Appearance",
                        subtitle = "Themes, wallpapers, bubble styles",
                        onClick = onAppearanceClick
                    )
                    SettingsItem(
                        icon = icons.edit, // Using edit for text
                        title = "Text & Accessibility",
                        subtitle = "Font size, contrast, animations",
                        onClick = onTextAccessibilityClick,
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Usage & Behavior") {
                    SettingsItem(
                        icon = icons.sparkles,
                        title = "Media & Downloads",
                        subtitle = "Auto-download, upload quality",
                        onClick = onMediaDownloadsClick
                    )
                    SettingsItem(
                        icon = icons.chat,
                        title = "Chat Behavior",
                        subtitle = "Swipe actions, enter key, auto-translate",
                        onClick = onChatBehaviorClick,
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Data Management") {
                    SettingsItem(
                        icon = icons.badge, // Using badge for storage
                        title = "Storage & Cache",
                        subtitle = "Manage local storage and database",
                        onClick = onStorageCacheClick
                    )
                    SettingsItem(
                        icon = icons.delete,
                        title = "Chat Cleanup",
                        subtitle = "Clear search history, emojis, cache",
                        onClick = onChatCleanupClick,
                        showDivider = false
                    )
                }
            }
        }
    }
}

