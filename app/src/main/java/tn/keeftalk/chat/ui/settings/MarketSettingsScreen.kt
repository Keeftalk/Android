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
fun MarketSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val market = settings.marketSettings
    val icons = LocalAppIcons.current

    var showCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "My Market", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Currencies & Display") {
                    SettingsItem(
                        icon = icons.finances,
                        title = "Default Currency",
                        subtitle = market.defaultCurrency,
                        onClick = { showCurrencyDialog = true }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.ShowChart,
                        title = "Show Trend Analysis Charts",
                        subtitle = "Display sparkline and technical charts for tickers",
                        checked = market.showTrendCharts,
                        onCheckedChange = { v -> viewModel.updateMarketSettings { it.copy(showTrendCharts = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Alerts & Refresh") {
                    SettingsToggleItem(
                        icon = icons.bell,
                        title = "Price Alert Notifications",
                        subtitle = "Notify on significant market movements",
                        checked = market.priceAlertsEnabled,
                        onCheckedChange = { v -> viewModel.updateMarketSettings { it.copy(priceAlertsEnabled = v) } }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Refresh,
                        title = "Sync Watchlists Across Devices",
                        subtitle = "Synchronize your tracked stocks and crypto",
                        checked = market.syncWatchlistsAcrossDevices,
                        onCheckedChange = { v -> viewModel.updateMarketSettings { it.copy(syncWatchlistsAcrossDevices = v) } },
                        showDivider = false
                    )
                }
            }
        }

        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Default Currency") },
                text = {
                    Column {
                        listOf("USD", "EUR", "GBP", "BTC", "ETH").forEach { curr ->
                            TextButton(
                                onClick = {
                                    viewModel.updateMarketSettings { it.copy(defaultCurrency = curr) }
                                    showCurrencyDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(curr, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
