package tn.keeftalk.chat.ui.habits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.dao.NativeHabitDao
import tn.keeftalk.chat.data.remote.HabitsRemoteDataSourceNoOp
import tn.keeftalk.chat.data.repository.HabitsRepositoryImpl
import tn.keeftalk.chat.domain.engine.HabitEngine
import tn.keeftalk.chat.domain.model.habit.*
import tn.keeftalk.chat.domain.repository.HabitsRepository
import tn.keeftalk.chat.util.SoundEffectManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class HabitsTab {
    HABITS, STATS
}

data class CategoryInfo(
    val key: String,
    val label: String,
    val icon: String,
    val days: Int,
    val range: String,
    val shortDesc: String,
    val badge: String,
    val examples: String
)

val HABIT_CATEGORIES = mapOf(
    "drink" to CategoryInfo("drink", "Starter Habit", "leaf", 18, "About 3–4 weeks", "Small habits that are easy to start and repeat.", "QUICK WIN", "Water • Floss • Make bed • Vitamins"),
    "hygiene" to CategoryInfo("hygiene", "Starter Habit", "heart", 30, "About 3–4 weeks", "Small habits that are easy to start and repeat.", "QUICK WIN", "Skincare • Vitamins • Floss"),
    "eating" to CategoryInfo("eating", "Core Routine", "bolt", 45, "About 6–8 weeks", "Everyday routines that grow through consistent repetition.", "BUILD MOMENTUM", "Reading • Meditation • Journaling"),
    "light" to CategoryInfo("light", "Active Lifestyle", "dumbbell", 106, "About 3–5 months", "Habits that require more effort, planning, or consistency.", "LONG-TERM GROWTH", "Gym • Walking • Yoga • Stretching"),
    "complex" to CategoryInfo("complex", "Transformative Habit", "rocket", 254, "6+ months", "Big lifestyle changes built through patience and sustained commitment.", "MAJOR TRANSFORMATION", "Marathon training • Major fitness transformation")
)

val CAT_KEYWORDS = listOf(
    Pair("drink", Regex("water|hydrat|drink|tea|juice|coffee|smoothie|electrolyte", RegexOption.IGNORE_CASE)),
    Pair("hygiene", Regex("floss|brush|vitamin|shower|skincare|wash|moistur|meds|pill|sunscreen", RegexOption.IGNORE_CASE)),
    Pair("complex", Regex("run|gym|workout|sit-?up|push-?up|weight|lift|hiit|crossfit|sprint|deadlift|squat|bench|swim", RegexOption.IGNORE_CASE)),
    Pair("light", Regex("walk|stretch|yoga|step|jog|bike|cycl|dance|pilates|mobility|breathe", RegexOption.IGNORE_CASE)),
    Pair("eating", Regex("eat|fruit|veg|meal|diet|breakfast|lunch|dinner|snack|read|book|study|learn|write|journal|meditat|mindful|draw|paint", RegexOption.IGNORE_CASE))
)

fun detectCat(name: String): String {
    if (name.isBlank()) return "drink"
    for ((cat, regex) in CAT_KEYWORDS) {
        if (regex.containsMatchIn(name)) return cat
    }
    return "drink"
}

fun slotFromTime(time: String?): String {
    if (time.isNullOrBlank()) return "Morning"
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val totalMins = (h * 60 + m + 1440) % 1440
    return when {
        totalMins in 240 until 480 -> "Early Morning"
        totalMins in 480 until 690 -> "Morning"
        totalMins in 690 until 840 -> "Midday"
        totalMins in 840 until 1050 -> "Afternoon"
        totalMins in 1050 until 1260 -> "Evening"
        else -> "Late Night"
    }
}

fun currentSlot(time: LocalTime = LocalTime.now()): String {
    val totalMins = time.hour * 60 + time.minute
    return when {
        totalMins in 240 until 480 -> "Early Morning"
        totalMins in 480 until 690 -> "Morning"
        totalMins in 690 until 840 -> "Midday"
        totalMins in 840 until 1050 -> "Afternoon"
        totalMins in 1050 until 1260 -> "Evening"
        else -> "Late Night"
    }
}

data class HabitDisplayModel(
    val habit: Habit,
    val status: HabitCalculatedStatus,
    val schedule: ScheduleVersion?,
    val recent7Days: List<DayStatus>,
    val colorHex: String
)

data class DayStatus(
    val date: LocalDate,
    val isDue: Boolean,
    val isCompleted: Boolean,
    val isForgiven: Boolean
)

data class MilestoneSpecInfo(
    val key: String,
    val title: String,
    val icon: String,
    val message: String,
    val current: Float,
    val target: Float,
    val nextKey: String?,
    val nextTitle: String?,
    val nextTarget: Float?
)

data class HabitsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val selectedTab: HabitsTab = HabitsTab.HABITS,
    val isRailExpanded: Boolean = false,
    val slotFilter: String = "All",
    val isAddEditSheetOpen: Boolean = false,
    val editingHabit: Habit? = null,
    val habits: List<HabitDisplayModel> = emptyList(),
    val availableBooks: List<tn.keeftalk.chat.domain.model.Book> = emptyList(),
    val readingProgressMap: Map<String, tn.keeftalk.chat.domain.engine.HabitReadingProgressSummary> = emptyMap(),
    val gracePoolRemaining: Int = 2,
    val isPreviewMode: Boolean = false,
    val statsRange: Int = 30, // 7, 30, 84, 180
    val introShown: Boolean = true,
    val lastCelebratedEpochDay: Long = -1L,
    val unlockedMilestones: Set<String> = emptySet(),
    val currentMilestonePopup: MilestoneSpecInfo? = null
)

val HABIT_COLORS = listOf("#00CCCC", "#19B6D6", "#329FE0", "#4B89EB", "#6472F5", "#7D5CFF", "#FF4081", "#2ECC71", "#E07B00", "#E11D48", "#9C27B0", "#009688")

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = tn.keeftalk.chat.di.AppModule.provideDatabase(application)
    private val nativeDao: NativeHabitDao = database.nativeHabitDao()
    private val settingsRepository = tn.keeftalk.chat.di.AppModule.provideSettingsRepository(application)
    private val repository: HabitsRepository = HabitsRepositoryImpl(
        context = application,
        nativeHabitDao = nativeDao,
        remoteDataSource = HabitsRemoteDataSourceNoOp()
    )
    private val bookRepository: tn.keeftalk.chat.domain.repository.BookRepository = tn.keeftalk.chat.di.AppModule.provideBookRepository(application)
    private val integrationManager: tn.keeftalk.chat.domain.engine.BookHabitIntegrationManager = tn.keeftalk.chat.di.AppModule.provideBookHabitIntegrationManager(application)
    private val engine = HabitEngine()

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultDataIfEmpty()
        }
        observeData()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val habitSettings = settings.habitSettings
                _uiState.update {
                    it.copy(
                        introShown = habitSettings.introShown,
                        lastCelebratedEpochDay = habitSettings.lastCelebratedEpochDay
                    )
                }
            }
        }
    }

    fun markIntroShown() {
        viewModelScope.launch {
            settingsRepository.updateHabitSettings { it.copy(introShown = true) }
        }
    }

    fun markCelebrationShown(epochDay: Long) {
        viewModelScope.launch {
            settingsRepository.updateHabitSettings { it.copy(lastCelebratedEpochDay = epochDay) }
        }
    }

    fun dismissMilestonePopup() {
        _uiState.update { it.copy(currentMilestonePopup = null) }
    }

    private fun observeData() {
        viewModelScope.launch {
            var emissionCount = 0
            combine(
                repository.observeHabits(),
                repository.observeSchedules(),
                repository.observeCheckIns(),
                repository.observeGraceUses(),
                repository.observeMilestones()
            ) { habits, schedules, checkIns, graceUses, milestones ->
                Triple(habits, schedules, checkIns) to Triple(graceUses, milestones, Unit)
            }.combine(bookRepository.getAllBooksFlow()) { (habitsTriple, graceTriple), books ->
                emissionCount++
                val isFirstEmission = emissionCount == 1

                val (habits, schedules, checkIns) = habitsTriple
                val (graceUses, milestones, _) = graceTriple

                val today = engine.today()
                val currentState = _uiState.value
                val selectedDate = currentState.selectedDate
                val isPreview = selectedDate != today

                val monthKey = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val usesThisMonth = graceUses.count { it.usedMonthKey == monthKey }
                val graceRemaining = maxOf(0, 2 - usesThisMonth)

                val habitDisplays = habits.filter { !it.deleted }.map { habit ->
                    val habitSchedule = schedules.filter { it.habitId == habit.id }
                    val status = engine.computeHabitStatus(habit, habitSchedule, checkIns, graceUses)

                    val recentDays = (6 downTo 0).map { offset ->
                        val d = selectedDate.minusDays(offset.toLong())
                        val due = engine.isDue(habit, habitSchedule, d)
                        val completed = checkIns.any { it.habitId == habit.id && it.date == d && it.completed }
                        val forgiven = graceUses.any { it.habitId == habit.id && it.forgivenDate == d }
                        DayStatus(d, due, completed, forgiven)
                    }

                    HabitDisplayModel(
                        habit = habit,
                        status = status,
                        schedule = habitSchedule.maxByOrNull { it.effectiveFrom },
                        recent7Days = recentDays,
                        colorHex = habit.colorHex
                    )
                }

                val progressMap = mutableMapOf<String, tn.keeftalk.chat.domain.engine.HabitReadingProgressSummary>()
                val readingHabits = habits.filter { !it.deleted && (it.isReadingHabit || integrationManager.isHabitReadingRelated(it)) }
                for (h in readingHabits) {
                    val summary = integrationManager.getProgressForHabitOnDate(h, selectedDate)
                    progressMap[h.id] = summary
                }

                val unlockedSet = milestones.map { it.key }.toSet()
                val totDone = checkIns.count { it.completed }
                val bestStreak = habitDisplays.maxOfOrNull { it.status.streak } ?: 0

                // Check milestones only after first database emission to avoid initial empty emission race condition
                var newlyUnlocked: MilestoneSpecInfo? = currentState.currentMilestonePopup
                if (!isFirstEmission && newlyUnlocked == null) {
                    val milestoneList = listOf(
                        Triple("first", "First check-in", totDone.toFloat() to 1f),
                        Triple("3s", "3-day streak", bestStreak.toFloat() to 3f),
                        Triple("7s", "7-day streak", bestStreak.toFloat() to 7f),
                        Triple("14s", "14-day streak", bestStreak.toFloat() to 14f),
                        Triple("21s", "21-day streak", bestStreak.toFloat() to 21f),
                        Triple("30s", "30-day streak", bestStreak.toFloat() to 30f),
                        Triple("45s", "45-day streak", bestStreak.toFloat() to 45f),
                        Triple("60s", "60-day streak", bestStreak.toFloat() to 60f),
                        Triple("90s", "90-day streak", bestStreak.toFloat() to 90f),
                        Triple("100s", "100-day streak", bestStreak.toFloat() to 100f)
                    )

                    for (i in milestoneList.indices) {
                        val (key, title, pair) = milestoneList[i]
                        val (cur, target) = pair
                        if (cur >= target && !unlockedSet.contains(key)) {
                            // Unlock milestone in repo
                            repository.unlockMilestone(key, today)
                            val next = milestoneList.getOrNull(i + 1)
                            newlyUnlocked = MilestoneSpecInfo(
                                key = key,
                                title = title,
                                icon = if (key.endsWith("s")) "flame" else "spark",
                                message = "You reached $title. Consistency compounds into mastery.",
                                current = cur,
                                target = target,
                                nextKey = next?.first,
                                nextTitle = next?.second,
                                nextTarget = next?.third?.second
                            )
                            break
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        today = today,
                        habits = habitDisplays,
                        availableBooks = books,
                        readingProgressMap = progressMap,
                        gracePoolRemaining = graceRemaining,
                        isPreviewMode = isPreview,
                        unlockedMilestones = unlockedSet,
                        currentMilestonePopup = newlyUnlocked
                    )
                }
            }.collect()
        }
    }

    fun selectDate(date: LocalDate) {
        val today = engine.today()
        _uiState.update { it.copy(selectedDate = date, isPreviewMode = date != today) }
    }

    fun selectTab(tab: HabitsTab) {
        _uiState.update { it.copy(selectedTab = tab, isRailExpanded = false) }
    }

    fun toggleRailExpanded() {
        _uiState.update { it.copy(isRailExpanded = !it.isRailExpanded) }
    }

    fun setRailExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isRailExpanded = expanded) }
    }

    fun triggerInitialRailAutoExpand() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRailExpanded = true) }
            kotlinx.coroutines.delay(2200)
            _uiState.update { it.copy(isRailExpanded = false) }
        }
    }

    fun setSlotFilter(slot: String) {
        _uiState.update { it.copy(slotFilter = slot) }
    }

    fun setStatsRange(range: Int) {
        _uiState.update { it.copy(statsRange = range) }
    }

    fun openAddSheet() {
        _uiState.update { it.copy(isAddEditSheetOpen = true, editingHabit = null) }
    }

    fun openEditSheet(habit: Habit) {
        _uiState.update { it.copy(isAddEditSheetOpen = true, editingHabit = habit) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(isAddEditSheetOpen = false, editingHabit = null) }
    }

    fun toggleCheckIn(habitId: String, completed: Boolean) {
        if (completed) {
            SoundEffectManager.playMediaSound(getApplication<Application>(), SoundEffectManager.RES_GOAL_DONE)
        }
        viewModelScope.launch {
            val today = engine.today()
            repository.setCheckIn(habitId, today, completed)
        }
    }

    fun useGrace(habitId: String, targetDate: LocalDate) {
        viewModelScope.launch {
            val today = engine.today()
            repository.useGrace(habitId, targetDate, today)
        }
    }

    fun saveHabit(
        id: String?,
        name: String,
        icon: String,
        category: String,
        baseDays: Int,
        slot: String,
        reminderTime: LocalTime?,
        daysMask: Set<DayOfWeek>,
        habitType: String,
        amount: Int? = null,
        unit: String? = null,
        colorHex: String = "#00CCCC",
        isReadingHabit: Boolean = false,
        autoCountReading: Boolean = true,
        linkedBookIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val habitId = id ?: java.util.UUID.randomUUID().toString()
            val today = engine.today()
            val habit = Habit(
                id = habitId,
                userId = "local_user",
                name = name,
                icon = icon,
                category = category,
                baseDays = baseDays,
                createdDate = today,
                reminderTime = reminderTime,
                slot = slot,
                habitType = habitType,
                amount = amount,
                unit = unit,
                colorHex = colorHex,
                isReadingHabit = isReadingHabit,
                autoCountReading = autoCountReading,
                linkedBookIds = linkedBookIds,
                updatedAt = System.currentTimeMillis()
            )
            val schedule = ScheduleVersion(
                habitId = habitId,
                effectiveFrom = today,
                daysMask = daysMask
            )
            if (id == null) {
                repository.saveHabit(habit, schedule)
            } else {
                repository.updateHabit(habit, schedule)
            }
            closeSheet()
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun validateCheckIn(habit: Habit, date: LocalDate): Boolean {
        return engine.isSlotValidForCheckIn(habit, date)
    }

    fun getSlotWindowLabel(slot: String): String {
        return engine.getSlotWindowLabel(slot)
    }

    fun getMissedDayCostMessage(category: String): String {
        return engine.getMissedDayCostMessage(category)
    }
}
