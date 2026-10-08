package tn.keeftalk.chat.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import tn.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSendTestNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings by viewModel.fullSettings.collectAsState()
    val notificationPrefs = settings.notificationSettings
    val icons = LocalAppIcons.current

    val hasOsPermission = remember(notificationPrefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Notification Settings", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Android OS Permission Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasOsPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (hasOsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasOsPermission) "Android Notification Permission Active" else "Android Permission Denied",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (hasOsPermission) "Keeftalk is authorized to post system notifications."
                                       else "Android has blocked Keeftalk notifications. Open OS settings to enable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!hasOsPermission) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Fix", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Global Master Switch
            item {
                SettingsSection(title = "Master Control") {
                    SettingsToggleItem(
                        icon = icons.bell,
                        title = "Master Notifications",
                        subtitle = "Global switch for all Keeftalk notifications",
                        checked = notificationPrefs.allEnabled,
                        onCheckedChange = { value ->
                            viewModel.updateNotificationSettings { it.copy(allEnabled = value) }
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Delivery Layers
                        SettingsSection(title = "Notification Layers") {
                            SettingsToggleItem(
                                icon = icons.sparkles,
                                title = "In-App Banners & Dropdown",
                                subtitle = "Show notifications inside Keeftalk",
                                checked = notificationPrefs.inAppEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(inAppEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.bell,
                                title = "Android System Notifications",
                                subtitle = "Show system notifications outside the app",
                                checked = notificationPrefs.systemEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(systemEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.send,
                                title = "FCM Push Delivery",
                                subtitle = "Receive background push notifications via Firebase",
                                checked = notificationPrefs.fcmEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(fcmEnabled = value) }
                                },
                                showDivider = false
                            )
                        }

                        // Privacy & Details
                        SettingsSection(title = "Privacy & Content Previews") {
                            SettingsToggleItem(
                                icon = icons.info,
                                title = "Show Message Previews",
                                subtitle = "Display text content in notification previews",
                                checked = notificationPrefs.showPreview,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(showPreview = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.lock,
                                title = "Hide Sensitive Content",
                                subtitle = "Mask Vault, Secrets, Wallet & Finances preview text",
                                checked = notificationPrefs.hideSensitiveContent,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(hideSensitiveContent = value) }
                                },
                                showDivider = false
                            )
                        }

                        // App & System Activity
                        SettingsSection(title = "App & System Activity") {
                            SettingsToggleItem(
                                icon = icons.info,
                                title = "Profile View Alerts",
                                subtitle = "Alert when someone views your profile",
                                checked = notificationPrefs.profileViewsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(profileViewsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.shield,
                                title = "Activity Log Alerts",
                                subtitle = "Security logins and session updates",
                                checked = notificationPrefs.activityLogsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(activityLogsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.sparkles,
                                title = "System Nudges",
                                subtitle = "App feature tips and nudges",
                                checked = notificationPrefs.systemNudgesEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(systemNudgesEnabled = value) }
                                },
                                showDivider = false
                            )
                        }

                        // Per-Feature Controls (All 15 Features)
                        SettingsSection(title = "Feature Notifications (15 Features)") {
                            SettingsToggleItem(
                                icon = icons.chat,
                                title = "My Chats",
                                subtitle = "Private messages, groups, mentions & replies",
                                checked = notificationPrefs.chatsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(chatsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.phone,
                                title = "My Calls",
                                subtitle = "Incoming and missed calls",
                                checked = notificationPrefs.callsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(callsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.chat,
                                title = "My SMS/MMS",
                                subtitle = "Incoming text messages and MMS",
                                checked = notificationPrefs.smsMmsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(smsMmsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.note,
                                title = "My Notes",
                                subtitle = "Note reminders and shared notes",
                                checked = notificationPrefs.notesEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(notesEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.lock,
                                title = "My Vault",
                                subtitle = "Backup status & file transfers (Masked)",
                                checked = notificationPrefs.vaultEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(vaultEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.key,
                                title = "My Secrets",
                                subtitle = "Secret expirations & security alerts (Masked)",
                                checked = notificationPrefs.secretsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(secretsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.calendar,
                                title = "My Calendar",
                                subtitle = "Event reminders and all-day event alerts",
                                checked = notificationPrefs.calendarEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(calendarEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.wallet,
                                title = "My Wallet",
                                subtitle = "Card & account activity (Masked)",
                                checked = notificationPrefs.walletEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(walletEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.finances,
                                title = "My Finances",
                                subtitle = "Budgets, debt due & salary alerts (Masked)",
                                checked = notificationPrefs.financesEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(financesEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.email,
                                title = "My E-Mail",
                                subtitle = "New email and sync alerts",
                                checked = notificationPrefs.emailEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(emailEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.feed,
                                title = "My Feed",
                                subtitle = "Feed updates and daily prayer reminders",
                                checked = notificationPrefs.feedEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(feedEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.note,
                                title = "My Books",
                                subtitle = "Daily reading goals & streak milestones",
                                checked = notificationPrefs.booksEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(booksEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.feed,
                                title = "My Market",
                                subtitle = "Listings, orders & buyer/seller messages",
                                checked = notificationPrefs.marketEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(marketEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.sparkles,
                                title = "My Habits",
                                subtitle = "Time slot alerts, expiry warnings & grace tactics",
                                checked = notificationPrefs.habitsEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(habitsEnabled = value) }
                                }
                            )
                            SettingsToggleItem(
                                icon = icons.info,
                                title = "My Browser",
                                subtitle = "Download progress & completion alerts",
                                checked = notificationPrefs.browserEnabled,
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(browserEnabled = value) }
                                },
                                showDivider = false
                            )
                        }


                        // Sound & Vibration
                        SettingsSection(title = "Sound & Vibration") {
                            SettingsItem(
                                icon = icons.bell,
                                title = "Notification Tone",
                                subtitle = notificationPrefs.sound,
                                onClick = { }
                            )
                            SettingsToggleItem(
                                icon = icons.palette,
                                title = "Vibration",
                                checked = notificationPrefs.vibration != "NONE",
                                onCheckedChange = { value ->
                                    viewModel.updateNotificationSettings { it.copy(vibration = if (value) "NORMAL" else "NONE") }
                                },
                                showDivider = false
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.sendTestNotification(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = notificationPrefs.allEnabled
                ) {
                    Text("Send Test Notification", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
