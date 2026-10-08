package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun ParentalControlsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val pc = settings.parentalControls
    val icons = tn.keeftalk.chat.ui.theme.LocalAppIcons.current

    Scaffold(
        topBar = { SettingsHeader(title = "Parental Controls", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsSection(title = "Status") {
                    SettingsToggleItem(
                        icon = icons.shield,
                        title = "Enable Controls",
                        subtitle = if (pc.isEnabled) "Currently active" else "Disabled",
                        checked = pc.isEnabled,
                        onCheckedChange = { val v = it; viewModel.updateParentalControls { it.copy(isEnabled = v) } }
                    )
                }
            }
            if (pc.isEnabled) {
                item {
                    SettingsSection(title = "Communication") {
                        SettingsItem(
                            icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                            title = "Allowed Contacts",
                            subtitle = pc.allowedContactLevel,
                            onClick = { /* Picker */ }
                        )
                        SettingsToggleItem(
                            icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                            title = "Can Message Unknown",
                            checked = pc.canMessageUnknown,
                            onCheckedChange = { val v = it; viewModel.updateParentalControls { it.copy(canMessageUnknown = v) } }
                        )
                    }
                }
            }
        }
    }
}
