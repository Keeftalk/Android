package tn.keeftalk.chat.ui.feed.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.prayer.PrayerTime
import java.util.*

@Composable
fun PrayerCardExact(
    hijriDate: String,
    selectedDayIndex: Int,
    prayerTimes: List<PrayerTime>,
    onSelectDay: (Int) -> Unit,
    onClick: () -> Unit,
    onOpenLocationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val goldColor = Color(0xFFE8C766)
    val darkGoldColor = Color(0xFFD4AF37)

    val nextPrayer = prayerTimes.find { it.isNext } ?: prayerTimes.firstOrNull()
    val nowMs = System.currentTimeMillis()

    val countdownText = remember(nextPrayer, nowMs) {
        if (nextPrayer == null) "—"
        else {
            val diffMs = nextPrayer.timestampMs - nowMs
            if (diffMs <= 0) "Now"
            else {
                val seconds = (diffMs / 1000) % 60
                val minutes = (diffMs / (1000 * 60)) % 60
                val hours = (diffMs / (1000 * 60 * 60))
                if (hours > 0) "in ${hours}h ${String.format(Locale.getDefault(), "%02d", minutes)}m ${String.format(Locale.getDefault(), "%02d", seconds)}s"
                else "in ${minutes}m ${String.format(Locale.getDefault(), "%02d", seconds)}s"
            }
        }
    }

    val progressFraction = remember(prayerTimes, nowMs) {
        if (prayerTimes.isEmpty()) 0f
        else {
            val passedCount = prayerTimes.count { it.isPassed }
            (passedCount.toFloat() / prayerTimes.size.toFloat()).coerceIn(0f, 1f)
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "goldPulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0F1530), Color(0xFF1C1640), Color(0xFF2E1650))
                    )
                )
        ) {
            // Background Twinkling Stars
            Canvas(modifier = Modifier.matchParentSize()) {
                val stars = listOf(
                    Offset(size.width * 0.20f, size.height * 0.30f),
                    Offset(size.width * 0.70f, size.height * 0.15f),
                    Offset(size.width * 0.85f, size.height * 0.60f),
                    Offset(size.width * 0.40f, size.height * 0.80f),
                    Offset(size.width * 0.12f, size.height * 0.65f),
                    Offset(size.width * 0.55f, size.height * 0.45f)
                )
                stars.forEach { starOffset ->
                    drawCircle(Color.White.copy(alpha = pulseAlpha * 0.5f), radius = 1.5.dp.toPx(), center = starOffset)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(goldColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = "Mosque icon",
                                tint = goldColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "PRAYER TIMES",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp,
                            color = goldColor
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = hijriDate,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(goldColor.copy(alpha = 0.16f))
                                .clickable { onOpenLocationSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Set Prayer Location",
                                tint = goldColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Day Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val days = listOf("Today", "Tomorrow", "Thu", "Fri", "Sat", "Sun", "Mon")
                    days.forEachIndexed { idx, label ->
                        val isSelected = idx == selectedDayIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(goldColor, darkGoldColor))
                                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                                )
                                .clickable { onSelectDay(idx) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF2A1A00) else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Row: Progress Ring & Next Prayer Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 96dp Circular Progress Ring
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 7.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                            // Background Track
                            drawArc(
                                color = Color.White.copy(alpha = 0.14f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth)
                            )

                            // Foreground Progress
                            drawArc(
                                brush = Brush.linearGradient(listOf(goldColor, Color(0xFF8B5CF6))),
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = nextPrayer?.timeFormatted ?: "--:--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.8).sp
                            )
                            Text(
                                text = "NEXT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                    }

                    // Next Prayer Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedDayIndex == 0) "NEXT PRAYER" else "FIRST PRAYER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(goldColor.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = nextPrayer?.name ?: "—",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.8).sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = nextPrayer?.timeFormatted ?: "—",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.65f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = countdownText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = goldColor,
                            letterSpacing = 0.2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(goldColor, Color(0xFF8B5CF6))))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5 Prayer Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    prayerTimes.forEach { p ->
                        val isNext = p.isNext
                        val isPassed = p.isPassed

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .offset(y = if (isNext) (-2).dp else 0.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isNext) Brush.linearGradient(listOf(goldColor, darkGoldColor))
                                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                                )
                                .then(
                                    if (isNext) Modifier.border(1.dp, goldColor.copy(alpha = pulseAlpha), RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = p.name,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    color = if (isNext) Color(0xFF3C2800).copy(alpha = 0.7f) else Color.White.copy(alpha = if (isPassed) 0.42f else 0.55f)
                                )
                                Text(
                                    text = p.timeFormatted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp,
                                    color = if (isNext) Color(0xFF3A2A00) else Color.White.copy(alpha = if (isPassed) 0.42f else 1.0f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
