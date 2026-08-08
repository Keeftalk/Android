package com.keeftalk.chat.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSendTestNotification: () -> Unit = {}
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val notificationPrefs = prefs.notifications
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Notifications", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Main Controls") {
                    SettingsToggleItem(
                        icon = icons.bell,
                        title = "All Notifications",
                        subtitle = "Global master switch",
                        checked = notificationPrefs.allEnabled,
                        onCheckedChange = { value ->
                            viewModel.updateNotificationPreferences { it.copy(allEnabled = value) }
                        },
                        showDivider = false
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = notificationPrefs.allEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSection(title = "Message Alerts") {
                            SettingsToggleItem(
                                icon = icons.chat,
                                title = "Private Messages",
                                checked = notificationPrefs.messagesEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationPreferences { it.copy(messagesEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.user, // Using user for groups
                                title = "Group Chats",
                                checked = notificationPrefs.groupsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationPreferences { it.copy(groupsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.phone,
                                title = "Incoming Calls",
                                checked = notificationPrefs.callsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationPreferences { it.copy(callsEnabled = value) }
                                },
                                showDivider = false
                            )
                        }

                        SettingsSection(title = "Details") {
                            SettingsToggleItem(
                                icon = icons.info,
                                title = "Show Message Preview",
                                subtitle = "Display message text in notifications",
                                checked = notificationPrefs.showPreviewContent,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationPreferences { it.copy(showPreviewContent = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.sparkles,
                                title = "In-App Banners",
                                subtitle = "Show notifications while using the app",
                                checked = notificationPrefs.inAppNotificationsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationPreferences { it.copy(inAppNotificationsEnabled = value) }
                                },
                                showDivider = false
                            )
                        }

                        SettingsSection(title = "Sound & Vibration") {
                            SettingsItem(
                                icon = icons.bell,
                                title = "Notification Tone",
                                subtitle = notificationPrefs.sound,
                                onClick = { /* Show sound picker */ }
                            )
                            SettingsToggleItem(
                                icon = icons.palette, // Using palette for LED
                                title = "LED Light",
                                checked = notificationPrefs.ledEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationPreferences { it.copy(ledEnabled = value) }
                                },
                                showDivider = false
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSendTestNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = notificationPrefs.allEnabled
                ) {
                    Text("Send Test Notification")
                }
            }
        }
    }
}

