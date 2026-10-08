package tn.keeftalk.chat.ui.books.components

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.R
import tn.keeftalk.chat.data.local.entities.ReadingSessionEntity
import tn.keeftalk.chat.domain.books.BookEtaResult
import tn.keeftalk.chat.domain.books.PaceMetrics
import tn.keeftalk.chat.domain.books.SessionRecords
import tn.keeftalk.chat.domain.model.Achievement
import tn.keeftalk.chat.domain.model.Book
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/* ==========================================================================
 * SESSION SUMMARY: native replica of the HTML "Reading Session" popup.
 *
 *  - quiet log  (< 3 min)        -> flat card, pages count-up, no XP / trophy / confetti
 *  - tier 1..5 (by session XP)   -> trophy pop, ring draw, odometer score, tier pill,
 *                                   confetti / sparkles / fireworks / MEGA flash scaled per tier
 *
 *  Everything is on one clock (ms since the dialog opened) so every delay and duration
 *  matches the CSS / JS timeline of the HTML.
 * ========================================================================== */

/* ---------- easing curves (same cubic-beziers as the HTML) ---------- */
private val SsEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)          // --ease-out
private val SsEaseSpring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)   // --ease-spring
private val SsEaseCss = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)       // CSS "ease"
private val SsEaseOutCss = CubicBezierEasing(0f, 0f, 0.58f, 1f)         // CSS "ease-out"
private val SsEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)       // CSS "ease-in-out"

/** Clock value meaning "every animation is finished" (used for reduce-motion). */
private const val SS_DONE_MS = 1_000_000L

/** HTML stacks the two buttons on viewports <= 420 px wide. Set to 0 to keep them side by side. */
private const val SS_STACK_BUTTONS_BELOW_DP = 420

/* ---------- pure rules (no Android types besides the entity => unit-testable) ---------- */

/** History a session may be compared against: never itself, never a flagged session. */
internal fun sessionRecordBaseline(
    past: List<ReadingSessionEntity>,
    currentId: String
): List<ReadingSessionEntity> = past.filter { it.id != currentId && it.flagged == 0 }

internal const val SESSION_ETA_MIN_SESSIONS = 3

/** ETA needs >= 3 sessions of the book. A negative count means "unknown": defer to the calculator. */
internal fun sessionEtaHasEnoughData(sessionCount: Int): Boolean =
    sessionCount < 0 || sessionCount >= SESSION_ETA_MIN_SESSIONS

private val SsStreakMilestones = intArrayOf(3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 300, 365)

/** Days left until the next streak milestone, or null once every milestone has been passed. */
internal fun daysToNextStreakMilestone(streakDays: Int): Int? =
    SsStreakMilestones.firstOrNull { it > streakDays }?.let { it - streakDays }

internal const val SS_ENCOURAGE_VARIANTS = 6

/** Stable per session (so rotation never reshuffles the line) yet random across sessions. */
internal fun ssEncouragementIndex(seed: String): Int =
    Random(seed.hashCode()).nextInt(SS_ENCOURAGE_VARIANTS)

private fun ssNumToLong(x: Any?): Long = (x as? Number)?.toLong() ?: 0L

/** Latin digits everywhere (same as the odometer), whatever the UI language. */
private fun ssNum(n: Number): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

private val SsEncourageIds = intArrayOf(
    R.string.ssum_encourage_1, R.string.ssum_encourage_2, R.string.ssum_encourage_3,
    R.string.ssum_encourage_4, R.string.ssum_encourage_5, R.string.ssum_encourage_6
)

/** Never negative: gap to the best + comparison with the last-7 average. */
private fun ssEncouragement(
    res: Resources, seed: String, gapXp: Long, bestXp: Long, currentXp: Long, last7Avg: Long
): Pair<String, String?> {
    val main = if (gapXp > 0 && bestXp > 0) {
        res.getString(SsEncourageIds[ssEncouragementIndex(seed)], ssNum(gapXp), ssNum(bestXp))
    } else {
        res.getString(R.string.ssum_encourage_start)
    }
    val sub = when {
        last7Avg <= 0 -> null
        currentXp > last7Avg -> res.getString(R.string.ssum_vs_avg_above, ssNum(currentXp - last7Avg), ssNum(last7Avg))
        else -> res.getString(R.string.ssum_vs_avg_steady, ssNum(last7Avg))
    }
    return main to sub
}

@StringRes
private fun ssTierLabelRes(level: Int): Int = when (level) {
    2 -> R.string.ssum_tier_2
    3 -> R.string.ssum_tier_3
    4 -> R.string.ssum_tier_4
    5 -> R.string.ssum_tier_5
    else -> R.string.ssum_tier_1
}

/** Arabic / RTL scripts join letters: any letter-spacing breaks the cursive shaping, so drop it there. */
@Composable
private fun ssTrack(v: Float): TextUnit =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.em else v.em

private fun ssProgress(nowMs: Long, delayMs: Int, durMs: Int): Float =
    ((nowMs - delayMs).toFloat() / durMs).coerceIn(0f, 1f)

private fun ssLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** `fadeUp` keyframes: opacity 0 -> 1, translateY 16 -> 0, ease-out, fill both. */
private fun Modifier.ssFadeUp(clock: MutableLongState, delayMs: Int, durMs: Int = 600): Modifier =
    graphicsLayer {
        val p = SsEaseOut.transform(ssProgress(clock.longValue, delayMs, durMs))
        alpha = p
        translationY = (1f - p) * 16.dp.toPx()
    }

/** Popup entrance: scale .7 -> 1 + translateY 40 -> 0 (spring 750 ms, quiet 550 ms ease-out) + opacity. */
private fun Modifier.ssPopupEntrance(clock: MutableLongState, quiet: Boolean): Modifier =
    graphicsLayer {
        val t = clock.longValue
        val e = if (quiet) SsEaseOut.transform(ssProgress(t, 0, 550))
        else SsEaseSpring.transform(ssProgress(t, 0, 750))
        val s = ssLerp(0.7f, 1f, e)
        scaleX = s
        scaleY = s
        translationY = ssLerp(40.dp.toPx(), 0f, e)
        alpha = SsEaseCss.transform(ssProgress(t, 0, if (quiet) 300 else 350)).coerceIn(0f, 1f)
    }

private fun Modifier.ssDashedBottom(): Modifier = drawBehind {
    drawLine(
        color = Color.White.copy(alpha = 0.06f),
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    )
}

/* ==========================================================================
 * PUBLIC ENTRY POINT (same signature as before, + optional soundEnabled)
 * ========================================================================== */
@Composable
fun SessionSummaryDialog(
    session: ReadingSessionEntity,
    book: Book?,
    records: SessionRecords,
    bookEta: BookEtaResult,
    paceMetrics: PaceMetrics,
    todayXp: Long,
    dailyGoalMins: Int,
    todayActiveMins: Int,
    streakDays: Int,
    levelProgress: LevelingSystem.ProgressData,
    unlockedThisSession: List<Achievement>,
    closestAchievements: List<Achievement>,
    recent7Sessions: List<ReadingSessionEntity>,
    onDismiss: () -> Unit,
    onKeepReading: () -> Unit,
    soundEnabled: Boolean = true,
    todayDayXp: Long = 0L,
    bestDayXp: Long = 0L,
    /** Sessions logged for this book (incl. this one). -1 = unknown -> trust the calculator. */
    bookSessionCount: Int = -1,
    /** Optional: plug the app's own cover composable. Default = lightweight built-in loader. */
    bookCover: (@Composable (Modifier) -> Unit)? = null
) {
    val context = LocalContext.current
    val dens = LocalDensity.current.density

    val reduceMotion = remember {
        val animScale = try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (_: Exception) {
            1f
        }
        animScale <= 0.05f
    }

    // ---- quiet-log rule + tier (real session XP -> same cutoffs as the HTML) ----
    val quiet = session.activeSec < SESSION_MIN_CELEBRATION_MINUTES * 60
    val tier = remember(session.id, quiet) {
        if (quiet) SESSION_TIER_ZERO else sessionXpToTier(session.totalXpGained)
    }

    // ---- badges / title (HTML FLAG_DEFS order: book, achievement, high score, goal) ----
    val flagged = session.flagged == 1
    val sessionMins = session.activeSec / 60
    // "finished" = finished DURING this session (not merely a 100 % book that is re-opened)
    val flagBook = book != null && book.percentProgress >= 100 &&
            (book.finishedAt ?: 0L) >= session.startAt
    val flagAchievement = unlockedThisSession.isNotEmpty()
    val newRecord = records.isBestSessionXp && !flagged
    val flagGoal = dailyGoalMins > 0 && todayActiveMins >= dailyGoalMins &&
            (todayActiveMins - sessionMins) < dailyGoalMins

    val badges = remember(session.id, quiet, flagBook, flagAchievement, newRecord, flagGoal) {
        if (quiet) emptyList()
        else buildList {
            if (flagBook) add(SsBadge(R.string.ssum_badge_book, SsBadgeKind.BookDone))
            if (flagAchievement) add(SsBadge(R.string.ssum_badge_achievement, SsBadgeKind.Achievement))
            if (newRecord) add(SsBadge(R.string.ssum_badge_high_score, SsBadgeKind.HighScore))
            if (flagGoal) add(SsBadge(R.string.ssum_badge_goal, SsBadgeKind.Goal))
            if (isEmpty()) add(SsBadge(R.string.ssum_badge_default, SsBadgeKind.Default))
        }
    }
    val title = stringResource(
        when {
            quiet -> R.string.ssum_title_logged
            flagBook -> R.string.ssum_title_book
            flagAchievement -> R.string.ssum_title_achievement
            newRecord -> R.string.ssum_title_high_score
            flagGoal -> R.string.ssum_title_goal
            else -> R.string.ssum_title_complete
        }
    )

    val minutesSecStr = remember(session.activeSec) {
        String.format(Locale.US, "%02d:%02d", session.activeSec / 60, session.activeSec % 60)
    }
    val pph = remember(session.activeSec, session.pagesConfirmed) {
        if (session.activeSec > 0) (session.pagesConfirmed * 3600.0 / session.activeSec).roundToInt() else 0
    }

    // ---- record banner content (never negative; 6 localized variants, one picked per session) ----
    val res = LocalContext.current.resources
    val uiConfig = LocalConfiguration.current
    val bestXp = ssNumToLong(records.bestXp)
    val encouragement = remember(session.id, uiConfig, records) {
        ssEncouragement(
            res = res, seed = session.id,
            gapXp = ssNumToLong(records.gapToBestXp), bestXp = bestXp,
            currentXp = session.totalXpGained, last7Avg = ssNumToLong(records.last7AvgXp)
        )
    }
    val otherRecords = remember(records, flagged) {
        if (flagged) emptyList()
        else buildList {
            if (records.isLongestActiveTime) add(R.string.ssum_record_longest)
            if (records.isMostPagesConfirmed) add(R.string.ssum_record_pages)
            if (records.isBestDayXp) add(R.string.ssum_record_best_day)
        }
    }

    // ---- shared clock + fx engine ----
    val clock = remember(session.id) { mutableLongStateOf(if (reduceMotion) SS_DONE_MS else 0L) }
    val flashStart = remember(session.id) { mutableLongStateOf(-1L) }
    val fx = remember(session.id) { SessionFxEngine() }
    val popupRect = remember(session.id) { mutableStateOf(Rect.Zero) } // px, root coordinates

    // ---- main timeline: advances the clock, runs scheduled FX events, steps the particles ----
    LaunchedEffect(session.id) {
        if (reduceMotion) return@LaunchedEffect

        val popupDp = {
            val r = popupRect.value
            Rect(r.left / dens, r.top / dens, r.right / dens, r.bottom / dens)
        }
        val events = if (quiet) emptyList() else ssBuildTimeline(
            level = tier.level,
            fx = fx,
            popupDp = popupDp,
            recordBurst = newRecord,
            onFlash = { now -> flashStart.longValue = now }
        )

        var next = 0
        val startNanos = withFrameNanos { it }
        var lastNanos = startNanos
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
            lastNanos = now
            val elapsed = (now - startNanos) / 1_000_000L

            while (next < events.size && events[next].atMs <= elapsed) {
                events[next].action(elapsed)
                next++
            }
            fx.update(dt)
            clock.longValue = elapsed // read only in draw / graphicsLayer / tiny leaf composables

            if (elapsed > 6_000L && next >= events.size && !fx.isActive) break
        }
    }

    // ---- sound (pre-rendered buffer, started aligned with the clock) ----
    val trackHolder = remember { arrayOfNulls<AudioTrack>(1) }
    DisposableEffect(Unit) {
        onDispose {
            try {
                trackHolder[0]?.release()
            } catch (_: Exception) {
            }
            trackHolder[0] = null
        }
    }
    LaunchedEffect(session.id, soundEnabled) {
        if (!soundEnabled) return@LaunchedEffect
        val samples = withContext(Dispatchers.Default) {
            if (quiet) SessionSounds.buildQuiet() else SessionSounds.buildCelebration(tier.level)
        }
        if (isActive) {
            trackHolder[0] = SessionSounds.play(samples, if (reduceMotion) 0L else clock.longValue)
        }
    }

    // ---- infinite loops (rays spin, halo pulse, border spin, badge dot pulse) ----
    val transition = rememberInfiniteTransition(label = "ss-loops")
    val zero = remember { mutableFloatStateOf(0f) }
    val borderAngle: State<Float> = if (reduceMotion) zero else transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "border"
    )
    val raysAngle: State<Float> = if (reduceMotion) zero else transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "rays"
    )
    val haloPulse: State<Float> = if (reduceMotion) zero else transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = SsEaseInOut), RepeatMode.Reverse), label = "halo"
    )
    val dotPulse: State<Float> = if (reduceMotion) zero else transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = SsEaseInOut), RepeatMode.Reverse), label = "dot"
    )

    val popupWidth = minOf(LocalConfiguration.current.screenWidthDp * 0.94f, 420f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SsDialogWindowEffects()

        Box(Modifier.fillMaxSize()) {
            // overlay scrim (rgba(6,5,14,.6), fades in 400 ms) -- tap outside closes
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = SsEaseOut.transform(ssProgress(clock.longValue, 0, 400)) }
                    .background(Color(0xFF06050E).copy(alpha = 0.6f))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                SsPopupCard(
                    clock = clock,
                    quiet = quiet,
                    tier = tier,
                    title = title,
                    badges = badges,
                    timeStr = minutesSecStr,
                    pages = session.pagesConfirmed,
                    pph = pph,
                    xp = session.totalXpGained,
                    borderAngle = borderAngle,
                    raysAngle = raysAngle,
                    haloPulse = haloPulse,
                    dotPulse = dotPulse,
                    onBounds = { popupRect.value = it },
                    modifier = Modifier.width(popupWidth),
                    onDone = onDismiss,
                    onKeep = onKeepReading,
                    bookRow = {
                        SsBookRow(
                            clock = clock,
                            title = session.bookTitle,
                            author = session.bookAuthor,
                            coverUrl = session.coverUrl,
                            coverSlot = bookCover
                        )
                    },
                    banner = {
                        SsRecordBanner(
                            clock = clock,
                            glow = haloPulse,
                            newRecord = newRecord,
                            mainText = encouragement.first,
                            subText = encouragement.second,
                            chips = otherRecords
                        )
                    },
                    flaggedLine = {
                        if (flagged) {
                            Box(Modifier.ssFadeUp(clock, if (quiet) 300 else 1000).padding(bottom = 8.dp)) {
                                SsNote("ℹ️", stringResource(R.string.ssum_flagged_line))
                            }
                        }
                    },
                    details = {
                        SsDetailsPanel(
                            quiet = quiet,
                            reduceMotion = reduceMotion,
                            session = session,
                            book = book,
                            records = records,
                            bookEta = bookEta,
                            bookSessionCount = bookSessionCount,
                            paceMetrics = paceMetrics,
                            totalXp = todayXp,
                            todayDayXp = todayDayXp,
                            bestDayXp = bestDayXp,
                            dailyGoalMins = dailyGoalMins,
                            todayActiveMins = todayActiveMins,
                            streakDays = streakDays,
                            levelProgress = levelProgress,
                            unlockedThisSession = unlockedThisSession,
                            closestAchievements = closestAchievements,
                            recent7Sessions = recent7Sessions
                        )
                    }
                )
            }

            // MEGA full-screen flash (z 25)
            if (tier.level >= 5 && !reduceMotion) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val start = flashStart.longValue
                            alpha = if (start < 0L) 0f else {
                                val p = ssProgress(clock.longValue, start.toInt(), 850)
                                when {
                                    p <= 0f || p >= 1f -> 0f
                                    p < 0.18f -> SsEaseOutCss.transform(p / 0.18f)
                                    else -> 1f - SsEaseOutCss.transform((p - 0.18f) / 0.82f)
                                }
                            }
                        }
                        .drawBehind {
                            val c = Offset(size.width * 0.5f, size.height * 0.4f)
                            val r = sqrt((size.width * 0.5f).pow(2) + (size.height * 0.6f).pow(2))
                            drawRect(
                                Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0f to SsColors.Cyan.copy(alpha = 0.55f),
                                        0.45f to SsColors.Purple.copy(alpha = 0.2f),
                                        0.7f to SsColors.Purple.copy(alpha = 0f),
                                        1f to SsColors.Purple.copy(alpha = 0f)
                                    ),
                                    center = c, radius = r
                                )
                            )
                        }
                )
            }

            // confetti / sparkle / shockwave canvas (z 30, never intercepts touches)
            if (!reduceMotion && !quiet) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { fx.setSize(it.width / dens, it.height / dens) }
                ) {
                    clock.longValue // subscribe to frame ticks
                    scale(dens, dens, Offset.Zero) { fx.draw(this) }
                }
            }
        }
    }
}

/* ==========================================================================
 * FX TIMELINE (ports celebrate(), the score-complete burst and the rain interval)
 * ========================================================================== */
private class SsEvent(val atMs: Long, val action: (nowMs: Long) -> Unit)

private fun ssBuildTimeline(
    level: Int,
    fx: SessionFxEngine,
    popupDp: () -> Rect,
    recordBurst: Boolean,
    onFlash: (Long) -> Unit
): List<SsEvent> {
    val ev = ArrayList<SsEvent>()
    fun at(ms: Long, a: (Long) -> Unit) { ev.add(SsEvent(ms, a)) }

    val sc = sessionTierScale(level)
    val base = 280L // celebrate() fires 280 ms after open
    val upRight = -(PI / 2.6).toFloat()
    val upLeft = -(PI / 1.6).toFloat()
    val sideRight = -(PI / 4.5).toFloat()
    val sideLeft = -(PI / 1.35).toFloat()

    at(base) {
        val r = popupDp()
        fx.wave(r.center.x, r.center.y, SsColors.Cyan.copy(alpha = 0.9f), 520f)
        fx.cannon(r.left + 12f, r.bottom - 24f, upRight, sc.cannonCount, 780f)
        fx.cannon(r.right - 12f, r.bottom - 24f, upLeft, sc.cannonCount, 780f)
    }
    at(base + 140) {
        val r = popupDp()
        fx.wave(r.center.x, r.center.y, SsColors.Purple.copy(alpha = 0.8f), 620f)
    }
    at(base + 220) {
        val r = popupDp()
        fx.burst(r.center.x, r.top + 130f, sc.burstCount, 620f)
    }
    for (i in 0 until sc.sideCannons) {
        at(base + 340 + i * 90L) {
            val r = popupDp()
            val y = r.top + 40f + i * 30f
            fx.cannon(r.left + 6f, y, sideRight, sc.cannonCount, 640f)
            fx.cannon(r.right - 6f, y, sideLeft, sc.cannonCount, 640f)
        }
    }
    if (sc.sparkles > 0) {
        at(base + 500) {
            val r = popupDp()
            fx.sparkles(sc.sparkles)
            fx.wave(r.center.x, r.center.y, SsColors.Cyan.copy(alpha = 0.65f), 720f, 2.5f)
        }
    }
    if (sc.fireworks > 0) {
        val fwColors = listOf(SsColors.Cyan, SsColors.Purple, SsColors.CyanLight, SsColors.PurpleLight, SsColors.Cyan)
        for (i in 0 until sc.fireworks) {
            at(base + 640 + i * 130L) {
                val x = fx.width * (0.25f + Random.nextFloat() * 0.5f)
                val y = fx.height * (0.20f + Random.nextFloat() * 0.25f)
                fx.firework(x, y, fwColors[i % fwColors.size])
            }
        }
    }
    if (sc.mega) {
        at(base + 60) { now -> onFlash(now) }
        at(base + 700) {
            fx.cannon(0f, fx.height * 0.6f, -(PI / 2.8).toFloat(), 60, 900f, SsBrandColors)
            fx.cannon(fx.width, fx.height * 0.6f, -(PI / 1.55).toFloat(), 60, 900f, SsBrandColors)
        }
        at(base + 900) {
            val r = popupDp()
            fx.sparkles(150)
            fx.wave(r.center.x, r.center.y, SsColors.Cyan, 900f, 4f)
            fx.wave(r.center.x, r.center.y, SsColors.Purple.copy(alpha = 0.85f), 1100f, 3f)
        }
    }
    // rain: every 120 ms, (6 + 4 * level) times
    val rainTicks = 6 + level * 4
    for (k in 1..rainTicks) at(base + 120L * k) { fx.rain(2 + level) }

    // odometer finished (starts 420 ms, runs 1500 ms) -> burst above the score
    at(420L + 1500L) {
        val r = popupDp()
        fx.burst(r.center.x, r.top + 260f, 26, 380f)
    }

    // "New personal record!": short confetti + glow burst on the banner, right after the count-up lands
    if (recordBurst) {
        at(2050L) {
            val r = popupDp()
            fx.wave(r.center.x, r.top + 340f, SsColors.Purple.copy(alpha = 0.8f), 520f, 2.5f)
            fx.burst(r.center.x, r.top + 340f, 34, 460f)
            fx.sparkles(24)
        }
    }

    ev.sortBy { it.atMs }
    return ev
}

/* ==========================================================================
 * POPUP CARD
 * ========================================================================== */
private enum class SsBadgeKind { Default, HighScore, Goal, Achievement, BookDone }
private data class SsBadge(@StringRes val labelRes: Int, val kind: SsBadgeKind)

@Composable
private fun SsPopupCard(
    clock: MutableLongState,
    quiet: Boolean,
    tier: SessionTier,
    title: String,
    badges: List<SsBadge>,
    timeStr: String,
    pages: Int,
    pph: Int,
    xp: Long,
    borderAngle: State<Float>,
    raysAngle: State<Float>,
    haloPulse: State<Float>,
    dotPulse: State<Float>,
    onBounds: (Rect) -> Unit,
    modifier: Modifier,
    onDone: () -> Unit,
    onKeep: () -> Unit,
    bookRow: @Composable () -> Unit,
    banner: @Composable () -> Unit,
    flaggedLine: @Composable () -> Unit,
    details: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(350, easing = SsEaseSpring),
        label = "chevron"
    )

    val shape = RoundedCornerShape(if (quiet) 24.dp else 32.dp)
    val bg = if (quiet) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(24, 21, 45).copy(alpha = 0.96f),
                1f to Color(14, 12, 30).copy(alpha = 0.98f)
            )
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(30, 26, 60).copy(alpha = 0.97f),
                0.55f to Color(18, 15, 40).copy(alpha = 0.98f),
                1f to Color(10, 8, 26)
            )
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .ssPopupEntrance(clock, quiet)
            .shadow(
                elevation = if (quiet) 18.dp else 30.dp,
                shape = shape,
                ambientColor = SsColors.Cyan.copy(alpha = 0.35f),
                spotColor = SsColors.Purple.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(bg)
            .pointerInput(Unit) { detectTapGestures { } } // swallow taps so the scrim click doesn't close
            .drawWithContent {
                drawContent()
                val corner = (if (quiet) 24.dp else 32.dp).toPx()
                if (quiet) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
                        size = Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                        cornerRadius = CornerRadius(corner),
                        style = Stroke(1.dp.toPx())
                    )
                } else {
                    // card-shine: one diagonal sweep, 1.3 s, starts at 0.5 s
                    val p = SsEaseOut.transform(ssProgress(clock.longValue, 500, 1300))
                    if (p in 0.001f..0.999f) {
                        val rw = size.width * 0.45f
                        val rh = size.height * 2.4f
                        val left = ssLerp(-size.width * 0.7f, size.width * 1.45f, p)
                        val top = -size.height * 0.7f
                        rotate(18f, Offset(left + rw / 2f, top + rh / 2f)) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.White.copy(alpha = 0f),
                                        0.5f to Color.White.copy(alpha = 0.14f),
                                        1f to Color.White.copy(alpha = 0f)
                                    ),
                                    startX = left, endX = left + rw
                                ),
                                topLeft = Offset(left, top),
                                size = Size(rw, rh)
                            )
                        }
                    }
                    // card-border: 1.5 px conic gradient ring spinning once per 7 s
                    val inset = 0.75.dp.toPx()
                    drawRoundRect(
                        brush = SsSpinningBorderBrush(borderAngle.value),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2f, size.height - inset * 2f),
                        cornerRadius = CornerRadius(corner - inset),
                        style = Stroke(1.5.dp.toPx())
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (quiet) 24.dp else 26.dp,
                    end = if (quiet) 24.dp else 26.dp,
                    top = if (quiet) 24.dp else 30.dp,
                    bottom = if (quiet) 20.dp else 22.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (quiet) {
                SsQuietHeader()
                Spacer(Modifier.height(16.dp))
                SsQuietSummary(clock = clock, pages = pages)
                Spacer(Modifier.height(18.dp)) // 4 px summary margin + 14 px empty-badges margin
            } else {
                SsHero(clock = clock, raysAngle = raysAngle, haloPulse = haloPulse)
                Spacer(Modifier.height(4.dp))

                // badges
                Spacer(Modifier.height(14.dp))
                SsBadges(badges = badges, clock = clock, dotPulse = dotPulse)
                Spacer(Modifier.height(12.dp))

                // title
                Text(
                    text = title,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.White,
                                0.2f to Color.White,
                                1f to Color(0xFFC7BCF0)
                            )
                        ),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = ssTrack(-0.025f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.ssFadeUp(clock, 530)
                )
                Spacer(Modifier.height(16.dp))

                // XP score
                SsXpStage(clock = clock, xp = xp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.ssum_experience_earned).uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = ssTrack(0.22f),
                    color = SsColors.TextDimmer,
                    modifier = Modifier.ssFadeUp(clock, 680)
                )
                Spacer(Modifier.height(8.dp))

                SsTierPill(tier = tier, clock = clock)
                Spacer(Modifier.height(14.dp))
                bookRow()
                banner()
            }
            flaggedLine()
            Spacer(Modifier.height(if (quiet) 0.dp else 4.dp))

            // stats row
            SsStats(
                clock = clock, quiet = quiet,
                timeStr = timeStr, pages = pages, pph = pph
            )
            Spacer(Modifier.height(6.dp))

            // expand toggle
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .ssFadeUp(clock, if (quiet) 320 else 880)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = (if (expanded) stringResource(R.string.session_summary_collapse_details)
                    else stringResource(R.string.session_summary_expand_details)).uppercase(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = ssTrack(0.1f),
                    color = SsColors.TextDim
                )
                Canvas(
                    Modifier
                        .size(12.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                ) {
                    val s = size.width / 24f
                    val p = Path().apply {
                        moveTo(6f * s, 9f * s); lineTo(12f * s, 15f * s); lineTo(18f * s, 9f * s)
                    }
                    drawPath(
                        p, SsColors.TextDim,
                        style = Stroke(2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(500, easing = SsEaseOut)) + fadeIn(tween(350)),
                exit = shrinkVertically(tween(500, easing = SsEaseOut)) + fadeOut(tween(350))
            ) {
                Column(Modifier.padding(top = 4.dp)) { details() }
            }

            // actions
            Spacer(Modifier.height(if (quiet) 14.dp else 18.dp))
            val stacked = LocalConfiguration.current.screenWidthDp <= SS_STACK_BUTTONS_BELOW_DP
            val actionsFade = Modifier.ssFadeUp(clock, if (quiet) 400 else 980)
            val doneText = stringResource(R.string.session_summary_done)
            val keepText = stringResource(R.string.ssum_keep_reading_arrow)
            if (stacked) {
                Column(
                    modifier = actionsFade.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SsButton(doneText, primary = false, onClick = onDone, modifier = Modifier.fillMaxWidth())
                    SsButton(keepText, primary = true, onClick = onKeep, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = actionsFade.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SsButton(doneText, primary = false, onClick = onDone, modifier = Modifier.weight(1f))
                    SsButton(keepText, primary = true, onClick = onKeep, modifier = Modifier.weight(1.35f))
                }
            }
        }
    }
}

/** Conic-gradient border from the HTML (.card-border), rotated by [angleDeg]. */
private class SsSpinningBorderBrush(private val angleDeg: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val cyan0 = SsColors.Cyan.copy(alpha = 0f).toArgb()
        val purple0 = SsColors.Purple.copy(alpha = 0f).toArgb()
        val colors = intArrayOf(
            cyan0,
            SsColors.Cyan.copy(alpha = 0.95f).toArgb(),
            SsColors.Cyan.copy(alpha = 0.15f).toArgb(),
            cyan0,
            purple0,
            SsColors.Purple.copy(alpha = 0.9f).toArgb(),
            purple0,
            purple0
        )
        val positions = floatArrayOf(0f, 30f / 360f, 70f / 360f, 120f / 360f, 200f / 360f, 240f / 360f, 290f / 360f, 1f)
        val sweep = android.graphics.SweepGradient(cx, cy, colors, positions)
        val m = android.graphics.Matrix()
        m.setRotate(angleDeg - 90f, cx, cy) // CSS conic starts at 12 o'clock
        sweep.setLocalMatrix(m)
        return sweep
    }
}

/* ==========================================================================
 * HERO: rays, halo, progress ring, trophy
 * ========================================================================== */
private val SsRayPath: Path by lazy {
    // 24 thin wedges (13deg..15deg of every 15deg), unit radius, angle measured from 12 o'clock
    Path().apply {
        for (i in 0 until 24) {
            val a1 = Math.toRadians((i * 15 + 13 - 90).toDouble())
            val a2 = Math.toRadians((i * 15 + 15 - 90).toDouble())
            moveTo(0f, 0f)
            lineTo(kotlin.math.cos(a1).toFloat(), kotlin.math.sin(a1).toFloat())
            lineTo(kotlin.math.cos(a2).toFloat(), kotlin.math.sin(a2).toFloat())
            close()
        }
    }
}

private class SsTrophyPaths {
    private fun p(d: String): Path = PathParser().parsePathString(d).toPath()
    val cup = p("M28 18 L72 18 L68 48 Q50 60 32 48 Z")
    val handleL = p("M28 24 Q10 24 10 40 Q10 54 32 54")
    val handleR = p("M72 24 Q90 24 90 40 Q90 54 68 54")
    val base = p("M30 68 L70 68 L74 82 Q74 86 70 86 L30 86 Q26 86 26 82 Z")
    val star = p("M50 27 L53.5 36 L63 36 L55.5 42 L58 51 L50 46 L42 51 L44.5 42 L37 36 L46.5 36 Z")
    val shine = p("M34 22 Q38 32 36 44")
    val cupB = cup.getBounds()
    val handleLB = handleL.getBounds()
    val handleRB = handleR.getBounds()
    val baseB = base.getBounds()
}

private fun ssBrandGrad(b: Rect): Brush = Brush.linearGradient(
    colorStops = arrayOf(0f to SsColors.CyanLight, 0.5f to SsColors.Cyan, 1f to SsColors.Purple),
    start = Offset(b.left, b.top),
    end = Offset(b.left + 0.4f * b.width, b.bottom)
)

private fun ssBrandGrad2(b: Rect): Brush = Brush.linearGradient(
    colors = listOf(SsColors.Cyan, SsColors.PurpleDark),
    start = Offset(b.left, b.top),
    end = Offset(b.left, b.bottom)
)

@Composable
private fun SsHero(clock: MutableLongState, raysAngle: State<Float>, haloPulse: State<Float>) {
    val trophy = remember { SsTrophyPaths() }
    Box(Modifier.size(118.dp), contentAlignment = Alignment.Center) {

        // rays (inset -80% => 306 dp box), radial mask 8% -> 62%, spin 28 s
        Canvas(
            Modifier
                .requiredSize(306.dp)
                .graphicsLayer { alpha = SsEaseCss.transform(ssProgress(clock.longValue, 300, 1000)) }
        ) {
            val radius = size.width / 2f
            val ray = SsColors.Cyan.copy(alpha = 0.22f)
            val rayClear = SsColors.Cyan.copy(alpha = 0f)
            withTransform({
                translate(center.x, center.y)
                rotate(raysAngle.value, Offset.Zero)
                scale(radius, radius, Offset.Zero)
            }) {
                drawPath(
                    path = SsRayPath,
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(0f to ray, 0.08f to ray, 0.62f to rayClear, 1f to rayClear),
                        center = Offset.Zero,
                        radius = 1.4142f // farthest-corner
                    )
                )
            }
        }

        // halo (pulse .9 <-> 1.18, opacity .6 <-> 1, 2.8 s)
        Canvas(
            Modifier
                .size(141.6.dp)
                .graphicsLayer {
                    val enter = SsEaseCss.transform(ssProgress(clock.longValue, 400, 800))
                    val v = haloPulse.value
                    val s = 0.9f + 0.28f * v
                    scaleX = s
                    scaleY = s
                    alpha = enter * (0.6f + 0.4f * v)
                }
        ) {
            val r = size.width / 2f * 1.4142f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to SsColors.Cyan.copy(alpha = 0.45f),
                        0.45f to SsColors.Purple.copy(alpha = 0.15f),
                        0.68f to SsColors.Purple.copy(alpha = 0f),
                        1f to SsColors.Purple.copy(alpha = 0f)
                    ),
                    center = center, radius = r
                ),
                radius = size.width / 2f
            )
        }

        // progress ring (draws 0 -> 82% over 1.6 s, starts 0.6 s)
        Canvas(
            Modifier
                .size(118.dp)
                .graphicsLayer { alpha = SsEaseCss.transform(ssProgress(clock.longValue, 350, 800)) }
        ) {
            val s = size.width / 120f
            val c = center
            val r = 54f * s
            drawCircle(
                color = SsColors.Cyan.copy(alpha = 0.12f), radius = r, center = c,
                style = Stroke(1.5f * s)
            )
            val p = SsEaseOut.transform(ssProgress(clock.longValue, 600, 1600))
            val dashOffset = ssLerp(339.292f, 60f, p)
            val sweep = (1f - dashOffset / 339.292f) * 360f
            if (sweep > 0.1f) {
                val topLeft = Offset(c.x - r, c.y - r)
                val arcSize = Size(2f * r, 2f * r)
                val grad = Brush.linearGradient(
                    colorStops = arrayOf(0f to SsColors.Cyan, 0.55f to SsColors.Sky, 1f to SsColors.Purple),
                    start = Offset.Zero, end = Offset(size.width, size.height)
                )
                rotate(-90f, c) {
                    drawArc(
                        color = SsColors.Cyan.copy(alpha = 0.16f), startAngle = 0f, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(9f * s, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = SsColors.Cyan.copy(alpha = 0.28f), startAngle = 0f, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(5f * s, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = grad, startAngle = 0f, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(2.5f * s, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // trophy (pop: scale 0 -> 1.18 -> 1, rotate -32 -> 8 -> 0, 0.95 s spring, delay 0.15 s)
        Canvas(
            Modifier
                .size(82.dp)
                .graphicsLayer {
                    val p = ssProgress(clock.longValue, 150, 950)
                    val sc: Float
                    val rot: Float
                    val al: Float
                    if (p < 0.6f) {
                        val u = SsEaseSpring.transform(p / 0.6f)
                        sc = ssLerp(0f, 1.18f, u)
                        rot = ssLerp(-32f, 8f, u)
                        al = u.coerceIn(0f, 1f)
                    } else {
                        val u = SsEaseSpring.transform((p - 0.6f) / 0.4f)
                        sc = ssLerp(1.18f, 1f, u)
                        rot = ssLerp(8f, 0f, u)
                        al = 1f
                    }
                    scaleX = max(sc, 0f)
                    scaleY = max(sc, 0f)
                    rotationZ = rot
                    alpha = al
                }
        ) {
            val s = size.width / 100f
            scale(s, s, Offset.Zero) {
                // drop-shadow glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SsColors.Cyan.copy(alpha = 0.42f), SsColors.Cyan.copy(alpha = 0f)),
                        center = Offset(50f, 60f), radius = 62f
                    ),
                    radius = 62f, center = Offset(50f, 60f)
                )
                val handleStroke = Stroke(6f, cap = StrokeCap.Round)
                drawPath(trophy.cup, ssBrandGrad(trophy.cupB))
                drawPath(trophy.handleL, ssBrandGrad2(trophy.handleLB), style = handleStroke)
                drawPath(trophy.handleR, ssBrandGrad2(trophy.handleRB), style = handleStroke)
                drawRect(
                    brush = ssBrandGrad2(Rect(45f, 56f, 55f, 68f)),
                    topLeft = Offset(45f, 56f), size = Size(10f, 12f)
                )
                drawPath(trophy.base, ssBrandGrad(trophy.baseB))
                drawPath(trophy.star, Color.White, alpha = 0.95f)
                drawPath(
                    trophy.shine, Color.White, alpha = 0.45f,
                    style = Stroke(2.5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/* ==========================================================================
 * BADGES
 * ========================================================================== */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SsBadges(badges: List<SsBadge>, clock: MutableLongState, dotPulse: State<Float>) {
    FlowRow(
        modifier = Modifier.ssFadeUp(clock, 450),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        badges.forEachIndexed { i, b ->
            val (fg, bgc, bd) = when (b.kind) {
                SsBadgeKind.Default -> Triple(SsColors.Cyan, SsColors.Cyan.copy(alpha = 0.10f), SsColors.Cyan.copy(alpha = 0.30f))
                SsBadgeKind.HighScore -> Triple(SsColors.PurpleLight, SsColors.Purple.copy(alpha = 0.12f), SsColors.Purple.copy(alpha = 0.35f))
                SsBadgeKind.Goal -> Triple(SsColors.CyanLight, SsColors.Cyan.copy(alpha = 0.12f), SsColors.Cyan.copy(alpha = 0.35f))
                SsBadgeKind.Achievement -> Triple(SsColors.Lavender, SsColors.PurpleLight.copy(alpha = 0.12f), SsColors.PurpleLight.copy(alpha = 0.35f))
                SsBadgeKind.BookDone -> Triple(SsColors.Purple, SsColors.Purple.copy(alpha = 0.12f), SsColors.Purple.copy(alpha = 0.38f))
            }
            val pill = RoundedCornerShape(100.dp)
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        // badgePop: .5 s spring, stagger 60 ms
                        val u = SsEaseSpring.transform(ssProgress(clock.longValue, (i * 60), 500))
                        alpha = u.coerceIn(0f, 1f)
                        val s = ssLerp(0.6f, 1f, u)
                        scaleX = s
                        scaleY = s
                        translationY = ssLerp(6.dp.toPx(), 0f, u)
                    }
                    .clip(pill)
                    .background(bgc)
                    .border(1.dp, bd, pill)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Canvas(
                    Modifier
                        .size(5.dp)
                        .graphicsLayer {
                            val v = dotPulse.value
                            val s = 1f - 0.25f * v
                            scaleX = s
                            scaleY = s
                            alpha = 1f - 0.5f * v
                        }
                ) {
                    drawCircle(fg.copy(alpha = 0.35f), radius = size.width * 1.4f)
                    drawCircle(fg)
                }
                Text(
                    text = stringResource(b.labelRes).uppercase(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = ssTrack(0.14f),
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/* ==========================================================================
 * XP SCORE (odometer + "+" fly-up + suffix + bump)
 * ========================================================================== */
@Composable
private fun SsXpStage(clock: MutableLongState, xp: Long) {
    Box(
        modifier = Modifier.ssFadeUp(clock, 610),
        contentAlignment = Alignment.Center
    ) {
        // .xp-plus: 1.2 s ease-out, scale .4 -> 1.6, translateY -50% -> -180%
        Text(
            text = "+",
            style = TextStyle(
                fontSize = 44.sp, fontWeight = FontWeight.Black,
                letterSpacing = ssTrack(-0.05f), color = SsColors.Cyan.copy(alpha = 0.3f)
            ),
            modifier = Modifier.graphicsLayer {
                val p = ssProgress(clock.longValue, 0, 1200)
                val op = when {
                    p <= 0f || p >= 1f -> 0f
                    p < 0.3f -> 0.9f * SsEaseOutCss.transform(p / 0.3f)
                    else -> 0.9f * (1f - SsEaseOutCss.transform((p - 0.3f) / 0.7f))
                }
                val tr = SsEaseOutCss.transform(p)
                val s = ssLerp(0.4f, 1.6f, tr)
                scaleX = s
                scaleY = s
                translationY = -1.3f * size.height * tr
                alpha = op
            }
        )
        Row(verticalAlignment = Alignment.Top) {
            SsOdometer(
                clock = clock, target = xp,
                modifier = Modifier.graphicsLayer {
                    // .score.bump: .55 s spring when the roll lands (420 + 1500 ms)
                    val bp = ssProgress(clock.longValue, 1920, 550)
                    val s = when {
                        bp <= 0f || bp >= 1f -> 1f
                        bp < 0.4f -> ssLerp(1f, 1.14f, SsEaseSpring.transform(bp / 0.4f))
                        else -> ssLerp(1.14f, 1f, SsEaseSpring.transform((bp - 0.4f) / 0.6f))
                    }
                    scaleX = s
                    scaleY = s
                }
            )
            Text(
                text = stringResource(R.string.ssum_xp_unit),
                style = TextStyle(
                    fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = ssTrack(0.06f),
                    color = SsColors.CyanLight,
                    shadow = Shadow(SsColors.Cyan.copy(alpha = 0.7f), Offset.Zero, 18f)
                ),
                modifier = Modifier
                    .padding(start = 6.dp, top = 6.dp)
                    .graphicsLayer {
                        // suffixIn: .6 s spring, delay 1.3 s
                        val u = SsEaseSpring.transform(ssProgress(clock.longValue, 1300, 600))
                        alpha = u.coerceIn(0f, 1f)
                        val s = ssLerp(0.4f, 1f, u)
                        scaleX = s
                        scaleY = s
                        translationX = ssLerp(-10.dp.toPx(), 0f, u)
                    }
            )
        }
    }
}

/** Slot-machine count-up: digits scramble, then lock left-to-right (1.5 s, starts 420 ms). */
@Composable
private fun SsOdometer(clock: MutableLongState, target: Long, modifier: Modifier = Modifier) {
    val targetStr = remember(target) {
        NumberFormat.getIntegerInstance(Locale.US).format(target)
    }
    val t by remember { derivedStateOf { ssProgress(clock.longValue, 420, 1500) } }
    val text = when {
        t <= 0f -> "0"
        t >= 1f -> targetStr
        else -> {
            val eased = 1f - (1f - t).pow(3.2f)
            buildString {
                targetStr.forEachIndexed { i, ch ->
                    if (ch == ',') {
                        append(ch)
                    } else {
                        val lockStart = (i.toFloat() / targetStr.length) * 0.72f
                        val dp = ((eased - lockStart) / 0.28f).coerceIn(0f, 1f)
                        append(if (dp >= 1f) ch else ('0' + Random.nextInt(10)))
                    }
                }
            }
        }
    }
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,
        style = TextStyle(
            brush = Brush.linearGradient(listOf(SsColors.Cyan, SsColors.Purple)),
            fontSize = 58.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = ssTrack(-0.045f),
            fontFeatureSettings = "tnum",
            shadow = Shadow(SsColors.Cyan.copy(alpha = 0.45f), Offset(0f, 14f), 40f)
        )
    )
}

@Composable
private fun SsTierPill(tier: SessionTier, clock: MutableLongState) {
    val shape = RoundedCornerShape(100.dp)
    val (fill, border, textColor) = when (tier.level) {
        3 -> Triple(
            Brush.linearGradient(listOf(SsColors.Cyan.copy(alpha = 0.15f), SsColors.Purple.copy(alpha = 0.15f))),
            SsColors.Purple.copy(alpha = 0.5f), SsColors.Lavender
        )
        4, 5 -> Triple(
            Brush.linearGradient(listOf(SsColors.Cyan.copy(alpha = 0.2f), SsColors.Purple.copy(alpha = 0.25f))),
            SsColors.PurpleLight.copy(alpha = 0.65f), Color.White
        )
        else -> Triple(
            SolidColor(SsColors.Cyan.copy(alpha = 0.1f)),
            SsColors.Cyan.copy(alpha = 0.35f), SsColors.CyanLight
        )
    }
    val glow = if (tier.level >= 4) {
        Modifier.shadow(
            10.dp, shape,
            ambientColor = SsColors.Purple.copy(alpha = 0.7f),
            spotColor = SsColors.Purple.copy(alpha = 0.7f)
        )
    } else Modifier
    Box(
        modifier = Modifier
            .ssFadeUp(clock, 1500, 500)
            .then(glow)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = ("✦ " + stringResource(ssTierLabelRes(tier.level))).uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = ssTrack(0.14f),
            color = textColor
        )
    }
}

/* ==========================================================================
 * QUIET MODE PIECES
 * ========================================================================== */
@Composable
private fun SsQuietHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(SsColors.Cyan.copy(alpha = 0.15f), SsColors.Purple.copy(alpha = 0.15f))
                    )
                )
                .border(1.5.dp, SsColors.Cyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(22.dp)) {
                val s = size.width / 24f
                val p = Path().apply {
                    moveTo(20f * s, 6f * s); lineTo(9f * s, 17f * s); lineTo(4f * s, 12f * s)
                }
                drawPath(p, SsColors.CyanLight, style = Stroke(3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Column {
            Text(
                text = stringResource(R.string.ssum_quiet_kicker).uppercase(),
                fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = ssTrack(0.18f), color = SsColors.TextDimmer
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ssum_quiet_title),
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = ssTrack(-0.02f), color = Color.White
            )
        }
    }
}

@Composable
private fun SsQuietSummary(clock: MutableLongState, pages: Int) {
    val t by remember { derivedStateOf { ssProgress(clock.longValue, 0, 700) } }
    val shown = (pages * (1f - (1f - t).pow(3f))).roundToInt()
    Column(
        modifier = Modifier
            .ssFadeUp(clock, 150, 500)
            .padding(top = 16.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ssum_pages_read).uppercase(),
            fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
            letterSpacing = ssTrack(0.22f), color = SsColors.TextDimmer
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = shown.toString(),
                fontSize = 46.sp, fontWeight = FontWeight.Black,
                letterSpacing = ssTrack(-0.04f), color = Color.White,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Text(
                text = stringResource(R.string.ssum_pages_unit),
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = ssTrack(0.06f), color = SsColors.TextDim,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }
    }
}

/* ==========================================================================
 * STATS ROW
 * ========================================================================== */
@Composable
private fun SsStats(
    clock: MutableLongState,
    quiet: Boolean,
    timeStr: String,
    pages: Int,
    pph: Int
) {
    val line = Color.White.copy(alpha = if (quiet) 0.06f else 0.07f)
    val sep = Color.White.copy(alpha = 0.06f)
    val vPad = if (quiet) 12.dp else 14.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ssFadeUp(clock, if (quiet) 250 else 780)
            .drawBehind {
                drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(vertical = vPad)
    ) {
        SsStat(timeStr, stringResource(R.string.ssum_stat_time).uppercase(), sep, first = true, modifier = Modifier.weight(1f))
        SsStat(pages.toString(), stringResource(R.string.ssum_stat_pages).uppercase(), sep, first = false, modifier = Modifier.weight(1f))
        SsStat(pph.toString(), stringResource(R.string.ssum_stat_pph).uppercase(), sep, first = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SsStat(value: String, label: String, sep: Color, first: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.drawBehind {
            if (!first) drawLine(sep, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = ssTrack(-0.02f), color = Color.White,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
        Text(
            text = label,
            fontSize = 9.5.sp, fontWeight = FontWeight.Bold,
            letterSpacing = ssTrack(0.14f), color = SsColors.TextDimmer
        )
    }
}

/* ==========================================================================
 * BUTTONS
 * ========================================================================== */
@Composable
private fun SsButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) (if (primary) 0.975f else 0.98f) else 1f,
        animationSpec = tween(220, easing = SsEaseOut),
        label = "btn"
    )
    val shape = RoundedCornerShape(16.dp)
    val fill: Brush = if (primary) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color(0xFF00D9D9),
                0.3f to SsColors.Cyan,
                1f to SsColors.Purple
            )
        )
    } else SolidColor(Color.White.copy(alpha = 0.06f))

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .then(
                if (primary) Modifier.shadow(
                    14.dp, shape,
                    ambientColor = SsColors.Cyan.copy(alpha = 0.6f),
                    spotColor = SsColors.Cyan.copy(alpha = 0.9f)
                ) else Modifier
            )
            .clip(shape)
            .background(fill)
            .then(if (!primary) Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (primary) Color.White else Color(0xFFD9D2F5),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ==========================================================================
 * DETAILS PANEL (HTML .details-inner look; keeps every metric of the old dialog)
 * ========================================================================== */
@Composable
private fun SsDetailsPanel(
    quiet: Boolean,
    reduceMotion: Boolean,
    session: ReadingSessionEntity,
    book: Book?,
    records: SessionRecords,
    bookEta: BookEtaResult,
    bookSessionCount: Int,
    paceMetrics: PaceMetrics,
    totalXp: Long,
    todayDayXp: Long,
    bestDayXp: Long,
    dailyGoalMins: Int,
    todayActiveMins: Int,
    streakDays: Int,
    levelProgress: LevelingSystem.ProgressData,
    unlockedThisSession: List<Achievement>,
    closestAchievements: List<Achievement>,
    recent7Sessions: List<ReadingSessionEntity>
) {
    val res = LocalContext.current.resources
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(
                1.dp,
                if (quiet) Color.White.copy(alpha = 0.08f) else SsColors.Cyan.copy(alpha = 0.12f),
                shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // 1. estimated finish ("not enough data" below 3 sessions of this book)
        val etaLabel = stringResource(R.string.session_summary_est_finish)
        if (sessionEtaHasEnoughData(bookSessionCount) && bookEta is BookEtaResult.Calculated) {
            val formattedDate = bookEta.projectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            SsRow(
                etaLabel,
                stringResource(R.string.session_summary_hours_left, bookEta.hoursRemaining, formattedDate),
                valueColor = SsColors.Cyan
            )
        } else {
            SsRow(etaLabel, stringResource(R.string.session_summary_not_enough_data), valueColor = SsColors.TextDim)
        }

        // 2. book progress
        if (book != null) {
            val pagesLeft = maxOf(0, book.totalPages - (book.totalPages * book.percentProgress / 100))
            SsRow(
                stringResource(R.string.session_summary_book_progress),
                res.getString(
                    R.string.ssum_progress_value,
                    ssNum(book.percentProgress),
                    res.getQuantityString(R.plurals.ssum_pages_left, pagesLeft, ssNum(pagesLeft))
                ),
                divider = false
            )
            SsBar(book.percentProgress / 100f)
            Spacer(Modifier.height(4.dp))
        }

        // 3. pace (+ delta vs the previous session)
        SsSectionTitle(stringResource(R.string.ssum_section_pace))
        SsRow(
            stringResource(R.string.ssum_pages_per_hour),
            String.format(Locale.US, "%.1f", paceMetrics.pagesPerHour),
            delta = paceMetrics.pagesPerHourDelta
        )
        SsRow(stringResource(R.string.ssum_avg_time_per_page), ssFormatSecPerPage(res, paceMetrics.secPerPage))
        SsRow(
            stringResource(R.string.ssum_xp_per_hour),
            String.format(Locale.US, "%.0f", paceMetrics.xpPerHour),
            delta = paceMetrics.xpPerHourDelta,
            divider = false
        )

        // 4. today vs goal (ring) + today vs best day
        SsSectionTitle(stringResource(R.string.ssum_section_today))
        SsGoalRow(
            label = stringResource(R.string.session_summary_today_vs_goal),
            value = res.getString(R.string.ssum_today_value, ssNum(todayActiveMins), ssNum(dailyGoalMins)),
            fraction = if (dailyGoalMins > 0) (todayActiveMins.toFloat() / dailyGoalMins).coerceIn(0f, 1f) else 0f,
            reduceMotion = reduceMotion
        )
        if (bestDayXp > 0) {
            SsRow(
                stringResource(R.string.ssum_best_day),
                res.getString(R.string.ssum_xp_of_xp, ssNum(todayDayXp), ssNum(bestDayXp)),
                divider = false
            )
            SsBar((todayDayXp.toFloat() / bestDayXp).coerceIn(0f, 1f))
            Spacer(Modifier.height(4.dp))
        }

        // 5. streak + consistency bonus (XP only, never the formula) + days to next milestone
        SsSectionTitle(stringResource(R.string.ssum_section_streak))
        SsRow(
            stringResource(R.string.ssum_streak),
            "🔥 " + res.getQuantityString(R.plurals.ssum_days, streakDays, ssNum(streakDays)),
            valueColor = SsColors.Cyan
        )
        if (session.xpBonus > 0) {
            SsRow(
                stringResource(R.string.ssum_consistency_bonus),
                res.getString(R.string.ssum_xp_gain, ssNum(session.xpBonus)),
                valueColor = SsColors.CyanLight
            )
        }
        daysToNextStreakMilestone(streakDays)?.let { left ->
            SsRow(
                stringResource(R.string.ssum_next_milestone),
                res.getQuantityString(R.plurals.ssum_days, left, ssNum(left)),
                divider = false
            )
        }

        // 6. level progress
        SsSectionTitle(stringResource(R.string.session_summary_level_progress))
        SsRow(
            stringResource(R.string.ssum_level),
            res.getString(R.string.ssum_level_value, ssNum(levelProgress.level), levelProgress.title),
            valueColor = SsColors.PurpleLight,
            divider = false
        )
        SsBar(levelProgress.progressFraction)
        Text(
            text = if (levelProgress.isMax) stringResource(R.string.ssum_max_level)
            else res.getString(
                R.string.ssum_xp_to_next,
                ssNum(levelProgress.xpNeededInLevel - levelProgress.xpInLevel)
            ),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDimmer,
            modifier = Modifier.padding(top = 5.dp, bottom = 4.dp)
        )
        SsRow(
            stringResource(R.string.ssum_total_xp),
            res.getString(R.string.ssum_xp_amount, ssNum(totalXp)),
            divider = false
        )

        // 7. achievements: unlocked this session + the 2 closest
        if (unlockedThisSession.isNotEmpty() || closestAchievements.isNotEmpty()) {
            SsSectionTitle(stringResource(R.string.session_summary_achievements))
            unlockedThisSession.forEach { ach ->
                Text(
                    res.getString(R.string.ssum_ach_unlocked, ach.title, ach.rewardName),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SsColors.CyanLight,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
            closestAchievements.take(2).forEach { ach ->
                Text(
                    res.getString(R.string.ssum_ach_closest, ach.title, ach.current.toString(), ach.target.toString()),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDim,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }

        // 8. 7-session XP sparkline, best session marked
        if (recent7Sessions.isNotEmpty()) {
            SsSectionTitle(stringResource(R.string.session_summary_recent_sessions))
            SsSparkline(
                sessions = recent7Sessions.take(7).reversed(), // oldest -> newest, current session last
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFD700)))
                Text(
                    stringResource(R.string.ssum_best_session),
                    fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDimmer
                )
            }
        }

        // 9. XP breakdown: base / consistency bonus / achievements (amounts only)
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .drawBehind {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                }
                .padding(top = 10.dp)
        ) {
            Text(
                stringResource(R.string.ssum_xp_breakdown).uppercase(),
                fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = ssTrack(0.18f), color = SsColors.TextDimmer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SsXpLine(stringResource(R.string.ssum_xp_reading), res.getString(R.string.ssum_xp_gain, ssNum(session.xpBase)))
            if (session.xpBonus > 0) {
                SsXpLine("🔥 " + stringResource(R.string.ssum_consistency_bonus), res.getString(R.string.ssum_xp_gain, ssNum(session.xpBonus)))
            }
            if (session.xpAchievements > 0) {
                SsXpLine("🏅 " + stringResource(R.string.ssum_xp_achievements), res.getString(R.string.ssum_xp_gain, ssNum(session.xpAchievements)))
            }
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, 0f), end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                        )
                    }
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ssum_xp_total), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    res.getString(R.string.ssum_xp_gain, ssNum(session.totalXpGained)),
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(SsColors.CyanLight, SsColors.PurpleLight)),
                        fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"
                    )
                )
            }
        }

        // quiet log note (no threshold disclosed)
        if (quiet) SsNote("⏱", stringResource(R.string.ssum_quiet_note))
    }
}

private fun ssFormatSecPerPage(res: Resources, secPerPage: Double): String {
    val tpp = secPerPage.roundToInt()
    return if (tpp >= 60) res.getString(R.string.ssum_fmt_min_sec, ssNum(tpp / 60), ssNum(tpp % 60))
    else res.getString(R.string.ssum_fmt_sec, ssNum(tpp))
}

/** Label + value on the left, animated progress ring on the right (today vs daily goal). */
@Composable
private fun SsGoalRow(label: String, value: String, fraction: Float, reduceMotion: Boolean) {
    val anim = remember { Animatable(if (reduceMotion) fraction else 0f) }
    LaunchedEffect(fraction, reduceMotion) {
        if (reduceMotion) anim.snapTo(fraction) else anim.animateTo(fraction, tween(900, easing = SsEaseOut))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDimmer)
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, style = TextStyle(fontFeatureSettings = "tnum"))
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Color.White.copy(alpha = 0.08f), startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke)
                )
                if (anim.value > 0.001f) {
                    drawArc(
                        brush = Brush.linearGradient(
                            listOf(SsColors.Cyan, SsColors.Purple),
                            start = Offset.Zero, end = Offset(size.width, size.height)
                        ),
                        startAngle = -90f, sweepAngle = 360f * anim.value, useCenter = false,
                        topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                String.format(Locale.US, "%d%%", (anim.value * 100f).roundToInt()),
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, style = TextStyle(fontFeatureSettings = "tnum")
            )
        }
    }
}

@Composable
private fun SsRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    delta: Double? = null,
    divider: Boolean = true,
    valueMaxLines: Int = Int.MAX_VALUE
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.ssDashedBottom() else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = ssTrack(0.02f), color = SsColors.TextDimmer
        )
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = valueColor,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = valueMaxLines, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            if (delta != null && delta != 0.0) {
                Text(
                    text = if (delta > 0) " ↑" else " ↓",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (delta > 0) SsColors.CyanLight else Color(0xFFFF8FA3)
                )
            }
        }
    }
}

@Composable
private fun SsBar(fraction: Float) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(SsColors.Cyan, SsColors.Purple)))
        )
    }
}

@Composable
private fun SsSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = ssTrack(0.18f), color = SsColors.TextDimmer,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
}

@Composable
private fun SsXpLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDim)
        Text(
            value,
            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
            color = SsColors.CyanLight, style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun SsNote(icon: String, text: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            text,
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = ssTrack(0.01f), color = SsColors.Muted, lineHeight = 15.sp
        )
    }
}

@Composable
private fun SsSparkline(
    sessions: List<ReadingSessionEntity>,
    modifier: Modifier = Modifier
) {
    val xps = sessions.map { it.totalXpGained.toFloat() }
    val maxXp = maxOf(1f, (xps.maxOrNull() ?: 1f) * 1.1f)
    // best marker = highest XP among the NON-flagged sessions shown (first one on a tie)
    val bestIdx = sessions.indices
        .filter { sessions[it].flagged == 0 && sessions[it].totalXpGained > 0 }
        .maxByOrNull { sessions[it].totalXpGained } ?: -1

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = xps.size
        if (count <= 0) return@Canvas

        val inset = 8.dp.toPx() // keep the end dots inside the panel
        val usable = w - inset * 2f
        val points = xps.mapIndexed { idx, xp ->
            val x = if (count > 1) inset + idx * (usable / (count - 1)) else w / 2f
            val y = (h - inset) - (xp / maxXp * (h - inset * 2f))
            Offset(x, y)
        }

        val strokePath = Path().apply {
            points.forEachIndexed { i, pt -> if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y) }
        }
        drawPath(
            path = strokePath,
            brush = Brush.horizontalGradient(listOf(SsColors.Cyan, SsColors.Purple)),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEachIndexed { i, pt ->
            val isBest = i == bestIdx
            if (isBest) drawCircle(Color(0xFFFFD700).copy(alpha = 0.25f), radius = 9.dp.toPx(), center = pt)
            drawCircle(
                color = if (isBest) Color(0xFFFFD700) else SsColors.PurpleLight,
                radius = if (isBest) 5.dp.toPx() else 3.dp.toPx(),
                center = pt
            )
        }
    }
}

/* ==========================================================================
 * BOOK ROW (cover + title) and RECORD BANNER: always visible, under the tier pill
 * ========================================================================== */
@Composable
private fun SsBookRow(
    clock: MutableLongState,
    title: String,
    author: String?,
    coverUrl: String?,
    coverSlot: (@Composable (Modifier) -> Unit)?
) {
    if (title.isBlank()) return
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .ssFadeUp(clock, 720)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val coverMod = Modifier
            .size(width = 38.dp, height = 56.dp)
            .clip(RoundedCornerShape(6.dp))
        if (coverSlot != null) coverSlot(coverMod) else SsBookCover(coverUrl, title, coverMod)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.ssum_book).uppercase(),
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = ssTrack(0.16f), color = SsColors.TextDimmer
            )
            Text(
                text = title,
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start
            )
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDim,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun SsBookCover(url: String?, title: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, url) {
        value = if (url.isNullOrBlank()) null else withContext(Dispatchers.IO) { ssLoadCover(context, url) }
    }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(SsColors.Cyan.copy(alpha = 0.35f), SsColors.Purple.copy(alpha = 0.45f)))
        ),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = title.trim().take(1).uppercase(),
                fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/** Off-main, size-capped, sub-sampled decode of http(s) / content / file / path covers. Null on any failure. */
private fun ssLoadCover(context: Context, src: String): ImageBitmap? = try {
    val bytes: ByteArray? = when {
        src.startsWith("http", ignoreCase = true) -> {
            val conn = (URL(src).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            try { conn.inputStream.use { it.readBytes() } } finally { conn.disconnect() }
        }
        src.startsWith("content:") || src.startsWith("file:") ->
            context.contentResolver.openInputStream(Uri.parse(src))?.use { it.readBytes() }
        else -> java.io.File(src).takeIf { it.isFile }?.readBytes()
    }
    if (bytes == null || bytes.isEmpty() || bytes.size > 6_000_000) {
        null
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outHeight / (sample * 2) >= 240) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    }
} catch (_: Exception) {
    null
}

/**
 * New best -> congratulation card with a pulsing glow (the confetti burst is a timeline event).
 * Otherwise -> encouraging line (+ comparison with the last 7 sessions). Other broken records show as chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SsRecordBanner(
    clock: MutableLongState,
    glow: State<Float>,
    newRecord: Boolean,
    mainText: String,
    subText: String?,
    chips: List<Int>
) {
    val shape = RoundedCornerShape(16.dp)
    val appearAt = if (newRecord) 1950 else 1000 // record reveals right after the count-up lands
    val fill: Brush = if (newRecord) {
        Brush.linearGradient(listOf(SsColors.Cyan.copy(alpha = 0.16f), SsColors.Purple.copy(alpha = 0.20f)))
    } else {
        SolidColor(Color.White.copy(alpha = 0.03f))
    }
    Column(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val u = SsEaseSpring.transform(ssProgress(clock.longValue, appearAt, 600))
                alpha = u.coerceIn(0f, 1f)
                val sc = ssLerp(0.92f, 1f, u)
                scaleX = sc
                scaleY = sc
                translationY = ssLerp(10.dp.toPx(), 0f, u)
            }
            .clip(shape)
            .background(fill)
            .drawBehind {
                val a = if (newRecord) 0.35f + 0.45f * glow.value else 0.08f // read in draw phase only
                drawRoundRect(
                    color = (if (newRecord) SsColors.PurpleLight else Color.White).copy(alpha = a),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (newRecord) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🏆", fontSize = 18.sp)
                Text(
                    text = stringResource(R.string.ssum_record_new),
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(SsColors.CyanLight, SsColors.PurpleLight)),
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                    )
                )
            }
            if (subText != null) {
                Text(subText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDim, textAlign = TextAlign.Center)
            }
        } else {
            Text(
                mainText,
                fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD9D2F5),
                textAlign = TextAlign.Center, lineHeight = 17.sp
            )
            if (subText != null) {
                Text(subText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SsColors.TextDimmer, textAlign = TextAlign.Center)
            }
        }
        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { id ->
                    val pill = RoundedCornerShape(100.dp)
                    Text(
                        text = "★ " + stringResource(id).uppercase(),
                        fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = ssTrack(0.12f), color = SsColors.Lavender,
                        modifier = Modifier
                            .clip(pill)
                            .background(SsColors.PurpleLight.copy(alpha = 0.12f))
                            .border(1.dp, SsColors.PurpleLight.copy(alpha = 0.35f), pill)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/* ==========================================================================
 * WINDOW: edge-to-edge dialog, backdrop blur (API 31+), no system dim
 * ========================================================================== */
@Composable
private fun SsDialogWindowEffects() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setDimAmount(0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val lp = window.attributes
                lp.blurBehindRadius = 22
                window.attributes = lp
            } catch (_: Exception) {
            }
        }
    }
}