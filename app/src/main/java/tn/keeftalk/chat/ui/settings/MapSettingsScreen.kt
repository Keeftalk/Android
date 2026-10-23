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
fun MapSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val map = settings.mapSettings
    val icons = LocalAppIcons.current

    var showMapTypeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "My Maps", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Map & Navigation") {
                    SettingsItem(
                        icon = Icons.Default.Map,
                        title = "Default Map Type",
                        subtitle = map.defaultMapType,
                        onClick = { showMapTypeDialog = true }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.GpsFixed,
                        title = "High Accuracy GPS",
                        subtitle = "Use high precision location tracking",
                        checked = map.highAccuracyGps,
                        onCheckedChange = { v -> viewModel.updateMapSettings { it.copy(highAccuracyGps = v) } }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Storage,
                        title = "Offline Map Cache",
                        subtitle = "Cache tiles locally for offline viewing",
                        checked = map.offlineCacheEnabled,
                        onCheckedChange = { v -> viewModel.updateMapSettings { it.copy(offlineCacheEnabled = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Cloud & Sync") {
                    SettingsToggleItem(
                        icon = Icons.Default.CloudSync,
                        title = "Sync Locations Across Devices",
                        subtitle = "Keep saved pins and locations synced",
                        checked = map.syncLocationsAcrossDevices,
                        onCheckedChange = { v -> viewModel.updateMapSettings { it.copy(syncLocationsAcrossDevices = v) } },
                        showDivider = false
                    )
                }
            }
        }

        if (showMapTypeDialog) {
            AlertDialog(
                onDismissRequest = { showMapTypeDialog = false },
                title = { Text("Select Default Map Type") },
                text = {
                    Column {
                        listOf("NORMAL", "SATELLITE", "TERRAIN", "HYBRID").forEach { type ->
                            TextButton(
                                onClick = {
                                    viewModel.updateMapSettings { it.copy(defaultMapType = type) }
                                    showMapTypeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(type, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMapTypeDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
