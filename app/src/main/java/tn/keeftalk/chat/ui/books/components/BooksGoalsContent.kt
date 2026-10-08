package tn.keeftalk.chat.ui.books.components

import android.app.TimePickerDialog
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BooksGoalsContent(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    isRailExpanded: Boolean,
    focusedPeriod: String, // "day", "month", "year"
    onSelectPeriod: (String) -> Unit,
    dailyMinutesCurrent: Int,
    dailyMinutesGoal: Int,
    monthlyHoursCurrent: Float,
    monthlyHoursGoal: Int,
    yearlyBooksCurrent: Int,
    yearlyBooksGoal: Int,
    streakDays: Int,
    historyMap: Map<String, Int>, // yyyy-MM-dd -> minutes
    reminderEnabled: Boolean,
    reminderTime: String, // "08:13"
    onToggleReminder: (Boolean) -> Unit,
    onUpdateTime: (String) -> Unit,
    onDecrementGoal: () -> Unit,
    onIncrementGoal: () -> Unit,
    onOpenReadNow: () -> Unit,
    onOpenInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeroRowWithRail(
                currentTab = currentTab,
                onSelectTab = onSelectTab,
                isRailExpanded = isRailExpanded
            ) {
                GoalsHeroCard(
                    focusedPeriod = focusedPeriod,
                    onSelectPeriod = onSelectPeriod,
                    dailyCurrent = dailyMinutesCurrent,
                    dailyGoal = dailyMinutesGoal,
                    monthlyCurrent = monthlyHoursCurrent,
                    monthlyGoal = monthlyHoursGoal,
                    yearlyCurrent = yearlyBooksCurrent,
                    yearlyGoal = yearlyBooksGoal,
                    onDecrementGoal = onDecrementGoal,
                    onIncrementGoal = onIncrementGoal,
                    onOpenInfo = onOpenInfo
                )
            }
        }

        item {
            StreakRunwayCard(streakDays = streakDays, dailyGoal = dailyMinutesGoal, todayMinutes = dailyMinutesCurrent)
        }

        item {
            Last28DaysCard(historyMap = historyMap, dailyGoal = dailyMinutesGoal)
        }

        item {
            ForecastCard(
                focusedPeriod = focusedPeriod,
                onSelectPeriod = onSelectPeriod,
                dailyCurrent = dailyMinutesCurrent,
                dailyGoal = dailyMinutesGoal,
                monthlyCurrent = monthlyHoursCurrent,
                monthlyGoal = monthlyHoursGoal,
                yearlyCurrent = yearlyBooksCurrent,
                yearlyGoal = yearlyBooksGoal
            )
        }

        item {
            ReminderCard(
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                onToggleReminder = onToggleReminder,
                onUpdateTime = { time -> onUpdateTime(time) }
            )
        }

        item {
            val dailyLeft = maxOf(0, dailyMinutesGoal - dailyMinutesCurrent)
            val isDone = dailyMinutesCurrent >= dailyMinutesGoal
            val buttonText = if (isDone) "▶ Keep reading" else "▶ Read $dailyLeft min now"

            val gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF6C63FF), Color(0xFF00CCCC))
            )
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradient)
                    .clickable { onOpenReadNow() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GoalsHeroCard(
    focusedPeriod: String,
    onSelectPeriod: (String) -> Unit,
    dailyCurrent: Int,
    dailyGoal: Int,
    monthlyCurrent: Float,
    monthlyGoal: Int,
    yearlyCurrent: Int,
    yearlyGoal: Int,
    onDecrementGoal: () -> Unit,
    onIncrementGoal: () -> Unit,
    onOpenInfo: () -> Unit = {}
) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6C63FF), Color(0xFF7D5CFF), Color(0xFF00CCCC))
    )

    val caption = when (focusedPeriod) {
        "month" -> "MY GOALS · UNTIL END OF MONTH"
        "year" -> "MY GOALS · UNTIL 31 DEC"
        else -> "MY GOALS · TODAY"
    }

    val currentVal = when (focusedPeriod) {
        "month" -> monthlyCurrent
        "year" -> yearlyCurrent.toFloat()
        else -> dailyCurrent.toFloat()
    }
    val goalVal = when (focusedPeriod) {
        "month" -> monthlyGoal.toFloat()
        "year" -> yearlyGoal.toFloat()
        else -> dailyGoal.toFloat()
    }
    val unitStr = when (focusedPeriod) {
        "month" -> "hours"
        "year" -> "books"
        else -> "minutes"
    }
    val stepLabel = when (focusedPeriod) {
        "month" -> "goal · 1 h"
        "year" -> "goal · 1 book"
        else -> "goal · 5 min"
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = caption,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 0.8.sp
                )

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
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: 3 concentric orbit rings in 72dp square
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(72.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val scale = size.width / 120f

                        // Year ring (r = 50)
                        val yearFrac = min(1f, yearlyCurrent.toFloat() / maxOf(1, yearlyGoal))
                        drawCircle(color = Color.White.copy(alpha = 0.22f), radius = 50f * scale, center = center, style = Stroke(width = if (focusedPeriod == "year") 8f else 4.5f))
                        if (yearFrac > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * yearFrac,
                                useCenter = false,
                                topLeft = Offset(center.x - 50f * scale, center.y - 50f * scale),
                                size = Size(100f * scale, 100f * scale),
                                style = Stroke(width = if (focusedPeriod == "year") 8f else 4.5f, cap = StrokeCap.Round)
                            )
                            val angleRad = Math.toRadians((-90.0 + 360.0 * yearFrac))
                            val dotX = center.x + (50f * scale) * cos(angleRad).toFloat()
                            val dotY = center.y + (50f * scale) * sin(angleRad).toFloat()
                            drawCircle(color = if (focusedPeriod == "year") Color(0xFFFFD43B) else Color.White, radius = if (focusedPeriod == "year") 3f else 2.5f, center = Offset(dotX, dotY))
                        }

                        // Month ring (r = 38)
                        val monthFrac = min(1f, monthlyCurrent / maxOf(1, monthlyGoal))
                        drawCircle(color = Color.White.copy(alpha = 0.22f), radius = 38f * scale, center = center, style = Stroke(width = if (focusedPeriod == "month") 8f else 4.5f))
                        if (monthFrac > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * monthFrac,
                                useCenter = false,
                                topLeft = Offset(center.x - 38f * scale, center.y - 38f * scale),
                                size = Size(76f * scale, 76f * scale),
                                style = Stroke(width = if (focusedPeriod == "month") 8f else 4.5f, cap = StrokeCap.Round)
                            )
                            val angleRad = Math.toRadians((-90.0 + 360.0 * monthFrac))
                            val dotX = center.x + (38f * scale) * cos(angleRad).toFloat()
                            val dotY = center.y + (38f * scale) * sin(angleRad).toFloat()
                            drawCircle(color = if (focusedPeriod == "month") Color(0xFFFFD43B) else Color.White, radius = if (focusedPeriod == "month") 3f else 2.5f, center = Offset(dotX, dotY))
                        }

                        // Day ring (r = 26)
                        val dayFrac = min(1f, dailyCurrent.toFloat() / maxOf(1, dailyGoal))
                        drawCircle(color = Color.White.copy(alpha = 0.22f), radius = 26f * scale, center = center, style = Stroke(width = if (focusedPeriod == "day") 8f else 4.5f))
                        if (dayFrac > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * dayFrac,
                                useCenter = false,
                                topLeft = Offset(center.x - 26f * scale, center.y - 26f * scale),
                                size = Size(52f * scale, 52f * scale),
                                style = Stroke(width = if (focusedPeriod == "day") 8f else 4.5f, cap = StrokeCap.Round)
                            )
                            val angleRad = Math.toRadians((-90.0 + 360.0 * dayFrac))
                            val dotX = center.x + (26f * scale) * cos(angleRad).toFloat()
                            val dotY = center.y + (26f * scale) * sin(angleRad).toFloat()
                            drawCircle(color = if (focusedPeriod == "day") Color(0xFFFFD43B) else Color.White, radius = if (focusedPeriod == "day") 3f else 2.5f, center = Offset(dotX, dotY))
                        }
                    }
                }

                // Right: Value, unit, stepper
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val valueStr = if (focusedPeriod == "month") String.format("%.1f", monthlyCurrent) else currentVal.roundToInt().toString()
                    val goalStr = goalVal.roundToInt().toString()

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = valueStr,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "/$goalStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                        )
                    }

                    Text(
                        text = unitStr,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StepperButton(text = "−", onClick = onDecrementGoal)
                        Text(
                            text = stepLabel,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        StepperButton(text = "+", onClick = onIncrementGoal)
                    }
                }
            }

            // Pills Day / Month / Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GoalPill(
                    label = "Day",
                    isSelected = focusedPeriod == "day",
                    onClick = { onSelectPeriod("day") },
                    modifier = Modifier.weight(1f)
                )
                GoalPill(
                    label = "Month",
                    isSelected = focusedPeriod == "month",
                    onClick = { onSelectPeriod("month") },
                    modifier = Modifier.weight(1f)
                )
                GoalPill(
                    label = "Year",
                    isSelected = focusedPeriod == "year",
                    onClick = { onSelectPeriod("year") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Insight strip
            val insightText = when (focusedPeriod) {
                "month" -> {
                    val leftH = maxOf(0f, monthlyGoal - monthlyCurrent)
                    val daysLeft = LocalDate.now().let { it.lengthOfMonth() - it.dayOfMonth + 1 }
                    if (monthlyCurrent >= monthlyGoal) "🛰 ✅ Month goal reached"
                    else {
                        val minPerDay = ceil((leftH * 60f) / maxOf(1, daysLeft)).roundToInt()
                        "🛰 ${String.format("%.1f", leftH)}h left · ${minPerDay} min/day for $daysLeft days"
                    }
                }
                "year" -> {
                    val leftBooks = maxOf(0, yearlyGoal - yearlyCurrent)
                    val daysInYear = LocalDate.now().let { if (it.isLeapYear) 366 else 365 }
                    val dayOfYear = LocalDate.now().dayOfYear
                    val daysLeftInYear = maxOf(1, daysInYear - dayOfYear + 1)
                    if (yearlyCurrent >= yearlyGoal) "🛰 ✅ Year goal reached"
                    else {
                        val interval = ceil(daysLeftInYear.toFloat() / maxOf(1, leftBooks)).roundToInt()
                        "🛰 $leftBooks books left · 1 every $interval days"
                    }
                }
                else -> {
                    val leftMin = maxOf(0, dailyGoal - dailyCurrent)
                    val pages = (leftMin * 38f / 60f).roundToInt()
                    if (dailyCurrent >= dailyGoal) "🛰 ✅ Done for today"
                    else "🛰 $leftMin min left · ≈ $pages pages"
                }
            }

            Surface(
                color = Color.White.copy(alpha = 0.13f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(26.dp)
            ) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = insightText,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.22f),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier
            .size(26.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GoalPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f)
    val textColor = if (isSelected) Color(0xFF6C63FF) else Color.White

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(32.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun StreakRunwayCard(
    streakDays: Int,
    dailyGoal: Int,
    todayMinutes: Int
) {
    val milestones = listOf(1, 3, 7, 14, 30, 60, 100)

    val infiniteTransition = rememberInfiniteTransition(label = "rocket")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rocketBob"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 STREAK RUNWAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$streakDays days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00CCCC)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "🚀",
                    fontSize = 20.sp,
                    modifier = Modifier.offset(y = offsetY.dp)
                )
            }

            // 6dp track with 7 milestone nodes (22dp circles)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                val progressFraction = min(1f, streakDays / 100f)
                val fillGradient = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00CCCC), Color(0xFF6C63FF))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(fillGradient)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    milestones.forEach { m ->
                        val isReached = streakDays >= m
                        val bg = if (isReached) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface
                        val borderCol = if (isReached) Color.Transparent else MaterialTheme.colorScheme.outline
                        val textColor = if (isReached) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(2.dp, borderCol, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val nextMilestone = milestones.firstOrNull { it > streakDays } ?: 100
            val daysToNext = nextMilestone - streakDays
            val rewardName = when (nextMilestone) {
                1 -> "🌱 Spark"
                3 -> "🔥 Ember"
                7 -> "⭐ Star"
                14 -> "🛰 Satellite"
                30 -> "🚀 Rocket"
                60 -> "🌌 Galaxy"
                else -> "👑 Legend"
            }
            val rewardStr = if (streakDays >= 100) "👑 Legend unlocked" else "$daysToNext days to $rewardName"
            val todayOpenStr = if (todayMinutes < dailyGoal && streakDays > 0) " · today is still open" else ""
            val runwayInsight = "🛰 $rewardStr · a day counts when you hit your daily goal$todayOpenStr"

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = runwayInsight,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun Last28DaysCard(
    historyMap: Map<String, Int>,
    dailyGoal: Int
) {
    val today = LocalDate.now()
    val past28Days = (27 downTo 0).map { today.minusDays(it.toLong()) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var hitCount = 0
    val cellStates = past28Days.map { date ->
        val dateStr = date.format(formatter)
        val mins = historyMap[dateStr] ?: 0
        val isToday = date == today
        if (mins >= dailyGoal && !isToday) hitCount++
        else if (isToday && mins >= dailyGoal) hitCount++
        Triple(date, mins, isToday)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAST 28 DAYS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$hitCount / 28 hit",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cellStates.chunked(7).forEach { weekChunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekChunk.forEach { (_, mins, isToday) ->
                            val alpha = when {
                                mins <= 0 -> 0.15f
                                mins < dailyGoal * 0.5f -> 0.4f
                                mins < dailyGoal -> 0.6f
                                mins < dailyGoal * 1.5f -> 0.8f
                                else -> 1f
                            }
                            val cellBg = if (mins <= 0) MaterialTheme.colorScheme.surface else Color(0xFF6C63FF).copy(alpha = alpha)
                            val border = if (isToday) BorderStroke(2.dp, Color(0xFF00CCCC)) else null

                            val icon = when {
                                isToday && mins < dailyGoal -> ""
                                mins >= dailyGoal -> "🏆"
                                mins > 0 -> "⚠️"
                                else -> "❌"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cellBg)
                                    .then(if (border != null) Modifier.border(border, RoundedCornerShape(6.dp)) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (icon.isNotEmpty()) {
                                    Text(text = icon, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "🏆 goal hit · ⚠️ read, but short · ❌ missed · darker = more minutes · outlined = today",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ForecastCard(
    focusedPeriod: String,
    onSelectPeriod: (String) -> Unit,
    dailyCurrent: Int,
    dailyGoal: Int,
    monthlyCurrent: Float,
    monthlyGoal: Int,
    yearlyCurrent: Int,
    yearlyGoal: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "FORECAST · TAP TO FOCUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ForecastRow(
                label = "Today",
                progress = dailyCurrent.toFloat() / maxOf(1, dailyGoal),
                insight = if (dailyCurrent >= dailyGoal) "✅ Done for today" else "${maxOf(0, dailyGoal - dailyCurrent)} min left",
                isSelected = focusedPeriod == "day",
                onClick = { onSelectPeriod("day") }
            )

            ForecastRow(
                label = "This month",
                progress = monthlyCurrent / maxOf(1, monthlyGoal),
                insight = if (monthlyCurrent >= monthlyGoal) "✅ Month reached" else "${String.format("%.1f", maxOf(0f, monthlyGoal - monthlyCurrent))}h left",
                isSelected = focusedPeriod == "month",
                onClick = { onSelectPeriod("month") }
            )

            ForecastRow(
                label = "This year",
                progress = yearlyCurrent.toFloat() / maxOf(1, yearlyGoal),
                insight = if (yearlyCurrent >= yearlyGoal) "✅ Year reached" else "${maxOf(0, yearlyGoal - yearlyCurrent)} books left",
                isSelected = focusedPeriod == "year",
                onClick = { onSelectPeriod("year") }
            )
        }
    }
}

@Composable
private fun ForecastRow(
    label: String,
    progress: Float,
    insight: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insight,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { min(1f, progress) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF6C63FF),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun ReminderCard(
    reminderEnabled: Boolean,
    reminderTime: String,
    onToggleReminder: (Boolean) -> Unit,
    onUpdateTime: (String) -> Unit
) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "REMINDER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Only PDF and EPUB books count toward your goals. Time is counted while the reader is open.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.clickable {
                        val parts = reminderTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 13
                        TimePickerDialog(context, { _, h, m ->
                            val newTime = String.format("%02d:%02d", h, m)
                            onUpdateTime(newTime)
                        }, hour, minute, true).show()
                    }
                ) {
                    Text(
                        text = "Daily reminder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Every day at $reminderTime",
                        fontSize = 12.sp,
                        color = Color(0xFF6C63FF),
                        fontWeight = FontWeight.Medium
                    )
                }

            Switch(
                checked = reminderEnabled,
                onCheckedChange = onToggleReminder,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6C63FF)
                )
            )
        }
    }
}
}
