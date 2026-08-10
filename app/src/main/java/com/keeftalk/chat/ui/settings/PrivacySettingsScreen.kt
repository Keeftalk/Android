package com.keeftalk.chat.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onProfileViewHistoryClick: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val icons = LocalAppIcons.current
    val context = LocalContext.current

    var showVisibilityPicker by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Privacy", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Profile Visibility") {
                    SettingsItem(
                        icon = icons.user,
                        title = "Profile Photo",
                        subtitle = prefs.profilePhotoVisibility.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showVisibilityPicker = "profilePhotoVisibility" }
                    )
                    SettingsItem(
                        icon = icons.info,
                        title = "About Info",
                        subtitle = prefs.aboutVisibility.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showVisibilityPicker = "aboutVisibility" }
                    )
                    SettingsItem(
                        icon = icons.clock,
                        title = "Last Seen",
                        subtitle = prefs.lastSeenVisibility.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showVisibilityPicker = "lastSeenVisibility" }
                    )
                    SettingsItem(
                        icon = icons.info,
                        title = "Connections List",
                        subtitle = prefs.connectionsVisibility.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showVisibilityPicker = "connectionsVisibility" },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Interactions") {
                    SettingsToggleItem(
                        icon = icons.check,
                        title = "Read Receipts",
                        subtitle = "If disabled, you won't send or receive read receipts",
                        checked = prefs.readReceiptsEnabled,
                        onCheckedChange = { viewModel.updatePrivacySetting("readReceiptsEnabled", it) }
                    )
                    SettingsToggleItem(
                        icon = icons.edit,
                        title = "Typing Indicators",
                        subtitle = "Allow others to see when you are typing",
                        checked = prefs.typingIndicatorsEnabled,
                        onCheckedChange = { viewModel.updatePrivacySetting("typingIndicatorsEnabled", it) }
                    )
                    SettingsItem(
                        icon = icons.block,
                        title = "Blocked Users",
                        subtitle = "Manage restricted accounts",
                        onClick = onBlockedUsersClick,
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Advanced Safety") {
                    SettingsToggleItem(
                        icon = icons.shield,
                        title = "Screenshot Protection",
                        subtitle = "Prevent screenshots in private chats",
                        checked = prefs.screenshotProtectionEnabled,
                        onCheckedChange = { viewModel.updatePrivacySetting("screenshotProtectionEnabled", it) }
                    )
                    SettingsToggleItem(
                        icon = icons.lock,
                        title = "Biometric Lock",
                        subtitle = "Use fingerprint/face to open app",
                        checked = prefs.biometricLockEnabled,
                        onCheckedChange = { viewModel.updatePrivacySetting("biometricLockEnabled", it) },
                        showDivider = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSection(title = "Account Actions") {
                    SettingsItem(
                        icon = icons.delete,
                        title = "Delete Account",
                        subtitle = "Permanently erase all your data",
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteDialog = true },
                        showDivider = false
                    )
                }
            }
        }
    }

    if (showVisibilityPicker != null) {
        val key = showVisibilityPicker!!
        val options = listOf("EVERYONE", "CONTACTS", "NOBODY")

        AlertDialog(
            onDismissRequest = { showVisibilityPicker = null },
            title = { Text("Select Visibility") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updatePrivacySetting(key, option)
                                    showVisibilityPicker = null
                                }
                                .padding(16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            val selected = when(key) {
                                "profilePhotoVisibility" -> prefs.profilePhotoVisibility
                                "aboutVisibility" -> prefs.aboutVisibility
                                "lastSeenVisibility" -> prefs.lastSeenVisibility
                                "connectionsVisibility" -> prefs.connectionsVisibility
                                else -> ""
                            }
                            RadioButton(
                                selected = (option == selected),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVisibilityPicker = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This action is permanent and cannot be undone. All your messages and profile data will be removed from Keeftalk.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
