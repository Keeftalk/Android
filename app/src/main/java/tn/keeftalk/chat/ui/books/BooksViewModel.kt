package tn.keeftalk.chat.ui.books

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.BookRepository
import tn.keeftalk.chat.util.KeeftalkStore
import tn.keeftalk.chat.util.SoundEffectManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

data class GlobalRankState(
    val rank: Long = 0L,
    val totalReaders: Long = 0L,
    val percentile: Int = 0,
    val isLoading: Boolean = false,
    val isRealBackend: Boolean = false
)

data class BooksUiState(
    val currentTab: String = "lib", // "lib", "quo", "gol", "sta", "awd"
    val isRailExpanded: Boolean = false,
    val hasRailAutoExpanded: Boolean = false,
    val viewMode: String = "list", // "list", "g3", "g2"
    val libraryFilter: String = "all", // "all", "rd", "fin", "fav", "arc"
    val quotesTab: String = "q", // "q", "n", "h", "b"
    val selectedQuotesBookFilterId: String? = null,
    val activeBookForSheet: Book? = null,
    val activeBookForReader: Book? = null,
    val activeBookForDetailStats: Book? = null,
    val activeBookForShare: Book? = null,
    val activeBookSessions: List<tn.keeftalk.chat.data.local.entities.ReadingSessionEntity> = emptyList(),
    val activeBookXp: Long = 0L,
    val isChangeCoverSheetOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val lastDeletedBook: Book? = null,
    val toastMessage: String? = null,
    val isImportingBook: Boolean = false,
    val importingBookTitle: String? = null,

    // Goals state
    val focusedPeriod: String = "day", // "day", "month", "year"
    val dailyMinutesGoal: Int = 30,
    val monthlyHoursGoal: Int = 15,
    val yearlyBooksGoal: Int = 25,
    val readingLog: Map<String, Int> = emptyMap(), // yyyy-MM-dd -> minutes
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "08:13",
    val lastOpenedBookId: String? = null,
    val notifiedDailyDate: String? = null,
    val notifiedMonth: String? = null,
    val notifiedYear: String? = null,
    val pendingCelebration: Boolean = false,
    val activeBannerMessage: String? = null,
    val showCelebrationDialog: Boolean = false,
    val showReadNowSheet: Boolean = false,
    val celebratedAchievements: Set<String> = emptySet(),
    val pendingSessionSummary: PendingSessionSummaryData? = null
)

data class PendingSessionSummaryData(
    val session: tn.keeftalk.chat.data.local.entities.ReadingSessionEntity,
    val book: Book?,
    val records: tn.keeftalk.chat.domain.books.SessionRecords,
    val bookEta: tn.keeftalk.chat.domain.books.BookEtaResult,
    val paceMetrics: tn.keeftalk.chat.domain.books.PaceMetrics,
    val todayXp: Long,
    val dailyGoalMins: Int,
    val todayActiveMins: Int,
    val streakDays: Int,
    val levelProgress: tn.keeftalk.chat.ui.books.components.LevelingSystem.ProgressData,
    val unlockedThisSession: List<tn.keeftalk.chat.domain.model.Achievement>,
    val closestAchievements: List<tn.keeftalk.chat.domain.model.Achievement>,
    val recent7Sessions: List<tn.keeftalk.chat.data.local.entities.ReadingSessionEntity>,
    val todayDayXp: Long = 0L,
    val bestDayXp: Long = 0L,
    val bookSessionCount: Int = 0
)

class BooksViewModel(
    private val bookRepository: BookRepository,
    private val readingXpRepository: tn.keeftalk.chat.data.repository.ReadingXpRepository,
    context: Context? = null
) : ViewModel() {

    private val appContext = context?.applicationContext

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val store by lazy { KeeftalkStore.getInstance() }

    private val _uiState = MutableStateFlow(BooksUiState())
    val uiState: StateFlow<BooksUiState> = _uiState.asStateFlow()

    val booksFlow: StateFlow<List<Book>> = bookRepository.getAllBooksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlightsFlow: StateFlow<List<BookHighlight>> = bookRepository.getAllHighlightsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksFlow: StateFlow<List<BookBookmark>> = bookRepository.getAllBookmarksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookSettingsFlow: StateFlow<BookSettings> = bookRepository.getBookSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookSettings())

    val readingDaysFlow: StateFlow<List<tn.keeftalk.chat.data.local.entities.ReadingDayEntity>> =
        readingXpRepository.getReadingDaysFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedAchievementsFlow: StateFlow<List<tn.keeftalk.chat.data.local.entities.UnlockedAchievementEntity>> =
        readingXpRepository.getUnlockedAchievementsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val xpMetaFlow: StateFlow<tn.keeftalk.chat.data.local.entities.XpMetaEntity?> =
        readingXpRepository.getXpMetaFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _globalRankState = MutableStateFlow(GlobalRankState())
    val globalRankState: StateFlow<GlobalRankState> = _globalRankState.asStateFlow()

    fun refreshGlobalRank(context: Context, totalXp: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _globalRankState.update { it.copy(isLoading = true) }
            val rankDto = readingXpRepository.syncAndFetchGlobalRank(context, totalXp)
            if (rankDto != null) {
                _globalRankState.value = GlobalRankState(
                    rank = rankDto.globalRank,
                    totalReaders = rankDto.totalReaders,
                    percentile = rankDto.percentile,
                    isLoading = false,
                    isRealBackend = true
                )
            } else {
                val meta = readingXpRepository.getXpMeta()
                if (meta != null && meta.lastGlobalRank > 0) {
                    _globalRankState.value = GlobalRankState(
                        rank = meta.lastGlobalRank,
                        totalReaders = meta.lastTotalReaders,
                        percentile = meta.lastPercentile,
                        isLoading = false,
                        isRealBackend = true
                    )
                } else {
                    _globalRankState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    val readingSessionsFlow: StateFlow<List<tn.keeftalk.chat.data.local.entities.ReadingSessionEntity>> =
        readingXpRepository.getAllReadingSessionsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBookPageProgressFlow: StateFlow<List<BookPageProgress>> = _uiState
        .map { it.activeBookForReader?.id }
        .distinctUntilChanged()
        .flatMapLatest { bookId ->
            if (bookId != null) {
                bookRepository.getPageProgressForBookFlow(bookId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadGoalsFromStore()
        syncBooksReadProgress()
        backfillContentHashes()
        loadPendingSessionSummary()
        viewModelScope.launch {
            bookRepository.syncBooksWithCloud()
        }
    }

    private fun backfillContentHashes() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val allBooks = bookRepository.getAllBooks()
            for (book in allBooks) {
                if (book.contentHash.isNullOrEmpty() && book.filePath.isNotEmpty()) {
                    val file = java.io.File(book.filePath)
                    if (file.exists()) {
                        val hash = tn.keeftalk.chat.util.ContentHashUtils.computeSha256(file)
                        if (!hash.isNullOrEmpty()) {
                            bookRepository.saveBook(book.copy(contentHash = hash))
                        }
                    } else {
                        val fallbackHash = "fallback_${book.id}_${book.title.hashCode()}"
                        bookRepository.saveBook(book.copy(contentHash = fallbackHash))
                    }
                } else if (book.contentHash.isNullOrEmpty()) {
                    val fallbackHash = "fallback_${book.id}_${book.title.hashCode()}"
                    bookRepository.saveBook(book.copy(contentHash = fallbackHash))
                }
            }
        }
    }

    fun confirmPageRead(
        context: android.content.Context,
        book: Book,
        pageNumber: Int,
        dwellSec: Double,
        tapX: Float = 0f,
        tapY: Float = 0f,
        isObscured: Boolean = false
    ) {
        viewModelScope.launch {
            val contentHash = book.contentHash ?: "hash_${book.id}_${book.title.hashCode()}"
            val isPdf = book.format.equals("PDF", ignoreCase = true)
            val result = if (isPdf) {
                readingXpRepository.confirmPdfPage(
                    context = context,
                    contentHash = contentHash,
                    pageNumber = pageNumber,
                    dwellSec = dwellSec,
                    tapX = tapX,
                    tapY = tapY,
                    isObscured = isObscured
                )
            } else {
                // TODO(xp-v2): Using estimated character offset [ (page - 1) * 1800, page * 1800 ] for reflowable EPUB page coverage
                val charsPerPage = 1800L
                val startChar = (pageNumber - 1) * charsPerPage
                val endChar = pageNumber * charsPerPage
                readingXpRepository.confirmEpubRange(
                    context = context,
                    contentHash = contentHash,
                    startChar = startChar,
                    endChar = endChar,
                    dwellSec = dwellSec,
                    tapX = tapX,
                    tapY = tapY,
                    isObscured = isObscured
                )
            }

            val currentPageProgress = activeBookPageProgressFlow.value.firstOrNull { it.pageNumber == pageNumber }
            val newProgress = (currentPageProgress ?: BookPageProgress(book.id, pageNumber))
                .copy(isRead = true, updatedAt = System.currentTimeMillis())
            savePageProgressEntity(newProgress)

            if (result.message != null) {
                _uiState.update { it.copy(toastMessage = result.message) }
            }

            evaluateAchievements()
        }
    }

    fun evaluateAchievements() {
        viewModelScope.launch {
            val books = booksFlow.value
            val highlightsCount = highlightsFlow.value.size
            val bookmarksCount = bookmarksFlow.value.size
            val readingDays = readingDaysFlow.value
            val unlockedEntities = unlockedAchievementsFlow.value
            val unlockedSet = unlockedEntities.map { it.id }.toSet()

            val achList = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateAchievementsList(
                books = books,
                highlightsCount = highlightsCount,
                bookmarksCount = bookmarksCount,
                readingDays = readingDays,
                unlockedIds = unlockedSet
            )

            for (ach in achList) {
                if (ach.isUnlocked && ach.icon !in unlockedSet) {
                    readingXpRepository.insertUnlockedAchievement(ach.icon)
                }
            }
        }
    }

    private fun syncBooksReadProgress() {
        viewModelScope.launch {
            val allBooks = bookRepository.getAllBooks()
            for (book in allBooks) {
                if (!book.isDeleted && !book.markedDone) {
                    val pageProgresses = bookRepository.getPageProgressForBook(book.id)
                    val readPagesCount = pageProgresses.count { it.isRead }
                    val percent = if (book.totalPages > 0) {
                        ((readPagesCount.toFloat() / book.totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
                    } else 0
                    if (percent != book.percentProgress) {
                        val isNowDone = percent >= 100
                        val finishedAtVal = if (isNowDone && book.finishedAt == null) System.currentTimeMillis() else book.finishedAt
                        val updated = book.copy(
                            percentProgress = percent,
                            markedDone = if (isNowDone) true else book.markedDone,
                            finishedAt = finishedAtVal,
                            updatedAt = System.currentTimeMillis()
                        )
                        bookRepository.saveBook(updated)
                    }
                }
            }
        }
    }

    private fun loadGoalsFromStore() {
        val storedDaily = store.getInt("goals_daily_minutes", 30)
        val daily = max(30, storedDaily)
        val monthly = store.getInt("goals_monthly_hours", 15)
        val yearly = store.getInt("goals_yearly_books", 25)
        val period = store.getString("goals_focused_period", "day") ?: "day"
        val reminderEn = store.getBoolean("goals_reminder_enabled", true)
        val reminderTm = store.getString("goals_reminder_time", "08:13") ?: "08:13"
        val lastBookId = store.getString("goals_last_opened_book_id", null)
        val notifDaily = store.getString("goals_notified_daily", null)
        val notifMonth = store.getString("goals_notified_month", null)
        val notifYear = store.getString("goals_notified_year", null)
        val pendingCel = store.getBoolean("goals_pending_celebration", false)

        val logJson = store.getString("goals_reading_log_json", null)
        val logMap = if (!logJson.isNullOrEmpty()) {
            try { json.decodeFromString<Map<String, Int>>(logJson) } catch (e: Exception) { emptyMap() }
        } else {
            emptyMap()
        }

        val celebratedJson = store.getString("goals_celebrated_achievements_json", null)
        val celebratedSet = if (!celebratedJson.isNullOrEmpty()) {
            try { json.decodeFromString<Set<String>>(celebratedJson) } catch (e: Exception) { emptySet() }
        } else {
            emptySet()
        }

        _uiState.update {
            it.copy(
                dailyMinutesGoal = daily,
                monthlyHoursGoal = monthly,
                yearlyBooksGoal = yearly,
                focusedPeriod = period,
                reminderEnabled = reminderEn,
                reminderTime = reminderTm,
                lastOpenedBookId = lastBookId,
                notifiedDailyDate = notifDaily,
                notifiedMonth = notifMonth,
                notifiedYear = notifYear,
                pendingCelebration = pendingCel,
                readingLog = logMap,
                celebratedAchievements = celebratedSet
            )
        }
        syncReminderSchedule()
    }

    private fun saveGoalsToStore() {
        val state = _uiState.value
        store.putInt("goals_daily_minutes", state.dailyMinutesGoal)
        store.putInt("goals_monthly_hours", state.monthlyHoursGoal)
        store.putInt("goals_yearly_books", state.yearlyBooksGoal)
        store.putString("goals_focused_period", state.focusedPeriod)
        store.putBoolean("goals_reminder_enabled", state.reminderEnabled)
        store.putString("goals_reminder_time", state.reminderTime)
        store.putString("goals_last_opened_book_id", state.lastOpenedBookId)
        store.putString("goals_notified_daily", state.notifiedDailyDate)
        store.putString("goals_notified_month", state.notifiedMonth)
        store.putString("goals_notified_year", state.notifiedYear)
        store.putBoolean("goals_pending_celebration", state.pendingCelebration)
        store.putString("goals_reading_log_json", json.encodeToString(state.readingLog))
        store.putString("goals_celebrated_achievements_json", json.encodeToString(state.celebratedAchievements))
    }

    fun markAchievementCelebrated(title: String) {
        _uiState.update { state ->
            if (title in state.celebratedAchievements) state
            else state.copy(celebratedAchievements = state.celebratedAchievements + title)
        }
        saveGoalsToStore()
    }

    // Goals Actions
    fun selectFocusedPeriod(period: String) {
        _uiState.update { it.copy(focusedPeriod = period) }
        saveGoalsToStore()
    }

    fun incrementGoal() {
        _uiState.update { state ->
            when (state.focusedPeriod) {
                "month" -> state.copy(monthlyHoursGoal = state.monthlyHoursGoal + 1)
                "year" -> state.copy(yearlyBooksGoal = state.yearlyBooksGoal + 1)
                else -> state.copy(dailyMinutesGoal = state.dailyMinutesGoal + 5)
            }
        }
        saveGoalsToStore()
        syncReminderSchedule()
    }

    fun decrementGoal() {
        _uiState.update { state ->
            when (state.focusedPeriod) {
                "month" -> {
                    if (state.monthlyHoursGoal > 1) state.copy(monthlyHoursGoal = state.monthlyHoursGoal - 1)
                    else state
                }
                "year" -> {
                    if (state.yearlyBooksGoal > 1) state.copy(yearlyBooksGoal = state.yearlyBooksGoal - 1)
                    else state
                }
                else -> {
                    if (state.dailyMinutesGoal > 30) {
                        state.copy(dailyMinutesGoal = state.dailyMinutesGoal - 5)
                    } else {
                        state.copy(toastMessage = "Minimum daily goal is 30 min")
                    }
                }
            }
        }
        saveGoalsToStore()
        syncReminderSchedule()
    }

    fun syncReminderSchedule(context: Context? = appContext) {
        val ctx = context?.applicationContext ?: appContext ?: return
        val state = _uiState.value
        if (state.reminderEnabled) {
            val parts = state.reminderTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            tn.keeftalk.chat.util.reminders.BookReminderScheduler.scheduleDailyReadingGoalReminder(
                context = ctx,
                bookId = "daily_reading_goal",
                bookTitle = "Daily Reading Goal",
                targetAmount = state.dailyMinutesGoal,
                targetUnit = "min",
                hour = hour,
                minute = minute
            )
        } else {
            tn.keeftalk.chat.util.reminders.BookReminderScheduler.cancelDailyReadingGoalReminder(ctx, "daily_reading_goal")
        }
    }

    fun toggleReminder(enabled: Boolean, context: Context? = null) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
        saveGoalsToStore()
        syncReminderSchedule(context)
    }

    fun updateReminderTime(time: String, context: Context? = null) {
        _uiState.update { it.copy(reminderTime = time) }
        saveGoalsToStore()
        syncReminderSchedule(context)
    }

    fun openReadNowSheet() {
        _uiState.update { it.copy(showReadNowSheet = true) }
    }

    fun closeReadNowSheet() {
        _uiState.update { it.copy(showReadNowSheet = false) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(activeBannerMessage = null) }
    }

    fun openCelebrationDialog() {
        _uiState.update { it.copy(showCelebrationDialog = true, pendingCelebration = false) }
        saveGoalsToStore()
    }

    fun closeCelebrationDialog() {
        _uiState.update { it.copy(showCelebrationDialog = false) }
    }

    // Reading Timer Job & Session Tracking
    private var readingTimerJob: Job? = null
    private var currentReaderStartTime: Long = 0L
    private var sessionStartAtMs: Long = 0L
    private var preSessionTotalXp: Long = 0L
    private var preSessionUnlockedSet = setOf<String>()
    private var preSessionConfirmedPagesCount = 0
    private var preSessionBaseXp: Long = 0L
    private var preSessionBonusXp: Long = 0L
    private var preSessionActiveSecTotal: Long = 0L
    private var preSessionTodayFlagged: Boolean = false
    private var baselineJob: Job? = null
    private var sessionFlagged: Boolean = false
    private var backgroundJob: Job? = null
    private val sessionMarkedPages = mutableSetOf<Int>()

    fun openReader(book: Book) {
        currentReaderStartTime = System.currentTimeMillis()
        sessionStartAtMs = currentReaderStartTime
        sessionMarkedPages.clear()
        sessionFlagged = false

        // Baseline snapshot (taken from the repository, not from WhileSubscribed flows that may be empty).
        // finishSessionAndPrepareSummary() joins this job before computing any "gain".
        baselineJob?.cancel()
        baselineJob = viewModelScope.launch {
            val unlockedIds = readingXpRepository.getUnlockedAchievements().map { it.id }.toSet()
            preSessionUnlockedSet = unlockedIds
            val legacyXp = readingXpRepository.getXpMeta()?.legacyReadingXp ?: 0L
            val days = readingXpRepository.getAllReadingDays()
            val settled = tn.keeftalk.chat.domain.xp.ReadingXp.settle(toDayRecordMap(days))
            val achs = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateAchievementsList(
                books = bookRepository.getAllBooks(),
                highlightsCount = highlightsFlow.value.size,
                bookmarksCount = bookmarksFlow.value.size,
                readingDays = days,
                unlockedIds = unlockedIds
            )
            preSessionTotalXp = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateTotalXp(legacyXp, settled, achs)
            preSessionBaseXp = settled.baseXp
            preSessionBonusXp = settled.bonusXp
            preSessionActiveSecTotal = days.sumOf { it.activeSeconds.toLong() }
            val todayStr = LocalDate.now().toString()
            preSessionTodayFlagged = days.any { it.date == todayStr && it.flagged == 1 }

            val contentHash = book.contentHash ?: "hash_${book.id}_${book.title.hashCode()}"
            preSessionConfirmedPagesCount = readingXpRepository.getConfirmedPagesForBook(contentHash).size
        }

        _uiState.update {
            it.copy(
                activeBookForReader = book,
                activeBookForSheet = null,
                lastOpenedBookId = book.id
            )
        }
        store.putString("goals_last_opened_book_id", book.id)
        startReadingTimer()
    }

    fun onAppBackgrounded() {
        if (_uiState.value.activeBookForReader != null) {
            backgroundJob?.cancel()
            backgroundJob = viewModelScope.launch {
                delay(120_000L) // 2 minutes background timeout
                val currentBook = _uiState.value.activeBookForReader
                if (currentBook != null) {
                    closeReader()
                }
            }
        }
    }

    fun onAppForegrounded() {
        backgroundJob?.cancel()
        backgroundJob = null
    }

    private fun toDayRecordMap(days: List<tn.keeftalk.chat.data.local.entities.ReadingDayEntity>) =
        days.associate {
            val date = try { LocalDate.parse(it.date) } catch (_: Exception) { LocalDate.now() }
            date to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1)
        }

    fun closeReader(dwellSec: Double = 0.0) {
        stopReadingTimer()
        backgroundJob?.cancel()
        val currentBook = _uiState.value.activeBookForReader
        val endTime = System.currentTimeMillis()

        if (currentBook != null && sessionStartAtMs > 0L) {
            val durationSecs = (endTime - sessionStartAtMs) / 1000
            val pagesRead = sessionMarkedPages.size
            if (durationSecs >= 5 || pagesRead > 0) {
                val session = BookReadingSession(
                    id = "s_${currentBook.id}_${sessionStartAtMs}",
                    bookId = currentBook.id,
                    startTime = sessionStartAtMs,
                    endTime = endTime,
                    pagesRead = pagesRead,
                    durationSeconds = durationSecs
                )
                viewModelScope.launch {
                    bookRepository.recordReadingSession(session)
                }
            }

            // Capture now: the lines below reset these fields before the coroutine resumes.
            val capturedStartAt = sessionStartAtMs
            val capturedMarkedPages = sessionMarkedPages.size
            _uiState.update { it.copy(activeBookForReader = null) } // reader closes instantly, no wait on DB work
            finishSessionAndPrepareSummary(
                openedBook = currentBook,
                startAtMs = capturedStartAt,
                markedPagesCount = capturedMarkedPages,
                endTime = endTime,
                activeDwellSec = dwellSec
            )
        } else {
            _uiState.update { it.copy(activeBookForReader = null) }
        }

        sessionMarkedPages.clear()
        currentReaderStartTime = 0L
        sessionStartAtMs = 0L
        saveGoalsToStore()
    }

    private fun finishSessionAndPrepareSummary(
        openedBook: Book,
        startAtMs: Long,
        markedPagesCount: Int,
        endTime: Long,
        activeDwellSec: Double
    ) {
        viewModelScope.launch {
            baselineJob?.join()
            // The Book captured when the reader opened is stale (progress / finishedAt). Re-read it.
            val currentBook = bookRepository.getBookById(openedBook.id) ?: openedBook
            val contentHash = currentBook.contentHash ?: "hash_${currentBook.id}_${currentBook.title.hashCode()}"
            val confirmedPagesNow = readingXpRepository.getConfirmedPagesForBook(contentHash).size
            val pagesConfirmed = maxOf(markedPagesCount, confirmedPagesNow - preSessionConfirmedPagesCount)

            val unlockedEntities = readingXpRepository.getUnlockedAchievements()
            val postUnlockedSet = unlockedEntities.map { it.id }.toSet()

            val meta = readingXpRepository.getXpMeta()
            val legacyXp = meta?.legacyReadingXp ?: 0L
            val readingDays = readingXpRepository.getAllReadingDays()
            val daysMap = readingDays.associate {
                try { LocalDate.parse(it.date) to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1) }
                catch (_: Exception) { LocalDate.now() to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1) }
            }
            val postSettled = tn.keeftalk.chat.domain.xp.ReadingXp.settle(daysMap)

            val postAchs = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateAchievementsList(
                books = bookRepository.getAllBooks(), // same source as the pre-session baseline
                highlightsCount = highlightsFlow.value.size,
                bookmarksCount = bookmarksFlow.value.size,
                readingDays = readingDays,
                unlockedIds = postUnlockedSet
            )
            val postSessionTotalXp = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateTotalXp(legacyXp, postSettled, postAchs)

            val totalXpGained = maxOf(0L, postSessionTotalXp - preSessionTotalXp)

            // base / consistency bonus come from the PRE-session snapshot; achievements are the remainder
            val baseGain = maxOf(0L, postSettled.baseXp - preSessionBaseXp).coerceAtMost(totalXpGained)
            val bonusGain = maxOf(0L, postSettled.bonusXp - preSessionBonusXp).coerceAtMost(totalXpGained - baseGain)
            val achGain = maxOf(0L, totalXpGained - baseGain - bonusGain)

            // ACTIVE time = growth of the same per-day activeSeconds the goals/XP code uses (not wall clock)
            val activeDelta = (readingDays.sumOf { it.activeSeconds.toLong() } - preSessionActiveSecTotal).toInt()
            val wallSec = ((endTime - startAtMs) / 1000L).toInt().coerceAtLeast(1)
            val activeSecFinal = maxOf(activeDelta, activeDwellSec.toInt()).coerceIn(1, wallSec)

            val todayStrFlag = LocalDate.now().toString()
            val sessionFlaggedFinal = sessionFlagged ||
                    (readingDays.any { it.date == todayStrFlag && it.flagged == 1 } && !preSessionTodayFlagged)

            val pastSessions = readingXpRepository.getAllReadingSessions()
            val pastDaysXpMap = readingDays.associate { it.date to (it.creditedPages * 50.0).toLong() }

            val sessionId = "session_${currentBook.id}_${startAtMs}"
            val sessionEntity = tn.keeftalk.chat.data.local.entities.ReadingSessionEntity(
                id = sessionId,
                contentHash = contentHash,
                bookId = currentBook.id,
                bookTitle = currentBook.title,
                bookAuthor = currentBook.author,
                coverUrl = currentBook.coverUrl,
                customCoverJson = currentBook.customCover?.let { json.encodeToString(it) },
                startAt = startAtMs,
                endAt = endTime,
                activeSec = activeSecFinal,
                pagesConfirmed = pagesConfirmed,
                creditedPages = pagesConfirmed.toDouble(),
                xpBase = baseGain,
                xpBonus = bonusGain,
                xpAchievements = achGain,
                totalXpGained = totalXpGained,
                flagged = if (sessionFlaggedFinal) 1 else 0,
                isRecord = 0,
                shown = 0
            )

            val records = tn.keeftalk.chat.domain.books.SessionSummaryCalculator.evaluateRecords(
                currentSession = sessionEntity,
                pastSessions = tn.keeftalk.chat.ui.books.components.sessionRecordBaseline(pastSessions, sessionId),
                todayTotalXp = postSessionTotalXp,
                pastDaysXpMap = pastDaysXpMap
            )

            val finalSessionEntity = sessionEntity.copy(isRecord = if (records.isBestSessionXp && !sessionFlaggedFinal) 1 else 0)
            readingXpRepository.insertReadingSession(finalSessionEntity)

            if (totalXpGained > 0) {
                val pastBookSessions = readingXpRepository.getSessionsForBook(contentHash)
                val pagesReadCount = (currentBook.totalPages * currentBook.percentProgress / 100)
                val remainingPages = maxOf(0, currentBook.totalPages - pagesReadCount)
                val dailyRate = currentBook.pagesPerDay.toFloat()
                val bookEta = tn.keeftalk.chat.domain.books.SessionSummaryCalculator.calculateBookEta(remainingPages, pastBookSessions, dailyRate)

                val prevSession = pastSessions.firstOrNull { it.contentHash == contentHash && it.id != sessionId }
                val paceMetrics = tn.keeftalk.chat.domain.books.SessionSummaryCalculator.calculatePaceMetrics(finalSessionEntity, prevSession)

                val levelProgress = tn.keeftalk.chat.ui.books.components.LevelingSystem.getProgress(postSessionTotalXp)

                val newlyUnlockedList = postAchs.filter { it.isUnlocked && it.icon in (postUnlockedSet - preSessionUnlockedSet) }
                val closestList = postAchs.filter { !it.isUnlocked }
                    .sortedByDescending { if (it.target > 0) it.current.toFloat() / it.target.toFloat() else 0f }
                    .take(2)

                val todayDateStr = LocalDate.now().toString()
                val todayDay = readingDays.firstOrNull { it.date == todayDateStr }
                val todayActiveMins = (todayDay?.activeSeconds ?: 0) / 60

                val streakDays = calculateStreak(uiState.value.readingLog, uiState.value.dailyMinutesGoal)

                val summaryData = PendingSessionSummaryData(
                    session = finalSessionEntity,
                    book = currentBook,
                    records = records,
                    bookEta = bookEta,
                    paceMetrics = paceMetrics,
                    todayXp = postSessionTotalXp,
                    dailyGoalMins = uiState.value.dailyMinutesGoal,
                    todayActiveMins = todayActiveMins,
                    streakDays = streakDays,
                    levelProgress = levelProgress,
                    unlockedThisSession = newlyUnlockedList,
                    closestAchievements = closestList,
                    // the session just inserted was not in `pastSessions` (read before insert) -> prepend it
                    recent7Sessions = (listOf(finalSessionEntity) + pastSessions.filter { it.id != sessionId }).take(7),
                    todayDayXp = pastDaysXpMap[todayDateStr] ?: 0L,
                    bestDayXp = pastDaysXpMap.values.maxOrNull() ?: 0L,
                    bookSessionCount = pastBookSessions.size
                )

                val state = _uiState.value
                val hasPending = state.pendingCelebration
                _uiState.update {
                    it.copy(
                        activeBookForReader = null,
                        pendingSessionSummary = summaryData,
                        showCelebrationDialog = if (hasPending) true else it.showCelebrationDialog,
                        pendingCelebration = if (hasPending) false else it.pendingCelebration
                    )
                }
            } else {
                readingXpRepository.markSessionShown(sessionId)
                val state = _uiState.value
                val hasPending = state.pendingCelebration
                _uiState.update {
                    it.copy(
                        activeBookForReader = null,
                        showCelebrationDialog = if (hasPending) true else it.showCelebrationDialog,
                        pendingCelebration = if (hasPending) false else it.pendingCelebration
                    )
                }
            }
        }
    }

    private fun loadPendingSessionSummary() {
        viewModelScope.launch {
            val pendingSession = readingXpRepository.getPendingUnshownSession()
            if (pendingSession != null && pendingSession.totalXpGained > 0) {
                val books = bookRepository.getAllBooks()
                val book = books.firstOrNull { it.id == pendingSession.bookId || it.contentHash == pendingSession.contentHash }
                val pastSessions = readingXpRepository.getAllReadingSessions()
                val readingDays = readingXpRepository.getAllReadingDays()
                val pastDaysXpMap = readingDays.associate { it.date to (it.creditedPages * 50.0).toLong() }

                val pastBookSessions = readingXpRepository.getSessionsForBook(pendingSession.contentHash)
                val pagesReadCount = book?.let { (it.totalPages * it.percentProgress / 100) } ?: 0
                val remainingPages = maxOf(0, (book?.totalPages ?: 100) - pagesReadCount)
                val dailyRate = (book?.pagesPerDay ?: 10).toFloat()
                val bookEta = tn.keeftalk.chat.domain.books.SessionSummaryCalculator.calculateBookEta(remainingPages, pastBookSessions, dailyRate)

                val prevSession = pastSessions.firstOrNull { it.contentHash == pendingSession.contentHash && it.id != pendingSession.id }
                val paceMetrics = tn.keeftalk.chat.domain.books.SessionSummaryCalculator.calculatePaceMetrics(pendingSession, prevSession)

                val meta = readingXpRepository.getXpMeta()
                val legacyXp = meta?.legacyReadingXp ?: 0L
                val daysMap = readingDays.associate {
                    try { LocalDate.parse(it.date) to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1) }
                    catch (_: Exception) { LocalDate.now() to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1) }
                }
                val settled = tn.keeftalk.chat.domain.xp.ReadingXp.settle(daysMap)
                val unlockedEntities = readingXpRepository.getUnlockedAchievements()
                val unlockedSet = unlockedEntities.map { it.id }.toSet()

                val achs = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateAchievementsList(
                    books = books,
                    highlightsCount = highlightsFlow.value.size,
                    bookmarksCount = bookmarksFlow.value.size,
                    readingDays = readingDays,
                    unlockedIds = unlockedSet
                )
                val totalXp = tn.keeftalk.chat.ui.books.components.XpCalculator.calculateTotalXp(legacyXp, settled, achs)
                val levelProgress = tn.keeftalk.chat.ui.books.components.LevelingSystem.getProgress(totalXp)

                // same inputs as the live path: non-flagged history only, current session excluded
                val records = tn.keeftalk.chat.domain.books.SessionSummaryCalculator.evaluateRecords(
                    currentSession = pendingSession,
                    pastSessions = tn.keeftalk.chat.ui.books.components.sessionRecordBaseline(pastSessions, pendingSession.id),
                    todayTotalXp = totalXp,
                    pastDaysXpMap = pastDaysXpMap
                )

                val todayDateStr = LocalDate.now().toString()
                val todayDay = readingDays.firstOrNull { it.date == todayDateStr }
                val todayActiveMins = (todayDay?.activeSeconds ?: 0) / 60

                val streakDays = calculateStreak(uiState.value.readingLog, uiState.value.dailyMinutesGoal)

                val summaryData = PendingSessionSummaryData(
                    session = pendingSession,
                    book = book,
                    records = records,
                    bookEta = bookEta,
                    paceMetrics = paceMetrics,
                    todayXp = totalXp,
                    dailyGoalMins = uiState.value.dailyMinutesGoal,
                    todayActiveMins = todayActiveMins,
                    streakDays = streakDays,
                    levelProgress = levelProgress,
                    unlockedThisSession = emptyList(),
                    closestAchievements = achs.filter { !it.isUnlocked }.sortedByDescending { if (it.target > 0) it.current.toFloat() / it.target.toFloat() else 0f }.take(2),
                    recent7Sessions = pastSessions.take(7),
                    todayDayXp = pastDaysXpMap[todayDateStr] ?: 0L,
                    bestDayXp = pastDaysXpMap.values.maxOrNull() ?: 0L,
                    bookSessionCount = pastBookSessions.size
                )

                _uiState.update { it.copy(pendingSessionSummary = summaryData) }
            }
        }
    }

    fun dismissSessionSummary() {
        val pending = _uiState.value.pendingSessionSummary
        if (pending != null) {
            viewModelScope.launch {
                readingXpRepository.markSessionShown(pending.session.id)
            }
        }
        _uiState.update { it.copy(pendingSessionSummary = null) }
    }

    fun keepReadingFromSummary() {
        val pending = _uiState.value.pendingSessionSummary
        val targetBook = pending?.book
        dismissSessionSummary()
        if (targetBook != null) {
            openReader(targetBook)
        }
    }

    private fun startReadingTimer() {
        readingTimerJob?.cancel()
        readingTimerJob = viewModelScope.launch {
            while (true) {
                delay(5000) // every 5 seconds
                tickReadingTime()
            }
        }
    }

    private var tickCounter = 0
    private fun tickReadingTime() {
        tickCounter++
        if (tickCounter >= 12) { // 12 * 5s = 60s = 1 min
            tickCounter = 0
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            _uiState.update { state ->
                val currentMins = state.readingLog[todayStr] ?: 0
                val newLog = state.readingLog.toMutableMap().apply {
                    this[todayStr] = currentMins + 1
                }
                val newState = state.copy(readingLog = newLog)
                checkGoalAnnouncements(newState)
            }
            saveGoalsToStore()

            // Record reading session checkpoint for active habit integration
            val currentBook = _uiState.value.activeBookForReader
            if (currentBook != null && currentReaderStartTime > 0L) {
                val durationSecs = (System.currentTimeMillis() - currentReaderStartTime) / 1000
                val pagesRead = sessionMarkedPages.size
                if (durationSecs >= 10 || pagesRead > 0) {
                    val session = BookReadingSession(
                        id = "s_${currentBook.id}_${currentReaderStartTime}",
                        bookId = currentBook.id,
                        startTime = currentReaderStartTime,
                        endTime = System.currentTimeMillis(),
                        pagesRead = pagesRead,
                        durationSeconds = durationSecs
                    )
                    viewModelScope.launch {
                        bookRepository.recordReadingSession(session)
                    }
                }
            }
        }
    }

    private fun checkGoalAnnouncements(state: BooksUiState): BooksUiState {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val monthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val yearStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"))

        val todayMins = state.readingLog[todayStr] ?: 0
        val monthPrefix = monthStr
        val monthMins = state.readingLog.filter { it.key.startsWith(monthPrefix) }.values.sum()
        val monthHours = monthMins / 60f

        return when {
            todayMins >= state.dailyMinutesGoal && state.notifiedDailyDate != todayStr -> {
                state.copy(
                    notifiedDailyDate = todayStr,
                    pendingCelebration = true,
                    activeBannerMessage = "🎯 Daily goal done! ${state.dailyMinutesGoal} min · tap to celebrate"
                )
            }
            monthHours >= state.monthlyHoursGoal && state.notifiedMonth != monthStr -> {
                state.copy(
                    notifiedMonth = monthStr,
                    activeBannerMessage = "📅 Monthly goal reached! ${state.monthlyHoursGoal} h"
                )
            }
            else -> state
        }
    }

    private fun stopReadingTimer() {
        readingTimerJob?.cancel()
        readingTimerJob = null
        tickCounter = 0
        saveGoalsToStore()
    }

    // Streak calculation helper
    fun calculateStreak(log: Map<String, Int>, dailyGoal: Int): Int {
        var streak = 0
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val todayMins = log[today.format(formatter)] ?: 0
        val checkFrom = if (todayMins >= dailyGoal) today else today.minusDays(1)

        var curr = checkFrom
        for (i in 0 until 400) {
            val dateStr = curr.format(formatter)
            val mins = log[dateStr] ?: 0
            if (mins >= dailyGoal) {
                streak++
                curr = curr.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    fun calculateMonthHours(log: Map<String, Int>): Float {
        val monthPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val totalMins = log.filter { it.key.startsWith(monthPrefix) }.values.sum()
        return (totalMins / 60f * 10).roundToInt() / 10f
    }

    fun calculateYearBooks(books: List<Book>): Int {
        val currentYear = LocalDate.now().year
        return books.count { book ->
            val finished = book.markedDone || book.percentProgress >= 100
            if (!finished) return@count false
            val finishedYear = book.finishedAt?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).year
            } ?: currentYear
            finishedYear == currentYear && !book.isDeleted
        }
    }

    // UI Actions
    fun importBookFromUri(context: android.content.Context, uri: android.net.Uri) {
        _uiState.update { it.copy(isImportingBook = true, importingBookTitle = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "Imported Book"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "Imported Book"
                    }
                }

                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                        contentResolver.getType(uri)?.contains("pdf", ignoreCase = true) == true
                val format = if (isPdf) "PDF" else "EPUB"
                val title = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")

                val booksDir = java.io.File(context.filesDir, "books").apply { mkdirs() }
                val targetFile = java.io.File(booksDir, "book_${System.currentTimeMillis()}.${if (isPdf) "pdf" else "epub"}")

                contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val contentHash = if (targetFile.exists()) tn.keeftalk.chat.util.ContentHashUtils.computeSha256(targetFile) else null
                val allBooks = bookRepository.getAllBooks()
                val existing = allBooks.firstOrNull {
                    (!it.isDeleted) && ((!contentHash.isNullOrEmpty() && it.contentHash == contentHash) ||
                            it.title.equals(title, ignoreCase = true))
                }

                if (existing != null) {
                    val updated = existing.copy(
                        filePath = targetFile.absolutePath,
                        contentHash = contentHash ?: existing.contentHash,
                        updatedAt = System.currentTimeMillis()
                    )
                    bookRepository.saveBook(updated)
                    _uiState.update { it.copy(toastMessage = "Updated book: $title") }
                    return@launch
                }

                var totalPages = 100
                if (isPdf && targetFile.exists()) {
                    try {
                        val pfd = android.os.ParcelFileDescriptor.open(targetFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = android.graphics.pdf.PdfRenderer(pfd)
                        totalPages = maxOf(1, renderer.pageCount)
                        renderer.close()
                        pfd.close()
                    } catch (e: Exception) {
                        totalPages = 100
                    }
                }

                val bookId = "b_${System.currentTimeMillis()}"
                val firstPageCoverPath = if (isPdf && targetFile.exists()) generateFirstPageCover(context, bookId, targetFile) else null
                val cover = if (!firstPageCoverPath.isNullOrEmpty()) CustomCover(imageUrl = firstPageCoverPath) else CustomCover(colors = listOf("#6C63FF", "#00CCCC"))

                val newBook = Book(
                    id = bookId,
                    title = title,
                    author = "Local Library",
                    totalPages = totalPages,
                    format = format,
                    filePath = targetFile.absolutePath,
                    customCover = cover,
                    percentProgress = 0,
                    lastReadPage = 1,
                    insightText = "Just added — start reading now",
                    contentHash = contentHash
                )

                bookRepository.saveBook(newBook)
                _uiState.update { it.copy(toastMessage = "Added book: $title") }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Failed to import book") }
            } finally {
                _uiState.update { it.copy(isImportingBook = false, importingBookTitle = null) }
            }
        }
    }

    fun importBookFromVaultItem(
        context: android.content.Context,
        vaultItem: VaultItem,
        vaultRepository: tn.keeftalk.chat.domain.repository.VaultRepository
    ) {
        _uiState.update { it.copy(isImportingBook = true, importingBookTitle = vaultItem.title) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val decryptedResult = vaultRepository.getDecryptedFile(vaultItem)
                val file = decryptedResult.getOrNull() ?: vaultItem.file?.localPath?.let { java.io.File(it) }
                if (file != null && file.exists()) {
                    val vaultFileName = vaultItem.file?.fileName ?: ""
                    val vaultMimeType = vaultItem.file?.mimeType ?: ""

                    val isPdf = file.name.endsWith(".pdf", ignoreCase = true) ||
                            vaultFileName.endsWith(".pdf", ignoreCase = true) ||
                            vaultMimeType.contains("pdf", ignoreCase = true) ||
                            vaultItem.title.endsWith(".pdf", ignoreCase = true)

                    val format = if (isPdf) "PDF" else "EPUB"
                    val ext = if (isPdf) "pdf" else "epub"

                    val rawTitle = vaultItem.title.ifBlank {
                        if (vaultFileName.isNotBlank()) vaultFileName.substringBeforeLast(".")
                        else file.nameWithoutExtension
                    }
                    val title = if (rawTitle.endsWith(".pdf", ignoreCase = true) || rawTitle.endsWith(".epub", ignoreCase = true)) {
                        rawTitle.substringBeforeLast(".")
                    } else rawTitle

                    val booksDir = java.io.File(context.filesDir, "books").apply { mkdirs() }
                    val targetFile = java.io.File(booksDir, "book_${System.currentTimeMillis()}.$ext")
                    file.copyTo(targetFile, overwrite = true)

                    val contentHash = if (targetFile.exists()) tn.keeftalk.chat.util.ContentHashUtils.computeSha256(targetFile) else null
                    val allBooks = bookRepository.getAllBooks()
                    val existing = allBooks.firstOrNull {
                        (!it.isDeleted) && ((!contentHash.isNullOrEmpty() && it.contentHash == contentHash) ||
                                it.title.equals(title, ignoreCase = true))
                    }

                    if (existing != null) {
                        val updated = existing.copy(
                            filePath = targetFile.absolutePath,
                            contentHash = contentHash ?: existing.contentHash,
                            updatedAt = System.currentTimeMillis()
                        )
                        bookRepository.saveBook(updated)
                        _uiState.update { it.copy(toastMessage = "Updated from Vault: $title") }
                        return@launch
                    }

                    var totalPages = 100
                    if (isPdf && targetFile.exists()) {
                        try {
                            val pfd = android.os.ParcelFileDescriptor.open(targetFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                            val renderer = android.graphics.pdf.PdfRenderer(pfd)
                            totalPages = maxOf(1, renderer.pageCount)
                            renderer.close()
                            pfd.close()
                        } catch (e: Exception) {
                            totalPages = 100
                        }
                    }

                    val bookId = "b_${System.currentTimeMillis()}"
                    val firstPageCoverPath = if (isPdf && targetFile.exists()) generateFirstPageCover(context, bookId, targetFile) else null
                    val cover = if (!firstPageCoverPath.isNullOrEmpty()) CustomCover(imageUrl = firstPageCoverPath) else CustomCover(colors = listOf("#4834D4", "#0A0A2A"))

                    val newBook = Book(
                        id = bookId,
                        title = title,
                        author = "My Vault",
                        totalPages = totalPages,
                        format = format,
                        filePath = targetFile.absolutePath,
                        customCover = cover,
                        percentProgress = 0,
                        lastReadPage = 1,
                        insightText = "Imported from Vault — start reading now",
                        contentHash = contentHash
                    )

                    bookRepository.saveBook(newBook)
                    _uiState.update { it.copy(toastMessage = "Imported from Vault: $title") }
                } else {
                    _uiState.update { it.copy(toastMessage = "Could not access vault file") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(toastMessage = "Failed to import from Vault") }
            } finally {
                _uiState.update { it.copy(isImportingBook = false, importingBookTitle = null) }
            }
        }
    }

    fun addNewBookManually(title: String, author: String, totalPages: Int, format: String) {
        viewModelScope.launch {
            val newBook = Book(
                id = "b_${System.currentTimeMillis()}",
                title = title.ifBlank { "Untitled Book" },
                author = author.ifBlank { "Unknown Author" },
                totalPages = maxOf(1, totalPages),
                format = format,
                customCover = CustomCover(colors = listOf("#6C63FF", "#7D5CFF")),
                percentProgress = 0,
                lastReadPage = 1,
                insightText = "Just added — start reading now"
            )
            bookRepository.saveBook(newBook)
            showToast("Added book: ${newBook.title}")
        }
    }

    private var autoCollapseJob: Job? = null

    fun triggerInitialRailAutoExpand() {
        if (!_uiState.value.hasRailAutoExpanded) {
            _uiState.update { it.copy(isRailExpanded = true, hasRailAutoExpanded = true) }
            autoCollapseJob?.cancel()
            autoCollapseJob = viewModelScope.launch {
                delay(2200)
                _uiState.update { it.copy(isRailExpanded = false) }
            }
        }
    }

    fun selectTab(tab: String) {
        autoCollapseJob?.cancel()
        if (_uiState.value.currentTab != tab) {
            _uiState.update { it.copy(currentTab = tab, isRailExpanded = false) }
        } else {
            val nextExpanded = !_uiState.value.isRailExpanded
            _uiState.update { it.copy(isRailExpanded = nextExpanded) }
            if (nextExpanded) {
                autoCollapseJob = viewModelScope.launch {
                    delay(2200)
                    _uiState.update { it.copy(isRailExpanded = false) }
                }
            }
        }
    }

    fun toggleViewMode() {
        _uiState.update {
            val next = when (it.viewMode) {
                "list" -> "g3"
                "g3" -> "g2"
                else -> "list"
            }
            it.copy(viewMode = next)
        }
    }

    fun selectLibraryFilter(filter: String) {
        _uiState.update { it.copy(libraryFilter = filter) }
    }

    fun selectQuotesTab(tab: String) {
        _uiState.update { it.copy(quotesTab = tab) }
    }

    fun selectQuotesBookFilter(bookId: String?) {
        _uiState.update { it.copy(selectedQuotesBookFilterId = bookId) }
    }

    fun openBookActionSheet(book: Book) {
        _uiState.update { it.copy(activeBookForSheet = book) }
    }

    fun closeBookActionSheet() {
        _uiState.update { it.copy(activeBookForSheet = null) }
    }

    fun openChangeCoverSheet(book: Book) {
        _uiState.update { it.copy(activeBookForSheet = book, isChangeCoverSheetOpen = true) }
    }

    fun closeChangeCoverSheet() {
        _uiState.update { it.copy(isChangeCoverSheetOpen = false) }
    }

    fun openDetailStats(book: Book) {
        viewModelScope.launch {
            val sessions = readingXpRepository.getSessionsForBook(book.contentHash ?: book.id)
            val confirmedPages = readingXpRepository.getConfirmedPagesForBook(book.contentHash ?: book.id).size
            val xp = confirmedPages * 50L + (book.hoursRead * 100).toLong()
            _uiState.update { 
                it.copy(
                    activeBookForDetailStats = book, 
                    activeBookForSheet = null,
                    activeBookSessions = sessions,
                    activeBookXp = xp
                ) 
            }
        }
    }

    fun openShareSheet(book: Book) {
        _uiState.update { it.copy(activeBookForShare = book, activeBookForSheet = null) }
    }

    fun closeShareSheet() {
        _uiState.update { it.copy(activeBookForShare = null) }
    }

    private fun generateFirstPageCover(context: android.content.Context, bookId: String, pdfFile: java.io.File): String? {
        return try {
            val pfd = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bitmap = android.graphics.Bitmap.createBitmap(300, 450, android.graphics.Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                val coverFile = java.io.File(context.filesDir, "cover_${bookId}.jpg")
                coverFile.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                coverFile.absolutePath
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun closeDetailStats() {
        _uiState.update { it.copy(activeBookForDetailStats = null) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun updateCustomCover(book: Book, cover: CustomCover?) {
        viewModelScope.launch {
            bookRepository.updateCustomCover(book.id, cover)
            closeChangeCoverSheet()
            showToast("Cover updated")
        }
    }

    fun toggleMarkDone(book: Book) {
        viewModelScope.launch {
            val nowDone = !book.markedDone
            val finishedAtVal = if (nowDone) System.currentTimeMillis() else null
            val updated = if (book.markedDone) {
                book.copy(
                    markedDone = false,
                    percentProgress = book.originalProgress,
                    insightText = book.originalInsight,
                    finishedAt = null
                )
            } else {
                appContext?.let { ctx ->
                    SoundEffectManager.playMediaSound(ctx, SoundEffectManager.RES_GOAL_DONE)
                }
                book.copy(
                    markedDone = true,
                    originalProgress = book.percentProgress,
                    originalInsight = book.insightText,
                    percentProgress = 100,
                    insightText = "Marked as done",
                    finishedAt = finishedAtVal
                )
            }
            bookRepository.saveBook(updated)
            showToast(if (updated.markedDone) "Marked as done" else "Marked as not finished")
        }
    }

    fun toggleArchive(book: Book) {
        viewModelScope.launch {
            val updated = book.copy(isArchived = !book.isArchived)
            bookRepository.saveBook(updated)
            showToast(if (updated.isArchived) "Archived" else "Back in library")
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            bookRepository.deleteBook(book.id)
            _uiState.update { it.copy(lastDeletedBook = book) }
            showToast("Deleted “${book.title}”")
        }
    }

    fun undoDelete() {
        val deleted = _uiState.value.lastDeletedBook ?: return
        viewModelScope.launch {
            bookRepository.restoreBook(deleted.id)
            _uiState.update { it.copy(lastDeletedBook = null) }
            showToast("Restored “${deleted.title}”")
        }
    }

    fun updatePagePosition(bookId: String, pageNumber: Int) {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId) ?: return@launch
            val percent = if (book.totalPages > 0) {
                ((pageNumber.toFloat() / book.totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0
            val isNowDone = percent >= 100
            val updated = book.copy(
                lastReadPage = pageNumber,
                percentProgress = maxOf(book.percentProgress, percent),
                markedDone = if (isNowDone) true else book.markedDone,
                finishedAt = if (isNowDone && book.finishedAt == null) System.currentTimeMillis() else book.finishedAt,
                updatedAt = System.currentTimeMillis()
            )
            bookRepository.saveBook(updated)
        }
    }

    fun togglePageBookmark(bookId: String, pageNumber: Int) {
        viewModelScope.launch {
            val existing = bookRepository.getBookmarksForBook(bookId).firstOrNull { it.pageNumber == pageNumber }
            if (existing != null) {
                bookRepository.deleteBookmark(bookId, pageNumber)
                showToast("Bookmark removed")
            } else {
                val newBookmark = BookBookmark(
                    bookId = bookId,
                    pageNumber = pageNumber,
                    chapterTitle = "Ch ${((pageNumber - 1) % 12) + 1}",
                    excerpt = "Bookmark at page $pageNumber"
                )
                bookRepository.saveBookmark(newBookmark)
                showToast("Page $pageNumber bookmarked")
            }
        }
    }

    fun saveHighlight(highlight: BookHighlight) {
        viewModelScope.launch {
            bookRepository.saveHighlight(highlight)
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            bookRepository.deleteHighlight(id)
            showToast("Highlight removed")
        }
    }

    fun deleteBookmark(bookmark: BookBookmark) {
        viewModelScope.launch {
            bookRepository.deleteBookmark(bookmark.bookId, bookmark.pageNumber)
            showToast("Bookmark removed")
        }
    }

    fun savePageProgressEntity(progress: BookPageProgress) {
        viewModelScope.launch {
            bookRepository.savePageProgress(progress)

            if (progress.isRead) {
                sessionMarkedPages.add(progress.pageNumber)
            } else {
                sessionMarkedPages.remove(progress.pageNumber)
            }

            val book = bookRepository.getBookById(progress.bookId) ?: return@launch
            val pageProgresses = bookRepository.getPageProgressForBook(progress.bookId)
            val readPagesCount = pageProgresses.count { it.isRead }

            val posPercent = if (book.totalPages > 0) {
                ((book.lastReadPage.toFloat() / book.totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0

            val pageReadPercent = if (book.totalPages > 0) {
                ((readPagesCount.toFloat() / book.totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0

            val percent = maxOf(book.percentProgress, posPercent, pageReadPercent)

            val isNowDone = percent >= 100 || (readPagesCount >= book.totalPages && book.totalPages > 0)
            val finishedAtVal = if (isNowDone && book.finishedAt == null) System.currentTimeMillis() else book.finishedAt

            val updatedBook = book.copy(
                percentProgress = percent,
                markedDone = if (isNowDone) true else book.markedDone,
                finishedAt = finishedAtVal,
                updatedAt = System.currentTimeMillis()
            )
            bookRepository.saveBook(updatedBook)

            if (_uiState.value.activeBookForReader?.id == book.id) {
                _uiState.update { state ->
                    state.copy(activeBookForReader = updatedBook)
                }
            }
        }
    }

    fun updateBookSettings(settings: BookSettings) {
        viewModelScope.launch {
            bookRepository.updateBookSettings(settings)
            if (settings.syncAcrossDevices) {
                bookRepository.syncBooksWithCloud()
            }
        }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}