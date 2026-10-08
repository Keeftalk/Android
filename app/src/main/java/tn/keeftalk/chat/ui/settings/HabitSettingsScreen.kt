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
fun HabitSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val habits = settings.habitSettings
    val icons = LocalAppIcons.current

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "My Habits", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Behavior & Defaults") {
                    SettingsItem(
                        icon = icons.calendar,
                        title = "Default Category",
                        subtitle = habits.defaultCategory.replaceFirstChar { it.uppercase() },
                        onClick = { showCategoryDialog = true }
                    )
                    SettingsItem(
                        icon = icons.calendar,
                        title = "Week Start Day",
                        subtitle = habits.weekStartDay,
                        onClick = { showWeekStartDialog = true }
                    )
                    SettingsToggleItem(
                        icon = icons.help,
                        title = "Hide Completed Today",
                        subtitle = "Automatically filter out habits done for the day",
                        checked = habits.hideCompletedToday,
                        onCheckedChange = { v -> viewModel.updateHabitSettings { it.copy(hideCompletedToday = v) } }
                    )
                    SettingsToggleItem(
                        icon = icons.lock,
                        title = "Auto-Archive Mastered",
                        subtitle = "Automatically collapse or archive mastered habits",
                        checked = habits.autoArchiveMastered,
                        onCheckedChange = { v -> viewModel.updateHabitSettings { it.copy(autoArchiveMastered = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Notifications & Grace Pool") {
                    SettingsToggleItem(
                        icon = icons.bell,
                        title = "Grace Pool Alerts",
                        subtitle = "Warn when grace uses are running low",
                        checked = habits.gracePoolAlertsEnabled,
                        onCheckedChange = { v -> viewModel.updateHabitSettings { it.copy(gracePoolAlertsEnabled = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Cloud & Sync") {
                    SettingsToggleItem(
                        icon = Icons.Default.Refresh,
                        title = "Sync Habits Across Devices",
                        subtitle = "Keep habits, schedules, and check-ins synced with Supabase",
                        checked = habits.syncHabitsAcrossDevices,
                        onCheckedChange = { v -> viewModel.updateHabitSettings { it.copy(syncHabitsAcrossDevices = v) } },
                        showDivider = false
                    )
                }
            }
        }

        if (showCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("Select Default Category") },
                text = {
                    Column {
                        listOf("drink", "hygiene", "eating", "light", "complex").forEach { cat ->
                            TextButton(
                                onClick = {
                                    viewModel.updateHabitSettings { it.copy(defaultCategory = cat) }
                                    showCategoryDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(cat.replaceFirstChar { it.uppercase() }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showWeekStartDialog) {
            AlertDialog(
                onDismissRequest = { showWeekStartDialog = false },
                title = { Text("Select Week Start Day") },
                text = {
                    Column {
                        listOf("MONDAY", "SUNDAY").forEach { day ->
                            TextButton(
                                onClick = {
                                    viewModel.updateHabitSettings { it.copy(weekStartDay = day) }
                                    showWeekStartDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(day, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWeekStartDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
