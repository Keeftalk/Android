package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun CalendarSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val cal = settings.calendarSettings

    Scaffold(
        topBar = { SettingsHeader(title = "My Calendar", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsSection(title = "Calendar") {
                    SettingsItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.calendar,
                        title = "Default View",
                        subtitle = cal.defaultView,
                        onClick = { /* Picker */ }
                    )
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Event Notifications",
                        checked = cal.eventNotificationsEnabled,
                        onCheckedChange = { val v = it; viewModel.updateCalendarSettings { it.copy(eventNotificationsEnabled = v) } }
                    )
                }
            }
            item {
                SettingsSection(title = "Sync") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Sync Device Calendar",
                        checked = cal.syncDeviceCalendar,
                        onCheckedChange = { val v = it; viewModel.updateCalendarSettings { it.copy(syncDeviceCalendar = v) } }
                    )
                }
            }
        }
    }
}
