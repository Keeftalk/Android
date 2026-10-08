package tn.keeftalk.chat.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.ui.theme.LocalAppIcons
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF00CCCC)): Color {
    return runCatching {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) Color(colorInt or 0xFF000000) else Color(colorInt)
    }.getOrDefault(defaultColor)
}

fun formatDurationText(minsFloat: Float): String {
    val total = minsFloat.roundToInt()
    val h = total / 60
    val m = total % 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m}m"
}

@Composable
fun getIconForFeature(featureKey: String): ImageVector {
    val icons = LocalAppIcons.current
    return when (featureKey.uppercase()) {
        "CHATS" -> Icons.Default.Chat
        "CALLS" -> icons.phone
        "SMS" -> icons.messageSquare
        "NOTES" -> icons.notes
        "VAULT" -> icons.folderLock
        "SECRETS" -> icons.lock
        "CALENDAR" -> icons.calendar
        "WALLET" -> icons.wallet
        "FINANCES" -> icons.finances
        "EMAIL" -> Icons.Default.Email
        "FEED" -> icons.feed
        "BOOKS" -> Icons.AutoMirrored.Filled.MenuBook
        "MARKET" -> Icons.Default.Storefront
        "HABITS" -> Icons.Default.CheckCircle
        "BROWSER" -> Icons.Default.Language
        "SETTINGS" -> Icons.Default.Settings
        else -> Icons.Default.Settings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOnKeeftalkScreen(
    viewModel: TimeOnKeeftalkViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val primaryGrad = listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Time on Keeftalk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Settings › Account Settings › Time on Keeftalk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4-Pill Segmented Range Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TimeRange.entries.forEach { range ->
                        val isSelected = uiState.selectedRange == range
                        val label = when (range) {
                            TimeRange.DAY -> "Day"
                            TimeRange.WEEK -> "Week"
                            TimeRange.MONTH -> "Month"
                            TimeRange.ALL_TIME -> "All time"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(primaryGrad)
                                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { viewModel.selectRange(range) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Hero Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(primaryGrad), RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.heroHeadline,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = formatDurationText(uiState.totalMinutes),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-1.5).sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "${if (uiState.deltaPercent >= 0) "▲ " else "▼ "}${kotlin.math.abs(uiState.deltaPercent)}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = uiState.deltaLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Stat Chips (3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(uiState.avgStatLabel, uiState.avgStatValue, 1f),
                                Triple(uiState.peakStatLabel, uiState.peakStatValue, 1f),
                                Triple("Top feature", uiState.topFeatureName, 1f)
                            ).forEach { (lbl, valStr, weight) ->
                                Surface(
                                    modifier = Modifier.weight(weight),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(lbl, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                        Text(valStr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Daily / Usage Bar Chart Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(uiState.barChartTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tap a bar to inspect details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (uiState.selectedBarIndex in uiState.barChartPoints.indices) {
                            val activePt = uiState.barChartPoints[uiState.selectedBarIndex]
                            Column(horizontalAlignment = Alignment.End) {
                                Text(activePt.tipLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatDurationText(activePt.minutes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val points = uiState.barChartPoints
                    val maxMins = (points.maxOfOrNull { it.minutes } ?: 10f).coerceAtLeast(1f) * 1.15f
                    val avgMins = uiState.barChartAvgMinutes

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(points) {
                                    detectTapGestures { offset ->
                                        if (points.isNotEmpty()) {
                                            val barWidth = size.width / points.size
                                            val tappedIdx = (offset.x / barWidth).toInt().coerceIn(0, points.size - 1)
                                            viewModel.selectBarIndex(tappedIdx)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val numPts = points.size
                            if (numPts == 0) return@Canvas

                            val slotW = w / numPts
                            val barW = (slotW * 0.62f).coerceAtMost(24.dp.toPx())
                            val chartH = h - 28.dp.toPx()

                            // Grid baseline
                            val avgY = chartH * (1f - (avgMins / maxMins).coerceIn(0f, 1f))
                            drawLine(
                                color = Color(0xFF7D5CFF),
                                start = Offset(0f, avgY),
                                end = Offset(w, avgY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Bars
                            points.forEachIndexed { i, pt ->
                                val barH = (chartH * (pt.minutes / maxMins)).coerceAtLeast(4.dp.toPx())
                                val x = slotW * i + (slotW - barW) / 2f
                                val y = chartH - barH

                                val isSelected = i == uiState.selectedBarIndex
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = if (isSelected) listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))
                                        else listOf(Color(0xFF00CCCC).copy(alpha = 0.5f), Color(0xFF7D5CFF).copy(alpha = 0.5f))
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barW, barH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEachIndexed { i, pt ->
                            if (pt.label.isNotBlank()) {
                                Text(
                                    text = pt.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Feature Distribution Donut Chart Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Feature distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap a feature row to inspect details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    val features = uiState.featureDistribution
                    val totalMins = uiState.totalMinutes.coerceAtLeast(1f)
                    val focusedItem = if (uiState.selectedFeatureIndex in features.indices) features[uiState.selectedFeatureIndex] else null

                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(180.dp)) {
                            var startAngle = -90f
                            val strokeW = 26.dp.toPx()

                            features.forEachIndexed { i, f ->
                                val sweepAngle = (f.minutes / totalMins) * 360f
                                val isSelected = i == uiState.selectedFeatureIndex
                                val drawStroke = if (isSelected) strokeW + 6.dp.toPx() else strokeW
                                val drawAlpha = if (focusedItem == null || isSelected) 1f else 0.3f

                                drawArc(
                                    color = parseHexColor(f.colorHex).copy(alpha = drawAlpha),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 2f,
                                    useCenter = false,
                                    style = Stroke(width = drawStroke)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = focusedItem?.name ?: "Total time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDurationText(focusedItem?.minutes ?: totalMins),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (focusedItem != null) "${(focusedItem.minutes / totalMins * 100f).roundToInt()}% of usage" else "${features.size} features",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Feature List
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        features.forEachIndexed { i, f ->
                            val isSelected = i == uiState.selectedFeatureIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectFeatureIndex(i) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val catColor = parseHexColor(f.colorHex)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(catColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val vector = getIconForFeature(f.key)
                                        Icon(imageVector = vector, contentDescription = f.name, tint = catColor, modifier = Modifier.size(18.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(f.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (f.minutes / features.first().minutes.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(99.dp)),
                                            color = catColor,
                                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatDurationText(f.minutes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("${f.percentage.roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trend Spline Line Chart Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(uiState.trendHeadline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (uiState.selectedTrendIndex in uiState.trendPoints.indices) {
                            val activePt = uiState.trendPoints[uiState.selectedTrendIndex]
                            Column(horizontalAlignment = Alignment.End) {
                                Text(activePt.tipLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${formatDurationText(activePt.dailyMinutes)}/day", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trend Range Toggles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(99.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TrendMode.entries.forEach { mode ->
                            val isSelected = uiState.selectedTrendMode == mode
                            val label = when (mode) {
                                TrendMode.WEEKLY -> "Weekly"
                                TrendMode.MONTHLY -> "Monthly"
                                TrendMode.YEARLY -> "Yearly"
                                TrendMode.ALL_TIME -> "All time"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { viewModel.selectTrendMode(mode) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val trendPoints = uiState.trendPoints
                    val minMins = trendPoints.minOfOrNull { it.dailyMinutes } ?: 0f
                    val maxMins = (trendPoints.maxOfOrNull { it.dailyMinutes } ?: 100f).coerceAtLeast(minMins + 10f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(trendPoints) {
                                    detectTapGestures { offset ->
                                        if (trendPoints.isNotEmpty()) {
                                            val stepX = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                                            val tappedIdx = (offset.x / stepX).roundToInt().coerceIn(0, trendPoints.size - 1)
                                            viewModel.selectTrendIndex(tappedIdx)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height - 24.dp.toPx()
                            val n = trendPoints.size
                            if (n < 2) return@Canvas

                            val stepX = w / (n - 1)

                            fun getY(v: Float): Float {
                                return h * (1f - (v - minMins) / (maxMins - minMins)) + 12.dp.toPx()
                            }

                            // Build spline path
                            val path = Path()
                            val fillPath = Path()

                            val firstY = getY(trendPoints[0].dailyMinutes)
                            path.moveTo(0f, firstY)
                            fillPath.moveTo(0f, h)
                            fillPath.lineTo(0f, firstY)

                            for (i in 1 until n) {
                                val prevX = stepX * (i - 1)
                                val prevY = getY(trendPoints[i - 1].dailyMinutes)
                                val currX = stepX * i
                                val currY = getY(trendPoints[i].dailyMinutes)
                                val midX = (prevX + currX) / 2f

                                path.cubicTo(midX, prevY, midX, currY, currX, currY)
                                fillPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                            }

                            fillPath.lineTo(w, h)
                            fillPath.close()

                            // Draw gradient area fill
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF7D5CFF).copy(alpha = 0.3f), Color.Transparent)
                                )
                            )

                            // Draw line
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))),
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Points / Selection dot
                            val activeIdx = if (uiState.selectedTrendIndex in 0 until n) uiState.selectedTrendIndex else n - 1
                            val activeX = stepX * activeIdx
                            val activeY = getY(trendPoints[activeIdx].dailyMinutes)

                            drawLine(
                                color = Color(0xFF7D5CFF),
                                start = Offset(activeX, 0f),
                                end = Offset(activeX, h),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(activeX, activeY))
                            drawCircle(color = Color(0xFF7D5CFF), radius = 5.dp.toPx(), center = Offset(activeX, activeY))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        trendPoints.forEachIndexed { i, pt ->
                            if (pt.label.isNotBlank()) {
                                Text(pt.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // "Since Day One" Lifetime Stats Grid
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Since day one", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Logged continuously from the day you joined", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LifetimeTile(modifier = Modifier.weight(1f), label = "Member since", value = uiState.lifetimeMemberSince)
                            LifetimeTile(modifier = Modifier.weight(1f), label = "Lifetime total", value = uiState.lifetimeTotalHours)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LifetimeTile(modifier = Modifier.weight(1f), label = "Active days", value = uiState.lifetimeActiveDays)
                            LifetimeTile(modifier = Modifier.weight(1f), label = "Most used", value = uiState.lifetimeMostUsed)
                        }
                    }
                }
            }

            Text(
                text = "Usage is logged on this device and synced to your account.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun LifetimeTile(modifier: Modifier = Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}
