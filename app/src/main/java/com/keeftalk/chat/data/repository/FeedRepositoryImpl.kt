package com.keeftalk.chat.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.keeftalk.chat.data.local.dao.FeedArticleWithSource
import com.keeftalk.chat.data.local.dao.FeedDao
import com.keeftalk.chat.data.local.entities.FeedArticleEntity
import com.keeftalk.chat.data.local.entities.FeedSourceEntity
import com.keeftalk.chat.data.local.entities.WeatherCacheEntity
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.CuratedFeed
import com.keeftalk.chat.domain.model.CuratedFeedProvider
import com.keeftalk.chat.domain.model.FeedArticle
import com.keeftalk.chat.domain.model.FeedSource
import com.keeftalk.chat.domain.model.WeatherInfo
import com.keeftalk.chat.domain.repository.FeedRepository
import com.keeftalk.chat.domain.service.LocationService
import com.keeftalk.chat.util.ArticleExtractor
import org.jsoup.Jsoup
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "FeedRepo"

class FeedRepositoryImpl(
    private val context: Context,
    private val feedDao: FeedDao,
    private val httpClient: OkHttpClient,
    private val locationService: LocationService,
    private val prefs: UserPreferencesRepository
) : FeedRepository {

    override fun getSources(): Flow<List<FeedSource>> = feedDao.getAllSources().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getArticles(): Flow<List<FeedArticle>> = feedDao.getAllArticles().map { list ->
        list.map { it.toDomain() }
    }

    override fun getWeather(): Flow<WeatherInfo?> = feedDao.getWeatherCache().map { it?.toDomain() }

    override suspend fun addSource(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Adding feed source: $url")
        try {
            val existing = feedDao.getAllSources().first()
            if (existing.any { it.url == url }) return@withContext Result.success(Unit)

            val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch URL: $url, Code: ${response.code}")
                return@withContext Result.failure(Exception("HTTP Error ${response.code}"))
            }
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            
            val title = detectFeedTitle(body) ?: "Unknown Source"
            val id = UUID.nameUUIDFromBytes(url.toByteArray()).toString()
            
            val source = FeedSourceEntity(
                id = id,
                url = url,
                title = title,
                type = "RSS",
                lastUpdated = System.currentTimeMillis()
            )
            feedDao.upsertSource(source)
            refreshSourceInternal(source, body)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception adding source: $url", e)
            Result.failure(e)
        }
    }

    override suspend fun addCuratedSources(feeds: List<CuratedFeed>) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Adding ${feeds.size} curated feeds")
        val existing = feedDao.getAllSources().first()
        feeds.forEach { feed ->
            if (existing.none { it.url == feed.url }) {
                val id = UUID.nameUUIDFromBytes(feed.url.toByteArray()).toString()
                val source = FeedSourceEntity(
                    id = id,
                    url = feed.url,
                    title = feed.title,
                    type = "RSS",
                    lastUpdated = 0L
                )
                feedDao.upsertSource(source)
            }
        }
        refreshAll()
    }

    override suspend fun removeSource(sourceId: String) {
        val sources = feedDao.getAllSources().first()
        sources.find { it.id == sourceId }?.let {
            feedDao.deleteSource(it)
        }
    }

    override suspend fun refreshAll() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Refreshing all feeds")
        val sources = feedDao.getAllSources().first()
        sources.forEach { source ->
            try {
                Log.d(TAG, "Refreshing source: ${source.title} (${source.url})")
                val response = httpClient.newCall(Request.Builder().url(source.url).build()).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        refreshSourceInternal(source, body)
                    } else {
                        Log.w(TAG, "Empty body for source: ${source.title}")
                    }
                } else {
                    Log.e(TAG, "HTTP ${response.code} for source: ${source.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh source: ${source.title}", e)
            }
        }
    }

    private suspend fun refreshSourceInternal(source: FeedSourceEntity, xml: String) {
        val articles = parseRss(source.id, xml)
        Log.d(TAG, "Parsed ${articles.size} articles for source: ${source.title}")
        if (articles.isEmpty()) {
            Log.w(TAG, "No articles parsed for ${source.title}. XML sample: ${xml.take(200)}")
        }
        try {
            // Update source metadata FIRST without replacing it (to avoid CASCADE DELETE)
            feedDao.upsertSource(source.copy(lastUpdated = System.currentTimeMillis()))
            
            // Then update articles
            feedDao.deleteArticlesBySource(source.id)
            feedDao.insertArticles(articles)
            
            val count = feedDao.getArticleCount()
            Log.d(TAG, "Successfully saved ${articles.size} articles to DB for ${source.title}. Total articles now in DB: $count")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save articles for ${source.title}", e)
        }
    }

    private fun detectFeedTitle(xml: String): String? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && (xpp.name == "title")) {
                    return xpp.nextText()
                }
                eventType = xpp.next()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting title", e)
            null
        }
    }

    private fun parseRss(sourceId: String, xml: String): List<FeedArticleEntity> {
        val articles = mutableListOf<FeedArticleEntity>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))
            
            var eventType = xpp.eventType
            var currentTitle: String? = null
            var currentLink: String? = null
            var currentDesc: String? = null
            var currentContent: String? = null
            var currentPubDate: Long = 0L
            var currentThumb: String? = null
            var currentCategory: String? = null
            var insideItem = false

            val sourceCategory = CuratedFeedProvider.categories.find { cat -> 
                cat.feeds.any { f -> UUID.nameUUIDFromBytes(f.url.toByteArray()).toString() == sourceId } 
            }?.title

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = xpp.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            insideItem = true
                        } else if (insideItem) {
                            when (tagName) {
                                "title" -> currentTitle = xpp.nextText()
                                "link" -> {
                                    val href = xpp.getAttributeValue(null, "href")
                                    if (href != null) {
                                        currentLink = href
                                    } else {
                                        currentLink = xpp.nextText()
                                    }
                                }
                                "description", "summary" -> {
                                    if (currentDesc == null) {
                                        val text = xpp.nextText()
                                        currentDesc = text
                                        // Extract image from HTML
                                        if (currentThumb == null && text.contains("<img", ignoreCase = true)) {
                                            val src = text.substringAfter("src=\"", "").substringBefore("\"")
                                            if (src.isNotEmpty()) currentThumb = src
                                        }
                                    }
                                }
                                "content", "encoded" -> {
                                    val url = xpp.getAttributeValue(null, "url") 
                                        ?: xpp.getAttributeValue(null, "href")
                                        ?: xpp.getAttributeValue(null, "src")
                                    if (url != null) {
                                        currentThumb = url 
                                    } else {
                                        val text = xpp.nextText()
                                        currentContent = text
                                        if (currentThumb == null && text.contains("<img", ignoreCase = true)) {
                                            val src = text.substringAfter("src=\"", "").substringBefore("\"")
                                            if (src.isNotEmpty()) currentThumb = src
                                        }
                                    }
                                }
                                "pubDate", "published", "updated", "date" -> {
                                    val dateStr = xpp.nextText()
                                    currentPubDate = parseDate(dateStr)
                                }
                                "thumbnail", "enclosure", "image" -> {
                                    val url = xpp.getAttributeValue(null, "url") 
                                        ?: xpp.getAttributeValue(null, "href")
                                        ?: xpp.getAttributeValue(null, "src")
                                    if (url != null && (currentThumb == null || tagName == "thumbnail")) {
                                        currentThumb = url
                                    }
                                }
                                "category" -> {
                                    if (currentCategory == null) currentCategory = xpp.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            if (!currentTitle.isNullOrBlank() && !currentLink.isNullOrBlank()) {
                                val fullText = (currentContent ?: currentDesc ?: "")
                                val wordCount = fullText.split("\\s+".toRegex()).size
                                val readingTime = (wordCount / 200).coerceAtLeast(1)

                                articles.add(FeedArticleEntity(
                                    id = UUID.nameUUIDFromBytes(currentLink!!.toByteArray()).toString(),
                                    sourceId = sourceId,
                                    title = currentTitle!!,
                                    description = currentDesc?.take(1000),
                                    link = currentLink!!,
                                    pubDate = if (currentPubDate > 0) currentPubDate else System.currentTimeMillis(),
                                    thumbnailUrl = currentThumb,
                                    content = currentContent,
                                    category = currentCategory ?: sourceCategory,
                                    readingTimeMinutes = readingTime
                                ))
                            }
                            currentTitle = null
                            currentLink = null
                            currentDesc = null
                            currentContent = null
                            currentPubDate = 0L
                            currentThumb = null
                            currentCategory = null
                            insideItem = false
                        }
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS", e)
        }
        return articles
    }

    private fun parseDate(dateStr: String): Long {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (format.endsWith("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr.trim())?.time ?: 0L
            } catch (e: Exception) {}
        }
        return 0L
    }

    override suspend fun refreshWeather() = withContext(Dispatchers.IO) {
        val userPrefs = prefs.userPreferencesFlow.first()
        val lat: Double
        val lon: Double
        val city: String?

        if (userPrefs.weatherLocationMode == "AUTO") {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                lat = location.latitude
                lon = location.longitude
                city = getCityName(lat, lon)
            } else {
                Log.w(TAG, "Location null in AUTO mode")
                return@withContext
            }
        } else {
            val manualLoc = userPrefs.weatherManualLocation ?: "London"
            val coords = getCoordsFromCity(manualLoc)
            if (coords != null) {
                lat = coords.first
                lon = coords.second
                city = manualLoc
            } else {
                Log.w(TAG, "Failed to resolve manual location: $manualLoc")
                return@withContext
            }
        }

        try {
            Log.d(TAG, "Fetching weather for $lat, $lon ($city)")
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,visibility&daily=sunrise,sunset,uv_index_max&timezone=auto"
            val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Weather API failed: ${response.code}")
                return@withContext
            }
            val json = JSONObject(response.body?.string() ?: "")
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            
            val weather = WeatherCacheEntity(
                locationName = city ?: "Unknown",
                temperature = current.getDouble("temperature_2m").toFloat(),
                condition = mapWmoCode(current.getInt("weather_code")),
                iconCode = current.getInt("weather_code").toString(),
                humidity = current.getInt("relative_humidity_2m"),
                windSpeed = current.getDouble("wind_speed_10m").toFloat(),
                apparentTemperature = current.getDouble("apparent_temperature").toFloat(),
                uvIndex = daily.getJSONArray("uv_index_max").getDouble(0).toFloat(),
                visibility = current.getDouble("visibility").toFloat() / 1000f, // to km
                pressure = current.getDouble("pressure_msl").toFloat(),
                sunrise = daily.getJSONArray("sunrise").getString(0).substringAfter("T"),
                sunset = daily.getJSONArray("sunset").getString(0).substringAfter("T"),
                isDay = current.getInt("is_day") == 1,
                cloudCover = current.getInt("cloud_cover"),
                precipitation = current.getDouble("precipitation").toFloat(),
                timestamp = System.currentTimeMillis()
            )
            feedDao.updateWeatherCache(weather)
            Log.d(TAG, "Weather updated: ${weather.locationName}, ${weather.temperature}°")
        } catch (e: Exception) {
            Log.e(TAG, "Weather refresh failed", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun getCityName(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.adminArea
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getCoordsFromCity(cityName: String): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(cityName, 1)
            addresses?.firstOrNull()?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapWmoCode(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing Drizzle"
        61, 63, 65 -> "Rainy"
        66, 67 -> "Freezing Rain"
        71, 73, 75 -> "Snowy"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    override fun shutdown() {}

    override suspend fun getFullArticle(articleId: String): FeedArticle? = withContext(Dispatchers.IO) {
        val articleWithSource = feedDao.getArticleById(articleId).first() ?: return@withContext null
        val entity = articleWithSource.article
        
        // If already extracted, just return
        if (entity.isExtracted) return@withContext articleWithSource.toDomain()
        
        // If not extracted, check if content looks like a full article (heuristic)
        val currentContent = entity.content ?: entity.description ?: ""
        if (currentContent.length > 3000) {
            // Probably full enough
            return@withContext articleWithSource.toDomain()
        }
        
        // Extract
        Log.d(TAG, "Attempting to extract full article for: ${entity.title}")
        val extracted = ArticleExtractor.extract(entity.link, httpClient)
        
        if (extracted != null) {
            val wordCount = Jsoup.parse(extracted.contentHtml).text().split("\\s+".toRegex()).size
            val readingTime = (wordCount / 200).coerceAtLeast(1)

            val updatedEntity = entity.copy(
                content = extracted.contentHtml,
                author = extracted.author,
                thumbnailUrl = extracted.heroImageUrl ?: entity.thumbnailUrl,
                isExtracted = true,
                readingTimeMinutes = readingTime
            )
            feedDao.insertArticles(listOf(updatedEntity)) // Room will upsert
            
            // Re-fetch to get source join
            return@withContext feedDao.getArticleById(articleId).first()?.toDomain()
        }
        
        articleWithSource.toDomain()
    }

    override suspend fun prefetchArticles(articleIds: List<String>) = coroutineScope {
        val semaphore = kotlinx.coroutines.sync.Semaphore(3)
        articleIds.forEach { id ->
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        val article = feedDao.getArticleById(id).first() ?: return@withPermit
                        if (!article.article.isExtracted) {
                            getFullArticle(id)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Prefetch failed for $id", e)
                    }
                }
            }
        }
    }
}

fun FeedSourceEntity.toDomain() = FeedSource(id, url, title, type, iconUrl, lastUpdated)
fun FeedArticleWithSource.toDomain() = FeedArticle(
    id = article.id,
    sourceId = article.sourceId,
    title = article.title,
    description = article.description,
    link = article.link,
    pubDate = article.pubDate,
    thumbnailUrl = article.thumbnailUrl,
    sourceName = sourceName,
    content = article.content,
    author = article.author,
    isExtracted = article.isExtracted,
    category = article.category,
    readingTimeMinutes = article.readingTimeMinutes
)
fun WeatherCacheEntity.toDomain() = WeatherInfo(
    temperature, condition, iconCode, locationName, humidity, windSpeed,
    apparentTemperature, uvIndex, visibility, pressure, sunrise, sunset, isDay, cloudCover, precipitation, timestamp
)
