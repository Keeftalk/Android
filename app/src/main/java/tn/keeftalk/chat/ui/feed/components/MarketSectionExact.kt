package tn.keeftalk.chat.ui.feed.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.MarketAsset
import java.util.Locale

@Composable
fun MarketSectionExact(
    assets: List<MarketAsset>,
    isLive: Boolean,
    onSelectAsset: (MarketAsset) -> Unit,
    modifier: Modifier = Modifier
) {
    val isKeef = false
    val headerTextColor = if (isKeef) MaterialTheme.colorScheme.onBackground else Color(0xFF0D1020)

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Markets",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = headerTextColor
            )

            Text(
                text = if (isLive) "Live" else "Demo",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                letterSpacing = (-0.1).sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            assets.forEach { asset ->
                MarketTickCard(
                    asset = asset,
                    onClick = { onSelectAsset(asset) }
                )
            }
        }
    }
}

@Composable
fun MarketTickCard(
    asset: MarketAsset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKeef = false
    val isUp = asset.change24h >= 0
    val changeColor = if (isUp) Color(0xFF12B76A) else Color(0xFFF04438)

    val badgeGradient = when (asset.id.lowercase()) {
        "btc" -> Brush.linearGradient(listOf(Color(0xFFF7931A), Color(0xFFF2A33C)))
        "eth" -> Brush.linearGradient(listOf(Color(0xFF627EEA), Color(0xFF8A9BF0)))
        "gold" -> Brush.linearGradient(listOf(Color(0xFFD4AF37), Color(0xFFF0D060)))
        else -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF60A5FA)))
    }

    val priceText = remember(asset.price, asset.decimals) {
        if (asset.price >= 1000) {
            "$${String.format(Locale.US, "%,.0f", asset.price)}"
        } else {
            "$${String.format(Locale.US, "%.${asset.decimals}f", asset.price)}"
        }
    }

    val cardBg = if (isKeef) MaterialTheme.colorScheme.surface else Color.White

    Surface(
        modifier = modifier
            .width(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = cardBg,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 8.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = asset.symbol,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (asset.id.lowercase() == "gold") Color(0xFF6B5300) else Color.White
                        )
                    }

                    Text(
                        text = asset.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isKeef) MaterialTheme.colorScheme.onSurface else Color(0xFF22263A)
                    )
                }

                Text(
                    text = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f", asset.change24h)}%",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = changeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price
            Text(
                text = priceText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0D1020),
                letterSpacing = (-0.6).sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Sparkline Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
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
                            listOf(changeColor.copy(alpha = 0.3f), changeColor.copy(alpha = 0f))
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = changeColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}
