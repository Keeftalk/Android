package tn.keeftalk.chat.ui.books.components

import tn.keeftalk.chat.ui.books.GlobalRankState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.random.Random
import tn.keeftalk.chat.domain.model.Achievement
import tn.keeftalk.chat.domain.model.Book
import kotlin.math.max
import kotlin.math.min

data class XpBreakdownItem(
    val category: String,
    val xp: Long,
    val color: Color
)

object LevelingSystem {
    private val milestoneXps = listOf(
        0L, 0L, 10000L, 25000L, 40000L, 60000L, 80000L, 90000L, 95000L, 98000L, 100000L,
        140000L, 180000L, 220000L, 260000L, 300000L, 340000L, 380000L, 420000L, 460000L, 500000L,
        650000L, 800000L, 950000L, 1100000L, 1250000L, 1450000L, 1650000L, 1850000L, 1950000L, 2000000L,
        2400000L, 2800000L, 3200000L, 3700000L, 4200000L, 4800000L, 5200000L, 5600000L, 5800000L, 6000000L,
        6900000L, 7800000L, 8800000L, 9900000L, 11000000L, 12200000L, 13400000L, 14200000L, 14700000L, 15000000L,
        17000000L, 19500000L, 22000000L, 25000000L, 28000000L, 31000000L, 33000000L, 34200000L, 34700000L, 35000000L,
        38500000L, 42000000L, 46000000L, 50000000L, 55000000L, 60000000L, 64000000L, 67500000L, 69200000L, 70000000L,
        78000000L, 87000000L, 97000000L, 108000000L, 120000000L, 133000000L, 142000000L, 147000000L, 149500000L, 150000000L,
        170000000L, 195000000L, 225000000L, 260000000L, 300000000L, 325000000L, 340000000L, 347000000L, 349500000L, 350000000L,
        415000000L, 490000000L, 580000000L, 680000000L, 790000000L, 880000000L, 940000000L, 975000000L, 995000000L, 1000000000L
    )

    fun getTitleForLevel(level: Int): String {
        return when {
            level <= 10 -> "Page Turner"
            level <= 20 -> "Casual Reader"
            level <= 30 -> "Bookworm"
            level <= 40 -> "Story Seeker"
            level <= 50 -> "Bibliophile"
            level <= 60 -> "Literary Scholar"
            level <= 70 -> "Sage"
            level <= 80 -> "Grandmaster"
            level <= 90 -> "Mythic Reader"
            else -> "Legend"
        }
    }

    fun calculateLevel(totalXp: Long): Int {
        if (totalXp >= 1_000_000_000L) return 100
        for (i in 100 downTo 1) {
            if (totalXp >= milestoneXps[i]) return i
        }
        return 1
    }

    data class ProgressData(
        val level: Int,
        val title: String,
        val currentXp: Long,
        val levelStartXp: Long,
        val nextLevelXp: Long,
        val xpInLevel: Long,
        val xpNeededInLevel: Long,
        val progressFraction: Float,
        val isMax: Boolean
    )

    fun getProgress(totalXp: Long): ProgressData {
        val cappedXp = totalXp.coerceAtMost(1_000_000_000L)
        val lvl = calculateLevel(cappedXp)
        val title = getTitleForLevel(lvl)
        if (lvl >= 100) {
            return ProgressData(100, title, cappedXp, 150000000L, 1000000000L, 0L, 0L, 1f, true)
        }
        val startXp = milestoneXps[lvl]
        val endXp = milestoneXps[lvl + 1]
        val inLvl = cappedXp - startXp
        val needed = endXp - startXp
        val frac = if (needed > 0) (inLvl.toFloat() / needed.toFloat()).coerceIn(0f, 1f) else 1f
        return ProgressData(lvl, title, cappedXp, startXp, endXp, inLvl, needed, frac, false)
    }
}

object XpCalculator {
    fun calculateAchievementsList(
        books: List<Book>,
        highlightsCount: Int,
        bookmarksCount: Int,
        readingDays: List<tn.keeftalk.chat.data.local.entities.ReadingDayEntity> = emptyList(),
        unlockedIds: Set<String> = emptySet(),
        confirmedPagesCount: Int = 0,
        confirmedRangesChars: Long = 0L
    ): List<Achievement> {
        val totalPagesRead = if (confirmedPagesCount > 0 || confirmedRangesChars > 0) {
            confirmedPagesCount + (confirmedRangesChars / 1800).toInt()
        } else {
            books.sumOf { (it.totalPages * it.percentProgress / 100).toDouble() }.toInt()
        }

        val totalActiveSeconds = readingDays.sumOf { it.activeSeconds }
        val totalMinutesRead = totalActiveSeconds / 60

        val finishedCount = books.count { it.totalPages >= 20 && (it.markedDone || it.percentProgress >= 90) }
        val librarySize = books.count { !it.isDeleted && it.totalPages >= 20 }

        val bmCount = minOf(20, bookmarksCount)
        val qCount = minOf(10, highlightsCount)

        val qualifyingDates = readingDays.filter { it.creditedPages >= tn.keeftalk.chat.domain.xp.ReadingXp.QUALIFY_PAGES && it.flagged == 0 }
            .mapNotNull {
                try { java.time.LocalDate.parse(it.date) } catch (_: Exception) { null }
            }.toSet()

        var bestStreak = 0
        var currentStreak = 0
        if (qualifyingDates.isNotEmpty()) {
            var checkDate = qualifyingDates.minOrNull()!!
            val maxDate = qualifyingDates.maxOrNull()!!
            while (!checkDate.isAfter(maxDate)) {
                if (checkDate in qualifyingDates) {
                    currentStreak++
                    bestStreak = maxOf(bestStreak, currentStreak)
                } else {
                    currentStreak = 0
                }
                checkDate = checkDate.plusDays(1)
            }
        }
        val qualifyingDaysCount = qualifyingDates.size

        val rawList = listOf(
            // Pages Read (+)
            Achievement("pages_100", "Read 100 pages", 100, totalPagesRead, "5 XP"),
            Achievement("pages_1k", "Read 1,000 pages", 1000, totalPagesRead, "20 XP"),
            Achievement("pages_10k", "Read 10,000 pages", 10000, totalPagesRead, "100 XP"),
            Achievement("pages_100k", "Read 100,000 pages", 100000, totalPagesRead, "500 XP"),
            Achievement("pages_1m", "Read 1,000,000 pages", 1000000, totalPagesRead, "2,500 XP"),

            // Bookmarks (+)
            Achievement("bm_1", "First bookmark added", 1, bmCount, "5 XP"),
            Achievement("bm_25", "25 bookmarks added", 25, bmCount, "20 XP"),
            Achievement("bm_100", "100 bookmarks added", 100, bmCount, "50 XP"),
            Achievement("bm_500", "500 bookmarks added", 500, bmCount, "200 XP"),

            // Quotes (+++)
            Achievement("q_1", "First quote saved", 1, qCount, "50 XP"),
            Achievement("q_10", "10 quotes saved", 10, qCount, "200 XP"),
            Achievement("q_50", "50 quotes saved", 50, qCount, "1,000 XP"),
            Achievement("q_100", "100 quotes saved", 100, qCount, "2,500 XP"),
            Achievement("q_500", "500 quotes saved", 500, qCount, "10,000 XP"),

            // Reading Time (Minutes & Hours - Balanced 5-step tiers)
            Achievement("t_10m", "10 minutes of reading", 10, totalMinutesRead, "100 XP"),
            Achievement("t_30m", "30 minutes of reading", 30, totalMinutesRead, "250 XP"),
            Achievement("t_90m", "90 minutes of reading", 90, totalMinutesRead, "750 XP"),

            // Tier 1: 1h to 10h
            Achievement("t_1h", "1 hour of reading", 60, totalMinutesRead, "500 XP"),
            Achievement("t_3h", "3 hours of reading", 180, totalMinutesRead, "1,200 XP"),
            Achievement("t_5h", "5 hours of reading", 300, totalMinutesRead, "2,500 XP"),
            Achievement("t_8h", "8 hours of reading", 480, totalMinutesRead, "6,000 XP"),
            Achievement("t_10h", "10 hours of reading", 600, totalMinutesRead, "10,000 XP"),

            // Tier 2: 10h to 50h
            Achievement("t_20h", "20 hours of reading", 1200, totalMinutesRead, "18,000 XP"),
            Achievement("t_30h", "30 hours of reading", 1800, totalMinutesRead, "28,000 XP"),
            Achievement("t_40h", "40 hours of reading", 2400, totalMinutesRead, "40,000 XP"),
            Achievement("t_50h", "50 hours of reading", 3000, totalMinutesRead, "55,000 XP"),

            // Tier 3: 50h to 100h
            Achievement("t_62h", "62 hours of reading", 3720, totalMinutesRead, "75,000 XP"),
            Achievement("t_75h", "75 hours of reading", 4500, totalMinutesRead, "100,000 XP"),
            Achievement("t_87h", "87 hours of reading", 5220, totalMinutesRead, "130,000 XP"),
            Achievement("t_100h", "100 hours of reading", 6000, totalMinutesRead, "165,000 XP"),

            // Tier 4: 100h to 250h
            Achievement("t_130h", "130 hours of reading", 7800, totalMinutesRead, "210,000 XP"),
            Achievement("t_160h", "160 hours of reading", 9600, totalMinutesRead, "265,000 XP"),
            Achievement("t_190h", "190 hours of reading", 11400, totalMinutesRead, "330,000 XP"),
            Achievement("t_220h", "220 hours of reading", 13200, totalMinutesRead, "410,000 XP"),
            Achievement("t_250h", "250 hours of reading", 15000, totalMinutesRead, "500,000 XP"),

            // Tier 5: 250h to 500h
            Achievement("t_310h", "310 hours of reading", 18600, totalMinutesRead, "610,000 XP"),
            Achievement("t_370h", "370 hours of reading", 22200, totalMinutesRead, "730,000 XP"),
            Achievement("t_430h", "430 hours of reading", 25800, totalMinutesRead, "870,000 XP"),
            Achievement("t_500h", "500 hours of reading", 30000, totalMinutesRead, "1,020,000 XP"),

            // Tier 6: 500h to 20,000h Elite Tiers
            Achievement("t_600h", "600 hours of reading", 36000, totalMinutesRead, "1,250,000 XP"),
            Achievement("t_700h", "700 hours of reading", 42000, totalMinutesRead, "1,550,000 XP"),
            Achievement("t_800h", "800 hours of reading", 48000, totalMinutesRead, "1,900,000 XP"),
            Achievement("t_900h", "900 hours of reading", 54000, totalMinutesRead, "2,300,000 XP"),
            Achievement("t_1000h", "1,000 hours of reading", 60000, totalMinutesRead, "10,000,000 XP"),
            Achievement("t_2000h", "2,000 hours of reading", 120000, totalMinutesRead, "22,000,000 XP"),
            Achievement("t_4000h", "4,000 hours of reading", 240000, totalMinutesRead, "45,000,000 XP"),
            Achievement("t_8000h", "8,000 hours of reading", 480000, totalMinutesRead, "100,000,000 XP"),
            Achievement("t_10000h", "10,000 hours of reading", 600000, totalMinutesRead, "200,000,000 XP"),
            Achievement("t_20000h", "20,000 hours of reading", 1200000, totalMinutesRead, "300,000,000 XP"),

            // Books Finished (Milestone)
            Achievement("f_1", "First book finished", 1, finishedCount, "1,000 XP"),
            Achievement("f_5", "5th book finished", 5, finishedCount, "2,500 XP"),
            Achievement("f_10", "10th book finished", 10, finishedCount, "5,000 XP"),
            Achievement("f_25", "25th book finished", 25, finishedCount, "15,000 XP"),
            Achievement("f_50", "50th book finished", 50, finishedCount, "30,000 XP"),
            Achievement("f_100", "100th book finished", 100, finishedCount, "75,000 XP"),

            // Adding Books to Library (+++++)
            Achievement("lib_1", "Add first book to library", 1, librarySize, "100 XP"),
            Achievement("lib_10", "Add 10 books to library", 10, librarySize, "500 XP"),
            Achievement("lib_50", "Add 50 books to library", 50, librarySize, "2,500 XP"),
            Achievement("lib_100", "Add 100 books to library", 100, librarySize, "5,000 XP"),
            Achievement("lib_500", "Add 500 books to library", 500, librarySize, "25,000 XP"),
            Achievement("lib_1000", "Add 1,000 books to library", 1000, librarySize, "50,000 XP"),

            // Consistency
            Achievement("c_3", "Read for 3 consecutive days", 3, bestStreak, "2,000 XP"),
            Achievement("c_7", "Read for 7 consecutive days", 7, bestStreak, "5,000 XP"),
            Achievement("c_30", "Read for 30 consecutive days", 30, bestStreak, "25,000 XP"),
            Achievement("c_100", "Read for 100 consecutive days", 100, bestStreak, "100,000 XP"),
            Achievement("c_365", "Read for 365 consecutive days", 365, bestStreak, "500,000 XP"),
            Achievement("c_tot100", "Read 100 total days", 100, qualifyingDaysCount, "10,000 XP"),
            Achievement("c_tot500", "Read 500 total days", 500, qualifyingDaysCount, "50,000 XP")
        )

        // Ensure unlocked status is persisted and non-reversible
        val list = rawList.map { ach ->
            if (ach.icon in unlockedIds) {
                ach.copy(current = maxOf(ach.current, ach.target))
            } else {
                ach
            }
        }.toMutableList()

        val unlockedCount = list.count { it.isUnlocked }
        list.add(Achievement("ach_12", "Unlock 12 total achievements", 12, unlockedCount, "10,000 XP"))
        list.add(Achievement("ach_50", "Unlock 50 total achievements", 50, unlockedCount, "100,000 XP"))
        list.add(Achievement("ach_100", "Unlock 100 total achievements", 100, unlockedCount, "500,000 XP"))

        val sortedList = list.sortedWith(
            compareByDescending<Achievement> { it.isUnlocked || it.icon in unlockedIds }
                .thenByDescending { if (it.target > 0) it.current.toFloat() / it.target.toFloat() else 0f }
                .thenBy { (it.target - it.current).coerceAtLeast(0) }
        )

        return sortedList
    }

    fun calculateTotalXp(
        legacyReadingXp: Long,
        settled: tn.keeftalk.chat.domain.xp.ReadingXp.Settled,
        achievements: List<Achievement>
    ): Long {
        var achievementXp = 0L
        for (ach in achievements) {
            if (ach.isUnlocked) {
                val rewardStr = ach.rewardName.replace(" XP", "").replace(",", "").trim()
                val xpVal = rewardStr.toLongOrNull() ?: 0L
                achievementXp += xpVal
            }
        }
        val total = legacyReadingXp + settled.total + achievementXp
        return total.coerceAtMost(1_000_000_000L)
    }

    fun calculateXpBreakdown(
        legacyReadingXp: Long,
        settled: tn.keeftalk.chat.domain.xp.ReadingXp.Settled,
        achievements: List<Achievement>
    ): List<XpBreakdownItem> {
        val verifiedPagesXp = legacyReadingXp + settled.baseXp
        val consistencyBonusXp = settled.bonusXp

        var achievementXp = 0L
        for (ach in achievements) {
            if (ach.isUnlocked) {
                val rewardStr = ach.rewardName.replace(" XP", "").replace(",", "").trim()
                achievementXp += (rewardStr.toLongOrNull() ?: 0L)
            }
        }

        return listOf(
            XpBreakdownItem("Verified Pages", verifiedPagesXp, Color(0xFF6C63FF)),
            XpBreakdownItem("Consistency Bonus", consistencyBonusXp, Color(0xFF00CCCC)),
            XpBreakdownItem("Unlocked Achievements", achievementXp, Color(0xFFFFD700))
        )
    }
}

fun getAchievementExplanation(title: String): String {
    return when {
        title.contains("pages", ignoreCase = true) -> "Accumulate total pages read across all books in your library to reach this milestone."
        title.contains("bookmark", ignoreCase = true) -> "Add bookmarks while reading books to quickly return to important pages."
        title.contains("quote", ignoreCase = true) -> "Select text inside the reader and save memorable quotes and notes."
        title.contains("hour", ignoreCase = true) -> "Spend dedicated reading time in the app to accumulate total reading hours."
        title.contains("book finished", ignoreCase = true) -> "Reach 100% progress on a book to finish it."
        title.contains("add", ignoreCase = true) && title.contains("book", ignoreCase = true) -> "Import EPUB/PDF books into your library to expand your reading collection."
        title.contains("consecutive", ignoreCase = true) -> "Keep your daily reading streak alive by reading every single day."
        title.contains("total days", ignoreCase = true) -> "Accumulate days with reading activity over time."
        title.contains("unlock", ignoreCase = true) -> "Unlock milestone achievements as you progress on your reading journey."
        else -> "Complete reading goals and milestones to unlock this achievement and earn XP."
    }
}

@Composable
fun BooksAchievementsContent(
    books: List<Book>,
    highlightsCount: Int = 0,
    bookmarksCount: Int = 0,
    readingDays: List<tn.keeftalk.chat.data.local.entities.ReadingDayEntity> = emptyList(),
    unlockedAchievements: List<tn.keeftalk.chat.data.local.entities.UnlockedAchievementEntity> = emptyList(),
    legacyReadingXp: Long = 0L,
    currentTab: String = "awd",
    onSelectTab: (String) -> Unit = {},
    isRailExpanded: Boolean = false,
    onOpenInfo: () -> Unit = {},
    globalRankState: GlobalRankState = GlobalRankState(),
    onRefreshGlobalRank: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLeaderboardExpanded by remember { mutableStateOf(false) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    var showXpHelpDialog by remember { mutableStateOf(false) }

    val daysMap = remember(readingDays) {
        readingDays.associate {
            try {
                java.time.LocalDate.parse(it.date) to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1)
            } catch (_: Exception) {
                java.time.LocalDate.now() to tn.keeftalk.chat.domain.xp.ReadingXp.DayRecord(it.creditedPages, it.flagged == 1)
            }
        }
    }

    val settled = remember(daysMap) {
        tn.keeftalk.chat.domain.xp.ReadingXp.settle(daysMap)
    }

    val unlockedSet = remember(unlockedAchievements) {
        unlockedAchievements.map { it.id }.toSet()
    }

    val achievements = remember(books, highlightsCount, bookmarksCount, readingDays, unlockedSet) {
        XpCalculator.calculateAchievementsList(
            books = books,
            highlightsCount = highlightsCount,
            bookmarksCount = bookmarksCount,
            readingDays = readingDays,
            unlockedIds = unlockedSet
        )
    }

    val totalXp = remember(legacyReadingXp, settled, achievements) {
        XpCalculator.calculateTotalXp(legacyReadingXp, settled, achievements)
    }

    LaunchedEffect(totalXp) {
        onRefreshGlobalRank(totalXp)
    }

    val progressData = remember(totalXp) {
        LevelingSystem.getProgress(totalXp)
    }
    val level = progressData.level
    val levelTitle = progressData.title
    val isMaxLevel = progressData.isMax

    val globalRank = if (globalRankState.isRealBackend && globalRankState.rank > 0) {
        globalRankState.rank
    } else {
        if (totalXp == 0L) 52310L else max(1L, (52310L * (1.0 - (totalXp.toDouble() / 1_000_000_000.toDouble()).coerceIn(0.0, 0.9999))).toLong())
    }

    val estimatedTotalReaders = if (globalRankState.isRealBackend && globalRankState.totalReaders > 0) {
        globalRankState.totalReaders
    } else {
        52310L
    }

    val percentile = if (globalRankState.isRealBackend && globalRankState.percentile > 0) {
        globalRankState.percentile
    } else {
        max(1, (globalRank.toDouble() / estimatedTotalReaders.toDouble() * 100).toInt())
    }

    val unlockedCount = achievements.count { it.isUnlocked }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isLeaderboardExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevronRotation"
    )

    // Popup Dialog when clicking an achievement
    if (selectedAchievement != null) {
        val ach = selectedAchievement!!
        AlertDialog(
            onDismissRequest = { selectedAchievement = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AchievementIcon(iconKey = ach.icon, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(ach.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ach.isUnlocked) {
                        Surface(
                            color = Color(0xFF2ECC71).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "🎉 Achieved",
                                color = Color(0xFF2ECC71),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            "Progress: ${ach.current} / ${ach.target}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Reward: ${ach.rewardName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getAchievementExplanation(ach.title),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAchievement = null }) {
                    Text("Close")
                }
            }
        )
    }

    // XP & Ranking Guide / Breakdown Dialog
    if (showXpHelpDialog) {
        val breakdown = remember(legacyReadingXp, settled, achievements) {
            XpCalculator.calculateXpBreakdown(legacyReadingXp, settled, achievements)
        }
        val maxBreakdownXp = maxOf(1L, breakdown.maxOfOrNull { it.xp } ?: 1L)

        AlertDialog(
            onDismissRequest = { showXpHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFF6C63FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("XP & Ranking Guide", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How XP & Ranking Works",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "XP (Experience Points) is calculated dynamically using a proprietary, confidential reading formula based on your reading times and pages read, combined with bonuses from unlocking achievements as you progress. Your total XP determines your reader level and calculates your global standing percentile among readers.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider()

                    Text(
                        text = "Score Breakdown ($totalXp Total XP)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    breakdown.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.category, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${item.xp} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = item.color)
                            }
                            LinearProgressIndicator(
                                progress = { (item.xp.toFloat() / maxBreakdownXp.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = item.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showXpHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Row: Level & XP Hero Card + Section Rail
        HeroRowWithRail(
            currentTab = currentTab,
            onSelectTab = onSelectTab,
            isRailExpanded = isRailExpanded,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp)
                    .clickable { isLeaderboardExpanded = !isLeaderboardExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 170.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF6C63FF), Color(0xFF7D5CFF), Color(0xFF00CCCC))
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMaxLevel) "MAX LEVEL · 1.00B XP" else "LEVEL $level · ${levelTitle.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { onOpenInfo() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                if (isMaxLevel) {
                                    Surface(
                                        color = Color(0xFFFFD700),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "MAX",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMaxLevel) "1,000,000,000 XP" else "${progressData.currentXp} / ${progressData.nextLevelXp} XP",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progressData.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = if (isMaxLevel) "Maximum level reached! Prestige unlocked ✨" else "${progressData.xpNeededInLevel - progressData.xpInLevel} XP to Level ${level + 1}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = "Medal",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Leaderboard / Global Standing expansion
                        AnimatedVisibility(visible = isLeaderboardExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "GLOBAL READING STANDING",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            IconButton(
                                                onClick = { showXpHelpDialog = true },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HelpOutline,
                                                    contentDescription = "XP Help",
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "#$globalRank ",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "of $estimatedTotalReaders readers",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Top $percentile%",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$totalXp total XP",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isLeaderboardExpanded) "Tap to hide" else "Tap to see global standing",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            DownChevronIcon(
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                            )
                        }
                    }
                }
            }
        }

        // Just Unlocked Banner
        val latestUnlocked = achievements.firstOrNull { it.isUnlocked }
        if (latestUnlocked != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedAchievement = latestUnlocked }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AchievementIcon(iconKey = latestUnlocked.icon, modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF6C63FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("UNLOCKED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                        }
                        Text(latestUnlocked.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Reward: ${latestUnlocked.rewardName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text("$unlockedCount of ${achievements.size} unlocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Badge Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(achievements) { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isUnlocked)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAchievement = item }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.graphicsLayer { alpha = if (item.isUnlocked) 0.5f else 1.0f }) {
                            AchievementIcon(iconKey = item.icon, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            item.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = if (item.isUnlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (item.isUnlocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color(0xFF2ECC71).copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(item.rewardName, fontSize = 10.sp, color = Color(0xFF2ECC71).copy(alpha = 0.7f), maxLines = 1)
                            }
                        } else {
                            LinearProgressIndicator(
                                progress = { (item.current.toFloat() / item.target.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF6C63FF)
                            )
                            Text("${item.current} / ${item.target}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementIcon(
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val (vector, tint) = when (iconKey) {
        // Pages Read
        "pages_100" -> Icons.Default.Description to Color(0xFF6C63FF)
        "pages_1k" -> Icons.Default.Book to Color(0xFF7D5CFF)
        "pages_10k" -> Icons.AutoMirrored.Filled.MenuBook to Color(0xFF00CCCC)
        "pages_100k" -> Icons.Default.LibraryBooks to Color(0xFF2ECC71)
        "pages_1m" -> Icons.Default.LocalLibrary to Color(0xFFFFD700)

        // Bookmarks
        "bm_1" -> Icons.Default.Bookmark to Color(0xFFFF5D73)
        "bm_25" -> Icons.Default.Bookmarks to Color(0xFFFF8E72)
        "bm_100" -> Icons.Default.BookmarkBorder to Color(0xFFFFA07A)
        "bm_500" -> Icons.Default.BookmarkAdded to Color(0xFFE67E22)

        // Quotes
        "q_1" -> Icons.Default.FormatQuote to Color(0xFF6C63FF)
        "q_10" -> Icons.Default.Comment to Color(0xFF3498DB)
        "q_50" -> Icons.Default.Chat to Color(0xFF2980B9)
        "q_100" -> Icons.Default.Forum to Color(0xFF8E44AD)
        "q_500" -> Icons.Default.SpeakerNotes to Color(0xFF9B59B6)

        // Reading Time
        "t_1h" -> Icons.Default.Timer to Color(0xFF00CCCC)
        "t_10h" -> Icons.Default.AccessTime to Color(0xFF1ABC9C)
        "t_50h" -> Icons.Default.HourglassEmpty to Color(0xFF16A085)
        "t_100h" -> Icons.Default.HourglassFull to Color(0xFF27AE60)
        "t_250h" -> Icons.Default.History to Color(0xFF2ECC71)
        "t_500h" -> Icons.Default.Update to Color(0xFFFFD700)
        "t_1000h" -> Icons.Default.SlowMotionVideo to Color(0xFFF1C40F)

        // Books Finished
        "f_1" -> Icons.Default.EmojiEvents to Color(0xFFFFD700)
        "f_5" -> Icons.Default.WorkspacePremium to Color(0xFFC0C0C0)
        "f_10" -> Icons.Default.MilitaryTech to Color(0xFFCD7F32)
        "f_25" -> Icons.Default.School to Color(0xFFE74C3C)
        "f_50" -> Icons.Default.Grade to Color(0xFFD35400)
        "f_100" -> Icons.Default.Star to Color(0xFFFFD700)

        // Library
        "lib_1" -> Icons.Default.Book to Color(0xFF00CCCC)
        "lib_10" -> Icons.Default.FolderSpecial to Color(0xFF2980B9)
        "lib_50" -> Icons.Default.Stars to Color(0xFF3498DB)
        "lib_100" -> Icons.Default.Security to Color(0xFF9B59B6)
        "lib_500" -> Icons.Default.Verified to Color(0xFF2ECC71)
        "lib_1000" -> Icons.Default.AutoAwesome to Color(0xFFFFD700)

        // Consistency
        "c_3" -> Icons.Default.Whatshot to Color(0xFFE67E22)
        "c_7" -> Icons.Default.LocalFireDepartment to Color(0xFFFF5722)
        "c_30" -> Icons.Default.FlashOn to Color(0xFFFF9800)
        "c_100" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        "c_365" -> Icons.Default.DoneAll to Color(0xFF009688)
        "c_tot100" -> Icons.Default.Landscape to Color(0xFF8BC34A)
        "c_tot500" -> Icons.Default.Public to Color(0xFF03A9F4)

        // Total Achievements
        "ach_12" -> Icons.Default.Extension to Color(0xFF9C27B0)
        "ach_50" -> Icons.Default.StarRate to Color(0xFF673AB7)
        "ach_100" -> Icons.Default.Cake to Color(0xFFE91E63)

        else -> Icons.Default.MilitaryTech to Color(0xFFFFD700)
    }

    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun DownChevronIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(13.dp)) {
        val strokeWidth = 2.6.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.85f, h * 0.35f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

sealed class CelebrationItem {
    data class LevelUp(val level: Int, val title: String) : CelebrationItem()
    data class AchievementUnlocked(val achievement: Achievement) : CelebrationItem()
}

@Composable
fun CelebrationDialog(
    item: CelebrationItem,
    onDismiss: () -> Unit
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F0C20), Color(0xFF1E1035), Color(0xFF110825))
    )

    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyScale"
    )

    val colors = listOf(Color(0xFFFFD43B), Color.White, Color(0xFF00CCCC), Color(0xFFFF5D73), Color(0xFF2ECC71))
    var confetti by remember {
        mutableStateOf(
            List(40) {
                ConfettiPiece(
                    x = Random.nextFloat() * 1000f,
                    y = Random.nextFloat() * -600f,
                    color = colors.random(),
                    size = Random.nextFloat() * 11f + 6f,
                    speed = Random.nextFloat() * 5f + 2.5f,
                    rotation = Random.nextFloat() * 360f
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(25)
            confetti = confetti.map { piece ->
                val newY = piece.y + piece.speed
                if (newY > 2000f) {
                    piece.copy(y = -100f, x = Random.nextFloat() * 1000f)
                } else {
                    piece.copy(y = newY, rotation = piece.rotation + 3f)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(20.dp)
            .zIndex(200f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            confetti.forEach { piece ->
                drawRect(
                    color = piece.color,
                    topLeft = Offset(piece.x * (size.width / 1000f), piece.y * (size.height / 2000f)),
                    size = Size(piece.size * 1.5f, piece.size * 0.9f)
                )
            }
        }

        Surface(
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFFFD43B), Color(0xFF00CCCC)))),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Trophy / Badge Icon with glowing background
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD43B).copy(alpha = 0.4f), Color.Transparent)
                            ),
                            radius = size.width / 1.8f
                        )
                    }

                    Text(
                        text = when (item) {
                            is CelebrationItem.LevelUp -> "🎖️"
                            is CelebrationItem.AchievementUnlocked -> "🏆"
                        },
                        fontSize = 72.sp,
                        modifier = Modifier.scale(scaleAnim)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (item) {
                        is CelebrationItem.LevelUp -> "🎉 LEVEL UP!"
                        is CelebrationItem.AchievementUnlocked -> "🏆 ACHIEVEMENT UNLOCKED!"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD43B),
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val mainTitle = when (item) {
                    is CelebrationItem.LevelUp -> "Level ${item.level} Master"
                    is CelebrationItem.AchievementUnlocked -> item.achievement.title
                }

                Text(
                    text = mainTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                val rewardText = when (item) {
                    is CelebrationItem.LevelUp -> "Title: ${item.title}"
                    is CelebrationItem.AchievementUnlocked -> "Reward: ${item.achievement.rewardName}"
                }

                Surface(
                    color = Color(0xFF00CCCC).copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00CCCC).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = rewardText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00CCCC),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF00CCCC))))
                        .clickable { onDismiss() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Awesome!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
