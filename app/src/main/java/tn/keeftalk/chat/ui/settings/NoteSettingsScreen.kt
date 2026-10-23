package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NoteSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val notes = settings.noteSettings

    var showDefaultViewDialog by remember { mutableStateOf(false) }
    var showSortingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "My Notes", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                tn.keeftalk.chat.ui.settings.components.FeatureCloudSyncCard(
                    featureId = tn.keeftalk.chat.domain.model.entitlement.FeatureId.NOTES,
                    description = "Sync your notes across your devices.",
                    onEnableClick = { },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                SettingsSection(title = "Behavior") {
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Autosave",
                        subtitle = "Automatically save notes while editing",
                        checked = notes.autoSaveEnabled,
                        onCheckedChange = { val v = it; viewModel.updateNoteSettings { it.copy(autoSaveEnabled = v) } }
                    )
                    SettingsItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Default View",
                        subtitle = notes.defaultView,
                        onClick = { showDefaultViewDialog = true }
                    )
                    SettingsItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Default Sorting",
                        subtitle = notes.defaultSorting,
                        onClick = { showSortingDialog = true }
                    )
                }
            }
            item {
                SettingsSection(title = "Sync") {
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Sync across devices",
                        checked = notes.syncNotesAcrossDevices,
                        onCheckedChange = { val v = it; viewModel.updateNoteSettings { it.copy(syncNotesAcrossDevices = v) } }
                    )
                }
            }
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.lock,
                        title = "Require Auth to Open",
                        checked = notes.requireAuthToOpen,
                        onCheckedChange = { val v = it; viewModel.updateNoteSettings { it.copy(requireAuthToOpen = v) } }
                    )
                }
            }
        }

        if (showDefaultViewDialog) {
            AlertDialog(
                onDismissRequest = { showDefaultViewDialog = false },
                title = { Text("Select Default View") },
                text = {
                    Column {
                        listOf("GRID", "LIST").forEach { view ->
                            TextButton(
                                onClick = {
                                    viewModel.updateNoteSettings { it.copy(defaultView = view) }
                                    showDefaultViewDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(view, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDefaultViewDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showSortingDialog) {
            AlertDialog(
                onDismissRequest = { showSortingDialog = false },
                title = { Text("Select Default Sorting") },
                text = {
                    Column {
                        listOf("UPDATED_AT", "CREATED_AT", "TITLE").forEach { sorting ->
                            TextButton(
                                onClick = {
                                    viewModel.updateNoteSettings { it.copy(defaultSorting = sorting) }
                                    showSortingDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(sorting, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortingDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
