package tn.keeftalk.chat.ui.books

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharedFlow
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.AppNotification
import tn.keeftalk.chat.domain.model.VaultItem
import tn.keeftalk.chat.ui.books.components.*
import tn.keeftalk.chat.ui.books.reader.BookReaderScreen
import tn.keeftalk.chat.ui.books.sheets.BookActionSheet
import tn.keeftalk.chat.ui.books.sheets.ChangeCoverSheet
import tn.keeftalk.chat.ui.books.sheets.ReadNowSheet
import tn.keeftalk.chat.ui.components.FabActionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BooksScreen(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    notifications: List<AppNotification> = emptyList(),
    onNotificationClick: (AppNotification) -> Unit = {},
    onMarkAllNotificationsAsRead: () -> Unit = {},
    fabActionFlow: SharedFlow<FabActionType>? = null,
    onReaderStateChanged: (Boolean) -> Unit = {},
    onDetailStatsStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: BooksViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repository = AppModule.provideBookRepository(context)
                val readingXpRepo = AppModule.provideReadingXpRepository(context)
                return BooksViewModel(repository, readingXpRepo, context.applicationContext) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val isDetailStatsActive = uiState.activeBookForDetailStats != null
    LaunchedEffect(isDetailStatsActive) {
        onDetailStatsStateChanged(isDetailStatsActive)
    }
    DisposableEffect(Unit) {
        onDispose {
            onDetailStatsStateChanged(false)
        }
    }
    val books by viewModel.booksFlow.collectAsState()
    val highlights by viewModel.highlightsFlow.collectAsState()
    val bookmarks by viewModel.bookmarksFlow.collectAsState()
    val bookSettings by viewModel.bookSettingsFlow.collectAsState()
    val pageProgress by viewModel.activeBookPageProgressFlow.collectAsState()
    val readingDays by viewModel.readingDaysFlow.collectAsState()
    val unlockedAchievements by viewModel.unlockedAchievementsFlow.collectAsState()
    val xpMeta by viewModel.xpMetaFlow.collectAsState()
    val globalRankState by viewModel.globalRankState.collectAsState()

    var showVaultImportDialog by remember { mutableStateOf(false) }

    // File picker launcher for importing PDF / EPUB books from device storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBookFromUri(context, uri)
        }
    }

    val isReaderActive = uiState.activeBookForReader != null

    val allAchievements = remember(books, highlights, bookmarks) {
        XpCalculator.calculateAchievementsList(books, highlights.size, bookmarks.size)
    }

    var pendingCelebrations by remember { mutableStateOf<List<CelebrationItem>>(emptyList()) }
    var activeCelebration by remember { mutableStateOf<CelebrationItem?>(null) }

    LaunchedEffect(allAchievements, uiState.celebratedAchievements) {
        if (books.isNotEmpty()) {
            val newlyUnlocked = allAchievements.filter { it.isUnlocked && it.title !in uiState.celebratedAchievements }
            val newItems = mutableListOf<CelebrationItem>()
            newlyUnlocked.forEach { ach ->
                if (pendingCelebrations.none { (it as? CelebrationItem.AchievementUnlocked)?.achievement?.title == ach.title } &&
                    (activeCelebration as? CelebrationItem.AchievementUnlocked)?.achievement?.title != ach.title) {
                    newItems.add(CelebrationItem.AchievementUnlocked(ach))
                }
            }
            if (newItems.isNotEmpty()) {
                pendingCelebrations = pendingCelebrations + newItems
            }
        }
    }

    LaunchedEffect(isReaderActive, pendingCelebrations, activeCelebration) {
        if (!isReaderActive && activeCelebration == null && pendingCelebrations.isNotEmpty()) {
            activeCelebration = pendingCelebrations.first()
            pendingCelebrations = pendingCelebrations.drop(1)
        }
    }

    if (activeCelebration != null) {
        CelebrationDialog(
            item = activeCelebration!!,
            onDismiss = {
                activeCelebration?.let { item ->
                    if (item is CelebrationItem.AchievementUnlocked) {
                        viewModel.markAchievementCelebrated(item.achievement.title)
                    }
                }
                activeCelebration = null
            }
        )
    }

    LaunchedEffect(isReaderActive) {
        onReaderStateChanged(isReaderActive)
    }

    DisposableEffect(Unit) {
        onDispose {
            onReaderStateChanged(false)
        }
    }

    // Handle Global Adaptive FAB Actions emitted via fabActionFlow
    LaunchedEffect(Unit) {
        fabActionFlow?.collect { action ->
            when (action) {
                FabActionType.ADD_BOOK_FILE -> filePickerLauncher.launch("*/*")
                FabActionType.IMPORT_BOOK_VAULT -> showVaultImportDialog = true
                else -> {}
            }
        }
    }

    // Handle Toast messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Trigger initial rail auto-expansion once on first launch
    LaunchedEffect(Unit) {
        viewModel.triggerInitialRailAutoExpand()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Goals Top Banner Overlay
        GoalsBanner(
            message = uiState.activeBannerMessage,
            onBannerClick = {
                viewModel.dismissBanner()
                viewModel.openCelebrationDialog()
            },
            onDismiss = { viewModel.dismissBanner() }
        )

        // Full Screen Celebration Dialog
        if (uiState.showCelebrationDialog) {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val todayMins = uiState.readingLog[todayStr] ?: 0
            val streak = viewModel.calculateStreak(uiState.readingLog, uiState.dailyMinutesGoal)
            val monthH = viewModel.calculateMonthHours(uiState.readingLog)
            CelebrationDialog(
                goalMinutes = uiState.dailyMinutesGoal,
                todayMinutes = todayMins,
                streakDays = streak,
                monthHours = monthH,
                onDismiss = { viewModel.closeCelebrationDialog() },
                onKeepReading = {
                    viewModel.closeCelebrationDialog()
                    val targetBook = books.firstOrNull { it.id == uiState.lastOpenedBookId } ?: books.firstOrNull()
                    if (targetBook != null) {
                        viewModel.openReader(targetBook)
                    }
                }
            )
        }

        // Read Now Sheet
        if (uiState.showReadNowSheet) {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val todayMins = uiState.readingLog[todayStr] ?: 0
            val dailyLeft = maxOf(0, uiState.dailyMinutesGoal - todayMins)
            val isDone = todayMins >= uiState.dailyMinutesGoal
            ReadNowSheet(
                books = books,
                lastOpenedBookId = uiState.lastOpenedBookId,
                dailyLeftMin = dailyLeft,
                isDailyGoalDone = isDone,
                onDismiss = { viewModel.closeReadNowSheet() },
                onBookClick = { book ->
                    viewModel.closeReadNowSheet()
                    viewModel.openReader(book)
                }
            )
        }

        // App lifecycle observer for backgrounding (>2 min timeout)
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> viewModel.onAppBackgrounded()
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> viewModel.onAppForegrounded()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Pending Session Summary Popup
        if (uiState.pendingSessionSummary != null) {
            val summary = uiState.pendingSessionSummary!!
            val recentSessions by viewModel.readingSessionsFlow.collectAsState()

            SessionSummaryDialog(
                session = summary.session,
                book = summary.book,
                records = summary.records,
                bookEta = summary.bookEta,
                paceMetrics = summary.paceMetrics,
                todayXp = summary.todayXp,
                dailyGoalMins = summary.dailyGoalMins,
                todayActiveMins = summary.todayActiveMins,
                streakDays = summary.streakDays,
                levelProgress = summary.levelProgress,
                unlockedThisSession = summary.unlockedThisSession,
                closestAchievements = summary.closestAchievements,
                recent7Sessions = recentSessions,
                onDismiss = { viewModel.dismissSessionSummary() },
                onKeepReading = { viewModel.keepReadingFromSummary() }
            )
        }

        // Full Screen Reader Overlay
        if (uiState.activeBookForReader != null) {
            BookReaderScreen(
                book = uiState.activeBookForReader!!,
                bookSettings = bookSettings,
                bookmarks = bookmarks.filter { it.bookId == uiState.activeBookForReader!!.id },
                highlights = highlights.filter { it.bookId == uiState.activeBookForReader!!.id },
                pageProgress = pageProgress,
                onBack = { viewModel.closeReader() },
                onSettingsClick = { viewModel.openSettings() },
                onPageChanged = { page -> viewModel.updatePagePosition(uiState.activeBookForReader!!.id, page) },
                onBookmarkToggled = { page -> viewModel.togglePageBookmark(uiState.activeBookForReader!!.id, page) },
                onSaveHighlight = { highlight -> viewModel.saveHighlight(highlight) },
                onDeleteHighlight = { id -> viewModel.deleteHighlight(id) },
                onSavePageProgress = { progress -> viewModel.savePageProgressEntity(progress) },
                onConfirmPageRead = { page, dwellSec, tapX, tapY, isObscured ->
                    viewModel.confirmPageRead(context, uiState.activeBookForReader!!, page, dwellSec, tapX, tapY, isObscured)
                },
                onUpdateTheme = { themeKey -> viewModel.updateBookSettings(bookSettings.copy(theme = themeKey)) },
                onUpdateSettings = { newSettings -> viewModel.updateBookSettings(newSettings) }
            )
        } else if (uiState.activeBookForDetailStats != null) {
            // Book Detail Stats Screen
            BookDetailStatsScreen(
                book = uiState.activeBookForDetailStats!!,
                sessions = uiState.activeBookSessions,
                totalXp = uiState.activeBookXp,
                onBack = { viewModel.closeDetailStats() },
                onContinueReading = { book -> viewModel.openReader(book) }
            )
        } else if (uiState.isSettingsOpen) {
            // Settings Screen
            BooksSettingsScreen(
                settings = bookSettings,
                onBack = { viewModel.closeSettings() },
                onSettingsChanged = { newSettings -> viewModel.updateBookSettings(newSettings) }
            )
        } else {
            var showManualLibraryTour by remember { mutableStateOf(false) }
            var showManualQuotesInfo by remember { mutableStateOf(false) }
            var showManualGoalsInfo by remember { mutableStateOf(false) }
            var showManualAchievementsInfo by remember { mutableStateOf(false) }

            val showLibraryTour = (uiState.currentTab == "lib" && !bookSettings.hasSeenLibraryTour) || showManualLibraryTour
            val showQuotesInfo = (uiState.currentTab == "quo" && !bookSettings.hasSeenQuotesCard) || showManualQuotesInfo
            val showGoalsInfo = (uiState.currentTab == "gol" && !bookSettings.hasSeenGoalsCard) || showManualGoalsInfo
            val showAchievementsInfo = (uiState.currentTab == "awd" && !bookSettings.hasSeenAchievementsCard) || showManualAchievementsInfo

            if (showLibraryTour) {
                LibraryTourDialog(
                    onDismiss = {
                        showManualLibraryTour = false
                        viewModel.updateBookSettings(bookSettings.copy(hasSeenLibraryTour = true))
                    }
                )
            }

            if (showQuotesInfo) {
                QuotesInfoDialog(
                    onDismiss = {
                        showManualQuotesInfo = false
                        viewModel.updateBookSettings(bookSettings.copy(hasSeenQuotesCard = true))
                    }
                )
            }

            if (showGoalsInfo) {
                GoalsInfoDialog(
                    onDismiss = {
                        showManualGoalsInfo = false
                        viewModel.updateBookSettings(bookSettings.copy(hasSeenGoalsCard = true))
                    }
                )
            }

            if (showAchievementsInfo) {
                AchievementsInfoDialog(
                    onDismiss = {
                        showManualAchievementsInfo = false
                        viewModel.updateBookSettings(bookSettings.copy(hasSeenAchievementsCard = true))
                    }
                )
            }

            // Main My Books Interface (Starts directly with hero card + rail at top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (uiState.currentTab) {
                    "gol" -> BooksGoalsContent(
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        isRailExpanded = uiState.isRailExpanded,
                        focusedPeriod = uiState.focusedPeriod,
                        onSelectPeriod = { viewModel.selectFocusedPeriod(it) },
                        dailyMinutesCurrent = uiState.readingLog[LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))] ?: 0,
                        dailyMinutesGoal = uiState.dailyMinutesGoal,
                        monthlyHoursCurrent = viewModel.calculateMonthHours(uiState.readingLog),
                        monthlyHoursGoal = uiState.monthlyHoursGoal,
                        yearlyBooksCurrent = viewModel.calculateYearBooks(books),
                        yearlyBooksGoal = uiState.yearlyBooksGoal,
                        streakDays = viewModel.calculateStreak(uiState.readingLog, uiState.dailyMinutesGoal),
                        historyMap = uiState.readingLog,
                        reminderEnabled = uiState.reminderEnabled,
                        reminderTime = uiState.reminderTime,
                        onToggleReminder = { viewModel.toggleReminder(it, context) },
                        onUpdateTime = { viewModel.updateReminderTime(it, context) },
                        onDecrementGoal = { viewModel.decrementGoal() },
                        onIncrementGoal = { viewModel.incrementGoal() },
                        onOpenReadNow = { viewModel.openReadNowSheet() },
                        onOpenInfo = { showManualGoalsInfo = true }
                    )
                    "quo" -> BooksQuotesContent(
                        books = books,
                        highlights = highlights,
                        bookmarks = bookmarks,
                        activeTab = uiState.quotesTab,
                        selectedBookFilterId = uiState.selectedQuotesBookFilterId,
                        onTabSelect = { viewModel.selectQuotesTab(it) },
                        onBookFilterSelect = { viewModel.selectQuotesBookFilter(it) },
                        onHighlightClick = { highlight ->
                            val book = books.firstOrNull { it.id == highlight.bookId }
                            if (book != null) viewModel.openReader(book)
                        },
                        onBookmarkClick = { bookmark ->
                            val book = books.firstOrNull { it.id == bookmark.bookId }
                            if (book != null) viewModel.openReader(book)
                        },
                        onDeleteHighlight = { id -> viewModel.deleteHighlight(id) },
                        onDeleteBookmark = { bm -> viewModel.deleteBookmark(bm) },
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        isRailExpanded = uiState.isRailExpanded,
                        onOpenInfo = { showManualQuotesInfo = true }
                    )
                    "sta" -> BooksStatsContent(
                        books = books,
                        onBookStatsClick = { book -> viewModel.openDetailStats(book) },
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        isRailExpanded = uiState.isRailExpanded
                    )
                    "awd" -> BooksAchievementsContent(
                        books = books,
                        highlightsCount = highlights.size,
                        bookmarksCount = bookmarks.size,
                        readingDays = readingDays,
                        unlockedAchievements = unlockedAchievements,
                        legacyReadingXp = xpMeta?.legacyReadingXp ?: 0L,
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        isRailExpanded = uiState.isRailExpanded,
                        onOpenInfo = { showManualAchievementsInfo = true },
                        globalRankState = globalRankState,
                        onRefreshGlobalRank = { totalXp -> viewModel.refreshGlobalRank(context, totalXp) }
                    )
                    else -> BooksLibraryContent(
                        books = books,
                        viewMode = uiState.viewMode,
                        selectedFilter = uiState.libraryFilter,
                        archivedCount = books.count { it.isArchived && !it.isDeleted },
                        onFilterSelect = { viewModel.selectLibraryFilter(it) },
                        onToggleViewMode = { viewModel.toggleViewMode() },
                        onBookClick = { book -> viewModel.openBookActionSheet(book) },
                        onContinueReadingClick = { book -> viewModel.openReader(book) },
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) },
                        isRailExpanded = uiState.isRailExpanded,
                        onOpenTour = { showManualLibraryTour = true }
                    )
                }
            }

            // Bottom Sheets
            if (uiState.activeBookForSheet != null && !uiState.isChangeCoverSheetOpen) {
                BookActionSheet(
                    book = uiState.activeBookForSheet!!,
                    onDismiss = { viewModel.closeBookActionSheet() },
                    onRead = { book -> viewModel.openReader(book) },
                    onStats = { book -> viewModel.openDetailStats(book) },
                    onShare = { book -> viewModel.openShareSheet(book) },
                    onChangeCoverClick = { book -> viewModel.openChangeCoverSheet(book) },
                    onToggleDone = { book -> viewModel.toggleMarkDone(book) },
                    onToggleArchive = { book -> viewModel.toggleArchive(book) },
                    onDeleteConfirm = { book -> viewModel.deleteBook(book) }
                )
            }

            if (uiState.activeBookForShare != null) {
                tn.keeftalk.chat.ui.books.sheets.BookShareBottomSheet(
                    book = uiState.activeBookForShare!!,
                    onDismiss = { viewModel.closeShareSheet() },
                    onDirectChatShare = { book ->
                        viewModel.closeShareSheet()
                        Toast.makeText(context, "Sharing “${book.title}” directly through Chats", Toast.LENGTH_SHORT).show()
                    },
                    onEmailShare = { book ->
                        viewModel.closeShareSheet()
                        Toast.makeText(context, "Sharing “${book.title}” through My Emails", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (uiState.isChangeCoverSheetOpen && uiState.activeBookForSheet != null) {
                ChangeCoverSheet(
                    book = uiState.activeBookForSheet!!,
                    onDismiss = { viewModel.closeChangeCoverSheet() },
                    onCoverSelected = { cover -> viewModel.updateCustomCover(uiState.activeBookForSheet!!, cover) }
                )
            }

            // Vault Import Dialog triggered from global FAB
            if (showVaultImportDialog) {
                VaultImportDialog(
                    onDismiss = { showVaultImportDialog = false },
                    onSelectVaultItem = { vaultItem ->
                        val vaultRepo = AppModule.provideVaultRepository(context)
                        viewModel.importBookFromVaultItem(context, vaultItem, vaultRepo)
                        showVaultImportDialog = false
                    }
                )
            }

            // Book Import Progress Window
            if (uiState.isImportingBook) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Importing Book",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                uiState.importingBookTitle?.let { "Importing “$it” from Vault..." } ?: "Decrypting and importing book...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultImportDialog(
    onDismiss: () -> Unit,
    onSelectVaultItem: (VaultItem) -> Unit
) {
    val context = LocalContext.current
    val vaultRepo = remember { AppModule.provideVaultRepository(context) }
    val vaultItems by vaultRepo.getAllItems().collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Vault", fontWeight = FontWeight.Bold) },
        text = {
            if (vaultItems.isEmpty()) {
                Text("No files currently stored in your encrypted Vault.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vaultItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVaultItem(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Vault",
                                    tint = Color(0xFF6C63FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(item.file?.fileName ?: "Vault Document", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
