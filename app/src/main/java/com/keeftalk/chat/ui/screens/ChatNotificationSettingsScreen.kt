package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.Chat
import com.keeftalk.chat.domain.model.ChatNotificationSettings
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.settings.SettingsSection
import com.keeftalk.chat.ui.settings.SettingsItem
import com.keeftalk.chat.ui.settings.SettingsToggleItem
import com.keeftalk.chat.ui.settings.SettingsHeader
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatNotificationSettingsScreen(
    chat: Chat,
    onBack: () -> Unit,
    onUpdateSettings: (ChatNotificationSettings) -> Unit
) {
    val settings = chat.notificationSettings
    val icons = LocalAppIcons.current
    var showMuteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Chat Alerts", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Status") {
                    SettingsItem(
                        icon = icons.mute,
                        title = "Notifications Status",
                        subtitle = if (chat.isMuted) "Silenced" else "Active",
                        onClick = { showMuteDialog = true },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Sound & Haptics") {
                    SettingsItem(
                        icon = icons.bell,
                        title = "Notification Tone",
                        subtitle = settings.customSoundUri ?: "Default Tone",
                        onClick = { /* Pick sound */ }
                    )
                    SettingsItem(
                        icon = icons.bell, // Using bell for vibration
                        title = "Vibration Pattern",
                        subtitle = settings.vibration ?: "Standard",
                        onClick = { /* Pick vibration */ },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Visibility") {
                    SettingsToggleItem(
                        icon = icons.info,
                        title = "Show Message Text",
                        subtitle = "Display content in banner",
                        checked = settings.showPreview ?: true,
                        onCheckedChange = { onUpdateSettings(settings.copy(showPreview = it)) }
                    )
                    SettingsToggleItem(
                        icon = icons.sparkles,
                        title = "High Priority",
                        subtitle = "Bypass 'Do Not Disturb'",
                        checked = settings.highPriority,
                        onCheckedChange = { onUpdateSettings(settings.copy(highPriority = it)) }
                    )
                    SettingsToggleItem(
                        icon = icons.pin,
                        title = "Lock Screen Sticky",
                        subtitle = "Keep notification until read",
                        checked = settings.pinnedNotification,
                        onCheckedChange = { onUpdateSettings(settings.copy(pinnedNotification = it)) },
                        showDivider = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { onUpdateSettings(ChatNotificationSettings()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Default Notification Settings", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showMuteDialog) {
        MuteDurationDialog(
            onDismiss = { showMuteDialog = false },
            onDurationSelected = { /* Handle */ }
        )
    }
}

@Composable
fun MuteDurationDialog(onDismiss: () -> Unit, onDurationSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mute Notifications") },
        text = {
            Column {
                listOf("1 hour", "8 hours", "24 hours", "7 days", "Always").forEach { duration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onDurationSelected(duration)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(selected = false, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(duration)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

