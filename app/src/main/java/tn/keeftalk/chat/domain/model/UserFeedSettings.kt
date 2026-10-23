package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserFeedSettings(
    val autoRefreshEnabled: Boolean = true,
    val defaultLayoutMode: String = "MOSAIC", // MOSAIC, STACKED, SWIPE
    val showWeatherCard: Boolean = true,
    val showPrayerCard: Boolean = true,
    val showVerseCard: Boolean = true,
    val syncFeedsAcrossDevices: Boolean = true
)
