package tn.keeftalk.chat.ui.feed.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.MarketAsset
import tn.keeftalk.chat.domain.model.VerseInfo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailModal(
    asset: MarketAsset,
    onDismiss: () -> Unit
) {
    val isUp = asset.change24h >= 0
    val changeColor = if (isUp) Color(0xFF12B76A) else Color(0xFFF04438)

    val badgeGradient = when (asset.id.lowercase()) {
        "btc" -> Brush.linearGradient(listOf(Color(0xFFF7931A), Color(0xFFF2A33C)))
        "eth" -> Brush.linearGradient(listOf(Color(0xFF627EEA), Color(0xFF8A9BF0)))
        "gold" -> Brush.linearGradient(listOf(Color(0xFFD4AF37), Color(0xFFF0D060)))
        else -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF60A5FA)))
    }

    val priceText = if (asset.price >= 1000) {
        "$${String.format(Locale.US, "%,.0f", asset.price)}"
    } else {
        "$${String.format(Locale.US, "%.${asset.decimals}f", asset.price)}"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFEEF0F7),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${asset.label} details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1020)
                )

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF3C4257), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(badgeGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = asset.symbol,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (asset.id.lowercase() == "gold") Color(0xFF6B5300) else Color.White
                            )
                        }

                        Column {
                            Text(asset.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1020))
                            Text(
                                if (asset.id == "eur") "Euro / US Dollar" else asset.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF8B90A4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = priceText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1020),
                        letterSpacing = (-1.4).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${if (isUp) "▲ +" else "▼ "}${String.format(Locale.US, "%.2f", asset.change24h)}% · 24h",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = changeColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7-day Chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        if (asset.sparkline.size > 1) {
                            val w = size.width
                            val h = size.height
                            val min = asset.sparkline.minOrNull() ?: 0.0
                            val max = asset.sparkline.maxOrNull() ?: 100.0
                            val range = (max - min).coerceAtLeast(0.0001)
                            val stepX = w / (asset.sparkline.size - 1)

                            val linePath = Path()
                            val areaPath = Path()

                            asset.sparkline.forEachIndexed { i, value ->
                                val x = i * stepX
                                val y = (h - ((value - min) / range * h)).toFloat().coerceIn(0f, h)
                                if (i == 0) {
                                    linePath.moveTo(x, y)
                                    areaPath.moveTo(x, h)
                                    areaPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    areaPath.lineTo(x, y)
                                }
                                if (i == asset.sparkline.size - 1) {
                                    areaPath.lineTo(x, h)
                                    areaPath.close()
                                }
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    listOf(changeColor.copy(alpha = 0.28f), changeColor.copy(alpha = 0f))
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = changeColor,
                                style = Stroke(width = 2.2.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Key Stats Header
            Text(
                text = "KEY STATS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF8B90A4),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Grid 2x2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(modifier = Modifier.weight(1f), label = "7-day change", value = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f", asset.change24h)}%", valueColor = changeColor)
                    StatBox(modifier = Modifier.weight(1f), label = "24h change", value = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f", asset.change24h)}%", valueColor = changeColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val maxVal = asset.sparkline.maxOrNull() ?: asset.price
                    val minVal = asset.sparkline.minOrNull() ?: asset.price
                    StatBox(modifier = Modifier.weight(1f), label = "7-day high", value = "$${String.format(Locale.US, "%.${asset.decimals}f", maxVal)}")
                    StatBox(modifier = Modifier.weight(1f), label = "7-day low", value = "$${String.format(Locale.US, "%.${asset.decimals}f", minVal)}")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF0D1020)
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B90A4), letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, letterSpacing = (-0.4).sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseDetailModal(
    verse: VerseInfo,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val goldColor = Color(0xFFE8C766)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFEEF0F7),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verse of the day",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1020)
                )

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF3C4257), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Verse Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp)),
                color = Color.Transparent,
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF131A30), Color(0xFF201A3D), Color(0xFF2C1A3E))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = goldColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TODAY", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = goldColor)
                            }
                            Text(verse.hijriDate, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.55f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = verse.arabic,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            style = LocalTextStyle.current.copy(
                                textDirection = TextDirection.Rtl,
                                lineHeight = 48.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = verse.english,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "— ${verse.reference}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF0D1020), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy verse", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1020))
                    }
                }

                Surface(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF0D1020), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1020))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
