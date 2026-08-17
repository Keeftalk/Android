package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun NoteSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val notes = settings.noteSettings

    Scaffold(
        topBar = { SettingsHeader(title = "My Notes", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsSection(title = "Behavior") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Autosave",
                        subtitle = "Automatically save notes while editing",
                        checked = notes.autoSaveEnabled,
                        onCheckedChange = { val v = it; viewModel.updateNoteSettings { it.copy(autoSaveEnabled = v) } }
                    )
                    SettingsItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Default View",
                        subtitle = notes.defaultView,
                        onClick = { /* Picker */ }
                    )
                }
            }
            item {
                SettingsSection(title = "Sync") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Sync across devices",
                        checked = notes.syncNotesAcrossDevices,
                        onCheckedChange = { val v = it; viewModel.updateNoteSettings { it.copy(syncNotesAcrossDevices = v) } }
                    )
                }
            }
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.lock,
                        title = "Require Auth to Open",
                        checked = notes.requireAuthToOpen,
                        onCheckedChange = { val v = it; viewModel.updateNoteSettings { it.copy(requireAuthToOpen = v) } }
                    )
                }
            }
        }
    }
}
