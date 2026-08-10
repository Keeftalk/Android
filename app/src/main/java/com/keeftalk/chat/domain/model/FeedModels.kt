package com.keeftalk.chat.domain.model

data class FeedSource(
    val id: String,
    val url: String,
    val title: String,
    val type: String,
    val iconUrl: String?,
    val lastUpdated: Long
)

data class FeedArticle(
    val id: String,
    val sourceId: String,
    val title: String,
    val description: String?,
    val link: String,
    val pubDate: Long,
    val thumbnailUrl: String? = null,
    val sourceName: String? = null,
    val content: String? = null,
    val author: String? = null,
    val isExtracted: Boolean = false,
    val category: String? = null,
    val readingTimeMinutes: Int = 0
)

data class WeatherInfo(
    val temperature: Float,
    val condition: String,
    val iconCode: String,
    val locationName: String,
    val humidity: Int,
    val windSpeed: Float,
    val apparentTemperature: Float,
    val uvIndex: Float,
    val visibility: Float,
    val pressure: Float,
    val sunrise: String,
    val sunset: String,
    val isDay: Boolean,
    val cloudCover: Int,
    val precipitation: Float,
    val timestamp: Long
)
