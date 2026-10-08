package tn.keeftalk.chat.ui.feed.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.WeatherInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherCardExact(
    weather: WeatherInfo?,
    isExpanded: Boolean,
    selectedDayIndex: Int,
    onToggleExpand: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onOpenLocationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDay = weather?.isDay ?: true
    val conditionCode = weather?.iconCode ?: "0"

    val backgroundGradient = remember(conditionCode, isDay) {
        getWeatherGradient(conditionCode, isDay)
    }

    val floatAnim = rememberInfiniteTransition(label = "float")
    val floatOffsetY by floatAnim.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)),
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundGradient)
        ) {
            // Radial background glow circles
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = 110.dp.toPx(),
                    center = Offset(size.width + 30.dp.toPx(), -40.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = 90.dp.toPx(),
                    center = Offset(-30.dp.toPx(), size.height + 40.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)
            ) {
                // Top Row: Location & Open Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White.copy(alpha = 0.96f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = weather?.locationName ?: "Locating…",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.96f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .clickable { onOpenLocationSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Set Weather Location",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .clickable { onOpenDetail() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NorthEast,
                                contentDescription = "Open Weather Details",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Temp & Artwork
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = weather?.temperature?.let { Math.round(it).toString() } ?: "--",
                                fontSize = 54.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-3).sp,
                                lineHeight = 54.sp
                            )
                            Text(
                                text = "°",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Text(
                            text = "${weather?.condition ?: "Loading forecast…"} · Feels like ${weather?.apparentTemperature?.toInt() ?: "--"}°",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.92f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "↑ ${Math.round(if (weather?.tempMax != null && weather.tempMax != 0f) weather.tempMax else (weather?.temperature?.plus(4) ?: 0f))}°",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "↓ ${Math.round(if (weather?.tempMin != null && weather.tempMin != 0f) weather.tempMin else (weather?.temperature?.minus(4) ?: 0f))}°",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "· ${weather?.windSpeed?.toInt() ?: 0} km/h",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Floating Weather Artwork
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .offset(y = floatOffsetY.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WeatherArtwork(
                            conditionCode = conditionCode,
                            isDay = isDay,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                // Expandable Section
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        // Day Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val days = listOf("Today", "Tomorrow", "Wed", "Thu", "Fri", "Sat", "Sun")
                            days.forEachIndexed { idx, label ->
                                val isSelected = idx == selectedDayIndex
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.96f)
                                            else Color.White.copy(alpha = 0.14f)
                                        )
                                        .clickable { onSelectDay(idx) }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0F1222) else Color.White.copy(alpha = 0.82f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (selectedDayIndex == 0) "NEXT 24 HOURS" else "24 HOURS FORECAST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Hourly Bar Chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val hourlyTemps = remember(weather, selectedDayIndex) {
                                if (weather != null && weather.hourlyTemps.isNotEmpty()) {
                                    val startIdx = (selectedDayIndex * 24).coerceAtMost(weather.hourlyTemps.size - 24)
                                    val endIdx = (startIdx + 24).coerceAtMost(weather.hourlyTemps.size)
                                    weather.hourlyTemps.subList(startIdx, endIdx)
                                } else {
                                    val base = (weather?.temperature ?: 20.0).toDouble()
                                    List(24) { base }
                                }
                            }
                            val minTemp = hourlyTemps.minOrNull() ?: 15.0
                            val maxTemp = hourlyTemps.maxOrNull() ?: 25.0
                            val range = (maxTemp - minTemp).coerceAtLeast(1.0)

                            hourlyTemps.forEachIndexed { i, temp ->
                                val heightPct = (0.2 + ((temp - minTemp) / range) * 0.8).toFloat()
                                val isNowBar = selectedDayIndex == 0 && i == 0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(heightPct)
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                        .background(
                                            if (isNowBar) Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFBBF24), Color(0xFFF59E0B)))
                                            else Brush.verticalGradient(listOf(Color(0xFF7ED3FF), Color(0xFF3B82F6), Color(0xFF4338CA)))
                                        )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("00", "03", "06", "09", "12", "15", "18", "21").forEach { lbl ->
                                Text(
                                    text = lbl,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Grid (4x2)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            val stats = listOf(
                                "FEELS" to "${weather?.apparentTemperature?.toInt() ?: "--"}°",
                                "HUMID" to "${weather?.humidity ?: "--"}%",
                                "UV" to String.format(Locale.getDefault(), "%.1f", weather?.uvIndex ?: 0f),
                                "WIND" to "${weather?.windSpeed?.toInt() ?: "--"} km/h",
                                "RISE" to (weather?.sunrise ?: "06:00"),
                                "SET" to (weather?.sunset ?: "18:00"),
                                "PRESS" to "${weather?.pressure?.toInt() ?: 1013} hPa",
                                "RAIN" to "${weather?.precipitation ?: 0f} mm"
                            )

                            stats.chunked(4).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { (label, value) ->
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 11.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = value,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val angle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        label = "arrowAngle"
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand toggle",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(angle)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "SHOW LESS" else "SHOW MORE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherArtwork(
    conditionCode: String,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        if (isDay) {
            // Sun Body
            drawCircle(
                color = Color(0xFFFFD25A),
                radius = w * 0.22f,
                center = Offset(cx, cy - h * 0.05f)
            )
            // Sun Outer Glow Ring
            drawCircle(
                color = Color(0xFFFFD25A).copy(alpha = 0.22f),
                radius = w * 0.35f,
                center = Offset(cx, cy - h * 0.05f)
            )
        } else {
            // Moon Path
            drawCircle(
                color = Color(0xFFE8EAFF),
                radius = w * 0.24f,
                center = Offset(cx, cy - h * 0.05f)
            )
        }

        // Cloud overlay
        val cloudColor = Color.White
        drawCircle(
            color = cloudColor,
            radius = w * 0.18f,
            center = Offset(cx - w * 0.12f, cy + h * 0.12f)
        )
        drawCircle(
            color = cloudColor,
            radius = w * 0.14f,
            center = Offset(cx + w * 0.12f, cy + h * 0.15f)
        )
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(cx - w * 0.22f, cy + h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.09f, h * 0.09f)
        )
    }
}

private fun getWeatherGradient(code: String, isDay: Boolean): Brush {
    if (!isDay) {
        return Brush.linearGradient(
            listOf(Color(0xFF0A0F28), Color(0xFF151238), Color(0xFF22184A))
        )
    }
    val codeInt = code.toIntOrNull() ?: 0
    return when {
        codeInt in 0..1 -> Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFA855F7)))
        codeInt in 2..3 -> Brush.linearGradient(listOf(Color(0xFF4B6CB7), Color(0xFF6A82C4), Color(0xFF8B9BC4)))
        codeInt in 45..48 -> Brush.linearGradient(listOf(Color(0xFF5A6678), Color(0xFF7A8694), Color(0xFF9AA5B0)))
        codeInt in 51..67 || codeInt in 80..82 -> Brush.linearGradient(listOf(Color(0xFF1E3A5F), Color(0xFF2C4C7A), Color(0xFF4A6A9A)))
        codeInt in 71..77 || codeInt in 85..86 -> Brush.linearGradient(listOf(Color(0xFF5B7FA8), Color(0xFF8AA8C8), Color(0xFFB8CFE0)))
        codeInt >= 95 -> Brush.linearGradient(listOf(Color(0xFF1A1E3A), Color(0xFF2D2354), Color(0xFF4A2D5A)))
        else -> Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFA855F7)))
    }
}
