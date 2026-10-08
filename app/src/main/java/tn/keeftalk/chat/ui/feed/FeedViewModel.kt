package tn.keeftalk.chat.ui.feed

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.data.service.LocationServiceImpl
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.prayer.*
import tn.keeftalk.chat.domain.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    // User Preferences Flow
    val userPrefs = userPrefsRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tn.keeftalk.chat.data.prefs.UserPreferences())

    // Sources
    val sources: StateFlow<List<FeedSource>> = feedRepository.getSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Selection
    private val _selectedCategory = MutableStateFlow("For You")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Articles & Filtering
    val articles: StateFlow<List<FeedArticle>> = feedRepository.getArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArticles: StateFlow<List<FeedArticle>> = combine(articles, _selectedCategory) { articles, category ->
        if (category == "For You" || category == "foryou") articles
        else articles.filter { it.category.equals(category, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pagination State (30 initial, load 30 more on scroll)
    private val _visibleArticleCount = MutableStateFlow(30)
    val visibleArticleCount: StateFlow<Int> = _visibleArticleCount.asStateFlow()

    val pagedArticles: StateFlow<List<FeedArticle>> = combine(filteredArticles, _visibleArticleCount) { list, count ->
        list.take(count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadMoreArticles() {
        val current = _visibleArticleCount.value
        val total = filteredArticles.value.size
        if (current < total) {
            _visibleArticleCount.value = (current + 30).coerceAtMost(total)
        }
    }

    val categories: StateFlow<List<String>> = articles.map { list ->
        listOf("For You", "Technology", "World", "Science", "Design")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("For You", "Technology", "World", "Science", "Design"))

    // Weather State
    val weather: StateFlow<WeatherInfo?> = feedRepository.getWeather()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isWeatherExpanded = MutableStateFlow(false)
    val isWeatherExpanded: StateFlow<Boolean> = _isWeatherExpanded.asStateFlow()

    private val _weatherDayIndex = MutableStateFlow(0)
    val weatherDayIndex: StateFlow<Int> = _weatherDayIndex.asStateFlow()

    // Real User Coordinates State (lat, lon)
    private val _weatherCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _prayerCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)

    // Prayer State
    private val _prayerDayIndex = MutableStateFlow(0)
    val prayerDayIndex: StateFlow<Int> = _prayerDayIndex.asStateFlow()

    val hijriDateText: StateFlow<String> = flow {
        emit(HijriCalendar.getHijriDate().format())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HijriCalendar.getHijriDate().format())

    val prayerTimesFlow: StateFlow<List<PrayerTime>> = combine(
        userPrefs,
        _prayerDayIndex,
        _prayerCoordinates
    ) { prefs, dayIdx, coords ->
        if (coords == null) {
            emptyList()
        } else {
            calculateDailyPrayerTimes(
                latitude = coords.first,
                longitude = coords.second,
                methodName = prefs.prayerCalculationMethod,
                madhhabName = prefs.prayerAsrMadhhab,
                dayOffset = dayIdx
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Verse of the Day State
    private val defaultVerses = listOf(
        VerseInfo("أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "Verily, in the remembrance of Allah do hearts find rest.", "Ar-Ra'd 13:28", HijriCalendar.getHijriDate().format()),
        VerseInfo("وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ", "And He is with you wherever you are.", "Al-Hadid 57:4", HijriCalendar.getHijriDate().format()),
        VerseInfo("إِنَّ مَعَ الْعُسْرِ يُسْرًا", "Indeed, with hardship comes ease.", "Ash-Sharh 94:6", HijriCalendar.getHijriDate().format()),
        VerseInfo("وَعَسَىٰ أَن تَكْرَهُوا شَيْئًا وَهُوَ خَيْرٌ لَّكُمْ", "Perhaps you dislike a thing and it is good for you.", "Al-Baqarah 2:216", HijriCalendar.getHijriDate().format()),
        VerseInfo("رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً", "Our Lord, give us good in this world and good in the Hereafter.", "Al-Baqarah 2:201", HijriCalendar.getHijriDate().format())
    )

    val verseOfTheDay: StateFlow<VerseInfo> = flow {
        val doy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val selectedVerse = defaultVerses[doy % defaultVerses.size]
        emit(selectedVerse.copy(hijriDate = HijriCalendar.getHijriDate().format()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultVerses[0])

    // Market Assets State
    private val _marketAssets = MutableStateFlow<List<MarketAsset>>(
        listOf(
            MarketAsset("btc", "c-btc", "₿", "BTC", 67432.0, 2.41, listOf(62.0, 64.0, 63.0, 66.0, 65.0, 67.0, 68.0), 0),
            MarketAsset("eth", "c-eth", "Ξ", "ETH", 3214.0, -1.18, listOf(34.0, 33.0, 34.0, 32.0, 33.0, 31.0, 30.0), 0),
            MarketAsset("gold", "c-gold", "Au", "Gold", 2388.0, 0.62, listOf(236.0, 237.0, 236.0, 238.0, 237.0, 239.0), 0),
            MarketAsset("eur", "c-eur", "€", "EUR", 1.0842, 0.14, listOf(1.08, 1.082, 1.081, 1.083, 1.0842), 4)
        )
    )
    val marketAssets: StateFlow<List<MarketAsset>> = _marketAssets.asStateFlow()

    // Modals & Reader States
    private val _selectedArticle = MutableStateFlow<FeedArticle?>(null)
    val selectedArticle: StateFlow<FeedArticle?> = _selectedArticle.asStateFlow()

    private val _selectedMarketAsset = MutableStateFlow<MarketAsset?>(null)
    val selectedMarketAsset: StateFlow<MarketAsset?> = _selectedMarketAsset.asStateFlow()

    private val _lightboxImages = MutableStateFlow<List<String>>(emptyList())
    val lightboxImages: StateFlow<List<String>> = _lightboxImages.asStateFlow()

    private val _lightboxIndex = MutableStateFlow(0)
    val lightboxIndex: StateFlow<Int> = _lightboxIndex.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    // Toast State
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
        fetchLiveMarkets()
    }

    fun updateCoordinates(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = userPrefs.value
                val locationService = LocationServiceImpl(context)

                // 1. Weather Coordinates
                if (prefs.weatherLocationMode == "AUTO") {
                    val loc = locationService.getCurrentLocation()
                    if (loc != null) {
                        _weatherCoordinates.value = Pair(loc.latitude, loc.longitude)
                        android.util.Log.d("FeedViewModel", "Weather coordinates updated to ${loc.latitude}, ${loc.longitude}")
                    } else {
                        android.util.Log.w("FeedViewModel", "AUTO location returned null")
                    }
                } else if (!prefs.weatherManualLocation.isNullOrBlank()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(prefs.weatherManualLocation!!, 1)
                    val addr = addresses?.firstOrNull()
                    if (addr != null) {
                        _weatherCoordinates.value = Pair(addr.latitude, addr.longitude)
                        android.util.Log.d("FeedViewModel", "Weather manual coordinates updated for ${prefs.weatherManualLocation}")
                    } else {
                        android.util.Log.w("FeedViewModel", "Failed to resolve manual weather location: ${prefs.weatherManualLocation}")
                    }
                }

                // 2. Prayer Coordinates (default same as weather, unless explicit manual prayer location is set)
                if (prefs.prayerLocationMode == "MANUAL" && !prefs.prayerManualLocation.isNullOrBlank()) {
                    if (prefs.prayerManualLocation == prefs.weatherManualLocation && _weatherCoordinates.value != null) {
                        _prayerCoordinates.value = _weatherCoordinates.value
                    } else {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(prefs.prayerManualLocation!!, 1)
                        val addr = addresses?.firstOrNull()
                        if (addr != null) {
                            _prayerCoordinates.value = Pair(addr.latitude, addr.longitude)
                        }
                    }
                } else {
                    _prayerCoordinates.value = _weatherCoordinates.value
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Failed to update coordinates", e)
            }
        }
    }

    fun setPrayerLocationSettings(context: Context, mode: String, location: String) {
        viewModelScope.launch {
            userPrefsRepository.updatePrayerLocationSettings(mode = mode, location = location)
            updateCoordinates(context)
        }
    }

    fun refresh(context: Context? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            showToast("Refreshing…")
            context?.let { updateCoordinates(it) }
            feedRepository.refreshAll()
            feedRepository.refreshWeather()
            fetchLiveMarkets()
            _visibleArticleCount.value = 30
            _isRefreshing.value = false
        }
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.value = msg
            delay(2400)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        _visibleArticleCount.value = 30
    }

    fun toggleWeatherExpand() {
        _isWeatherExpanded.value = !_isWeatherExpanded.value
    }

    fun setWeatherDayIndex(idx: Int) {
        _weatherDayIndex.value = idx
    }

    fun setPrayerDayIndex(idx: Int) {
        _prayerDayIndex.value = idx
    }

    fun selectArticle(article: FeedArticle?) {
        _selectedArticle.value = article
        if (article != null && !article.isExtracted) {
            viewModelScope.launch {
                try {
                    val full = feedRepository.getFullArticle(article.id)
                    if (full != null && _selectedArticle.value?.id == article.id) {
                        _selectedArticle.value = full
                    }
                } catch (e: Exception) {
                    Log.e("FeedViewModel", "Failed to extract full article", e)
                }
            }
        }
    }

    fun selectMarketAsset(asset: MarketAsset?) {
        _selectedMarketAsset.value = asset
    }

    fun openLightbox(images: List<String>, startIndex: Int) {
        _lightboxImages.value = images
        _lightboxIndex.value = startIndex.coerceIn(0, images.size - 1)
    }

    fun setLightboxIndex(idx: Int) {
        _lightboxIndex.value = idx
    }

    fun closeLightbox() {
        _lightboxImages.value = emptyList()
    }

    fun openSettingsSheet() {
        _showSettingsSheet.value = true
    }

    fun closeSettingsSheet() {
        _showSettingsSheet.value = false
    }

    fun setWeatherMode(mode: String) {
        viewModelScope.launch {
            userPrefsRepository.updateWeatherSettings(mode = mode)
            feedRepository.refreshWeather()
        }
    }

    fun setManualLocation(location: String) {
        viewModelScope.launch {
            userPrefsRepository.updateWeatherSettings(location = location)
            feedRepository.refreshWeather()
        }
    }

    fun addCuratedSources(feeds: List<CuratedFeed>) {
        viewModelScope.launch {
            feedRepository.addCuratedSources(feeds)
        }
    }

    fun addSource(input: String) {
        viewModelScope.launch {
            val urls = input.split(",").map { it.trim() }.filter { it.isNotBlank() }
            urls.forEach { url ->
                feedRepository.addSource(url)
            }
        }
    }

    fun removeSource(sourceId: String) {
        viewModelScope.launch {
            feedRepository.removeSource(sourceId)
        }
    }

    // Customization & Settings Updates
    fun moveCardUp(cardId: String) {
        viewModelScope.launch {
            val currentOrder = userPrefs.value.feedCardOrder.toMutableList()
            val idx = currentOrder.indexOf(cardId)
            if (idx > 0) {
                val tmp = currentOrder[idx - 1]
                currentOrder[idx - 1] = currentOrder[idx]
                currentOrder[idx] = tmp
                userPrefsRepository.updateFeedPreferences(order = currentOrder)
            }
        }
    }

    fun moveCardDown(cardId: String) {
        viewModelScope.launch {
            val currentOrder = userPrefs.value.feedCardOrder.toMutableList()
            val idx = currentOrder.indexOf(cardId)
            if (idx >= 0 && idx < currentOrder.size - 1) {
                val tmp = currentOrder[idx + 1]
                currentOrder[idx + 1] = currentOrder[idx]
                currentOrder[idx] = tmp
                userPrefsRepository.updateFeedPreferences(order = currentOrder)
            }
        }
    }

    fun toggleCardHidden(cardId: String, isHidden: Boolean) {
        viewModelScope.launch {
            val hiddenList = userPrefs.value.feedHiddenCards.toMutableList()
            if (isHidden) {
                if (!hiddenList.contains(cardId)) hiddenList.add(cardId)
            } else {
                hiddenList.remove(cardId)
            }
            userPrefsRepository.updateFeedPreferences(hidden = hiddenList)
        }
    }

    fun setLayoutMode(mode: String) {
        viewModelScope.launch {
            userPrefsRepository.updateFeedPreferences(layoutMode = mode)
            showToast(if (mode == "SWIPE") "Swipe mode on" else "Stacked mode on")
        }
    }

    fun setPrayerCalculationMethod(method: String) {
        viewModelScope.launch {
            userPrefsRepository.updateFeedPreferences(method = method)
            showToast("Calculation method updated")
        }
    }

    fun setPrayerAsrMadhhab(madhhab: String) {
        viewModelScope.launch {
            userPrefsRepository.updateFeedPreferences(madhhab = madhhab)
            showToast("Asr madhhab updated")
        }
    }

    fun togglePrayerReminder(context: Context, prayerName: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentMap = try {
                Json.decodeFromString<Map<String, Boolean>>(userPrefs.value.prayerRemindersJson)
            } catch (_: Exception) {
                mapOf("Fajr" to true, "Dhuhr" to true, "Asr" to true, "Maghrib" to true, "Isha" to true)
            }.toMutableMap()

            currentMap[prayerName] = enabled
            val updatedJson = Json.encodeToString(currentMap)
            userPrefsRepository.updateFeedPreferences(remindersJson = updatedJson)

            if (enabled) {
                val pTime = prayerTimesFlow.value.find { it.name.equals(prayerName, ignoreCase = true) }
                if (pTime != null) {
                    PrayerReminderScheduler.scheduleReminder(context, prayerName, pTime.timestampMs)
                }
                showToast("Reminder enabled for $prayerName")
            } else {
                PrayerReminderScheduler.cancelReminder(context, prayerName)
                showToast("Reminder disabled for $prayerName")
            }
        }
    }

    fun toggleLikeArticle(article: FeedArticle) {
        viewModelScope.launch {
            val likedList = try {
                Json.decodeFromString<List<String>>(userPrefs.value.likedArticleLinksJson)
            } catch (_: Exception) { emptyList() }.toMutableList()

            val likedSources = try {
                Json.decodeFromString<Map<String, Int>>(userPrefs.value.likedSourcesMapJson)
            } catch (_: Exception) { emptyMap() }.toMutableMap()

            val sourceName = article.sourceName ?: "Feed"

            if (likedList.contains(article.link)) {
                likedList.remove(article.link)
                val currentCount = likedSources[sourceName] ?: 0
                if (currentCount <= 1) likedSources.remove(sourceName)
                else likedSources[sourceName] = currentCount - 1
                showToast("Removed like")
            } else {
                likedList.add(article.link)
                likedSources[sourceName] = (likedSources[sourceName] ?: 0) + 1
                showToast("Liked · more from $sourceName will show")
            }

            userPrefsRepository.updateFeedPreferences(
                likedArticlesJson = Json.encodeToString(likedList),
                likedSourcesJson = Json.encodeToString(likedSources)
            )
        }
    }

    fun toggleSaveArticle(article: FeedArticle) {
        viewModelScope.launch {
            val savedList = try {
                Json.decodeFromString<List<String>>(userPrefs.value.savedArticleLinksJson)
            } catch (_: Exception) { emptyList() }.toMutableList()

            if (savedList.contains(article.link)) {
                savedList.remove(article.link)
                showToast("Removed from saved")
            } else {
                savedList.add(article.link)
                showToast("Saved to your library")
            }

            userPrefsRepository.updateFeedPreferences(savedArticlesJson = Json.encodeToString(savedList))
        }
    }

    // Helper: Astronomical Calculation with Real Location
    private fun calculateDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        methodName: String,
        madhhabName: String,
        dayOffset: Int
    ): List<PrayerTime> {
        val method = CalculationMethod.fromName(methodName)
        val madhhab = AsrMadhhab.fromName(madhhabName)

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }

        val timings = PrayerCalculationEngine.calculateTimes(
            latitude = latitude,
            longitude = longitude,
            date = cal.time,
            timeZone = TimeZone.getDefault(),
            method = method,
            asrMadhhab = madhhab
        )

        val nowMs = System.currentTimeMillis()
        val rawList = listOf(
            "Fajr" to timings.fajr,
            "Dhuhr" to timings.dhuhr,
            "Asr" to timings.asr,
            "Maghrib" to timings.maghrib,
            "Isha" to timings.isha
        )

        var nextFound = false
        return rawList.map { (name, date) ->
            val formatted = PrayerCalculationEngine.formatTime(date)
            val isPassed = dayOffset == 0 && date.time < nowMs
            val isNext = if (dayOffset == 0 && !isPassed && !nextFound) {
                nextFound = true
                true
            } else if (dayOffset > 0 && name == "Fajr" && !nextFound) {
                nextFound = true
                true
            } else false

            PrayerTime(
                name = name,
                timeFormatted = formatted,
                timestampMs = date.time,
                isPassed = isPassed,
                isNext = isNext
            )
        }
    }

    private fun fetchLiveMarkets() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch CoinGecko API
                val cgUrl = URL("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin,ethereum,pax-gold&sparkline=true&price_change_percentage=24h")
                val conn = cgUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(responseStr)
                    val newList = mutableListOf<MarketAsset>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.getString("id")
                        val currentPrice = obj.getDouble("current_price")
                        val change24h = if (obj.isNull("price_change_percentage_24h")) 0.0 else obj.getDouble("price_change_percentage_24h")
                        val sparkArray = obj.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
                        val sparkList = mutableListOf<Double>()
                        if (sparkArray != null) {
                            for (j in 0 until sparkArray.length()) {
                                sparkList.add(sparkArray.getDouble(j))
                            }
                        }
                        when (id) {
                            "bitcoin" -> newList.add(MarketAsset("btc", "c-btc", "₿", "BTC", currentPrice, change24h, if (sparkList.isNotEmpty()) sparkList else listOf(62.0, 64.0, 67.0), 0))
                            "ethereum" -> newList.add(MarketAsset("eth", "c-eth", "Ξ", "ETH", currentPrice, change24h, if (sparkList.isNotEmpty()) sparkList else listOf(34.0, 32.0, 30.0), 0))
                            "pax-gold" -> newList.add(MarketAsset("gold", "c-gold", "Au", "Gold", currentPrice, change24h, if (sparkList.isNotEmpty()) sparkList else listOf(236.0, 238.0, 239.0), 0))
                        }
                    }
                    // Fetch EUR Forex
                    try {
                        val fxUrl = URL("https://api.frankfurter.app/latest?from=USD&to=EUR")
                        val fxConn = fxUrl.openConnection() as HttpURLConnection
                        fxConn.connectTimeout = 5000
                        if (fxConn.responseCode == 200) {
                            val fxStr = fxConn.inputStream.bufferedReader().use { it.readText() }
                            val fxObj = JSONObject(fxStr)
                            val eurRate = fxObj.getJSONObject("rates").getDouble("EUR")
                            val eurPrice = 1.0 / eurRate
                            newList.add(MarketAsset("eur", "c-eur", "€", "EUR", eurPrice, 0.14, listOf(1.08, 1.082, 1.0842), 4))
                        }
                    } catch (_: Exception) {
                        newList.add(MarketAsset("eur", "c-eur", "€", "EUR", 1.0842, 0.14, listOf(1.08, 1.082, 1.0842), 4))
                    }
                    if (newList.isNotEmpty()) {
                        _marketAssets.value = newList
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
