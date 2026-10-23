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
fun FeedSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val feed = settings.feedSettings
    val icons = LocalAppIcons.current

    var showLayoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "My Feed", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                tn.keeftalk.chat.ui.settings.components.FeatureCloudSyncCard(
                    featureId = tn.keeftalk.chat.domain.model.entitlement.FeatureId.FEED,
                    description = "Sync your saved Feed data and preferences across your devices.",
                    onEnableClick = { },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SettingsSection(title = "Aggregator & Layout") {
                    SettingsItem(
                        icon = icons.calendar,
                        title = "Default Layout Mode",
                        subtitle = feed.defaultLayoutMode,
                        onClick = { showLayoutDialog = true }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Refresh,
                        title = "Auto-Refresh Feeds",
                        subtitle = "Periodically fetch latest RSS articles",
                        checked = feed.autoRefreshEnabled,
                        onCheckedChange = { v -> viewModel.updateFeedSettings { it.copy(autoRefreshEnabled = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Contextual Widget Cards") {
                    SettingsToggleItem(
                        icon = icons.calendar,
                        title = "Show Weather Card",
                        subtitle = "Display live weather widget in feed",
                        checked = feed.showWeatherCard,
                        onCheckedChange = { v -> viewModel.updateFeedSettings { it.copy(showWeatherCard = v) } }
                    )
                    SettingsToggleItem(
                        icon = icons.calendar,
                        title = "Show Prayer Times Card",
                        subtitle = "Display Islamic prayer times widget",
                        checked = feed.showPrayerCard,
                        onCheckedChange = { v -> viewModel.updateFeedSettings { it.copy(showPrayerCard = v) } }
                    )
                    SettingsToggleItem(
                        icon = icons.calendar,
                        title = "Show Verse Card",
                        subtitle = "Display daily inspirational verse",
                        checked = feed.showVerseCard,
                        onCheckedChange = { v -> viewModel.updateFeedSettings { it.copy(showVerseCard = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Cloud & Sync") {
                    SettingsToggleItem(
                        icon = Icons.Default.CloudSync,
                        title = "Sync Feeds Across Devices",
                        subtitle = "Keep saved feeds and articles synchronized",
                        checked = feed.syncFeedsAcrossDevices,
                        onCheckedChange = { v -> viewModel.updateFeedSettings { it.copy(syncFeedsAcrossDevices = v) } },
                        showDivider = false
                    )
                }
            }
        }

        if (showLayoutDialog) {
            AlertDialog(
                onDismissRequest = { showLayoutDialog = false },
                title = { Text("Select Layout Mode") },
                text = {
                    Column {
                        listOf("MOSAIC", "STACKED", "SWIPE").forEach { mode ->
                            TextButton(
                                onClick = {
                                    viewModel.updateFeedSettings { it.copy(defaultLayoutMode = mode) }
                                    showLayoutDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(mode, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLayoutDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
