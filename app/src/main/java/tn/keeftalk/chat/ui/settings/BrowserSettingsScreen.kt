package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.domain.model.UserBrowserSettings
import tn.keeftalk.chat.ui.components.KeeftalkFeatureTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val fullSettings by viewModel.fullSettings.collectAsState()
    val browserSettings = fullSettings.browserSettings

    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = "My Browser Settings",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Section 1: Weather and time
            item {
                SectionHeader("WEATHER AND TIME")
            }
            item {
                SettingsSegmentedItem(
                    icon = Icons.Default.Thermostat,
                    title = "Temperature",
                    options = listOf("C" to "°C", "F" to "°F"),
                    selectedKey = browserSettings.unit,
                    onSelect = { u -> viewModel.updateBrowserSettings { it.copy(unit = u) } }
                )
            }
            item {
                SettingsSegmentedItem(
                    icon = Icons.Default.Timer,
                    title = "Clock",
                    options = listOf("true" to "24h", "false" to "12h"),
                    selectedKey = browserSettings.h24.toString(),
                    onSelect = { h -> viewModel.updateBrowserSettings { it.copy(h24 = h.toBoolean()) } }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.WbSunny,
                    title = "Weather effects",
                    subtitle = "Clouds, rain, snow and lightning",
                    checked = browserSettings.fx,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(fx = v) } }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Thunder sound",
                    subtitle = "A low rumble that follows each flash",
                    checked = browserSettings.sound,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(sound = v) } }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Vibration,
                    title = "Haptics",
                    subtitle = "Vibrate on close lightning",
                    checked = browserSettings.haptic,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(haptic = v) } }
                )
            }

            // Section 2: Privacy
            item {
                SectionHeader("PRIVACY")
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Security,
                    title = "Block trackers",
                    subtitle = "Hides ads and trackers on pages",
                    checked = browserSettings.trk,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(trk = v) } }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Lock,
                    title = "Block third-party cookies",
                    subtitle = "Stops cross-site tracking cookies",
                    checked = browserSettings.b3p,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(b3p = v) } }
                )
            }

            // Section 3: Search
            item {
                SectionHeader("SEARCH")
            }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Search,
                    title = "Search engine",
                    subtitle = browserSettings.searchEngine,
                    onClick = { showSearchEngineDialog = true }
                )
            }

            // Section 4: Home screen
            item {
                SectionHeader("HOME SCREEN")
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Home,
                    title = "Quick links",
                    subtitle = "Display quick links grid on home screen",
                    checked = browserSettings.ql,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(ql = v) } }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Home,
                    title = "Open tabs",
                    subtitle = "Display open tabs rail on home screen",
                    checked = browserSettings.tabs,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(tabs = v) } }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Home,
                    title = "Reading list",
                    subtitle = "Display reading list card on home screen",
                    checked = browserSettings.reads,
                    onCheckedChange = { v -> viewModel.updateBrowserSettings { it.copy(reads = v) } }
                )
            }

            // Section 5: Location
            item {
                SectionHeader("LOCATION")
            }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.LocationOn,
                    title = "Use device location",
                    subtitle = "Makes the sun and moon exact for where you are",
                    onClick = {
                        // Location handled via system/AstronomyEngine
                    }
                )
            }

            // Section 6: Data & Reset
            item {
                SectionHeader("DATA")
            }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Delete,
                    title = "Clear tabs and history",
                    subtitle = "Closes every tab and clears browsing record",
                    onClick = { showClearDataDialog = true }
                )
            }
            item {
                SettingsClickableItem(
                    icon = Icons.Default.Restore,
                    title = "Reset settings",
                    subtitle = "Back to default settings",
                    onClick = { showResetDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSearchEngineDialog) {
        val engines = listOf("Keeftalk", "Private", "Web", "Google", "DuckDuckGo", "Bing", "Ecosia")
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Select Search Engine") },
            text = {
                Column {
                    engines.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateBrowserSettings { it.copy(searchEngine = engine) }
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (browserSettings.searchEngine == engine),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = engine, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Tabs and History") },
            text = { Text("Are you sure you want to close all open tabs and clear history?") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            android.webkit.WebView(context).clearCache(true)
                        } catch (_: Exception) {}
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Reset all My Browser settings to defaults?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBrowserSettings { UserBrowserSettings() }
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsSegmentedItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    options.forEach { (key, label) ->
                        val isSelected = key == selectedKey
                        Surface(
                            onClick = { onSelect(key) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (isSelected) 1.dp else 0.dp
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
