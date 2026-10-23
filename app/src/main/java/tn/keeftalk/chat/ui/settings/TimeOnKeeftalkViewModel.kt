package tn.keeftalk.chat.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.TimeTrackingEntity
import tn.keeftalk.chat.data.repository.TimeTrackingRepository
import tn.keeftalk.chat.di.AppModule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class TimeRange {
    DAY, WEEK, MONTH, ALL_TIME
}

enum class TrendMode {
    WEEKLY, MONTHLY, YEARLY, ALL_TIME
}

data class FeatureSpec(
    val key: String,
    val name: String,
    val iconKey: String,
    val colorHex: String
)

val FEATURE_SPECS = listOf(
    FeatureSpec("CHATS", "My Chats", "chat", "#7D5CFF"),
    FeatureSpec("CALLS", "My Calls", "phone", "#19B6D6"),
    FeatureSpec("SMS", "My SMS/MMS", "sms", "#4B89EB"),
    FeatureSpec("NOTES", "My Notes", "pen", "#2ED9A0"),
    FeatureSpec("VAULT", "My Vault", "shield", "#1BA6A6"),
    FeatureSpec("SECRETS", "My Secrets", "key", "#E8456B"),
    FeatureSpec("CALENDAR", "My Calendar", "calendar", "#FFB020"),
    FeatureSpec("WALLET", "My Wallet", "package", "#FF8A5B"),
    FeatureSpec("FINANCES", "My Finances", "trend", "#2ECC71"),
    FeatureSpec("EMAIL", "My E-Mail", "mail", "#B388FF"),
    FeatureSpec("FEED", "My Feed", "rss", "#00CCCC"),
    FeatureSpec("BOOKS", "My Books", "book", "#5B6CF0"),
    FeatureSpec("MY_DIN", "My Din", "mosque", "#00CCCC"),
    FeatureSpec("MARKET", "My Market", "shopping", "#FF6FB5"),
    FeatureSpec("HABITS", "My Habits", "check", "#8BE04E"),
    FeatureSpec("BROWSER", "My Browser", "globe", "#3D9BFF"),
    FeatureSpec("SETTINGS", "Settings", "settings", "#8A97B5")
)

data class BarPoint(
    val label: String,
    val subLabel: String = "",
    val minutes: Float,
    val tipLabel: String
)

data class FeatureUsageModel(
    val key: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val minutes: Float,
    val percentage: Float
)

data class TrendPoint(
    val label: String,
    val dailyMinutes: Float,
    val totalMinutes: Float,
    val tipLabel: String
)

data class TimeOnKeeftalkUiState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val heroHeadline: String = "Last 7 days",
    val totalMinutes: Float = 0f,
    val deltaPercent: Int = 0,
    val deltaLabel: String = "vs prior 7 days",
    val avgStatLabel: String = "Avg per day",
    val avgStatValue: String = "0m",
    val peakStatLabel: String = "Peak day",
    val peakStatValue: String = "0m",
    val topFeatureName: String = "None",
    val barChartTitle: String = "Daily usage",
    val barChartPoints: List<BarPoint> = emptyList(),
    val selectedBarIndex: Int = -1,
    val barChartAvgMinutes: Float = 0f,
    val featureDistribution: List<FeatureUsageModel> = emptyList(),
    val selectedFeatureIndex: Int = -1,
    val selectedTrendMode: TrendMode = TrendMode.WEEKLY,
    val trendHeadline: String = "Usage trend",
    val trendPoints: List<TrendPoint> = emptyList(),
    val selectedTrendIndex: Int = -1,
    val lifetimeMemberSince: String = "Today",
    val lifetimeTotalHours: String = "0h",
    val lifetimeActiveDays: String = "0 days",
    val lifetimeMostUsed: String = "None"
)

class TimeOnKeeftalkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimeTrackingRepository(
        application,
        AppModule.provideDatabase(application).timeTrackingDao()
    )

    private val _uiState = MutableStateFlow(TimeOnKeeftalkUiState())
    val uiState: StateFlow<TimeOnKeeftalkUiState> = _uiState.asStateFlow()

    private var cachedLogs: List<TimeTrackingEntity> = emptyList()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.observeAllLogs().collect { logs ->
                cachedLogs = logs
                computeState()
            }
        }
    }

    fun selectRange(range: TimeRange) {
        _uiState.update { it.copy(selectedRange = range, selectedBarIndex = -1, selectedFeatureIndex = -1) }
        computeState()
    }

    fun selectBarIndex(index: Int) {
        _uiState.update { it.copy(selectedBarIndex = index) }
    }

    fun selectFeatureIndex(index: Int) {
        _uiState.update { state ->
            val newIdx = if (state.selectedFeatureIndex == index) -1 else index
            state.copy(selectedFeatureIndex = newIdx)
        }
    }

    fun selectTrendMode(mode: TrendMode) {
        _uiState.update { it.copy(selectedTrendMode = mode, selectedTrendIndex = -1) }
        computeState()
    }

    fun selectTrendIndex(index: Int) {
        _uiState.update { it.copy(selectedTrendIndex = index) }
    }

    private fun computeState() {
        val today = LocalDate.now()
        val range = _uiState.value.selectedRange
        val trendMode = _uiState.value.selectedTrendMode

        val logsByDay = cachedLogs.groupBy { it.epochDay }

        // Compute Range Data
        val (numDays, headline, deltaLabel, deltaPercent) = when (range) {
            TimeRange.DAY -> Quad(1, "Today", "vs yesterday", 0)
            TimeRange.WEEK -> Quad(7, "Last 7 days", "vs prior 7 days", 0)
            TimeRange.MONTH -> Quad(30, "Last 30 days", "vs prior 30 days", 0)
            TimeRange.ALL_TIME -> Quad(365, "Since day one", "vs prior period", 0)
        }

        // Build bar points from real logs
        val barPoints = mutableListOf<BarPoint>()
        var totalRangeSecs = 0L

        if (range == TimeRange.DAY) {
            val todayLogs = logsByDay[today.toEpochDay()] ?: emptyList()
            totalRangeSecs = todayLogs.sumOf { it.durationSeconds }
            val hourAvgMins = (totalRangeSecs / 24f) / 60f
            for (h in 0 until 24) {
                val label = if (h % 6 == 0) "${if (h % 12 == 0) 12 else h % 12}${if (h < 12) "a" else "p"}" else ""
                val tip = "Today, ${if (h % 12 == 0) 12 else h % 12} ${if (h < 12) "AM" else "PM"}"
                barPoints.add(BarPoint(label = label, minutes = hourAvgMins, tipLabel = tip))
            }
        } else {
            val daysCount = if (range == TimeRange.ALL_TIME) 90 else numDays
            for (i in (daysCount - 1) downTo 0) {
                val d = today.minusDays(i.toLong())
                val dayLogs = logsByDay[d.toEpochDay()] ?: emptyList()
                val daySecs = dayLogs.sumOf { it.durationSeconds }
                totalRangeSecs += daySecs
                val dayMins = daySecs / 60f
                val label = when (range) {
                    TimeRange.WEEK -> d.format(DateTimeFormatter.ofPattern("EEE"))
                    TimeRange.MONTH -> if (i % 5 == 0) d.format(DateTimeFormatter.ofPattern("d")) else ""
                    else -> if (i % 15 == 0) d.format(DateTimeFormatter.ofPattern("MMM")) else ""
                }
                val tip = d.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                barPoints.add(BarPoint(label = label, minutes = dayMins, tipLabel = tip))
            }
        }

        val totalRangeMins = totalRangeSecs / 60f
        val avgMinsPerDay = if (numDays > 0) totalRangeMins / numDays else 0f
        val peakMins = barPoints.maxOfOrNull { it.minutes } ?: 0f

        // Compute Feature Distribution from real logs
        val featureTotals = mutableMapOf<String, Float>()
        for (spec in FEATURE_SPECS) {
            featureTotals[spec.key] = 0f
        }

        if (range == TimeRange.DAY) {
            val todayLogs = logsByDay[today.toEpochDay()] ?: emptyList()
            for (l in todayLogs) {
                featureTotals[l.featureKey] = (featureTotals[l.featureKey] ?: 0f) + (l.durationSeconds / 60f)
            }
        } else {
            val daysCount = if (range == TimeRange.ALL_TIME) 365 else numDays
            for (i in 0 until daysCount) {
                val d = today.minusDays(i.toLong())
                val dayLogs = logsByDay[d.toEpochDay()] ?: emptyList()
                for (l in dayLogs) {
                    featureTotals[l.featureKey] = (featureTotals[l.featureKey] ?: 0f) + (l.durationSeconds / 60f)
                }
            }
        }

        val sumFeatureMins = featureTotals.values.sum()

        val featureModels = FEATURE_SPECS.map { spec ->
            val mins = featureTotals[spec.key] ?: 0f
            val pct = if (sumFeatureMins > 0f) (mins / sumFeatureMins * 100f) else 0f
            FeatureUsageModel(
                key = spec.key,
                name = spec.name,
                iconKey = spec.iconKey,
                colorHex = spec.colorHex,
                minutes = mins,
                percentage = pct
            )
        }.sortedByDescending { it.minutes }

        val topFeature = if (sumFeatureMins > 0f) (featureModels.firstOrNull { it.minutes > 0f }?.name ?: "None") else "None"

        // Compute Trend Points from real logs
        val trendPoints = mutableListOf<TrendPoint>()
        val numTrendPoints = when (trendMode) {
            TrendMode.WEEKLY -> 12
            TrendMode.MONTHLY -> 12
            TrendMode.YEARLY -> 5
            TrendMode.ALL_TIME -> 12
        }

        for (i in 0 until numTrendPoints) {
            val idx = numTrendPoints - 1 - i
            val (label, tip, trendSecs) = when (trendMode) {
                TrendMode.WEEKLY -> {
                    val endDay = today.minusWeeks(idx.toLong())
                    val startDay = endDay.minusDays(6)
                    val secs = cachedLogs.filter { it.epochDay in startDay.toEpochDay()..endDay.toEpochDay() }.sumOf { it.durationSeconds }
                    Triple(
                        "W${i + 1}",
                        "Week ending ${endDay.format(DateTimeFormatter.ofPattern("MMM d"))}",
                        secs
                    )
                }
                TrendMode.MONTHLY -> {
                    val mDate = today.minusMonths(idx.toLong())
                    val startM = mDate.withDayOfMonth(1)
                    val endM = mDate.withDayOfMonth(mDate.lengthOfMonth())
                    val secs = cachedLogs.filter { it.epochDay in startM.toEpochDay()..endM.toEpochDay() }.sumOf { it.durationSeconds }
                    Triple(
                        mDate.format(DateTimeFormatter.ofPattern("MMM")),
                        mDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        secs
                    )
                }
                TrendMode.YEARLY -> {
                    val yYear = today.year - idx
                    val startY = LocalDate.of(yYear, 1, 1)
                    val endY = LocalDate.of(yYear, 12, 31)
                    val secs = cachedLogs.filter { it.epochDay in startY.toEpochDay()..endY.toEpochDay() }.sumOf { it.durationSeconds }
                    Triple(
                        "$yYear",
                        "Year $yYear",
                        secs
                    )
                }
                TrendMode.ALL_TIME -> {
                    val mDate = today.minusMonths(idx.toLong())
                    val startM = mDate.withDayOfMonth(1)
                    val endM = mDate.withDayOfMonth(mDate.lengthOfMonth())
                    val secs = cachedLogs.filter { it.epochDay in startM.toEpochDay()..endM.toEpochDay() }.sumOf { it.durationSeconds }
                    Triple(
                        if (i % 3 == 0) mDate.format(DateTimeFormatter.ofPattern("MMM yyyy")) else "",
                        mDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        secs
                    )
                }
            }

            val totalMins = trendSecs / 60f
            val dailyMins = if (trendMode == TrendMode.WEEKLY) totalMins / 7f else totalMins / 30f
            trendPoints.add(TrendPoint(label = label, dailyMinutes = dailyMins, totalMinutes = totalMins, tipLabel = tip))
        }

        // Real Lifetime Stats
        val lifetimeSecs = cachedLogs.sumOf { it.durationSeconds }
        val lifetimeHours = (lifetimeSecs / 3600f).roundToInt()
        val activeDays = cachedLogs.filter { it.durationSeconds > 0 }.map { it.epochDay }.distinct().size
        val earliestEpochDay = cachedLogs.filter { it.durationSeconds > 0 }.minOfOrNull { it.epochDay }
        val memberSince = earliestEpochDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM yyyy")) } ?: today.format(DateTimeFormatter.ofPattern("MMM yyyy"))

        _uiState.update {
            it.copy(
                selectedRange = range,
                heroHeadline = headline,
                totalMinutes = sumFeatureMins,
                deltaPercent = deltaPercent,
                deltaLabel = deltaLabel,
                avgStatLabel = if (range == TimeRange.DAY) "Avg per hour" else "Avg per day",
                avgStatValue = formatMinutes(avgMinsPerDay),
                peakStatLabel = if (range == TimeRange.DAY) "Peak hour" else "Peak day",
                peakStatValue = formatMinutes(peakMins),
                topFeatureName = topFeature,
                barChartTitle = if (range == TimeRange.DAY) "Hourly usage" else if (range == TimeRange.ALL_TIME) "Monthly usage" else "Daily usage",
                barChartPoints = barPoints,
                barChartAvgMinutes = avgMinsPerDay,
                featureDistribution = featureModels,
                selectedTrendMode = trendMode,
                trendHeadline = "Usage trend (${trendMode.name.lowercase()})",
                trendPoints = trendPoints,
                lifetimeMemberSince = memberSince,
                lifetimeTotalHours = "${lifetimeHours}h",
                lifetimeActiveDays = "$activeDays days",
                lifetimeMostUsed = topFeature
            )
        }
    }

    private fun formatMinutes(mins: Float): String {
        val total = mins.roundToInt()
        val h = total / 60
        val m = total % 60
        return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m}m"
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
