package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatCleanupSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit
) {
    val icons = LocalAppIcons.current
    var showConfirmReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Cleanup Tools", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Historical Data") {
                    SettingsItem(
                        icon = icons.search,
                        title = "Clear Search History",
                        subtitle = "Deletes all local message searches",
                        onClick = { viewModel.clearSearchHistory() }
                    )
                    SettingsItem(
                        icon = icons.emoji,
                        title = "Reset Recent Emojis",
                        subtitle = "Clears most frequently used list",
                        onClick = { viewModel.clearRecentEmojis() },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "App Data") {
                    SettingsItem(
                        icon = icons.palette,
                        title = "Reset Appearance",
                        subtitle = "Restore default theme and bubbles",
                        onClick = { showConfirmReset = true }
                    )
                    SettingsItem(
                        icon = icons.delete,
                        title = "Mass Delete Downloads",
                        subtitle = "Erase all downloaded media files",
                        onClick = { /* Delete logic */ },
                        contentColor = MaterialTheme.colorScheme.error,
                        showDivider = false
                    )
                }
            }
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Restore Defaults?") },
            text = { Text("This will reset all your theme and chat behavior customizations to their factory settings.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.resetToDefault()
                        showConfirmReset = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

