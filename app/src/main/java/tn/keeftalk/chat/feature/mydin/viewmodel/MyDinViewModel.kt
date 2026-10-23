package tn.keeftalk.chat.feature.mydin.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tn.keeftalk.chat.data.service.LocationServiceImpl
import tn.keeftalk.chat.domain.prayer.AsrMadhhab
import tn.keeftalk.chat.domain.prayer.CalculationMethod
import tn.keeftalk.chat.domain.prayer.PrayerCalculationEngine
import tn.keeftalk.chat.domain.prayer.PrayerTime
import tn.keeftalk.chat.feature.mydin.model.*
import tn.keeftalk.chat.feature.mydin.service.MyDinAudioService
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class DownloadDialogInfo(
    val surah: SurahMeta,
    val reciter: Reciter,
    val estimatedMb: Float
)

data class DownloadProgressInfo(
    val surahNumber: Int,
    val completedAyahs: Int,
    val totalAyahs: Int,
    val percent: Float,
    val isDownloading: Boolean
)

data class BatchDownloadProgress(
    val currentSurahNum: Int,
    val totalSurahs: Int,
    val surahName: String,
    val completedAyahs: Int,
    val totalAyahsInSurah: Int,
    val overallPercent: Float,
    val isDownloading: Boolean
)

class MyDinViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mydin_prefs", Context.MODE_PRIVATE)

    // --- Tab 1: Salat State ---
    private val _locationMode = MutableStateFlow(prefs.getString("location_mode", "AUTO") ?: "AUTO")
    val locationMode: StateFlow<String> = _locationMode.asStateFlow()

    private val _selectedCity = MutableStateFlow(loadSavedCity())
    val selectedCity: StateFlow<MyDinCity> = _selectedCity.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("is_24h", false))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _prayerTimes = MutableStateFlow<List<PrayerTime>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTime>> = _prayerTimes.asStateFlow()

    private val _prayerReminders = MutableStateFlow(loadPrayerToggles("prayer_reminder_"))
    val prayerReminders: StateFlow<Map<String, Boolean>> = _prayerReminders.asStateFlow()

    private val _prayerAdhans = MutableStateFlow(loadPrayerToggles("prayer_adhan_"))
    val prayerAdhans: StateFlow<Map<String, Boolean>> = _prayerAdhans.asStateFlow()

    private val _nextPrayer = MutableStateFlow<PrayerTime?>(null)
    val nextPrayer: StateFlow<PrayerTime?> = _nextPrayer.asStateFlow()

    private val _prevPrayer = MutableStateFlow<PrayerTime?>(null)
    val prevPrayer: StateFlow<PrayerTime?> = _prevPrayer.asStateFlow()

    private val _countdownText = MutableStateFlow("—")
    val countdownText: StateFlow<String> = _countdownText.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private val _hijriDateText = MutableStateFlow(calculateHijriDateText())
    val hijriDateText: StateFlow<String> = _hijriDateText.asStateFlow()

    private var clockJob: Job? = null

    // --- Tab 2: Douaa State ---
    private val _douaaCategory = MutableStateFlow("all")
    val douaaCategory: StateFlow<String> = _douaaCategory.asStateFlow()

    private val _douaaSearchQuery = MutableStateFlow("")
    val douaaSearchQuery: StateFlow<String> = _douaaSearchQuery.asStateFlow()

    private val _dhikrCounters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dhikrCounters: StateFlow<Map<String, Int>> = _dhikrCounters.asStateFlow()

    private val _favoriteDouaaIds = MutableStateFlow(loadFavorites())
    val favoriteDouaaIds: StateFlow<Set<String>> = _favoriteDouaaIds.asStateFlow()

    // --- Tab 3: Hadith State ---
    private val _hadithCategory = MutableStateFlow("all")
    val hadithCategory: StateFlow<String> = _hadithCategory.asStateFlow()

    private val _hadithSearchQuery = MutableStateFlow("")
    val hadithSearchQuery: StateFlow<String> = _hadithSearchQuery.asStateFlow()

    private val _selectedHadithBook = MutableStateFlow<HadithBook?>(null)
    val selectedHadithBook: StateFlow<HadithBook?> = _selectedHadithBook.asStateFlow()

    private val _bookHadithsList = MutableStateFlow<List<HadithItem>>(emptyList())
    val bookHadithsList: StateFlow<List<HadithItem>> = _bookHadithsList.asStateFlow()

    private val _isBookHadithsLoading = MutableStateFlow(false)
    val isBookHadithsLoading: StateFlow<Boolean> = _isBookHadithsLoading.asStateFlow()

    // --- Tab 4: Quran State ---
    private val _quranSearchQuery = MutableStateFlow("")
    val quranSearchQuery: StateFlow<String> = _quranSearchQuery.asStateFlow()

    private val _lastReadSurah = MutableStateFlow(loadLastReadSurah())
    val lastReadSurah: StateFlow<SurahMeta> = _lastReadSurah.asStateFlow()

    private val _selectedSurah = MutableStateFlow<SurahMeta?>(null)
    val selectedSurah: StateFlow<SurahMeta?> = _selectedSurah.asStateFlow()

    private val _isReaderOpen = MutableStateFlow(false)
    val isReaderOpen: StateFlow<Boolean> = _isReaderOpen.asStateFlow()

    private val _isTajweedEnabled = MutableStateFlow(true)
    val isTajweedEnabled: StateFlow<Boolean> = _isTajweedEnabled.asStateFlow()

    private val _isTajweedAvailable = MutableStateFlow(false)
    val isTajweedAvailable: StateFlow<Boolean> = _isTajweedAvailable.asStateFlow()

    private val _ayahsList = MutableStateFlow<List<AyahDetail>>(emptyList())
    val ayahsList: StateFlow<List<AyahDetail>> = _ayahsList.asStateFlow()

    private val _isSurahLoading = MutableStateFlow(false)
    val isSurahLoading: StateFlow<Boolean> = _isSurahLoading.asStateFlow()

    private val _tafsirAyahNumber = MutableStateFlow<Int?>(null)
    val tafsirAyahNumber: StateFlow<Int?> = _tafsirAyahNumber.asStateFlow()

    // Audio Player & Download State
    private val _selectedReciter = MutableStateFlow(loadSavedReciter())
    val selectedReciter: StateFlow<Reciter> = _selectedReciter.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.AUTOPLAY_NEXT_SURAH)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _audioPlayingAyah = MutableStateFlow<Int?>(null)
    val audioPlayingAyah: StateFlow<Int?> = _audioPlayingAyah.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _isAudioLoading = MutableStateFlow(false)
    val isAudioLoading: StateFlow<Boolean> = _isAudioLoading.asStateFlow()

    private val _downloadDialogInfo = MutableStateFlow<DownloadDialogInfo?>(null)
    val downloadDialogInfo: StateFlow<DownloadDialogInfo?> = _downloadDialogInfo.asStateFlow()

    private val _downloadProgressState = MutableStateFlow<DownloadProgressInfo?>(null)
    val downloadProgressState: StateFlow<DownloadProgressInfo?> = _downloadProgressState.asStateFlow()

    private val _batchDownloadProgress = MutableStateFlow<BatchDownloadProgress?>(null)
    val batchDownloadProgress: StateFlow<BatchDownloadProgress?> = _batchDownloadProgress.asStateFlow()

    private val _showCdnPrompt = MutableStateFlow(false)
    val showCdnPrompt: StateFlow<Boolean> = _showCdnPrompt.asStateFlow()

    private var downloadJob: Job? = null
    private var batchDownloadJob: Job? = null

    // Toast event
    private val _toastEvent = MutableStateFlow<String?>(null)
    val toastEvent: StateFlow<String?> = _toastEvent.asStateFlow()

    init {
        recalculatePrayerTimes()
        startClock()
    }

    // --- Hidden Quran Storage Helpers ---
    private fun getQuranStorageBaseDir(context: Context): File {
        val externalDir = context.getExternalFilesDir(null) ?: context.filesDir
        val quranDir = File(externalDir, "keeftalk/.MyQuran")
        if (!quranDir.exists()) quranDir.mkdirs()
        val nomedia = File(quranDir, ".nomedia")
        if (!nomedia.exists()) try { nomedia.createNewFile() } catch (_: Exception) {}
        return quranDir
    }

    private fun getQuranAudioDir(context: Context, reciterId: String): File {
        val base = getQuranStorageBaseDir(context)
        val audioDir = File(base, "audio/$reciterId")
        if (!audioDir.exists()) audioDir.mkdirs()
        return audioDir
    }

    private fun getQuranSurahsDir(context: Context): File {
        val base = getQuranStorageBaseDir(context)
        val surahsDir = File(base, "surahs")
        if (!surahsDir.exists()) surahsDir.mkdirs()
        return surahsDir
    }

    // --- Toast Helper ---
    fun showToast(msg: String) {
        _toastEvent.value = msg
    }

    fun clearToast() {
        _toastEvent.value = null
    }

    // --- Tab 1: Salat & Location Logic ---
    private fun loadPrayerToggles(prefix: String): Map<String, Boolean> {
        val prayers = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        return prayers.associateWith { name ->
            prefs.getBoolean("$prefix$name", name != "الشروق")
        }
    }

    fun togglePrayerReminder(context: Context, prayerName: String) {
        val currentMap = _prayerReminders.value.toMutableMap()
        val newValue = !(currentMap[prayerName] ?: false)
        currentMap[prayerName] = newValue
        _prayerReminders.value = currentMap
        prefs.edit().putBoolean("prayer_reminder_$prayerName", newValue).apply()

        if (newValue) {
            ensureCdnSoundFilesDownloaded(context, "reminder.mp3")
            showToast("تم تفعيل التذكير لصلاة $prayerName")
        } else {
            showToast("تم إيقاف التذكير لصلاة $prayerName")
        }
    }

    fun togglePrayerAdhan(context: Context, prayerName: String) {
        val currentMap = _prayerAdhans.value.toMutableMap()
        val newValue = !(currentMap[prayerName] ?: false)
        currentMap[prayerName] = newValue
        _prayerAdhans.value = currentMap
        prefs.edit().putBoolean("prayer_adhan_$prayerName", newValue).apply()

        if (newValue) {
            ensureCdnSoundFilesDownloaded(context, "adhan.mp3")
            showToast("تم تفعيل الأذان لصلاة $prayerName")
        } else {
            showToast("تم إيقاف الأذان لصلاة $prayerName")
        }
    }

    private fun ensureCdnSoundFilesDownloaded(context: Context, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseDir = getQuranStorageBaseDir(context)
                val soundsDir = File(baseDir, "sounds")
                if (!soundsDir.exists()) soundsDir.mkdirs()

                val soundFile = File(soundsDir, fileName)
                if (!soundFile.exists() || soundFile.length() < 100) {
                    val urlString = "https://public.cdn.keeftalk.com/mydin/sounds/$fileName"
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    if (conn.responseCode == 200) {
                        val input = conn.inputStream
                        val output = FileOutputStream(soundFile)
                        input.copyTo(output)
                        output.flush()
                        output.close()
                        input.close()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun setLocationMode(mode: String, context: Context? = null) {
        _locationMode.value = mode
        prefs.edit().putString("location_mode", mode).apply()
        if (mode == "AUTO" && context != null) {
            updateGpsLocation(context)
        } else {
            recalculatePrayerTimes()
        }
    }

    fun updateGpsLocation(context: Context) {
        viewModelScope.launch {
            try {
                val locationService = LocationServiceImpl(context)
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    var cityAr = "موقعي الحالي"
                    var countryAr = "GPS"

                    try {
                        val geocoder = Geocoder(context, Locale("ar"))
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val addr = addresses?.firstOrNull()
                        if (addr != null) {
                            cityAr = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "موقعي الحالي"
                            countryAr = addr.countryName ?: "GPS"
                        }
                    } catch (_: Exception) {}

                    val gpsCity = MyDinCity(
                        name = "GPS_Location",
                        country = "GPS",
                        arName = cityAr,
                        countryArName = countryAr,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _selectedCity.value = gpsCity
                    recalculatePrayerTimes()
                } else {
                    recalculatePrayerTimes()
                }
            } catch (_: Exception) {
                recalculatePrayerTimes()
            }
        }
    }

    fun selectCity(city: MyDinCity) {
        _locationMode.value = "MANUAL"
        prefs.edit().putString("location_mode", "MANUAL").apply()
        _selectedCity.value = city
        saveCity(city)
        recalculatePrayerTimes()
        showToast("تم تحديث مواقيت الصلاة · ${city.arName}")
    }

    fun toggle24HourFormat() {
        val newValue = !_is24HourFormat.value
        _is24HourFormat.value = newValue
        prefs.edit().putBoolean("is_24h", newValue).apply()
        recalculatePrayerTimes()
        showToast(if (newValue) "نظام ٢٤ ساعة" else "نظام ١٢ ساعة")
    }

    private fun loadSavedCity(): MyDinCity {
        val name = prefs.getString("city_name", "Cairo") ?: "Cairo"
        return CITIES.find { it.name.equals(name, ignoreCase = true) } ?: CITIES[0]
    }

    private fun saveCity(city: MyDinCity) {
        prefs.edit().putString("city_name", city.name).apply()
    }

    private fun recalculatePrayerTimes() {
        val city = _selectedCity.value
        val lat = city.latitude ?: 30.0444
        val lon = city.longitude ?: 31.2357
        val now = Date()

        val timings = PrayerCalculationEngine.calculateTimes(
            latitude = lat,
            longitude = lon,
            date = now,
            timeZone = TimeZone.getDefault(),
            method = CalculationMethod.MWL,
            asrMadhhab = AsrMadhhab.SHAFI
        )

        val list = listOf(
            Triple("Fajr", "الفجر", timings.fajr),
            Triple("Sunrise", "الشروق", timings.sunrise),
            Triple("Dhuhr", "الظهر", timings.dhuhr),
            Triple("Asr", "العصر", timings.asr),
            Triple("Maghrib", "المغرب", timings.maghrib),
            Triple("Isha", "العشاء", timings.isha)
        )

        val nowMs = now.time
        val is24 = _is24HourFormat.value

        var nextFound = false
        val prayerTimeList = mutableListOf<PrayerTime>()

        for (item in list) {
            val isPassed = item.third.time <= nowMs
            val isNext = !isPassed && !nextFound
            if (isNext) nextFound = true

            val formattedTime = formatPrayerTimeDisplay(item.third, is24)
            prayerTimeList.add(
                PrayerTime(
                    name = item.second,
                    timeFormatted = formattedTime,
                    timestampMs = item.third.time,
                    isPassed = isPassed,
                    isNext = isNext
                )
            )
        }

        if (!nextFound && prayerTimeList.isNotEmpty()) {
            val first = prayerTimeList.first()
            prayerTimeList[0] = first.copy(isNext = true)
        }

        _prayerTimes.value = prayerTimeList
        updateClockAndNextPrayer()
    }

    private fun formatPrayerTimeDisplay(date: Date, is24: Boolean): String {
        val sdf = if (is24) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
        return sdf.format(date)
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                updateClockAndNextPrayer()
                delay(1000)
            }
        }
    }

    private fun updateClockAndNextPrayer() {
        val list = _prayerTimes.value
        if (list.isEmpty()) return

        val nowMs = System.currentTimeMillis()
        val next = list.find { it.isNext } ?: list.first()
        val nextIdx = list.indexOf(next)
        val prevIdx = if (nextIdx > 0) nextIdx - 1 else list.lastIndex
        val prev = list[prevIdx]

        _nextPrayer.value = next
        _prevPrayer.value = prev

        var diffMs = next.timestampMs - nowMs
        if (diffMs < 0) {
            diffMs += 24 * 3600 * 1000
        }

        val hh = (diffMs / (3600 * 1000)).toInt()
        val mm = ((diffMs % (3600 * 1000)) / (60 * 1000)).toInt()
        val ss = ((diffMs % (60 * 1000)) / 1000).toInt()

        _countdownText.value = String.format(Locale.getDefault(), "بعد %02d:%02d:%02d", hh, mm, ss)

        val totalInterval = Math.max(1L, next.timestampMs - prev.timestampMs)
        val elapsed = Math.max(0L, nowMs - prev.timestampMs)
        _progressFraction.value = (elapsed.toFloat() / totalInterval.toFloat()).coerceIn(0f, 1f)
    }

    private fun calculateHijriDateText(): String {
        return try {
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
            format.format(cal.time) + " هـ"
        } catch (_: Exception) {
            "اليوم"
        }
    }

    // --- Tab 2: Douaa Logic ---
    fun setDouaaCategory(cat: String) {
        _douaaCategory.value = cat
    }

    fun setDouaaSearchQuery(query: String) {
        _douaaSearchQuery.value = query
    }

    fun incrementDhikr(id: String, maxRepeat: Int) {
        val currentMap = _dhikrCounters.value.toMutableMap()
        val cur = currentMap[id] ?: 0
        if (cur >= maxRepeat) {
            currentMap[id] = 0
            _dhikrCounters.value = currentMap
            showToast("تمت إعادة العداد")
            return
        }
        val newCount = cur + 1
        currentMap[id] = newCount
        _dhikrCounters.value = currentMap

        if (newCount == maxRepeat) {
            showToast("تم ✓ تقبّل الله منك")
        }
    }

    fun resetDhikr(id: String) {
        val currentMap = _dhikrCounters.value.toMutableMap()
        currentMap[id] = 0
        _dhikrCounters.value = currentMap
        showToast("تمت إعادة العداد")
    }

    fun toggleFavoriteDouaa(id: String) {
        val currentFavs = _favoriteDouaaIds.value.toMutableSet()
        if (currentFavs.contains(id)) {
            currentFavs.remove(id)
            showToast("تمت الإزالة من المحفوظات")
        } else {
            currentFavs.add(id)
            showToast("تم الحفظ في أدعيتك")
        }
        _favoriteDouaaIds.value = currentFavs
        saveFavorites(currentFavs)
    }

    private fun loadFavorites(): Set<String> {
        val set = prefs.getStringSet("douaa_favs", emptySet()) ?: emptySet()
        return set.toSet()
    }

    private fun saveFavorites(favs: Set<String>) {
        prefs.edit().putStringSet("douaa_favs", favs).apply()
    }

    // --- Tab 3: Hadith & Books API Logic ---
    fun setHadithCategory(cat: String) {
        _hadithCategory.value = cat
    }

    fun setHadithSearchQuery(query: String) {
        _hadithSearchQuery.value = query
    }

    fun openHadithBook(context: Context, book: HadithBook) {
        _selectedHadithBook.value = book
        viewModelScope.launch {
            _isBookHadithsLoading.value = true
            val list = fetchHadithBookFromApiOrCache(context, book)
            _bookHadithsList.value = list
            _isBookHadithsLoading.value = false
        }
    }

    fun closeHadithBook() {
        _selectedHadithBook.value = null
        _bookHadithsList.value = emptyList()
    }

    private suspend fun fetchHadithBookFromApiOrCache(context: Context, book: HadithBook): List<HadithItem> = withContext(Dispatchers.IO) {
        val hadithDir = File(context.filesDir, "mydin/hadith")
        if (!hadithDir.exists()) hadithDir.mkdirs()
        val localFile = File(hadithDir, "book_${book.id}.json")

        if (localFile.exists()) {
            try {
                val jsonStr = localFile.readText()
                val parsed = parseHadithBookJson(jsonStr, book)
                if (parsed.isNotEmpty()) return@withContext parsed
            } catch (_: Exception) {}
        }

        // Primary: Keeftalk CDN
        try {
            val urlString = "https://public.cdn.keeftalk.com/mydin/hadith/book_${book.id}.json"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.useCaches = true
            conn.setRequestProperty("Cache-Control", "max-age=31536000, immutable")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val jsonStr = sb.toString()
                localFile.writeText(jsonStr)
                val parsed = parseHadithBookJson(jsonStr, book)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        } catch (_: Exception) {}

        // Fallback API
        try {
            val urlString = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-${book.id}.json"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val jsonStr = sb.toString()
                localFile.writeText(jsonStr)
                val parsed = parseHadithBookJson(jsonStr, book)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        } catch (_: Exception) {}

        // Fallback to local curated Hadiths for this book
        return@withContext HADITH_ITEMS.filter { it.book.contains(book.nameAr) || book.id == "bukhari" }
    }

    private fun parseHadithBookJson(jsonStr: String, book: HadithBook): List<HadithItem> {
        return try {
            val root = JSONObject(jsonStr)
            val hadithsArr = root.optJSONArray("hadiths") ?: JSONArray()
            val result = mutableListOf<HadithItem>()

            for (i in 0 until Math.min(hadithsArr.length(), 200)) {
                val obj = hadithsArr.getJSONObject(i)
                val text = obj.optString("text", "")
                val num = obj.optInt("hadithnumber", i + 1)
                if (text.isNotBlank()) {
                    result.add(
                        HadithItem(
                            id = "${book.id}_$num",
                            book = book.nameAr,
                            narrator = "حديث رقم $num في ${book.nameAr}",
                            arabicText = text,
                            sourceRef = "${book.nameAr} · حديث $num",
                            category = "all",
                            grade = "حديث صحيح"
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- Tab 4: Quran & Download Logic ---
    fun setQuranSearchQuery(query: String) {
        _quranSearchQuery.value = query
    }

    private fun loadLastReadSurah(): SurahMeta {
        val num = prefs.getInt("last_read_surah_num", 1)
        return SURAH_LIST.find { it.number == num } ?: SURAH_LIST[0]
    }

    private fun saveLastReadSurah(surah: SurahMeta) {
        prefs.edit().putInt("last_read_surah_num", surah.number).apply()
        _lastReadSurah.value = surah
    }

    fun toggleTajweed() {
        val newValue = !_isTajweedEnabled.value
        _isTajweedEnabled.value = newValue
        showToast(if (newValue) "تم تفعيل ألوان التجويد" else "تم إخفاء ألوان التجويد")
    }

    fun openSurah(context: Context, surahNumber: Int) {
        val surah = SURAH_LIST.find { it.number == surahNumber } ?: return
        _selectedSurah.value = surah
        _isReaderOpen.value = true
        saveLastReadSurah(surah)
        // Background audio rule: DO NOT stop audio playback when opening reader!

        viewModelScope.launch {
            _isSurahLoading.value = true
            val data = fetchSurahFromApiOrCache(context, surahNumber)
            if (data != null) {
                _ayahsList.value = data.first
                _isTajweedAvailable.value = data.second
            } else if (surahNumber == 1) {
                _ayahsList.value = FATIHAH_FALLBACK.mapIndexed { i, text ->
                    AyahDetail(numberInSurah = i + 1, plainText = text)
                }
                _isTajweedAvailable.value = false
            } else {
                _ayahsList.value = emptyList()
                _isTajweedAvailable.value = false
                showToast("تعذّر تحميل أيات السورة")
            }
            _isSurahLoading.value = false
        }
    }

    fun closeReader(context: Context) {
        // Background audio rule: DO NOT stop audio playback when closing reader or navigating back!
        _isReaderOpen.value = false
        _selectedSurah.value = null
        _ayahsList.value = emptyList()
    }

    fun openTafsir(ayahNum: Int) {
        _tafsirAyahNumber.value = ayahNum
    }

    fun closeTafsir() {
        _tafsirAyahNumber.value = null
    }

    fun navTafsir(delta: Int) {
        val current = _tafsirAyahNumber.value ?: return
        val list = _ayahsList.value
        val currentIndex = list.indexOfFirst { it.numberInSurah == current }
        if (currentIndex != -1) {
            val targetIndex = currentIndex + delta
            if (targetIndex in list.indices) {
                _tafsirAyahNumber.value = list[targetIndex].numberInSurah
            }
        }
    }

    // --- Audio Playback & Service Bridge ---
    fun togglePlaybackMode() {
        val current = _playbackMode.value
        val nextMode = when (current) {
            PlaybackMode.AUTOPLAY_NEXT_SURAH -> PlaybackMode.REPEAT_SURAH
            PlaybackMode.REPEAT_SURAH -> PlaybackMode.REPEAT_AYAH
            PlaybackMode.REPEAT_AYAH -> PlaybackMode.STOP_AT_END
            PlaybackMode.STOP_AT_END -> PlaybackMode.AUTOPLAY_NEXT_SURAH
        }
        _playbackMode.value = nextMode
        showToast(nextMode.title)
    }

    fun changeReciter(context: Context, reciter: Reciter) {
        _selectedReciter.value = reciter
        prefs.edit().putString("reciter_id", reciter.id).apply()
        showToast("تم تغيير المقرئ · ${reciter.shortName}")
        val currentAyah = _audioPlayingAyah.value
        if (currentAyah != null && _isAudioPlaying.value) {
            playAyah(context, currentAyah)
        }
    }

    private fun loadSavedReciter(): Reciter {
        val id = prefs.getString("reciter_id", "Husary_128kbps") ?: "Husary_128kbps"
        return RECITERS.find { it.id == id } ?: RECITERS[0]
    }

    fun playAyah(context: Context, ayahNumber: Int) {
        val surah = _selectedSurah.value ?: return
        _audioPlayingAyah.value = ayahNumber

        val surahPadded = String.format(Locale.US, "%03d", surah.number)
        val ayahPadded = String.format(Locale.US, "%03d", ayahNumber)
        val reciterId = _selectedReciter.value.id

        // Hidden local storage path: /storage/.../keeftalk/.MyQuran/audio/{reciterId}/{surahNum}_{ayahNum}.mp3
        val audioDir = getQuranAudioDir(context, reciterId)
        val localAudioFile = File(audioDir, "${surah.number}_$ayahNumber.mp3")
        val localPath = if (localAudioFile.exists() && localAudioFile.length() > 100) localAudioFile.absolutePath else null
        val remoteUrl = "https://everyayah.com/data/$reciterId/$surahPadded$ayahPadded.mp3"

        val intent = Intent(context, MyDinAudioService::class.java).apply {
            action = MyDinAudioService.ACTION_PLAY_AYAH
            putExtra(MyDinAudioService.EXTRA_SURAH_NUM, surah.number)
            putExtra(MyDinAudioService.EXTRA_SURAH_NAME, surah.arName)
            putExtra(MyDinAudioService.EXTRA_AYAH_NUM, ayahNumber)
            putExtra(MyDinAudioService.EXTRA_TOTAL_AYAHS, surah.ayahsCount)
            putExtra(MyDinAudioService.EXTRA_RECITER_ID, reciterId)
            putExtra(MyDinAudioService.EXTRA_RECITER_NAME, _selectedReciter.value.shortName)
            putExtra(MyDinAudioService.EXTRA_AUDIO_URL, remoteUrl)
            putExtra(MyDinAudioService.EXTRA_LOCAL_PATH, localPath)
            putExtra(MyDinAudioService.EXTRA_PLAYBACK_MODE, _playbackMode.value.name)
        }

        startServiceSafely(context, intent)
        _isAudioPlaying.value = true
    }

    fun togglePlayPauseAudio(context: Context) {
        val currentAyah = _audioPlayingAyah.value ?: 1
        _audioPlayingAyah.value = currentAyah

        if (!_isAudioPlaying.value) {
            playAyah(context, currentAyah)
        } else {
            val intent = Intent(context, MyDinAudioService::class.java).apply {
                action = MyDinAudioService.ACTION_TOGGLE_PLAY
            }
            startServiceSafely(context, intent)
            _isAudioPlaying.value = false
        }
    }

    fun prevAyahAudio(context: Context) {
        val cur = _audioPlayingAyah.value ?: 1
        if (cur > 1) {
            playAyah(context, cur - 1)
        }
    }

    fun nextAyahAudio(context: Context) {
        val cur = _audioPlayingAyah.value ?: 1
        val total = _ayahsList.value.size
        if (cur < total) {
            playAyah(context, cur + 1)
        }
    }

    fun stopAudio(context: Context) {
        val intent = Intent(context, MyDinAudioService::class.java).apply {
            action = MyDinAudioService.ACTION_STOP
        }
        startServiceSafely(context, intent)
        _isAudioPlaying.value = false
        _isAudioLoading.value = false
        _audioPlayingAyah.value = null
    }

    private fun startServiceSafely(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }

    // --- Downloads & Offline Storage ---
    fun requestAudioDownload(surah: SurahMeta, reciter: Reciter) {
        val estimatedMb = (surah.ayahsCount * 0.35f)
        _downloadDialogInfo.value = DownloadDialogInfo(surah, reciter, estimatedMb)
    }

    fun dismissDownloadDialog() {
        _downloadDialogInfo.value = null
    }

    fun confirmAudioDownload(context: Context) {
        val dialogInfo = _downloadDialogInfo.value ?: return
        _downloadDialogInfo.value = null

        val surah = dialogInfo.surah
        val reciter = dialogInfo.reciter

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _downloadProgressState.value = DownloadProgressInfo(
                    surahNumber = surah.number,
                    completedAyahs = 0,
                    totalAyahs = surah.ayahsCount,
                    percent = 0f,
                    isDownloading = true
                )

                // 1. Download text & tafsir to keeftalk/.MyQuran/surahs
                fetchSurahFromApiOrCache(context, surah.number)

                // 2. Download audio files for all ayahs to keeftalk/.MyQuran/audio/{reciterId}
                val audioDir = getQuranAudioDir(context, reciter.id)

                for (i in 1..surah.ayahsCount) {
                    val targetFile = File(audioDir, "${surah.number}_$i.mp3")
                    if (!targetFile.exists() || targetFile.length() < 100) {
                        val surahPadded = String.format(Locale.US, "%03d", surah.number)
                        val ayahPadded = String.format(Locale.US, "%03d", i)
                        val urlString = "https://everyayah.com/data/${reciter.id}/$surahPadded$ayahPadded.mp3"

                        val url = URL(urlString)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000

                        if (conn.responseCode == 200) {
                            val input = conn.inputStream
                            val output = FileOutputStream(targetFile)
                            input.copyTo(output)
                            output.flush()
                            output.close()
                            input.close()
                        }
                    }

                    val pct = (i.toFloat() / surah.ayahsCount.toFloat()) * 100f
                    _downloadProgressState.value = DownloadProgressInfo(
                        surahNumber = surah.number,
                        completedAyahs = i,
                        totalAyahs = surah.ayahsCount,
                        percent = pct,
                        isDownloading = true
                    )
                }

                _downloadProgressState.value = DownloadProgressInfo(
                    surahNumber = surah.number,
                    completedAyahs = surah.ayahsCount,
                    totalAyahs = surah.ayahsCount,
                    percent = 100f,
                    isDownloading = false
                )
                showToast("تم تحميل سورة ${surah.arName} وتلاوتها بنجاح")
            } catch (_: Exception) {
                _downloadProgressState.value = null
                showToast("تعذّر اكتمال التحميل")
            }
        }
    }

    fun downloadAllQuran(
        context: Context,
        downloadTexts: Boolean,
        downloadAudio: Boolean,
        reciter: Reciter,
        selectedSurahs: List<SurahMeta>
    ) {
        batchDownloadJob?.cancel()
        batchDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalSurahsCount = selectedSurahs.size
                for ((idx, surah) in selectedSurahs.withIndex()) {
                    _batchDownloadProgress.value = BatchDownloadProgress(
                        currentSurahNum = surah.number,
                        totalSurahs = totalSurahsCount,
                        surahName = surah.arName,
                        completedAyahs = 0,
                        totalAyahsInSurah = surah.ayahsCount,
                        overallPercent = (idx.toFloat() / totalSurahsCount.toFloat()) * 100f,
                        isDownloading = true
                    )

                    if (downloadTexts) {
                        fetchSurahFromApiOrCache(context, surah.number)
                    }

                    if (downloadAudio) {
                        val audioDir = getQuranAudioDir(context, reciter.id)
                        for (i in 1..surah.ayahsCount) {
                            val targetFile = File(audioDir, "${surah.number}_$i.mp3")
                            // Prevent redundancy: skip if valid file already exists
                            if (!targetFile.exists() || targetFile.length() < 100) {
                                val surahPadded = String.format(Locale.US, "%03d", surah.number)
                                val ayahPadded = String.format(Locale.US, "%03d", i)
                                val urlString = "https://everyayah.com/data/${reciter.id}/$surahPadded$ayahPadded.mp3"

                                val url = URL(urlString)
                                val conn = url.openConnection() as HttpURLConnection
                                conn.connectTimeout = 8000
                                conn.readTimeout = 8000

                                if (conn.responseCode == 200) {
                                    val input = conn.inputStream
                                    val output = FileOutputStream(targetFile)
                                    input.copyTo(output)
                                    output.flush()
                                    output.close()
                                    input.close()
                                }
                            }

                            val overallPct = ((idx.toFloat() + (i.toFloat() / surah.ayahsCount.toFloat())) / totalSurahsCount.toFloat()) * 100f
                            _batchDownloadProgress.value = BatchDownloadProgress(
                                currentSurahNum = surah.number,
                                totalSurahs = totalSurahsCount,
                                surahName = surah.arName,
                                completedAyahs = i,
                                totalAyahsInSurah = surah.ayahsCount,
                                overallPercent = overallPct,
                                isDownloading = true
                            )
                        }
                    }
                }

                _batchDownloadProgress.value = BatchDownloadProgress(
                    currentSurahNum = 114,
                    totalSurahs = totalSurahsCount,
                    surahName = "القرآن الكريم",
                    completedAyahs = 0,
                    totalAyahsInSurah = 0,
                    overallPercent = 100f,
                    isDownloading = false
                )
                showToast("تم اكتمال تحميل القرآن الكريم بنجاح")
            } catch (_: Exception) {
                _batchDownloadProgress.value = null
                showToast("تعذّر اكتمال التحميل الجماعي")
            }
        }
    }

    fun cancelBatchDownload() {
        batchDownloadJob?.cancel()
        batchDownloadJob = null
        _batchDownloadProgress.value = null
        showToast("تم إلغاء التحميل")
    }

    fun cancelAudioDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadProgressState.value = null
        showToast("تم إلغاء التحميل")
    }

    fun checkCdnPrompt() {
        val asked = prefs.getBoolean("cdn_pack_asked", false)
        if (!asked) {
            _showCdnPrompt.value = true
        }
    }

    fun dismissCdnPrompt() {
        prefs.edit().putBoolean("cdn_pack_asked", true).apply()
        _showCdnPrompt.value = false
    }

    fun downloadCustomCdnPack(context: Context) {
        prefs.edit().putBoolean("cdn_pack_asked", true).apply()
        _showCdnPrompt.value = false
        showToast("الاتصال بخادم Keeftalk CDN…")
        downloadAllQuran(
            context = context,
            downloadTexts = true,
            downloadAudio = false,
            reciter = selectedReciter.value,
            selectedSurahs = SURAH_LIST
        )
    }

    fun isSurahAudioDownloaded(context: Context, surahNum: Int, reciterId: String): Boolean {
        val count = getDownloadedAyahsCount(context, surahNum, reciterId)
        return count.first > 0 && count.first == count.second
    }

    fun getDownloadedAyahsCount(context: Context, surahNum: Int, reciterId: String): Pair<Int, Int> {
        val surah = SURAH_LIST.find { it.number == surahNum } ?: return Pair(0, 0)
        val audioDir = getQuranAudioDir(context, reciterId)
        if (!audioDir.exists()) return Pair(0, surah.ayahsCount)
        var downloaded = 0
        for (i in 1..surah.ayahsCount) {
            val file = File(audioDir, "${surahNum}_$i.mp3")
            if (file.exists() && file.length() > 100) downloaded++
        }
        return Pair(downloaded, surah.ayahsCount)
    }

    fun deleteDownloadedAudio(context: Context, surahNum: Int, reciterId: String) {
        val surah = SURAH_LIST.find { it.number == surahNum } ?: return
        val audioDir = getQuranAudioDir(context, reciterId)
        for (i in 1..surah.ayahsCount) {
            val file = File(audioDir, "${surahNum}_$i.mp3")
            if (file.exists()) file.delete()
        }
        _downloadProgressState.value = null
        showToast("تم حذف التلاوة المحمّلة")
    }

    fun getDownloadedSurahsTextCount(context: Context): Int {
        val surahsDir = getQuranSurahsDir(context)
        if (!surahsDir.exists()) return 0
        var count = 0
        for (i in 1..114) {
            val f = File(surahsDir, "surah_$i.json")
            if (f.exists() && f.length() > 100) count++
        }
        return count
    }

    fun deleteAllDownloadedData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseDir = getQuranStorageBaseDir(context)
                if (baseDir.exists()) {
                    baseDir.deleteRecursively()
                    baseDir.mkdirs()
                    File(baseDir, ".nomedia").createNewFile()
                }
                withContext(Dispatchers.Main) {
                    _downloadProgressState.value = null
                    _batchDownloadProgress.value = null
                    showToast("تم حذف جميع البيانات المحمّلة")
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("تعذّر حذف البيانات المحمّلة")
                }
            }
        }
    }

    private suspend fun fetchSurahFromApiOrCache(context: Context, surahNum: Int): Pair<List<AyahDetail>, Boolean>? = withContext(Dispatchers.IO) {
        val surahDir = getQuranSurahsDir(context)
        val localFile = File(surahDir, "surah_$surahNum.json")

        if (localFile.exists()) {
            try {
                val jsonStr = localFile.readText()
                val parsed = parseSurahJson(jsonStr, surahNum)
                if (parsed != null) return@withContext parsed
            } catch (_: Exception) {}
        }

        // Primary: Keeftalk CDN
        try {
            val urlString = "https://public.cdn.keeftalk.com/mydin/surahs/surah_$surahNum.json"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.useCaches = true
            conn.setRequestProperty("Cache-Control", "max-age=31536000, immutable")

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val jsonStr = sb.toString()
                localFile.writeText(jsonStr)
                return@withContext parseSurahJson(jsonStr, surahNum)
            }
        } catch (_: Exception) {}

        // Fallback API 1: AlQuran Cloud Tajweed Edition
        try {
            val urlString = "https://api.alquran.cloud/v1/surah/$surahNum/editions/quran-uthmani,quran-tajweed,ar.muyassar"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val stream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val jsonStr = sb.toString()
                localFile.writeText(jsonStr)
                return@withContext parseSurahJson(jsonStr, surahNum)
            }
        } catch (_: Exception) {}

        // Fallback
        try {
            val urlString = "https://api.alquran.cloud/v1/surah/$surahNum/editions/quran-uthmani,ar.muyassar"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                val jsonStr = sb.toString()
                localFile.writeText(jsonStr)
                return@withContext parseSurahJson(jsonStr, surahNum)
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun parseSurahJson(jsonStr: String, surahNum: Int): Pair<List<AyahDetail>, Boolean>? {
        return try {
            val root = JSONObject(jsonStr)
            val dataArray = root.getJSONArray("data")
            if (dataArray.length() < 2) return null

            val uthmaniObj = dataArray.getJSONObject(0)
            val uthmaniAyahs = uthmaniObj.getJSONArray("ayahs")

            val hasTajweed = dataArray.length() >= 3
            val tajweedAyahs = if (hasTajweed) dataArray.getJSONObject(1).getJSONArray("ayahs") else null
            val tafsirAyahs = if (hasTajweed) dataArray.getJSONObject(2).getJSONArray("ayahs") else dataArray.getJSONObject(1).getJSONArray("ayahs")

            val result = mutableListOf<AyahDetail>()

            for (i in 0 until uthmaniAyahs.length()) {
                val uObj = uthmaniAyahs.getJSONObject(i)
                var plainText = uObj.optString("text", "")

                if (i == 0 && surahNum != 1 && surahNum != 9 && plainText.startsWith("بِسْمِ")) {
                    val parts = plainText.split(Regex("\\s+"))
                    if (parts.size > 4) {
                        plainText = parts.subList(4, parts.size).joinToString(" ")
                    }
                }

                val tajweedHtml = if (tajweedAyahs != null && i < tajweedAyahs.length()) {
                    tajweedAyahs.getJSONObject(i).optString("text", "")
                } else ""

                val tafsirText = if (tafsirAyahs != null && i < tafsirAyahs.length()) {
                    tafsirAyahs.getJSONObject(i).optString("text", "")
                } else ""

                val num = uObj.optInt("numberInSurah", i + 1)

                result.add(
                    AyahDetail(
                        numberInSurah = num,
                        plainText = plainText,
                        tajweedHtml = tajweedHtml,
                        tafsirText = tafsirText
                    )
                )
            }

            Pair(result, hasTajweed)
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
    }
}
