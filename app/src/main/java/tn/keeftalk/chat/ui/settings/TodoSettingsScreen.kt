package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.ui.theme.LocalAppIcons

@Composable
fun TodoSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val todo = settings.todoSettings
    val icons = LocalAppIcons.current

    var showPriorityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "My Todo", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                tn.keeftalk.chat.ui.settings.components.FeatureCloudSyncCard(
                    featureId = tn.keeftalk.chat.domain.model.entitlement.FeatureId.TODO,
                    description = "Sync your tasks and to-do lists across your devices.",
                    onEnableClick = { },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SettingsSection(title = "Task Preferences") {
                    SettingsItem(
                        icon = Icons.Default.Flag,
                        title = "Default Task Priority",
                        subtitle = todo.defaultPriority,
                        onClick = { showPriorityDialog = true }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Show Completed Tasks",
                        subtitle = "Keep finished tasks visible in lists",
                        checked = todo.showCompletedTasks,
                        onCheckedChange = { v -> viewModel.updateTodoSettings { it.copy(showCompletedTasks = v) } }
                    )
                    SettingsToggleItem(
                        icon = icons.bell,
                        title = "Due Date Reminders",
                        subtitle = "Get notified when tasks are due",
                        checked = todo.dueRemindersEnabled,
                        onCheckedChange = { v -> viewModel.updateTodoSettings { it.copy(dueRemindersEnabled = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Cloud & Sync") {
                    SettingsToggleItem(
                        icon = Icons.Default.CloudSync,
                        title = "Sync Todos Across Devices",
                        subtitle = "Synchronize task lists with Supabase",
                        checked = todo.syncTodosAcrossDevices,
                        onCheckedChange = { v -> viewModel.updateTodoSettings { it.copy(syncTodosAcrossDevices = v) } },
                        showDivider = false
                    )
                }
            }
        }

        if (showPriorityDialog) {
            AlertDialog(
                onDismissRequest = { showPriorityDialog = false },
                title = { Text("Select Default Priority") },
                text = {
                    Column {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { priority ->
                            TextButton(
                                onClick = {
                                    viewModel.updateTodoSettings { it.copy(defaultPriority = priority) }
                                    showPriorityDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(priority, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPriorityDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
