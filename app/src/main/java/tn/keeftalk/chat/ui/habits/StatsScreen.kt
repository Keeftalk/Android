package tn.keeftalk.chat.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.ui.habits.components.HabitsHeroRowWithRail
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    habits: List<HabitDisplayModel>,
    selectedRange: Int = 30,
    gracePoolRemaining: Int = 2,
    onRangeSelected: (Int) -> Unit = {},
    selectedTab: HabitsTab = HabitsTab.STATS,
    onSelectTab: (HabitsTab) -> Unit = {},
    isRailExpanded: Boolean = false,
    onToggleRail: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ranges = listOf(7 to "This week", 30 to "This month", 84 to "12 weeks", 180 to "Since day 1")
    val today = LocalDate.now()

    // ── Metric calculations for selectedRange ─────────────────────────
    var totDue = 0
    var totDone = 0
    for (o in 0 until selectedRange) {
        val d = today.minusDays(o.toLong())
        habits.forEach { model ->
            val dayStatus = model.recent7Days.find { it.date == d }
            val isDue = dayStatus?.isDue ?: (d >= model.habit.createdDate && model.schedule?.daysMask?.contains(d.dayOfWeek) == true)
            if (isDue) {
                totDue++
                val isCompleted = dayStatus?.isCompleted == true || dayStatus?.isForgiven == true
                if (isCompleted) totDone++
            }
        }
    }
    val avgPercent = if (totDue > 0) (totDone.toFloat() / totDue.toFloat() * 100).toInt() else 0
    val bestStreak = habits.maxOfOrNull { it.status.streak } ?: 0

    // Range label text for subtitle
    val rangeLabel = when (selectedRange) {
        7 -> "this week"
        30 -> "the last 30 days"
        84 -> "the last 12 weeks"
        else -> "since " + today.minusDays(selectedRange.toLong() - 1).format(DateTimeFormatter.ofPattern("MMM d"))
    }

    // Active recovery challenge tip
    val activeRecList = habits.filter { !it.status.isMastered && it.status.recoveryActive }
    val topRec = activeRecList.maxByOrNull { it.status.recoveryProgress.toFloat() / maxOf(1, it.status.recoveryRequired).toFloat() }

    // Mastered count
    val masteredCount = habits.count { it.status.isMastered }

    // Ranked habit performance for tips
    val rankedPerformance = habits.map { model ->
        var due = 0
        var done = 0
        for (o in 0 until selectedRange) {
            val d = today.minusDays(o.toLong())
            val dayStatus = model.recent7Days.find { it.date == d }
            val isDue = dayStatus?.isDue ?: (d >= model.habit.createdDate && model.schedule?.daysMask?.contains(d.dayOfWeek) == true)
            if (isDue) {
                due++
                if (dayStatus?.isCompleted == true || dayStatus?.isForgiven == true) done++
            }
        }
        val rate = if (due > 0) done.toFloat() / due.toFloat() else 0f
        Triple(model, due, rate)
    }.filter { it.second > 0 }.sortedByDescending { it.third }

    val strongest = rankedPerformance.firstOrNull()
    val lowest = if (rankedPerformance.size > 1) rankedPerformance.lastOrNull() else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 0.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Hero Row with Hero Card + Section Rail ─────────────────────
        item {
            HabitsHeroRowWithRail(
                selectedTab = HabitsTab.STATS,
                onSelectTab = onSelectTab,
                isRailExpanded = isRailExpanded,
                onToggleRail = onToggleRail
            ) {
                StatsHeroCard(
                    ranges = ranges,
                    selectedRange = selectedRange,
                    onRangeSelected = onRangeSelected,
                    avgPercent = avgPercent,
                    bestStreak = bestStreak,
                    totDone = totDone,
                    gracePoolRemaining = gracePoolRemaining,
                    onOpenHelp = onOpenHelp
                )
            }
        }

        // ── 3. Dynamic Insight Tips (#ins) ───────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Active Recovery Challenge Tip
                if (topRec != null) {
                    val status = topRec.status
                    val left = maxOf(0, status.recoveryRequired - status.recoveryProgress)
                    val progPct = if (status.recoveryRequired > 0) status.recoveryProgress.toFloat() / status.recoveryRequired.toFloat() else 0f

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Grace Challenge · ${topRec.habit.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Complete $left more day${if (left == 1) "" else "s"} in a row and your missed day is graced. Your progress is safe.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progPct },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${status.recoveryProgress} / ${status.recoveryRequired} days · target becomes ${status.effTarget} days (+${status.effMissed} miss${if (status.effMissed == 1) "" else "es"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mastered Habits Tip
                if (masteredCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF2ECC71).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(22.dp))
                            Text(
                                text = "$masteredCount habit${if (masteredCount == 1) "" else "s"} now automatic. Retired at 200% of the effective target — the behavior is yours now.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Automaticity Spark Rule Tip
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Text(
                            text = "One missed day has 0% impact on automaticity. Missed days increase your target by +1 each — but a completed recovery streak earns grace and keeps you moving forward.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Strongest Habit Tip
                if (strongest != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFE07B00), modifier = Modifier.size(22.dp))
                            Text(
                                text = "${strongest.first.habit.name} is your strongest habit at ${(strongest.third * 100).toInt()}%.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Lowest Habit Tip
                if (lowest != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFE07B00).copy(alpha = 0.13f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AdsClick, contentDescription = null, tint = Color(0xFFE07B00), modifier = Modifier.size(22.dp))
                            Text(
                                text = "${lowest.first.habit.name} needs attention at ${(lowest.third * 100).toInt()}%. A fixed reminder time helps.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Completion Trend Panel ────────────────────────────────
        item {
            StatPanel("Completion trend", "Average $avgPercent% over $rangeLabel · swipe the chart to explore", icon = Icons.Default.TrendingUp) {
                TrendChartCanvas(
                    habits = habits,
                    rangeDays = selectedRange,
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                )
            }
        }

        // ── 5. Automaticity Threshold Panel ─────────────────────────
        item {
            StatPanel("Automaticity threshold", "Days completed vs. your effective target — misses increase the target, grace keeps it in reach", icon = Icons.Default.AutoAwesome) {
                val sortedHabits = habits.sortedByDescending { it.status.automaticity }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    sortedHabits.forEach { model ->
                        val autoPct = (model.status.automaticity * 100).toInt()
                        val done = model.status.doneCount
                        val target = model.status.effTarget
                        val extra = model.status.effMissed
                        val left = maxOf(0, target - done)
                        val extraTxt = if (extra > 0) " · +$extra from misses" else ""
                        val statusText = when {
                            model.status.isMastered -> "retired at 200%"
                            model.status.recoveryActive -> "recovery ${model.status.recoveryProgress}/${model.status.recoveryRequired}"
                            model.status.automaticity >= 1.0f -> "automatic ✓"
                            else -> "~$left to go"
                        }
                        val habitColor = parseHexColor(model.colorHex)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HabitIconView(iconKey = model.habit.icon, modifier = Modifier.size(18.dp), tint = habitColor)
                                    Text(model.habit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = HABIT_CATEGORIES[model.habit.category]?.label ?: model.habit.category,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text("$autoPct%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { model.status.automaticity.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                                color = habitColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$done of ~$target reps$extraTxt · $statusText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── 6. "This Week" Panel (#wk) ─────────────────────────────
        item {
            StatPanel("This week", "Share of scheduled habits done each day", icon = Icons.Default.BarChart) {
                WeeklyBarChartCanvas(
                    habits = habits,
                    today = today,
                    modifier = Modifier.fillMaxWidth().height(130.dp)
                )
            }
        }

        // ── 7. Consistency / 12-Week Heatmap Panel (#heat) ───────────
        item {
            StatPanel("Consistency", "Last 12 weeks, darker is better", icon = Icons.Default.CalendarToday) {
                HeatmapCanvas(
                    habits = habits,
                    today = today,
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            }
        }

        // ── 8. "By Time of Day" Panel (#don) ─────────────────────────
        item {
            StatPanel("By time of day", "Check-ins by scheduled time slot", icon = Icons.Default.PieChart) {
                DonutChartPanel(habits = habits, selectedRange = selectedRange)
            }
        }

        // ── 9. Habit Performance Panel (#rates) ─────────────────────
        item {
            StatPanel("Habit performance", "", icon = Icons.Default.AdsClick) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    habits.forEach { model ->
                        var due = 0
                        var done = 0
                        for (o in 0 until selectedRange) {
                            val d = today.minusDays(o.toLong())
                            val dayStatus = model.recent7Days.find { it.date == d }
                            val isDue = dayStatus?.isDue ?: (d >= model.habit.createdDate && model.schedule?.daysMask?.contains(d.dayOfWeek) == true)
                            if (isDue) {
                                due++
                                if (dayStatus?.isCompleted == true || dayStatus?.isForgiven == true) done++
                            }
                        }
                        val rate = if (due > 0) done.toFloat() / due.toFloat() else 0f
                        val habitColor = parseHexColor(model.colorHex)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HabitIconView(iconKey = model.habit.icon, modifier = Modifier.size(20.dp), tint = habitColor)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.habit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { rate },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = habitColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            Text(if (due > 0) "${(rate * 100).toInt()}%" else "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 10. Missed-day Cost Panel (#cost) ────────────────────────
        item {
            StatPanel("Missed-day cost", "A missed day creates a challenge, not a reset. Progress stays — grace is earned.", icon = Icons.Default.Bolt) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CostCategoryRow("drop", "Simple drinking", "~18 days · Water, tea, juice with meals", "1 day: No impact", "safe", "1 week: Small delay", "warn")
                    CostCategoryRow("heart", "Simple health", "~30 days · Flossing, vitamins, skincare", "1 day: No impact", "safe", "1 week: Momentum loss", "warn")
                    CostCategoryRow("book", "Healthy eating / mental", "~45 days · Fruit, reading, journaling", "1 day: No impact", "safe", "1 week: Slower curve", "warn")
                    CostCategoryRow("leaf", "Light activity", "~106 days · Stretching, walking, yoga", "1 day: No impact", "safe", "1 week: Real setback", "danger")
                    CostCategoryRow("dumbbell", "Complex exercise", "~254 days · Gym, running, HIIT", "1 day: No impact", "safe", "1 week: Relapse risk", "danger")

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Grace days: you get 2 per month (non-cumulative). Use one to fully forgive a missed day — no target increase, streak stays intact. Need more? A Grace Challenge automatically unlocks after a miss — complete the recovery streak to earn your grace.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── 11. Milestones & Rewards Panel (#bdg) ────────────────────
        item {
            StatPanel("Milestones & rewards", "Sorted by closest to unlock — the ring shows how far you are", icon = Icons.Default.CardGiftcard) {
                MilestonesGrid(habits = habits, totDone = totDone, bestStreak = bestStreak)
            }
        }

        // ── 12. Your Storage Panel (#storage) ────────────────────────
        item {
            StatPanel("Your storage", "Start with 5 GB free. Reach a 1-year streak to unlock all the way to 10 GB", icon = Icons.Default.Storage) {
                StoragePanelContent(bestStreak = bestStreak)
            }
        }
    }
}

// ── Supporting Composables & Canvas Components ─────────────────────────

@Composable
fun KPITile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatPanel(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun TrendChartCanvas(
    habits: List<HabitDisplayModel>,
    rangeDays: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = Color(0xFF00CCCC)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val today = LocalDate.now()

    val points = (rangeDays - 1 downTo 0).map { offset ->
        val d = today.minusDays(offset.toLong())
        var due = 0
        var done = 0
        habits.forEach { model ->
            val dayStatus = model.recent7Days.find { it.date == d }
            val isDue = dayStatus?.isDue ?: (d >= model.habit.createdDate && model.schedule?.daysMask?.contains(d.dayOfWeek) == true)
            if (isDue) {
                due++
                if (dayStatus?.isCompleted == true || dayStatus?.isForgiven == true) done++
            }
        }
        if (due > 0) done.toFloat() / due.toFloat() else 0f
    }

    val startDateLabel = today.minusDays(rangeDays.toLong() - 1).format(DateTimeFormatter.ofPattern("MMM d"))
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier.width(maxOf(340.dp, (rangeDays * 9).dp)).fillMaxHeight()
        ) {
            val leftMargin = 36.dp.toPx()
            val rightMargin = 16.dp.toPx()
            val topMargin = 12.dp.toPx()
            val bottomMargin = 28.dp.toPx()
            val chartWidth = size.width - leftMargin - rightMargin
            val chartHeight = size.height - topMargin - bottomMargin

            // Gridlines & Y-axis labels at 0%, 50%, 100%
            listOf(0f, 0.5f, 1.0f).forEach { g ->
                val y = topMargin + (1f - g) * chartHeight
                drawLine(
                    color = gridColor,
                    start = Offset(leftMargin, y),
                    end = Offset(size.width - rightMargin, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.isNotEmpty()) {
                val widthStep = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { index, v ->
                    val x = leftMargin + index * widthStep
                    val y = topMargin + (1f - v.coerceIn(0f, 1f)) * chartHeight
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, topMargin + chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(leftMargin + (points.size - 1) * widthStep, topMargin + chartHeight)
                fillPath.close()

                // Area fill gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.0f)),
                        startY = topMargin,
                        endY = topMargin + chartHeight
                    )
                )

                // Line stroke gradient
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(secondaryColor, primaryColor),
                        startX = leftMargin,
                        endX = size.width - rightMargin
                    ),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Today dot
                val lastX = leftMargin + (points.size - 1) * widthStep
                val lastY = topMargin + (1f - points.last().coerceIn(0f, 1f)) * chartHeight
                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastY))
            }
        }
    }
}

@Composable
fun WeeklyBarChartCanvas(
    habits: List<HabitDisplayModel>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = Color(0xFF00CCCC)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    val days = (6 downTo 0).map { offset ->
        val d = today.minusDays(offset.toLong())
        var due = 0
        var done = 0
        habits.forEach { model ->
            val dayStatus = model.recent7Days.find { it.date == d }
            val isDue = dayStatus?.isDue ?: (d >= model.habit.createdDate && model.schedule?.daysMask?.contains(d.dayOfWeek) == true)
            if (isDue) {
                due++
                if (dayStatus?.isCompleted == true || dayStatus?.isForgiven == true) done++
            }
        }
        val pct = if (due > 0) (done.toFloat() / due.toFloat()) else 0f
        Triple(d, pct, offset == 0)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { (date, pct, isToday) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(pct.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isToday) Brush.verticalGradient(listOf(primaryColor, secondaryColor))
                                else Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.7f), primaryColor.copy(alpha = 0.5f)))
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEE")),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HeatmapCanvas(
    habits: List<HabitDisplayModel>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val dayOfWeekOffset = (today.dayOfWeek.value % 7)

    Canvas(modifier = modifier) {
        val cols = 12
        val rows = 7
        val labelWidth = 16.dp.toPx()
        val spacing = 4.dp.toPx()
        val cellWidth = (size.width - labelWidth - (cols - 1) * spacing) / cols
        val cellHeight = (size.height - (rows - 1) * spacing) / rows

        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val offsetDays = (11 - c) * 7 + (dayOfWeekOffset - r)
                if (offsetDays < 0) continue
                val d = today.minusDays(offsetDays.toLong())

                var due = 0
                var done = 0
                habits.forEach { model ->
                    val dayStatus = model.recent7Days.find { it.date == d }
                    val isDue = dayStatus?.isDue ?: (d >= model.habit.createdDate && model.schedule?.daysMask?.contains(d.dayOfWeek) == true)
                    if (isDue) {
                        due++
                        if (dayStatus?.isCompleted == true || dayStatus?.isForgiven == true) done++
                    }
                }
                val pct = if (due > 0) done.toFloat() / due.toFloat() else 0f

                val x = labelWidth + c * (cellWidth + spacing)
                val y = r * (cellHeight + spacing)
                val color = if (due > 0) primaryColor.copy(alpha = maxOf(0.15f, pct)) else surfaceVariant.copy(alpha = 0.3f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cellWidth, cellHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun DonutChartPanel(habits: List<HabitDisplayModel>, selectedRange: Int) {
    val slots = listOf("Early Morning", "Morning", "Midday", "Afternoon", "Evening", "Late Night")
    val slotIcons = listOf(Icons.Default.WbSunny, Icons.Default.WbSunny, Icons.Default.AutoAwesome, Icons.Default.Cloud, Icons.Default.WbTwilight, Icons.Default.DarkMode)
    val slotColors = listOf(Color(0xFF00CCCC), Color(0xFF19B6D6), Color(0xFF329FE0), Color(0xFF4B89EB), Color(0xFF6472F5), Color(0xFF7D5CFF))
    val today = LocalDate.now()

    val slotCounts = slots.map { slotName ->
        var count = 0
        habits.filter { it.habit.slot.equals(slotName, ignoreCase = true) }.forEach { model ->
            for (o in 0 until selectedRange) {
                val d = today.minusDays(o.toLong())
                val dayStatus = model.recent7Days.find { it.date == d }
                if (dayStatus?.isCompleted == true || dayStatus?.isForgiven == true) count++
            }
        }
        count
    }
    val totalDoneAll = slotCounts.sum()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                var startAngle = -90f
                slotCounts.forEachIndexed { index, cnt ->
                    val sweep = if (totalDoneAll > 0) (cnt.toFloat() / totalDoneAll.toFloat()) * 360f else 0f
                    if (sweep > 0) {
                        drawArc(
                            color = slotColors[index],
                            startAngle = startAngle,
                            sweepAngle = sweep - 2f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    startAngle += sweep
                }
                if (totalDoneAll == 0) {
                    drawCircle(color = Color.LightGray.copy(alpha = 0.3f), style = Stroke(width = strokeWidth))
                }
            }
            Text("$totalDoneAll", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slots.forEachIndexed { i, slotName ->
                val cnt = slotCounts[i]
                val pct = if (totalDoneAll > 0) (cnt.toFloat() / totalDoneAll.toFloat() * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(slotColors[i])
                        )
                        Icon(slotIcons[i], contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(slotName, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("$pct%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CostCategoryRow(
    iconKey: String,
    title: String,
    subtitle: String,
    p1: String,
    tag1: String,
    p2: String,
    tag2: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                HabitIconView(iconKey = iconKey, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFF2ECC71).copy(alpha = 0.18f)) {
                Text(p1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
            }
            Spacer(modifier = Modifier.height(2.dp))
            val col2 = if (tag2 == "danger") Color(0xFFE11D48) else Color(0xFFE07B00)
            Surface(shape = RoundedCornerShape(99.dp), color = col2.copy(alpha = 0.18f)) {
                Text(p2, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = col2)
            }
        }
    }
}

@Composable
fun MilestonesGrid(
    habits: List<HabitDisplayModel>,
    totDone: Int,
    bestStreak: Int
) {
    val bestAuto = habits.maxOfOrNull { it.status.automaticity } ?: 0f
    val masteredCount = habits.count { it.status.isMastered }

    data class MilestoneSpec(
        val key: String,
        val iconKey: String,
        val label: String,
        val cur: Float,
        val max: Float,
        val fmtVal: String
    )

    val allMilestones = listOf(
        MilestoneSpec("first", "spark", "First check-in", totDone.toFloat(), 1f, "${minOf(totDone, 1)}/1"),
        MilestoneSpec("10c", "check", "10 check-ins", totDone.toFloat(), 10f, "${minOf(totDone, 10)}/10"),
        MilestoneSpec("3s", "bolt", "3-day streak", bestStreak.toFloat(), 3f, "${minOf(bestStreak, 3)}/3"),
        MilestoneSpec("7s", "flame", "7-day streak", bestStreak.toFloat(), 7f, "${minOf(bestStreak, 7)}/7"),
        MilestoneSpec("30s", "flame", "30-day streak", bestStreak.toFloat(), 30f, "${minOf(bestStreak, 30)}/30"),
        MilestoneSpec("100c", "trophy", "100 check-ins", totDone.toFloat(), 100f, "${minOf(totDone, 100)}/100"),
        MilestoneSpec("perfectW", "trophy", "Perfect week", if (bestStreak >= 7) 1f else 0f, 1f, if (bestStreak >= 7) "1/1" else "0/1"),
        MilestoneSpec("100s", "medal", "100-day streak", bestStreak.toFloat(), 100f, "${minOf(bestStreak, 100)}/100"),
        MilestoneSpec("auto50", "target", "50% automatic", bestAuto, 0.5f, "${(minOf(bestAuto / 0.5f, 1f) * 100).toInt()}%"),
        MilestoneSpec("auto100", "spark", "Fully automatic", bestAuto, 1.0f, "${(minOf(bestAuto, 1f) * 100).toInt()}%"),
        MilestoneSpec("365s", "medal", "1-year streak", bestStreak.toFloat(), 365f, "${minOf(bestStreak, 365)}/365"),
        MilestoneSpec("retired", "archive", "A habit retired", masteredCount.toFloat(), 1f, "${minOf(masteredCount, 1)}/1")
    ).map { spec ->
        val prog = minOf(1.0f, spec.cur / spec.max)
        val unlocked = spec.cur >= spec.max
        Triple(spec, prog, unlocked)
    }.sortedWith(compareBy({ it.third }, { -it.second })) // Locked items first (progress desc), unlocked last

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        allMilestones.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (spec, prog, unlocked) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    drawArc(
                                        brush = Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))),
                                        startAngle = -90f,
                                        sweepAngle = 360f * prog,
                                        useCenter = false,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                HabitIconView(iconKey = spec.iconKey, modifier = Modifier.size(18.dp), tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(spec.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(spec.fmtVal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (row.size < 3) {
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun StoragePanelContent(bestStreak: Int) {
    val r1 = bestStreak >= 7
    val r2 = bestStreak >= 30
    val r3 = bestStreak >= 7 // perfect week
    val r4 = bestStreak >= 100
    val r5 = bestStreak >= 365

    val earned = (if (r1) 0.5 else 0.0) + (if (r2) 1.0 else 0.0) + (if (r3) 0.5 else 0.0) + (if (r4) 1.0 else 0.0) + (if (r5) 2.0 else 0.0)
    val totalGB = minOf(10.0, 5.0 + earned)
    val pctFill = (totalGB / 10.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("${totalGB.format(1)} GB of 10 GB", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (earned > 0) {
                Text("+${earned.format(1)} GB earned", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pctFill)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("5 GB free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(10.0 - totalGB).format(1)} GB more to unlock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(4.dp))

        StorageRewardRow("flame", "7-day streak", r1, 0.5)
        StorageRewardRow("flame", "30-day streak", r2, 1.0)
        StorageRewardRow("trophy", "Perfect week", r3, 0.5)
        StorageRewardRow("trophy", "100-day streak", r4, 1.0)
        StorageRewardRow("medal", "1-year streak", r5, 2.0)

        val nextUnlock = listOf(
            Triple(7, "7-day streak", 0.5),
            Triple(30, "30-day streak", 1.0),
            Triple(100, "100-day streak", 1.0),
            Triple(365, "1-year streak", 2.0)
        ).firstOrNull { !r1 || (it.first == 30 && !r2) || (it.first == 100 && !r4) || (it.first == 365 && !r5) }

        if (nextUnlock != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Next unlock: ${nextUnlock.second} — earn +${nextUnlock.third.format(1)} GB of storage.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StorageRewardRow(iconKey: String, title: String, unlocked: Boolean, gb: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (unlocked) Color(0xFF2ECC71).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (unlocked) Color(0xFF2ECC71).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (unlocked) Color(0xFF2ECC71) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    HabitIconView(iconKey = if (unlocked) "check" else iconKey, modifier = Modifier.size(15.dp), tint = if (unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(if (unlocked) "Unlocked · +${gb.format(1)} GB storage" else "Locked · +${gb.format(1)} GB storage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("+${gb.format(1)} GB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (unlocked) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF7D5CFF)
    }
}

@Composable
fun StatsHeroCard(
    ranges: List<Pair<Int, String>>,
    selectedRange: Int,
    onRangeSelected: (Int) -> Unit,
    avgPercent: Int,
    bestStreak: Int,
    totDone: Int,
    gracePoolRemaining: Int,
    onOpenHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF5A52D9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Time range segment buttons + Help ? Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(13.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ranges.forEach { (days, label) ->
                        val selected = selectedRange == days
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clickable { onRangeSelected(days) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color.White else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) Color(0xFF5A52D9) else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Help ? Icon Button
                Surface(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onOpenHelp() },
                    color = Color.White.copy(alpha = 0.22f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Stats Help",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // 2. 2x2 Tiles Grid inside card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroKPITile(
                        title = "Completion rate",
                        value = "$avgPercent%",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    HeroKPITile(
                        title = "Best active streak",
                        value = "$bestStreak ${if (bestStreak == 1) "day" else "days"}",
                        icon = Icons.Default.LocalFireDepartment,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroKPITile(
                        title = "Check-ins",
                        value = "$totDone",
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f)
                    )
                    HeroKPITile(
                        title = "Grace days left",
                        value = "$gracePoolRemaining/2",
                        icon = Icons.Default.Security,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroKPITile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1
            )
        }
    }
}

fun Double.format(digits: Int) = java.lang.String.format(java.util.Locale.getDefault(), "%.${digits}f", this)
