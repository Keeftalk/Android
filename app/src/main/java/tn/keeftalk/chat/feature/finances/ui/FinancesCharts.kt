package tn.keeftalk.chat.feature.finances.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.feature.finances.viewmodel.MonthlySummary
import kotlin.math.max

@Composable
fun SparklineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White
) {
    if (data.size < 2) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minVal = data.minOrNull() ?: 0.0
        val maxVal = data.maxOrNull() ?: 1.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal

        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = height - 6.dp.toPx() - (((value - minVal) / range).toFloat() * (height - 14.dp.toPx()))
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        val gradient = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.0f)),
            startY = 0f,
            endY = height
        )

        drawPath(path = fillPath, brush = gradient)
        drawPath(path = strokePath, color = lineColor, style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val lastPoint = points.last()
        drawCircle(color = lineColor, radius = 4.5f.dp.toPx(), center = Offset(lastPoint.x - 4.dp.toPx(), lastPoint.y))
    }
}

@Composable
fun DualBarChart(
    data: List<MonthlySummary>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxVal = max(1.0, data.maxOf { max(it.inc, it.exp) }) * 1.1

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val width = size.width
            val height = size.height
            val groupWidth = width / data.size
            val barWidth = (groupWidth / 3f).coerceAtMost(14.dp.toPx())

            data.forEachIndexed { i, summary ->
                val centerX = groupWidth * i + groupWidth / 2f
                val hInc = ((summary.inc / maxVal) * height).toFloat()
                val hExp = ((summary.exp / maxVal) * height).toFloat()

                // Income bar (Cyan)
                val incX = centerX - barWidth - 2.dp.toPx()
                val incY = height - hInc
                drawRoundRect(
                    color = Color(0xFF00CCCC),
                    topLeft = Offset(incX, incY),
                    size = Size(barWidth, hInc),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Expense bar (Purple)
                val expX = centerX + 2.dp.toPx()
                val expY = height - hExp
                drawRoundRect(
                    color = Color(0xFF7D5CFF),
                    topLeft = Offset(expX, expY),
                    size = Size(barWidth, hExp),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { summary ->
                Text(
                    text = summary.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class DonutSegment(val value: Double, val color: Color, val label: String)

@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier
) {
    val total = max(1.0, segments.sumOf { it.value })

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw background circle
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        var startAngle = -90f
        segments.forEach { segment ->
            val sweepAngle = ((segment.value / total) * 360f).toFloat()
            if (sweepAngle > 0f) {
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = (sweepAngle - 2f).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            startAngle += sweepAngle
        }
    }
}

@Composable
fun DailySpendBarChart(
    dailyAmounts: List<Double>,
    average: Double,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White
) {
    if (dailyAmounts.isEmpty()) return
    val maxVal = max(1.0, dailyAmounts.maxOrNull() ?: 1.0)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val step = width / dailyAmounts.size
        val barWidth = (step - 3.dp.toPx()).coerceAtLeast(2.dp.toPx())

        dailyAmounts.forEachIndexed { i, amt ->
            val h = ((amt / maxVal) * (height - 8.dp.toPx())).toFloat().coerceAtLeast(2.dp.toPx())
            val x = i * step + 1.dp.toPx()
            val y = height - h
            val alpha = if (i == dailyAmounts.size - 1) 1f else 0.5f

            drawRoundRect(
                color = barColor.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Draw average line
        val avgY = height - ((average / maxVal) * (height - 8.dp.toPx())).toFloat()
        drawLine(
            color = barColor.copy(alpha = 0.8f),
            start = Offset(0f, avgY),
            end = Offset(width, avgY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

@Composable
fun LineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF7D5CFF)
) {
    if (data.size < 2) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val minVal = minOf(0.0, data.minOrNull() ?: 0.0)
        val maxVal = maxOf(1.0, data.maxOrNull() ?: 1.0)
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal

        val points = data.mapIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = height - 8.dp.toPx() - (((value - minVal) / range).toFloat() * (height - 16.dp.toPx()))
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        val zeroY = height - 8.dp.toPx() - (((0.0 - minVal) / range).toFloat() * (height - 16.dp.toPx()))

        drawPath(path = fillPath, color = lineColor.copy(alpha = 0.13f))
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, zeroY),
            end = Offset(width, zeroY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HealthRingChart(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background ring
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            val sweepAngle = (score / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$score",
            fontSize = 28.sp,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
