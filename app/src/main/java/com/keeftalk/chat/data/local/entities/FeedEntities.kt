package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "feed_sources")
data class FeedSourceEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val type: String, // "RSS", "NEWS", etc.
    val iconUrl: String? = null,
    val lastUpdated: Long = 0L
)

@Entity(
    tableName = "feed_articles",
    foreignKeys = [
        ForeignKey(
            entity = FeedSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sourceId"])]
)
data class FeedArticleEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val title: String,
    val description: String?,
    val link: String,
    val pubDate: Long,
    val thumbnailUrl: String? = null,
    val content: String? = null,
    val author: String? = null,
    val isExtracted: Boolean = false,
    val category: String? = null,
    val readingTimeMinutes: Int = 0
)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val id: String = "current",
    val locationName: String,
    val temperature: Float,
    val condition: String,
    val iconCode: String,
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
