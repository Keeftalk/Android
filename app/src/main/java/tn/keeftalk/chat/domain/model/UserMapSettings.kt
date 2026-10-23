package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMapSettings(
    val defaultMapType: String = "NORMAL",
    val highAccuracyGps: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val distanceUnit: String = "KM",
    val syncLocationsAcrossDevices: Boolean = true
)
