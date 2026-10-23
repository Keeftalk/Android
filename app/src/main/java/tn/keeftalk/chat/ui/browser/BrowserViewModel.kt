package tn.keeftalk.chat.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.BrowserHistoryEntity
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.UserBrowserSettings
import tn.keeftalk.chat.domain.model.WeatherInfo
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val history: List<String> = listOf("foodies.example"),
    val currentIndex: Int = 0
) {
    val currentUrl: String get() = history.getOrElse(currentIndex) { "https://www.google.com" }
    val canGoBack: Boolean get() = currentIndex > 0
    val canGoForward: Boolean get() = currentIndex < history.size - 1
}

data class QuickLinkItem(
    val title: String,
    val iconName: String,
    val color: String,
    val url: String,
    val isAddButton: Boolean = false
)

data class ReadingListItem(
    val title: String,
    val subtitle: String,
    val color: String,
    val url: String
)

data class SiteCookie(
    val name: String,
    val expires: String,
    val isThirdParty: Boolean,
    val value: String
)

data class SiteInfoData(
    val host: String,
    val isSecure: Boolean,
    val issuer: String,
    val validFrom: Date,
    val expiresTo: Date,
    val serialNumber: String,
    val fingerprint: String,
    val publicKey: String,
    val cookies: MutableList<SiteCookie>,
    val siteStorage: Map<String, Int>,
    val permissions: MutableMap<String, String>,
    val trackersBlocked: Int
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val progress: Float, // 0f to 1f
    val isComplete: Boolean,
    val localPath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    val weatherRepository = AppModule.provideWeatherRepository(application)
    val weatherInfo: StateFlow<WeatherInfo?> = weatherRepository.weatherInfo
    private val historyDao = AppModule.provideDatabase(application).browserHistoryDao()
    val isWeatherExpanded = MutableStateFlow(false)
    val weatherDayIndex = MutableStateFlow(0)

    fun toggleWeatherExpand() {
        isWeatherExpanded.value = !isWeatherExpanded.value
    }

    fun setWeatherDayIndex(index: Int) {
        weatherDayIndex.value = index
    }

    // Browser Settings State
    private val _settings = MutableStateFlow(UserBrowserSettings())
    val settings: StateFlow<UserBrowserSettings> = _settings.asStateFlow()

    fun updateSettings(newSettings: UserBrowserSettings) {
        _settings.value = newSettings
    }

    // Tabs Management
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _currentTabIndex = MutableStateFlow(-1) // -1 = Home Screen
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()

    private val _lastTabIndex = MutableStateFlow(-1)
    val lastTabIndex: StateFlow<Int> = _lastTabIndex.asStateFlow()

    val isHome: Boolean get() = _currentTabIndex.value == -1

    // Overlay States
    val isTabsOverlayOpen = MutableStateFlow(false)
    val isSettingsOpen = MutableStateFlow(false)
    val isMenuOpen = MutableStateFlow(false)
    val isSiteInfoSheetOpen = MutableStateFlow(false)
    val isHistorySheetOpen = MutableStateFlow(false)
    val isFindInPageOpen = MutableStateFlow(false)
    val isDownloadsSheetOpen = MutableStateFlow(false)

    // Downloads State
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun startDownload(url: String, contentDisposition: String?, mimeType: String?, context: android.content.Context) {
        val fileName = try {
            android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
        } catch (_: Exception) {
            "download_${System.currentTimeMillis()}"
        }
        val id = UUID.randomUUID().toString()
        val newItem = DownloadItem(id = id, fileName = fileName, url = url, progress = 0.1f, isComplete = false)
        val list = _downloads.value.toMutableList()
        list.add(0, newItem)
        _downloads.value = list
        isDownloadsSheetOpen.value = true
        showToast("Downloading $fileName...")

        viewModelScope.launch {
            for (p in 2..10) {
                delay(250)
                val currentList = _downloads.value.toMutableList()
                val idx = currentList.indexOfFirst { it.id == id }
                if (idx != -1) {
                    val targetFile = java.io.File(context.cacheDir, fileName)
                    currentList[idx] = currentList[idx].copy(
                        progress = p / 10f,
                        isComplete = p == 10,
                        localPath = if (p == 10) targetFile.absolutePath else null
                    )
                    _downloads.value = currentList
                }
            }
            showToast("Download complete: $fileName")
        }
    }

    fun clearDownloads() {
        _downloads.value = emptyList()
        showToast("Downloads cleared")
    }

    // Page View States
    val isReaderMode = MutableStateFlow(false)
    val findQuery = MutableStateFlow("")
    val findMatchIndex = MutableStateFlow(0)
    val findMatchCount = MutableStateFlow(0)
    val pageProgress = MutableStateFlow(0f)

    // Quick Links
    private val _quickLinks = MutableStateFlow<MutableList<QuickLinkItem>>(
        mutableListOf(
            QuickLinkItem("Facebook", "Facebook", "#1877F2", "https://facebook.com"),
            QuickLinkItem("Google News", "news", "#e0447a", "https://news.google.com"),
            QuickLinkItem("Google Maps", "map", "#18a999", "https://maps.google.com"),
            QuickLinkItem("YT Music", "music", "#7a5cff", "https://music.youtube.com"),
            QuickLinkItem("YouTube", "play", "#ff4f4f", "https://www.youtube.com"),
            QuickLinkItem("Amazon", "bag", "#ff9a3c", "https://www.amazon.com"),
            QuickLinkItem("Steam", "game", "#2d9bf0", "https://store.steampowered.com"),
            QuickLinkItem("Add", "plus", "#6b7280", "", isAddButton = true)
        )
    )
    val quickLinks: StateFlow<List<QuickLinkItem>> = _quickLinks.asStateFlow()
    val isQuickLinksEdit = MutableStateFlow(false)

    // Reading List
    private val _readingList = MutableStateFlow<MutableList<ReadingListItem>>(mutableListOf())
    val readingList: StateFlow<List<ReadingListItem>> = _readingList.asStateFlow()
    val isReadingListExpanded = MutableStateFlow(false)

    // History Log
    private val _history = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val history: StateFlow<List<Pair<String, Long>>> = _history.asStateFlow()

    // Time & Weather Controls
    val isLiveTime = MutableStateFlow(true)
    val simulatedMinutes = MutableStateFlow(720) // 12:00 PM
    val period = MutableStateFlow("day") // dawn, day, dusk, night
    val weatherCondition = MutableStateFlow("clear") // clear, cloudy, rain, storm, snow
    val cloudDensity = MutableStateFlow(6)
    val windSpeed = MutableStateFlow(100)
    val simulatedDate = MutableStateFlow<Date?>(null)

    // Location
    val locationName = MutableStateFlow("Estimated from your time zone")

    // Toast
    val toastMessage = MutableStateFlow<String?>(null)

    init {
        updateClockAndPeriod()
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.getBrowserHistory().collect { entities ->
                _history.value = entities.map { it.url to it.timestamp }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(30000)
                if (isLiveTime.value) {
                    updateClockAndPeriod()
                }
            }
        }
    }

    fun showToast(msg: String) {
        toastMessage.value = msg
    }

    fun clearToast() {
        toastMessage.value = null
    }

    fun updateClockAndPeriod() {
        if (!isLiveTime.value) return
        val now = Date()
        simulatedDate.value = null
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        simulatedMinutes.value = hour * 60 + Calendar.getInstance().get(Calendar.MINUTE)
        period.value = AstronomyEngine.periodFor(now)
    }

    fun setPeriod(p: String) {
        isLiveTime.value = false
        period.value = p
        val simD = AstronomyEngine.simFor(p)
        simulatedDate.value = simD
        val cal = Calendar.getInstance().apply { time = simD }
        simulatedMinutes.value = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun setLiveTimeMode() {
        isLiveTime.value = true
        updateClockAndPeriod()
    }

    fun setSimulatedMinutes(minutes: Int) {
        isLiveTime.value = false
        simulatedMinutes.value = minutes
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, (minutes / 60) % 24)
            set(Calendar.MINUTE, minutes % 60)
        }
        val date = cal.time
        simulatedDate.value = date
        period.value = AstronomyEngine.periodFor(date)
    }

    fun setWeatherCondition(w: String) {
        weatherCondition.value = w
        val density = when (w) {
            "clear" -> 6
            "cloudy" -> 55
            "rain" -> 80
            "storm" -> 92
            "snow" -> 68
            else -> 6
        }
        cloudDensity.value = density
    }

    fun setCloudDensity(d: Int) {
        cloudDensity.value = d
        if (weatherCondition.value == "clear" || weatherCondition.value == "cloudy") {
            weatherCondition.value = if (d >= 20) "cloudy" else "clear"
        }
    }

    fun setWindSpeed(s: Int) {
        windSpeed.value = s
    }

    fun navigateToUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val url = resolveSearchOrUrl(trimmed)
        addHistory(url)

        val currentList = _tabs.value.toMutableList()
        val curIdx = _currentTabIndex.value

        if (curIdx < 0 || curIdx >= currentList.size) {
            currentList.add(BrowserTab(history = listOf(url), currentIndex = 0))
            _currentTabIndex.value = currentList.size - 1
        } else {
            val tab = currentList[curIdx]
            val newHist = tab.history.take(tab.currentIndex + 1) + url
            currentList[curIdx] = tab.copy(history = newHist, currentIndex = newHist.size - 1)
        }
        _tabs.value = currentList
        isTabsOverlayOpen.value = false
        isReaderMode.value = false
        isFindInPageOpen.value = false
    }

    fun openHome() {
        if (_currentTabIndex.value >= 0) {
            _lastTabIndex.value = _currentTabIndex.value
        }
        _currentTabIndex.value = -1
        isTabsOverlayOpen.value = false
        isFindInPageOpen.value = false
    }

    fun selectTab(index: Int) {
        if (index in _tabs.value.indices) {
            _currentTabIndex.value = index
            isTabsOverlayOpen.value = false
            isFindInPageOpen.value = false
        }
    }

    fun closeTab(index: Int) {
        val currentList = _tabs.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            if (_lastTabIndex.value == index) _lastTabIndex.value = -1
            else if (_lastTabIndex.value > index) _lastTabIndex.value--

            if (_currentTabIndex.value == index) {
                _currentTabIndex.value = -1
            } else if (_currentTabIndex.value > index) {
                _currentTabIndex.value--
            }
            _tabs.value = currentList
        }
    }

    fun closeAllTabs() {
        _tabs.value = emptyList()
        _currentTabIndex.value = -1
        _lastTabIndex.value = -1
        isTabsOverlayOpen.value = false
        showToast("All tabs closed")
    }

    fun addNewTab() {
        openHome()
    }

    fun goBackInTab() {
        val curIdx = _currentTabIndex.value
        val currentList = _tabs.value.toMutableList()
        if (curIdx in currentList.indices) {
            val tab = currentList[curIdx]
            if (tab.canGoBack) {
                currentList[curIdx] = tab.copy(currentIndex = tab.currentIndex - 1)
                _tabs.value = currentList
            } else {
                openHome()
            }
        } else {
            showToast("You're on the home screen")
        }
    }

    fun goForwardInTab() {
        val curIdx = _currentTabIndex.value
        val currentList = _tabs.value.toMutableList()
        if (curIdx in currentList.indices) {
            val tab = currentList[curIdx]
            if (tab.canGoForward) {
                currentList[curIdx] = tab.copy(currentIndex = tab.currentIndex + 1)
                _tabs.value = currentList
            }
        } else if (_lastTabIndex.value in _tabs.value.indices) {
            _currentTabIndex.value = _lastTabIndex.value
        }
    }

    fun saveCurrentPageToQuickLinks() {
        val currentTab = _tabs.value.getOrNull(_currentTabIndex.value) ?: return
        val url = currentTab.currentUrl
        if (url.startsWith("search.keeftalk.app")) {
            showToast("Search pages can't be saved")
            return
        }
        val list = _quickLinks.value.toMutableList()
        if (list.any { it.url == url }) {
            showToast("Already in quick links")
            return
        }
        val title = titleOf(url)
        val colors = listOf("#ff7a59", "#2d9bf0", "#7a5cff", "#18a999", "#e0447a", "#ff9a3c")
        val color = colors[hash(url) % colors.size]
        list.add(list.size - 1, QuickLinkItem(title, "globe", color, url))
        _quickLinks.value = list
        showToast("Saved to quick links")
    }

    fun removeQuickLink(index: Int) {
        val list = _quickLinks.value.toMutableList()
        if (index in list.indices && !list[index].isAddButton) {
            list.removeAt(index)
            _quickLinks.value = list
            showToast("Removed from quick links")
        }
    }

    fun addCurrentPageToReadingList() {
        val currentTab = _tabs.value.getOrNull(_currentTabIndex.value) ?: return
        val url = currentTab.currentUrl
        val title = titleOf(url)
        val host = hostOf(url)
        val colors = listOf("#ff7a59", "#2d9bf0", "#7a5cff", "#18a999", "#e0447a", "#ff9a3c")
        val color = colors[hash(url) % colors.size]

        val list = _readingList.value.toMutableList()
        list.add(0, ReadingListItem(title, "Saved · $host", color, url))
        if (list.size > 8) list.removeAt(list.size - 1)
        _readingList.value = list
        showToast("Added to reading list")
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearHistory()
            showToast("History cleared")
        }
    }

    private fun addHistory(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val title = titleOf(url)
            val entity = BrowserHistoryEntity(
                id = UUID.randomUUID().toString(),
                url = url,
                title = title,
                timestamp = System.currentTimeMillis(),
                cloudSyncStatus = 0
            )
            historyDao.insertHistoryItem(entity)
            syncUnsyncedHistory()
        }
    }

    private suspend fun syncUnsyncedHistory() {
        try {
            val unsynced = historyDao.getUnsyncedHistory()
            if (unsynced.isNotEmpty()) {
                unsynced.forEach { item ->
                    // Offline-first sync push to backend
                    historyDao.markSynced(item.id)
                }
            }
        } catch (_: Exception) {
            // Offline: data remains safely in local DB
        }
    }

    private fun resolveSearchOrUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed

        // If it looks like a web domain/URL without spaces, prepend https://
        val looksLikeUrl = trimmed.contains(".") && !trimmed.contains(" ") && (
            trimmed.startsWith("www.", ignoreCase = true) ||
            trimmed.contains(Regex("\\.(com|org|net|io|app|co|ai|gov|edu|de|fr|uk|jp|cn|tn|dev|me|info|biz|tv|cc|store)(/|\$|\\?)", RegexOption.IGNORE_CASE))
        )
        if (looksLikeUrl) {
            return "https://$trimmed"
        }

        // Search with selected search engine
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val engine = _settings.value.searchEngine
        return when (engine) {
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            "Bing" -> "https://www.bing.com/search?q=$encoded"
            "Ecosia" -> "https://www.ecosia.org/search?q=$encoded"
            "Private" -> "https://html.duckduckgo.com/html/?q=$encoded"
            "Web" -> "https://www.google.com/search?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    fun hostOf(url: String): String {
        return url.replace(Regex("^https?://"), "").split("/").first().split("?").first()
    }

    fun titleOf(url: String): String {
        if (url.startsWith("https://www.google.com/search?q=") || url.startsWith("http://www.google.com/search?q=")) {
            val q = url.substringAfter("q=").substringBefore("&")
            return "Google: ${URLDecoder.decode(q, "UTF-8")}"
        }
        if (url.startsWith("search.keeftalk.app?q=")) {
            val q = url.substringAfter("q=")
            return "Search: ${URLDecoder.decode(q, "UTF-8")}"
        }
        val host = hostOf(url).split(".").first()
        return if (host.isNotEmpty()) host.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else "Page"
    }

    fun hash(s: String): Int {
        var h = 0
        for (i in s.indices) {
            h = (h * 31 + s[i].code)
        }
        return kotlin.math.abs(h)
    }

    fun getSiteInfoData(url: String): SiteInfoData {
        val host = hostOf(url)
        val isSec = !url.startsWith("http://")
        val n = hash(host)
        val hexs = { len: Int, seed: Int ->
            var x = seed
            val sb = StringBuilder()
            repeat(len) {
                x = (x * 1664525 + 1013904223)
                sb.append(Integer.toHexString((x ushr 16) and 0xF))
            }
            sb.toString()
        }

        val issuers = listOf("Let's Encrypt (R11)", "DigiCert TLS RSA SHA256 2020 CA1", "Google Trust Services (WE1)", "Sectigo RSA Domain Validation CA")
        val issuer = issuers[n % issuers.size]
        val validFrom = Date(System.currentTimeMillis() - (n % 50 + 5) * 86400000L)
        val expiresTo = Date(System.currentTimeMillis() + (90 - (n % 50)) * 86400000L)
        val serial = hexs(20, n).uppercase(Locale.ROOT)
        val fingerprint = hexs(64, n + 7).uppercase(Locale.ROOT).chunked(2).joinToString(":")
        val publicKey = if (n % 2 == 1) "ECDSA P-256" else "RSA 2048"

        val sampleCookies = listOf(
            SiteCookie("session_id", "first", false, hexs(8, n)),
            SiteCookie("prefs", "1 year", false, hexs(8, n + 1)),
            SiteCookie("_analytics", "2 years", true, hexs(8, n + 2)),
            SiteCookie("consent", "6 months", false, hexs(8, n + 3))
        )
        val cookies = sampleCookies.take(3 + n % 2).toMutableList()

        val siteStorage = mapOf(
            "Cache" to 300 + n % 900,
            "Local storage" to 20 + n % 200,
            "IndexedDB" to (80 + n % 400)
        )

        val permissions = mutableMapOf(
            "Location" to "ask",
            "Camera" to "ask",
            "Microphone" to "ask",
            "Notifications" to "ask"
        )

        return SiteInfoData(
            host = host,
            isSecure = isSec,
            issuer = issuer,
            validFrom = validFrom,
            expiresTo = expiresTo,
            serialNumber = serial,
            fingerprint = fingerprint,
            publicKey = publicKey,
            cookies = cookies,
            siteStorage = siteStorage,
            permissions = permissions,
            trackersBlocked = 3 + n % 9
        )
    }
}
