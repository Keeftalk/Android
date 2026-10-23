package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMarketSettings(
    val defaultCurrency: String = "USD",
    val autoRefreshIntervalSeconds: Int = 30,
    val priceAlertsEnabled: Boolean = true,
    val showTrendCharts: Boolean = true,
    val syncWatchlistsAcrossDevices: Boolean = true
)
