package tn.keeftalk.chat.ui.books.reader

import android.graphics.Bitmap
import android.widget.Toast
import tn.keeftalk.chat.extension.registry.DictionaryLookupResult
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.books.components.parseHexColor
import tn.keeftalk.chat.util.PdfPageTextResult
import tn.keeftalk.chat.util.PdfTextExtractor
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class ReaderThemeColors(
    val key: String,
    val name: String,
    val bg: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val isDarkInverted: Boolean = false
)

val READER_THEMES = listOf(
    ReaderThemeColors("original", "Original", Color(0xFFFFFFFF), Color(0xFF1A1A1A), Color(0xFF757575), Color(0x1F000000)),
    ReaderThemeColors("dark", "Dark", Color(0xFF121214), Color(0xFFD4D4DC), Color(0xFF8A8A98), Color(0x18FFFFFF), isDarkInverted = true),
    ReaderThemeColors("twilight", "Twilight", Color(0xFF1D1A30), Color(0xFFD9D3F0), Color(0xFF8F88B3), Color(0x1AFFFFFF), isDarkInverted = true),
    ReaderThemeColors("paper", "Paper", Color(0xFFFBFAF5), Color(0xFF23221F), Color(0xFF8C897E), Color(0x14000000)),
    ReaderThemeColors("sepia", "Sepia", Color(0xFFF1E4C8), Color(0xFF4A3820), Color(0xFF8F7B58), Color(0x224A3820)),
    ReaderThemeColors("sepiap", "Sepia+", Color(0xFFD8BF94), Color(0xFF33210D), Color(0xFF7D6642), Color(0x2633210D))
)

val RibbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(size.width / 2f, size.height * 0.76f)
    lineTo(0f, size.height)
    close()
}

fun createThemeColorMatrix(theme: ReaderThemeColors): ColorMatrix? {
    if (theme.key == "original" || theme.key == "normal") {
        return null
    }

    val dR = theme.bg.red - theme.text.red
    val dG = theme.bg.green - theme.text.green
    val dB = theme.bg.blue - theme.text.blue

    return ColorMatrix(
        floatArrayOf(
            dR * 0.2126f, dR * 0.7152f, dR * 0.0722f, 0f, theme.text.red * 255f,
            dG * 0.2126f, dG * 0.7152f, dG * 0.0722f, 0f, theme.text.green * 255f,
            dB * 0.2126f, dB * 0.7152f, dB * 0.0722f, 0f, theme.text.blue * 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

fun extractEpubText(file: File): List<String> {
    val paragraphs = mutableListOf<String>()
    try {
        if (file.name.endsWith(".epub", true)) {
            java.util.zip.ZipFile(file).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && (entry.name.endsWith(".html", true) || entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".htm", true))) {
                        zipFile.getInputStream(entry).bufferedReader().use { reader ->
                            val html = reader.readText()
                            val cleanText = html.replace(Regex("<[^>]*>"), " ")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            if (cleanText.length > 20) {
                                cleanText.split(Regex("(?<=[.!?])\\s+"))
                                    .forEach { p -> if (p.isNotBlank()) paragraphs.add(p.trim()) }
                            }
                        }
                    }
                }
            }
        } else {
            file.readLines().forEach { line ->
                if (line.isNotBlank()) paragraphs.add(line.trim())
            }
        }
    } catch (_: Exception) {
        // fallback
    }
    return paragraphs
}

/** Idle time (no touch) before the reader hides all bars and enters full view. */
private const val AUTO_FULL_VIEW_DELAY_MS = 4_000L

/** Thin bottom bar: read pages. */
private val READ_BAR_COLOR = Color(0xFF2ECC71)

/** Thin bottom bar: seen / turned pages (app accent purple). */
private val SEEN_BAR_COLOR = Color(0xFF6C63FF)

/** Short pause so the user sees the page turn green before it auto-advances. */
private const val AUTO_NEXT_DELAY_MS = 1000L

/** Rescales to a fixed longest side so OCR sees the same image size whatever the render DPI. */
private fun scaleForOcr(bmp: Bitmap, targetLongestSide: Int = 2000): Bitmap {
    val longest = max(bmp.width, bmp.height)
    if (longest == targetLongestSide || longest <= 0) return bmp
    val s = targetLongestSide.toFloat() / longest
    return Bitmap.createScaledBitmap(
        bmp,
        (bmp.width * s).roundToInt().coerceAtLeast(1),
        (bmp.height * s).roundToInt().coerceAtLeast(1),
        true
    )
}

/**
 * Pinch-to-zoom + pan that only consumes events when it actually needs them:
 * - 2+ fingers (pinch) -> always handled
 * - 1 finger while already zoomed -> pan
 * - 1 finger at 1x -> NOT consumed, so HorizontalPager can swipe pages.
 */
private fun Modifier.zoomPanGestures(
    enabled: Boolean,
    isZoomed: () -> Boolean,
    onTransform: (zoom: Float, pan: Offset) -> Unit
): Modifier = if (!enabled) this else pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val consumedByOther = event.changes.any { it.isConsumed }
            if (!consumedByOther) {
                val pointers = event.changes.count { it.pressed }
                if (pointers >= 2 || isZoomed()) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (zoom != 1f || pan != Offset.Zero) {
                        onTransform(zoom, pan)
                        event.changes.forEach { c: PointerInputChange ->
                            if (c.positionChanged()) c.consume()
                        }
                    }
                }
            }
        } while (!consumedByOther && event.changes.any { it.pressed })
    }
}

enum class ReaderTimerMode {
    COUNTDOWN,
    STOPWATCH
}

fun formatReaderTimerTime(totalSeconds: Int): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

fun isRtlText(text: String): Boolean {
    if (text.isBlank()) return false
    val rtlRegex = Regex("[\u0590-\u05FF\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]")
    return rtlRegex.containsMatchIn(text)
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun BookReaderScreen(
    book: Book,
    bookSettings: BookSettings,
    bookmarks: List<BookBookmark>,
    highlights: List<BookHighlight>,
    pageProgress: List<BookPageProgress>,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onBookmarkToggled: (Int) -> Unit,
    onSaveHighlight: (BookHighlight) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onSavePageProgress: (BookPageProgress) -> Unit,
    onConfirmPageRead: (page: Int, dwellSec: Double, tapX: Float, tapY: Float, isObscured: Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateTheme: (String) -> Unit,
    onUpdateSettings: (BookSettings) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val isPdf = book.format.equals("PDF", ignoreCase = true)
    val totalPages = maxOf(1, book.totalPages)

    DisposableEffect(bookSettings.keepScreenOn) {
        if (bookSettings.keepScreenOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ---- Page state: the pager is the single source of truth -------------------------------
    val initialPage = (book.lastReadPage - 1).coerceIn(0, totalPages - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    var currentPage by remember { mutableIntStateOf(initialPage + 1) }
    var sliderPos by remember { mutableStateOf<Float?>(null) } // live value while dragging the slider

    // Pages the user has actually landed on (turned to) in this reading session.
    // Drives the purple segments of the thin bottom bar; the opening page counts as seen.
    var seenPages by remember { mutableStateOf(setOf(initialPage + 1)) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            val newPage = settled + 1
            seenPages = seenPages + newPage
            if (currentPage != newPage) {
                currentPage = newPage
                onPageChanged(newPage)
            }
        }
    }

    var isFocusMode by remember { mutableStateOf(false) }
    var currentThemeKey by remember { mutableStateOf(bookSettings.theme.ifEmpty { "original" }) }
    var autoCropMargins by remember { mutableStateOf(true) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }

    // ---- Reading Timer / Pomodoro state ---------------------------------------------
    var timerMode by remember { mutableStateOf(ReaderTimerMode.COUNTDOWN) }
    var countdownPresetMinutes by remember { mutableIntStateOf(25) }
    var timerSecondsRemaining by remember { mutableIntStateOf(25 * 60) }
    var stopwatchSecondsElapsed by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showTimerFinishedDialog by remember { mutableStateOf(false) }

    // Timer tick loop
    LaunchedEffect(isTimerRunning, timerMode) {
        while (isTimerRunning) {
            delay(1000L)
            if (timerMode == ReaderTimerMode.COUNTDOWN) {
                if (timerSecondsRemaining > 0) {
                    timerSecondsRemaining--
                    if (timerSecondsRemaining == 0) {
                        isTimerRunning = false
                        showTimerFinishedDialog = true
                    }
                } else {
                    isTimerRunning = false
                    showTimerFinishedDialog = true
                }
            } else {
                stopwatchSecondsElapsed++
            }
        }
    }

    // ---- Zoom & pan (applies only to the currently settled page) ---------------------------
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // ---- Selection state ---------------------------------------------------------------------
    var pdfSelectionState by remember { mutableStateOf<PdfSelectionState?>(null) }
    var showNoteEditorSheet by remember { mutableStateOf(false) }
    var showDictionarySheet by remember { mutableStateOf(false) }
    var dictionaryResult by remember { mutableStateOf<DictionaryLookupResult?>(null) }
    var dictionaryLookupWord by remember { mutableStateOf("") }
    var hasInstalledDictionary by remember { mutableStateOf(false) }

    val dwellTracker = remember { PageDwellTracker() }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> dwellTracker.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> dwellTracker.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Reset zoom + clear selection whenever the page changes
    LaunchedEffect(currentPage) {
        dwellTracker.onPageChanged()
        zoomScale = 1f
        panOffsetX = 0f
        panOffsetY = 0f
        pdfSelectionState = null
        showNoteEditorSheet = false
    }

    BackHandler {
        when {
            showNoteEditorSheet -> showNoteEditorSheet = false
            pdfSelectionState != null -> pdfSelectionState = null
            else -> onBack()
        }
    }

    val activeTheme = READER_THEMES.firstOrNull { it.key == currentThemeKey } ?: READER_THEMES[0]
    val themeColorMatrix = remember(activeTheme) { createThemeColorMatrix(activeTheme) }
    val colorFilter = remember(themeColorMatrix) {
        themeColorMatrix?.let { ColorFilter.colorMatrix(it) }
    }

    DisposableEffect(isFocusMode) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFocusMode) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val isBookmarked = bookmarks.any { it.pageNumber == currentPage }
    val currentPageProgress = pageProgress.firstOrNull { it.pageNumber == currentPage }
    val isReadPage = currentPageProgress?.isRead ?: false

    val shownPage = sliderPos?.toInt()?.coerceIn(1, totalPages) ?: currentPage
    val minutesLeft = ((totalPages - shownPage) * 60f / maxOf(1, book.speedPagesPerHour)).toInt()

    // ---- EPUB / text loading ---------------------------------------------------------------
    var epubParagraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isEpubLoading by remember { mutableStateOf(false) }

    LaunchedEffect(book.filePath) {
        if (!isPdf && book.filePath.isNotEmpty()) {
            isEpubLoading = true
            withContext(Dispatchers.IO) {
                val file = File(book.filePath)
                if (file.exists()) {
                    epubParagraphs = extractEpubText(file)
                }
            }
            isEpubLoading = false
        }
    }

    // ---- RTL Language Detection & Reading Direction -------------------------------
    val detectedIsRtl = remember(book.title, book.author, epubParagraphs) {
        isRtlText(book.title) ||
        isRtlText(book.author) ||
        epubParagraphs.take(5).any { isRtlText(it) }
    }

    val isRtlReading = when (bookSettings.readingDirection) {
        "RTL" -> true
        "LTR" -> false
        else -> detectedIsRtl // "AUTO"
    }

    var showRtlPromptDialog by remember(detectedIsRtl, bookSettings.hasSeenRtlPrompt) {
        mutableStateOf(detectedIsRtl && !bookSettings.hasSeenRtlPrompt)
    }

    // ---- PDF renderer manager ----------------------------------------------------------------
    val pdfManager = remember(book.filePath) {
        if (isPdf && book.filePath.isNotEmpty()) PdfRendererManager(book.filePath) else null
    }
    // Pages must not render before initialize() finished (previously a race)
    var managerReady by remember(pdfManager) { mutableStateOf(pdfManager == null) }

    LaunchedEffect(pdfManager) {
        if (pdfManager != null) {
            try {
                pdfManager.initialize()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // pages will show the error state
            }
            managerReady = true
        }
    }

    // rememberCoroutineScope is cancelled when leaving composition, so closing from it leaks.
    DisposableEffect(pdfManager) {
        onDispose {
            pdfManager?.let { mgr ->
                GlobalScope.launch(Dispatchers.IO) { runCatching { mgr.close() } }
            }
        }
    }

    val effectiveDpi = bookSettings.effectiveDpi()

    // ---- Auto full view after 5 s without activity ------------------------------------------
    // A StateFlow (not Compose state) so touch events never trigger recomposition.
    val activityPulse = remember { MutableStateFlow(0L) }
    val idleBlocked = showReaderSettingsSheet || showNoteEditorSheet ||
            pdfSelectionState != null || sliderPos != null

    LaunchedEffect(isFocusMode, idleBlocked) {
        if (isFocusMode || idleBlocked) return@LaunchedEffect
        // collectLatest restarts the delay on every pulse; the first emission starts the timer
        activityPulse.collectLatest {
            delay(AUTO_FULL_VIEW_DELAY_MS)
            isFocusMode = true
        }
    }

    // ---- Highlight helper (replaces 4 copy-pasted blocks) ----------------------------------
    fun commitHighlight(
        sel: PdfSelectionState,
        color: String? = null,
        quote: Boolean? = null,
        note: String? = null,
        defaultColor: String = "#FFD43B"
    ) {
        val existing = sel.activeHighlight
        val h = BookHighlight(
            id = existing?.id ?: "h_${System.currentTimeMillis()}",
            bookId = book.id,
            pageNumber = currentPage,
            startOffset = minOf(sel.startWordIndex, sel.endWordIndex),
            endOffset = maxOf(sel.startWordIndex, sel.endWordIndex),
            selectedText = sel.selectedText,
            colorHex = color ?: existing?.colorHex ?: defaultColor,
            isQuote = quote ?: existing?.isQuote ?: false,
            noteText = note ?: existing?.noteText ?: ""
        )
        onSaveHighlight(h)
        pdfSelectionState = sel.copy(activeHighlight = h)
    }

    val density = LocalDensity.current
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    // Page numbers marked as read (green segments of the thin bottom bar)
    val readPageSet = remember(pageProgress) {
        pageProgress.filter { it.isRead }.map { it.pageNumber }.toHashSet()
    }

    val layoutDirection = if (isRtlReading) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(activeTheme.bg)
            // Initial pass = we only watch, never consume, so no other gesture is affected
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        activityPulse.value = System.nanoTime()
                    }
                }
            }
    ) {
        ReadMarkerOverlay(
            isPageRead = isReadPage,
            greenOpacityPercent = bookSettings.greenOpacityPercent,
            readMarkerColorHex = bookSettings.readMarkerColorHex,
            showMovableButton = bookSettings.showReadMarker,
            sizePx = bookSettings.buttonSizePx,
            dockX = currentPageProgress?.freeX,
            dockY = currentPageProgress?.freeY,
            onToggleReadWithDetails = { tapX, tapY, isObscured ->
                val dwellSec = dwellTracker.dwellSec()
                onConfirmPageRead(currentPage, dwellSec, tapX, tapY, isObscured)
            },
            onToggleRead = {
                val newProgress = (currentPageProgress ?: BookPageProgress(book.id, currentPage))
                    .copy(isRead = !isReadPage, updatedAt = System.currentTimeMillis())
                onSavePageProgress(newProgress)
                Toast.makeText(context, if (!isReadPage) "Page $currentPage marked as read" else "Page $currentPage unmarked", Toast.LENGTH_SHORT).show()

                // Setting: turn to the next page right after marking this one as read
                val markedPage = currentPage
                if (!isReadPage && bookSettings.autoNextPageOnRead && markedPage < totalPages) {
                    coroutineScope.launch {
                        delay(AUTO_NEXT_DELAY_MS)
                        // only if the user hasn't already moved elsewhere in the meantime
                        if (pagerState.currentPage + 1 == markedPage) {
                            pagerState.animateScrollToPage(markedPage) // index = markedPage (next page)
                        }
                    }
                }
            },
            onDockPositionChanged = { x, y ->
                val newProgress = (currentPageProgress ?: BookPageProgress(book.id, currentPage))
                    .copy(freeX = x, freeY = y, updatedAt = System.currentTimeMillis())
                onSavePageProgress(newProgress)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = zoomScale <= 1.05f,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val pageNum = pageIndex + 1
                val isCurrent = pageNum == currentPage

                var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var renderFailed by remember { mutableStateOf(false) }
                var pageText by remember(pageBitmap) { mutableStateOf<PdfPageTextResult?>(null) }
                var boxSize by remember { mutableStateOf(IntSize.Zero) }

                // Render (keeps showing the previous bitmap while a new DPI is rendering)
                LaunchedEffect(pdfManager, managerReady, pageNum, autoCropMargins, effectiveDpi) {
                    if (pdfManager == null || !managerReady) return@LaunchedEffect
                    val cached = pdfManager.getCachedPage(pageNum, effectiveDpi, autoCropMargins)
                    if (cached != null) {
                        pageBitmap = cached
                        renderFailed = false
                        return@LaunchedEffect
                    }
                    val rendered = try {
                        pdfManager.renderPage(pageNum, effectiveDpi, autoCropMargins)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    if (rendered != null) {
                        pageBitmap = rendered
                        renderFailed = false
                    } else if (pageBitmap == null) {
                        renderFailed = true // previously: spinner forever
                    }
                }

                // OCR / text extraction per page, only for the page being read.
                // (Previously one global bitmap was updated only if the page was already
                //  current when it finished rendering -> words of the wrong page.)
                // Also runs for non-current pages that already have saved highlights, so those
                // highlights are drawn while swiping instead of popping in after the page settles.
                val needsText = isCurrent || highlights.any { it.pageNumber == pageNum }
                LaunchedEffect(pageBitmap, needsText) {
                    val bmp = pageBitmap
                    if (needsText && bmp != null && pageText == null) {
                        pageText = withContext(Dispatchers.IO) {
                            var scaled: Bitmap? = null
                            try {
                                // Same OCR input size at every DPI -> same words/indices, so
                                // saved highlights don't shift when DPI or auto-crop changes size.
                                scaled = scaleForOcr(bmp)
                                PdfTextExtractor.extractTextFromBitmap(scaled)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                null
                            } finally {
                                if (scaled != null && scaled !== bmp) scaled.recycle()
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = pageBitmap
                    when {
                        isPdf && bmp != null -> {
                            val aspect = bmp.width.toFloat() / bmp.height.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { boxSize = it }
                                    .pointerInput(isCurrent) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (!isCurrent) return@detectTapGestures
                                                if (zoomScale > 1.05f) {
                                                    zoomScale = 1f
                                                    panOffsetX = 0f
                                                    panOffsetY = 0f
                                                } else {
                                                    zoomScale = 2.5f
                                                }
                                            },
                                            onTap = {
                                                if (pdfSelectionState != null) pdfSelectionState = null
                                                else isFocusMode = !isFocusMode
                                            }
                                        )
                                    }
                                    .zoomPanGestures(
                                        enabled = isCurrent,
                                        isZoomed = { zoomScale > 1.05f },
                                        onTransform = { zoom, pan ->
                                            val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                            val maxX = boxSize.width * (newScale - 1f) / 2f
                                            val maxY = boxSize.height * (newScale - 1f) / 2f
                                            zoomScale = newScale
                                            if (newScale <= 1.01f) {
                                                zoomScale = 1f
                                                panOffsetX = 0f
                                                panOffsetY = 0f
                                            } else {
                                                panOffsetX = (panOffsetX + pan.x).coerceIn(-maxX, maxX)
                                                panOffsetY = (panOffsetY + pan.y).coerceIn(-maxY, maxY)
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // aspectRatio keeps the box exactly the size of the drawn bitmap,
                                // even for tall pages (tablet / landscape), so the overlay
                                // coordinates always line up with the image.
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(aspect)
                                        .graphicsLayer(
                                            scaleX = if (isCurrent) zoomScale else 1f,
                                            scaleY = if (isCurrent) zoomScale else 1f,
                                            translationX = if (isCurrent) panOffsetX else 0f,
                                            translationY = if (isCurrent) panOffsetY else 0f
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "PDF Page $pageNum",
                                        colorFilter = colorFilter,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    val words = pageText?.words
                                    if (!words.isNullOrEmpty()) {
                                        PdfTextSelectionOverlay(
                                            words = words,
                                            savedHighlights = highlights.filter { it.pageNumber == pageNum },
                                            selectionState = if (isCurrent) pdfSelectionState else null,
                                            onSelectionChanged = { pdfSelectionState = it },
                                            onOpenToolbar = { state -> pdfSelectionState = state },
                                            interactive = isCurrent,
                                            zoomScale = if (isCurrent) zoomScale else 1f,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }

                                    if (bookmarks.any { it.pageNumber == pageNum }) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(end = 12.dp)
                                                .size(width = 26.dp, height = 38.dp)
                                                .clip(RibbonShape)
                                                .background(Color(0xFFFF5D73))
                                                .zIndex(50f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "Bookmarked",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Never fall through to the "text" branch for a PDF (it would print binary garbage)
                        isPdf && renderFailed -> {
                            Text(
                                text = "Can't render page $pageNum",
                                color = activeTheme.muted,
                                fontSize = 13.sp
                            )
                        }

                        isPdf -> CircularProgressIndicator(color = activeTheme.text)

                        isEpubLoading -> CircularProgressIndicator(color = activeTheme.text)

                        else -> {
                            val paragraphsPerPage = 6
                            val pageParagraphs = if (epubParagraphs.isNotEmpty()) {
                                val startIndex = ((pageNum - 1) * paragraphsPerPage).coerceAtLeast(0)
                                val endIndex = (startIndex + paragraphsPerPage).coerceAtMost(epubParagraphs.size)
                                if (startIndex < epubParagraphs.size) epubParagraphs.subList(startIndex, endIndex)
                                else listOf("End of book.")
                            } else {
                                listOf("Page $pageNum of $totalPages")
                            }

                            val pageHighlights = highlights.filter { it.pageNumber == pageNum }

                            SelectionContainer {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                        .navigationBarsPadding()
                                        .padding(top = 56.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "${book.title} · ${book.author} · Page $pageNum of $totalPages",
                                        fontSize = 12.sp,
                                        color = activeTheme.muted,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = book.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeTheme.text,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )

                                    pageParagraphs.forEach { text ->
                                        val annotated = buildAnnotatedString {
                                            append(text)
                                            pageHighlights.forEach { h ->
                                                val hColor = parseHexColor(h.colorHex, Color(0xFFFFD43B)).copy(alpha = 0.35f)
                                                if (h.selectedText.isNotBlank()) {
                                                    var startIndex = 0
                                                    while (startIndex < text.length) {
                                                        val foundIdx = text.indexOf(h.selectedText, startIndex, ignoreCase = true)
                                                        if (foundIdx == -1) break
                                                        val endIdx = foundIdx + h.selectedText.length
                                                        addStyle(SpanStyle(background = hColor), foundIdx, endIdx)
                                                        startIndex = endIdx
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = annotated,
                                            fontSize = if (autoCropMargins) 16.5.sp else 15.5.sp,
                                            lineHeight = if (autoCropMargins) 28.sp else 26.sp,
                                            fontWeight = if (currentThemeKey == "sepiap") FontWeight.Medium else FontWeight.Normal,
                                            fontFamily = FontFamily.Serif,
                                            color = activeTheme.text,
                                            modifier = Modifier
                                                .padding(bottom = 12.dp)
                                                .clickable {
                                                    if (pdfSelectionState != null) pdfSelectionState = null
                                                    else isFocusMode = !isFocusMode
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Selection toolbar (null-safe: no more `pdfSelectionState!!` crashes) -----------
        pdfSelectionState?.let { sel ->
            val activeHighlight = sel.activeHighlight
            TextSelectionToolbar(
                selectedText = sel.selectedText,
                existingHighlightColor = activeHighlight?.colorHex,
                isQuote = activeHighlight?.isQuote ?: false,
                hasNote = !activeHighlight?.noteText.isNullOrEmpty(),
                onCopy = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    pdfSelectionState = null
                },
                onToggleQuote = {
                    val newQuote = !(activeHighlight?.isQuote ?: false)
                    commitHighlight(sel, quote = newQuote, defaultColor = "#6C63FF")
                    Toast.makeText(context, if (newQuote) "Saved as quote" else "Quote removed", Toast.LENGTH_SHORT).show()
                },
                onOpenNoteEditor = { showNoteEditorSheet = true },
                onLookupDictionary = { text ->
                    val host = try { tn.keeftalk.chat.di.AppModule.provideExtensionHost(context) } catch (_: Exception) { null }
                    val dictRegistry = host?.registry?.dictionaryRegistry
                    val installedDicts = dictRegistry?.installedDictionaries?.value ?: emptyList()
                    val res = dictRegistry?.lookupWord(text)

                    dictionaryLookupWord = text
                    dictionaryResult = res
                    hasInstalledDictionary = installedDicts.isNotEmpty()
                    showDictionarySheet = true
                },
                onColorSelected = { hex ->
                    commitHighlight(sel, color = hex)
                    Toast.makeText(context, "Highlighted", Toast.LENGTH_SHORT).show()
                },
                onRemoveHighlight = if (activeHighlight != null) {
                    {
                        onDeleteHighlight(activeHighlight.id)
                        pdfSelectionState = null
                        Toast.makeText(context, "Highlight removed", Toast.LENGTH_SHORT).show()
                    }
                } else null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .zIndex(150f)
            )
        }

        // Note editor must draw above the top/bottom bars and arrows -> high zIndex + IME padding
        val noteSel = pdfSelectionState
        if (showNoteEditorSheet && noteSel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(200f)
            ) {
                NoteEditorSheet(
                    quotedText = noteSel.selectedText,
                    initialNote = noteSel.activeHighlight?.noteText ?: "",
                    highlightColorHex = noteSel.activeHighlight?.colorHex ?: "#6C63FF",
                    onDismiss = { showNoteEditorSheet = false },
                    onSaveNote = { noteText ->
                        commitHighlight(noteSel, note = noteText)
                        showNoteEditorSheet = false
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showDictionarySheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(200f)
            ) {
                DictionaryResultBottomSheet(
                    word = dictionaryLookupWord.ifBlank { pdfSelectionState?.selectedText ?: "" },
                    result = dictionaryResult,
                    hasInstalledDictionary = hasInstalledDictionary,
                    onDismiss = { showDictionarySheet = false }
                )
            }
        }

        AnimatedVisibility(
            visible = !isFocusMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeTheme.bg)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = activeTheme.text
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onBookmarkToggled(currentPage) }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFFF5D73) else activeTheme.text
                    )
                }

                // Reading Timer Badge
                if (bookSettings.showTimerBadge) {
                    val displayTimeStr = if (timerMode == ReaderTimerMode.COUNTDOWN) {
                        formatReaderTimerTime(timerSecondsRemaining)
                    } else {
                        formatReaderTimerTime(stopwatchSecondsElapsed)
                    }

                    Surface(
                        onClick = { showReaderSettingsSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isTimerRunning) Color(0xFF6C63FF).copy(alpha = 0.2f) else activeTheme.text.copy(alpha = 0.08f),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .alpha((bookSettings.timerBadgeOpacityPercent / 100f).coerceIn(0.1f, 1.0f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = if (isTimerRunning) Color(0xFF6C63FF) else activeTheme.text,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = displayTimeStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimerRunning) Color(0xFF6C63FF) else activeTheme.text
                            )
                        }
                    }
                }

                IconButton(onClick = { showReaderSettingsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = activeTheme.text
                    )
                }
            }
        }

        // Persistent Floating Timer Badge when in Focus Mode (reading mode)
        if (bookSettings.showTimerBadge && isFocusMode) {
            val displayTimeStr = if (timerMode == ReaderTimerMode.COUNTDOWN) {
                formatReaderTimerTime(timerSecondsRemaining)
            } else {
                formatReaderTimerTime(stopwatchSecondsElapsed)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 16.dp)
                    .zIndex(100f)
            ) {
                Surface(
                    onClick = { showReaderSettingsSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isTimerRunning) Color(0xFF6C63FF) else activeTheme.text.copy(alpha = 0.12f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .alpha((bookSettings.timerBadgeOpacityPercent / 100f).coerceIn(0.1f, 1.0f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (isTimerRunning) Color.White else activeTheme.text,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayTimeStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTimerRunning) Color.White else activeTheme.text
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isFocusMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { bottomBarHeightPx = it.height }
                    .background(activeTheme.bg)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page $shownPage / $totalPages · ${book.percentProgress}% · ${minutesLeft}m left",
                        fontSize = 11.sp,
                        color = activeTheme.muted
                    )
                    IconButton(
                        onClick = { autoCropMargins = !autoCropMargins },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Auto Crop",
                            tint = if (autoCropMargins) Color(0xFF6C63FF) else activeTheme.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (totalPages > 1) {
                    Slider(
                        value = sliderPos ?: currentPage.toFloat(),
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = {
                            val target = (sliderPos?.toInt() ?: currentPage).coerceIn(1, totalPages)
                            sliderPos = null
                            // settledPage collector updates currentPage + calls onPageChanged
                            coroutineScope.launch { pagerState.scrollToPage(target - 1) }
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6C63FF),
                            activeTrackColor = Color(0xFF6C63FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Manual page-turn arrows: bottom corners (just above the bottom bar), 20% opacity
        AnimatedVisibility(
            visible = !isFocusMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val arrowBottomPadding = with(density) { bottomBarHeightPx.toDp() } + 8.dp
            val canLeft = if (isRtlReading) currentPage < totalPages else currentPage > 1
            val targetLeft = if (isRtlReading) (pagerState.currentPage + 1).coerceAtMost(totalPages - 1) else (pagerState.currentPage - 1).coerceAtLeast(0)
            val descLeft = if (isRtlReading) "Next Page" else "Previous Page"

            val canRight = if (isRtlReading) currentPage > 1 else currentPage < totalPages
            val targetRight = if (isRtlReading) (pagerState.currentPage - 1).coerceAtLeast(0) else (pagerState.currentPage + 1).coerceAtMost(totalPages - 1)
            val descRight = if (isRtlReading) "Previous Page" else "Next Page"

            Box(modifier = Modifier.fillMaxSize()) {
                if (canLeft) {
                    PageArrowButton(
                        icon = Icons.Default.ChevronLeft,
                        description = descLeft,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = arrowBottomPadding),
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetLeft)
                            }
                        }
                    )
                }

                if (canRight) {
                    PageArrowButton(
                        icon = Icons.Default.ChevronRight,
                        description = descRight,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = arrowBottomPadding),
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetRight)
                            }
                        }
                    )
                }
            }
        }

        // Very thin per-page progress line: visible in normal AND full view, touch-transparent.
        // One segment per page:  read = green · seen/turned = purple · not seen = grey (the track)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding() // 0 in full view (system bars hidden), above nav bar otherwise
                .height(2.dp)
                .background(activeTheme.muted.copy(alpha = 0.25f)) // grey = not seen / not read
                .zIndex(20f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segW = size.width / totalPages
                // 2 = read (wins over seen), 1 = seen/turned, 0 = untouched
                fun stateOf(page: Int): Int = when {
                    page in readPageSet -> 2
                    page in seenPages -> 1
                    else -> 0
                }
                // Merge neighbouring pages with the same state into one rect (no hairline seams)
                var i = 1
                while (i <= totalPages) {
                    val st = stateOf(i)
                    var j = i
                    while (j < totalPages && stateOf(j + 1) == st) j++
                    if (st != 0) {
                        drawRect(
                            color = if (st == 2) READ_BAR_COLOR else SEEN_BAR_COLOR,
                            topLeft = Offset((i - 1) * segW, 0f),
                            size = Size((j - i + 1) * segW, size.height)
                        )
                    }
                    i = j + 1
                }
            }
        }

        if (showRtlPromptDialog) {
            AlertDialog(
                onDismissRequest = {
                    showRtlPromptDialog = false
                    onUpdateSettings(bookSettings.copy(hasSeenRtlPrompt = true))
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(0xFF6C63FF),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text("Arabic / RTL Language Detected 📖", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                text = {
                    Text(
                        "We detected that this book (\"${book.title}\") is written in Arabic or a Right-to-Left (RTL) language.\n\nWould you like to turn page reading direction to Right-to-Left (RTL) or continue in standard Left-to-Right (LTR)?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRtlPromptDialog = false
                            onUpdateSettings(bookSettings.copy(readingDirection = "RTL", hasSeenRtlPrompt = true))
                            Toast.makeText(context, "Switched reading direction to RTL", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("Turn Reading to RTL")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showRtlPromptDialog = false
                            onUpdateSettings(bookSettings.copy(readingDirection = "LTR", hasSeenRtlPrompt = true))
                        }
                    ) {
                        Text("Continue LTR")
                    }
                }
            )
        }

        if (showTimerFinishedDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTimerFinishedDialog = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2ECC71),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Reading Session Complete! 📚", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Great work! You completed your $countdownPresetMinutes-minute reading countdown.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            "Would you like to finish or add more reading time?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val addTimeOptions = listOf(5, 10, 15, 30)
                            addTimeOptions.forEach { extraMins ->
                                OutlinedButton(
                                    onClick = {
                                        timerSecondsRemaining = extraMins * 60
                                        countdownPresetMinutes = extraMins
                                        isTimerRunning = true
                                        showTimerFinishedDialog = false
                                        Toast.makeText(context, "Added +$extraMins min to timer", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("+$extraMins min", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showTimerFinishedDialog = false
                            timerSecondsRemaining = countdownPresetMinutes * 60
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showTimerFinishedDialog = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        if (showReaderSettingsSheet) {
            BookSettingsSheet(
                settings = bookSettings,
                currentThemeKey = currentThemeKey,
                timerMode = timerMode,
                countdownPresetMinutes = countdownPresetMinutes,
                timerSecondsRemaining = timerSecondsRemaining,
                stopwatchSecondsElapsed = stopwatchSecondsElapsed,
                isTimerRunning = isTimerRunning,
                onTimerModeChange = { mode -> timerMode = mode },
                onCountdownPresetChange = { mins ->
                    countdownPresetMinutes = mins
                    timerSecondsRemaining = mins * 60
                    isTimerRunning = false
                },
                onTimerToggle = { isTimerRunning = !isTimerRunning },
                onTimerReset = {
                    isTimerRunning = false
                    if (timerMode == ReaderTimerMode.COUNTDOWN) {
                        timerSecondsRemaining = countdownPresetMinutes * 60
                    } else {
                        stopwatchSecondsElapsed = 0
                    }
                },
                onDismiss = { showReaderSettingsSheet = false },
                onUpdateSettings = { newSettings -> onUpdateSettings(newSettings) },
                onUpdateTheme = { themeKey ->
                    currentThemeKey = themeKey
                    onUpdateTheme(themeKey)
                }
            )
        }
    }
}
}

/** Round page-turn button. The whole control (circle + icon) is drawn at 20% opacity. */
@Composable
private fun PageArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Black,
        shape = CircleShape,
        modifier = modifier
            .size(48.dp)
            .alpha(0.2f)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSettingsSheet(
    settings: BookSettings,
    currentThemeKey: String,
    timerMode: ReaderTimerMode,
    countdownPresetMinutes: Int,
    timerSecondsRemaining: Int,
    stopwatchSecondsElapsed: Int,
    isTimerRunning: Boolean,
    onTimerModeChange: (ReaderTimerMode) -> Unit,
    onCountdownPresetChange: (Int) -> Unit,
    onTimerToggle: () -> Unit,
    onTimerReset: () -> Unit,
    onDismiss: () -> Unit,
    onUpdateSettings: (BookSettings) -> Unit,
    onUpdateTheme: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reader Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)

            // ---- Reading Timer & Pomodoro Section ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Reading Timer & Pomodoro", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    val timerDisplay = if (timerMode == ReaderTimerMode.COUNTDOWN) {
                        formatReaderTimerTime(timerSecondsRemaining)
                    } else {
                        formatReaderTimerTime(stopwatchSecondsElapsed)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTimerRunning) Color(0xFF6C63FF).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = timerDisplay,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTimerRunning) Color(0xFF6C63FF) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Mode Selector (Countdown / Stopwatch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = timerMode == ReaderTimerMode.COUNTDOWN,
                        onClick = { onTimerModeChange(ReaderTimerMode.COUNTDOWN) },
                        label = { Text("Countdown (Pomodoro)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = timerMode == ReaderTimerMode.STOPWATCH,
                        onClick = { onTimerModeChange(ReaderTimerMode.STOPWATCH) },
                        label = { Text("Stopwatch") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (timerMode == ReaderTimerMode.COUNTDOWN) {
                    Text("Target Reading Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(5, 15, 25, 30, 45, 60)
                        presets.forEach { mins ->
                            FilterChip(
                                selected = countdownPresetMinutes == mins,
                                onClick = { onCountdownPresetChange(mins) },
                                label = { Text("${mins}m${if (mins == 25) " (Pomodoro)" else ""}") }
                            )
                        }
                    }

                    var customSliderVal by remember(countdownPresetMinutes) { mutableFloatStateOf(countdownPresetMinutes.toFloat()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom duration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${customSliderVal.roundToInt()} min", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                    }
                    Slider(
                        value = customSliderVal,
                        onValueChange = { customSliderVal = it },
                        onValueChangeFinished = {
                            val mins = customSliderVal.roundToInt().coerceIn(1, 180)
                            onCountdownPresetChange(mins)
                        },
                        valueRange = 1f..180f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF6C63FF), activeTrackColor = Color(0xFF6C63FF)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onTimerToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else Color(0xFF6C63FF)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTimerRunning) "Pause" else "Start Timer")
                    }

                    OutlinedButton(
                        onClick = onTimerReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }
                }

                // Timer Badge Visibility & Opacity
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Timer Badge on Screen", fontSize = 13.sp)
                    Switch(
                        checked = settings.showTimerBadge,
                        onCheckedChange = { onUpdateSettings(settings.copy(showTimerBadge = it)) }
                    )
                }

                if (settings.showTimerBadge) {
                    var localBadgeOpacity by remember(settings.timerBadgeOpacityPercent) { mutableFloatStateOf(settings.timerBadgeOpacityPercent.toFloat()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Timer Badge Opacity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Adjust badge transparency on reader screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${localBadgeOpacity.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                    }
                    Slider(
                        value = localBadgeOpacity,
                        onValueChange = { localBadgeOpacity = it },
                        onValueChangeFinished = {
                            onUpdateSettings(settings.copy(timerBadgeOpacityPercent = localBadgeOpacity.toInt()))
                        },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF6C63FF), activeTrackColor = Color(0xFF6C63FF))
                    )
                }
            }

            HorizontalDivider()

            Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            // 6 chips overflow a phone screen -> make the row scrollable
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                READER_THEMES.forEach { theme ->
                    FilterChip(
                        selected = theme.key == currentThemeKey,
                        onClick = { onUpdateTheme(theme.key) },
                        label = { Text(theme.name) }
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val effectiveDpi = settings.effectiveDpi()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PDF DPI Quality", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Controls rendering sharpness", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$effectiveDpi DPI", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf("lowest" to "Lowest (108)", "middle" to "Middle (162)", "custom" to "Custom")
                    presets.forEach { (optionKey, label) ->
                        FilterChip(
                            selected = settings.pdfDpiOption == optionKey,
                            onClick = { onUpdateSettings(settings.copy(pdfDpiOption = optionKey)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                if (settings.pdfDpiOption == "custom") {
                    var localCustomDpi by remember(settings.pdfCustomDpi) { mutableFloatStateOf(settings.pdfCustomDpi.toFloat()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom DPI value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${localCustomDpi.toInt()} DPI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                    }
                    Slider(
                        value = localCustomDpi,
                        onValueChange = { localCustomDpi = it },
                        onValueChangeFinished = {
                            onUpdateSettings(settings.copy(pdfDpiOption = "custom", pdfCustomDpi = localCustomDpi.toInt()))
                        },
                        valueRange = 100f..750f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF6C63FF), activeTrackColor = Color(0xFF6C63FF)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Read Marker", fontSize = 14.sp)
                Switch(
                    checked = settings.showReadMarker,
                    onCheckedChange = { onUpdateSettings(settings.copy(showReadMarker = it)) }
                )
            }

            var localOpacity by remember(settings.greenOpacityPercent) { mutableFloatStateOf(settings.greenOpacityPercent.toFloat()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Read Marker Opacity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Default: 50% opacity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${localOpacity.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
            }
            Slider(
                value = localOpacity,
                onValueChange = { localOpacity = it },
                onValueChangeFinished = {
                    onUpdateSettings(settings.copy(greenOpacityPercent = localOpacity.toInt()))
                },
                valueRange = 10f..90f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF2ECC71), activeTrackColor = Color(0xFF2ECC71))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keep Screen On", fontSize = 14.sp)
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onUpdateSettings(settings.copy(keepScreenOn = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-turn page when marked read", fontSize = 14.sp)
                    Text("Go to the next page after marking this one read", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.autoNextPageOnRead,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoNextPageOnRead = it)) }
                )
            }

            HorizontalDivider()

            // ---- Reading Direction (RTL / Arabic Support) ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Reading Direction (RTL / Arabic)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dirOptions = listOf(
                        "AUTO" to "Auto",
                        "LTR" to "LTR (Left-to-Right)",
                        "RTL" to "RTL (Right-to-Left)"
                    )
                    dirOptions.forEach { (dirKey, label) ->
                        FilterChip(
                            selected = settings.readingDirection == dirKey,
                            onClick = { onUpdateSettings(settings.copy(readingDirection = dirKey)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}