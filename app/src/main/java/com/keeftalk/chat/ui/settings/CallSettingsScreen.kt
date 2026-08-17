package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.PrivacyVisibility

@Composable
fun CallSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val calls = settings.callSettings

    Scaffold(
        topBar = { SettingsHeader(title = "My Calls", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsSection(title = "Calling") {
                    SettingsItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.phone,
                        title = "Incoming Call Behavior",
                        subtitle = calls.incomingCallBehavior.replace("_", " "),
                        onClick = { /* Show dialog */ }
                    )
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Speaker on Start",
                        subtitle = "Always start calls on speakerphone",
                        checked = calls.speakerOnStart,
                        onCheckedChange = { val value = it; viewModel.updateCallSettings { it.copy(speakerOnStart = value) } }
                    )
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Bluetooth Auto-Connect",
                        subtitle = "Automatically connect to Bluetooth headsets",
                        checked = calls.bluetoothAutoConnect,
                        onCheckedChange = { val value = it; viewModel.updateCallSettings { it.copy(bluetoothAutoConnect = value) } }
                    )
                }
            }

            item {
                SettingsSection(title = "Privacy") {
                    SettingsItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.lock,
                        title = "Who can call me",
                        subtitle = calls.whoCanCallMe.name,
                        onClick = { /* Show visibility picker */ }
                    )
                    SettingsItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Unknown Callers",
                        subtitle = calls.unknownCallerBehavior,
                        onClick = { /* Show behavior picker */ }
                    )
                }
            }

            item {
                SettingsSection(title = "Data") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Wi-Fi Only Calling",
                        subtitle = "Prevent calls over mobile data",
                        checked = calls.wifiOnlyCalling,
                        onCheckedChange = { val value = it; viewModel.updateCallSettings { it.copy(wifiOnlyCalling = value) } }
                    )
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Data Saving Mode",
                        subtitle = "Reduce call quality to save data",
                        checked = calls.dataSavingMode,
                        onCheckedChange = { val value = it; viewModel.updateCallSettings { it.copy(dataSavingMode = value) } }
                    )
                }
            }
        }
    }
}
