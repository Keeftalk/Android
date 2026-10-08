package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun ActivityHeatmap(
    activityData: List<Float>, // 126 values (18 weeks x 7 days)
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Y-axis labels (M, W, F, S)
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "W", "F", "S").forEach { day ->
                Text(text = day, fontSize = 9.sp, color = Color(0xFF8E8BAB))
            }
        }

        // 18-week Grid (7 rows x 18 columns)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (week in 0 until 18) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (day in 0 until 7) {
                        val index = week * 7 + day
                        val val01 = activityData.getOrNull(index) ?: 0f
                        val opacity = if (val01 <= 0f) 0.12f else (0.25f + val01 * 0.75f).coerceAtMost(1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF6C63FF).copy(alpha = opacity))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(
    values: List<Int>,
    unit: String = "",
    labels: List<String>? = null,
    height: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val maxValue = max(1, values.maxOrNull() ?: 1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Y-axis labels (100%, 67%, 33%, 0%)
            Column(
                modifier = Modifier
                    .height(height)
                    .width(26.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1f, 0.67f, 0.33f, 0f).forEach { pct ->
                    Text(
                        text = "${(maxValue * pct).toInt()}$unit",
                        fontSize = 9.sp,
                        color = Color(0xFF8E8BAB)
                    )
                }
            }

            // Bars
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .background(Color(0xFF2B2847).copy(alpha = 0.2f))
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEach { valInt ->
                    val fraction = (valInt.toFloat() / maxValue.toFloat()).coerceIn(0.03f, 1f)
                    val isMax = valInt == maxValue && valInt > 0
                    val brush = if (isMax) {
                        Brush.verticalGradient(listOf(Color(0xFFFFD43B), Color(0xFFFF922B)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF00CCCC), Color(0xFF6C63FF)))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(brush)
                    )
                }
            }
        }

        // X-axis labels
        if (!labels.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(text = label, fontSize = 10.sp, color = Color(0xFF8E8BAB))
                }
            }
        }
    }
}

@Composable
fun AreaChart(
    actualValues: List<Int>,
    unit: String = "",
    projectedValues: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    val allValues = actualValues + projectedValues
    val maxVal = max(1f, (allValues.maxOrNull() ?: 1) * 1.08f)

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .height(88.dp)
                .width(26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(1f, 0.67f, 0.33f, 0f).forEach { pct ->
                Text(
                    text = "${(maxVal * pct).toInt()}$unit",
                    fontSize = 9.sp,
                    color = Color(0xFF8E8BAB)
                )
            }
        }

        // Canvas Area Path
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(88.dp)
        ) {
            val w = size.width
            val h = size.height
            val totalCount = allValues.size - 1
            if (totalCount <= 0) return@Canvas

            fun getX(i: Int): Float = i.toFloat() / totalCount * w
            fun getY(v: Number): Float = h - (v.toFloat() / maxVal * h)

            // Fill area under actual path
            val fillPath = Path().apply {
                moveTo(0f, h)
                actualValues.forEachIndexed { i, valNum ->
                    lineTo(getX(i), getY(valNum))
                }
                lineTo(getX(actualValues.size - 1), h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00CCCC).copy(alpha = 0.5f),
                        Color(0xFF6C63FF).copy(alpha = 0.0f)
                    )
                )
            )

            // Solid Actual Line
            val strokePath = Path().apply {
                actualValues.forEachIndexed { i, valNum ->
                    if (i == 0) moveTo(getX(i), getY(valNum)) else lineTo(getX(i), getY(valNum))
                }
            }
            drawPath(
                path = strokePath,
                color = Color(0xFF00CCCC),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Dashed Projected Line
            if (projectedValues.isNotEmpty()) {
                val projectedPath = Path().apply {
                    val startIdx = actualValues.size - 1
                    projectedValues.forEachIndexed { i, valNum ->
                        val idx = startIdx + i
                        if (i == 0) moveTo(getX(idx), getY(valNum)) else lineTo(getX(idx), getY(valNum))
                    }
                }
                drawPath(
                    path = projectedPath,
                    color = Color(0xFFFFD43B),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            }
        }
    }
}

@Composable
fun DonutProgressRing(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    ringColor: Color = Color(0xFF6C63FF),
    trackColor: Color = Color(0xFF2B2847)
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(
                (this.size.width - radius * 2f) / 2f,
                (this.size.height - radius * 2f) / 2f
            )
            val ringSize = Size(radius * 2f, radius * 2f)

            // Track arc
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            val sweep = (percent.coerceIn(0, 100) / 100f) * 360f
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$percent%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
