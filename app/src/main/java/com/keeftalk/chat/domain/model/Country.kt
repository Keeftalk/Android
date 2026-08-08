package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String,
    val isoCode: String, // e.g., "TN"
    val phoneCode: String, // e.g., "+216"
    val flagEmoji: String
)
