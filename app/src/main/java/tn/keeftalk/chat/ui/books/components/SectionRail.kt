package tn.keeftalk.chat.ui.books.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun HeroRowWithRail(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    isRailExpanded: Boolean,
    modifier: Modifier = Modifier,
    heroCardContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp)
    ) {
        // Hero Card slot with 48dp end padding to make room for rail (40dp) + gap (8dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 48.dp)
                .heightIn(min = 170.dp)
        ) {
            heroCardContent()
        }

        // Section Rail overlay pinned to TopEnd
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(10f)
        ) {
            SectionRail(
                currentTab = currentTab,
                onSelectTab = onSelectTab,
                isExpanded = isRailExpanded
            )
        }
    }
}

@Composable
fun SectionRail(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) 136.dp else 40.dp,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "railWidth"
    )

    val shadowElevation = if (isExpanded) 8.dp else 0.dp

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = shadowElevation,
        modifier = modifier
            .width(animatedWidth)
            .height(170.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val tabs = listOf(
                RailTabItem("lib", "Library"),
                RailTabItem("quo", "Quotes"),
                RailTabItem("gol", "Goals"),
                RailTabItem("sta", "Stats"),
                RailTabItem("awd", "Achievements")
            )

            tabs.forEach { item ->
                val isSelected = currentTab == item.key
                RailButton(
                    item = item,
                    isSelected = isSelected,
                    isRailExpanded = isExpanded,
                    onClick = { onSelectTab(item.key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class RailTabItem(
    val key: String,
    val label: String
)

@Composable
private fun RailButton(
    item: RailTabItem,
    isSelected: Boolean,
    isRailExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF6C63FF)
    val containerColor = if (isSelected) activeColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    val textAlpha by animateFloatAsState(
        targetValue = if (isRailExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = if (isRailExpanded) 120 else 0
        ),
        label = "textAlpha"
    )

    val textOffsetX by animateDpAsState(
        targetValue = if (isRailExpanded) 0.dp else 6.dp,
        animationSpec = tween(
            durationMillis = 350,
            delayMillis = if (isRailExpanded) 120 else 0
        ),
        label = "textOffset"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = item.label }
            .padding(start = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            SectionRailIcon(
                tabKey = item.key,
                tint = contentColor
            )

            if (textAlpha > 0.01f) {
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = item.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier
                        .offset(x = textOffsetX)
                        .alpha(textAlpha)
                )
            }
        }
    }
}

@Composable
fun SectionRailIcon(
    tabKey: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 1.9.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val scaleX = w / 24f
        val scaleY = h / 24f

        when (tabKey) {
            "lib" -> {
                drawPath(
                    path = Path().apply {
                        addRect(Rect(5f * scaleX, 4f * scaleY, 9f * scaleX, 20f * scaleY))
                    },
                    color = tint,
                    style = stroke
                )
                drawPath(
                    path = Path().apply {
                        addRect(Rect(11f * scaleX, 4f * scaleY, 15f * scaleX, 20f * scaleY))
                    },
                    color = tint,
                    style = stroke
                )
                drawPath(
                    path = Path().apply {
                        moveTo(17.5f * scaleX, 5.5f * scaleY)
                        lineTo(20.9f * scaleX, 6.5f * scaleY)
                        lineTo(17.3f * scaleX, 20f * scaleY)
                        lineTo(13.9f * scaleX, 19f * scaleY)
                        close()
                    },
                    color = tint,
                    style = stroke
                )
            }
            "quo" -> {
                drawPath(
                    path = Path().apply {
                        moveTo(10f * scaleX, 6f * scaleY)
                        lineTo(6.5f * scaleX, 6f * scaleY)
                        cubicTo(5.12f * scaleX, 6f * scaleY, 4f * scaleX, 7.12f * scaleY, 4f * scaleX, 8.5f * scaleY)
                        lineTo(4f * scaleX, 12f * scaleY)
                        lineTo(8.5f * scaleX, 12f * scaleY)
                        lineTo(8.5f * scaleX, 16f * scaleY)
                        lineTo(4f * scaleX, 16f * scaleY)
                    },
                    color = tint,
                    style = stroke
                )
                drawPath(
                    path = Path().apply {
                        moveTo(20f * scaleX, 6f * scaleY)
                        lineTo(16.5f * scaleX, 6f * scaleY)
                        cubicTo(15.12f * scaleX, 6f * scaleY, 14f * scaleX, 7.12f * scaleY, 14f * scaleX, 8.5f * scaleY)
                        lineTo(14f * scaleX, 12f * scaleY)
                        lineTo(18.5f * scaleX, 12f * scaleY)
                        lineTo(18.5f * scaleX, 16f * scaleY)
                        lineTo(14f * scaleX, 16f * scaleY)
                    },
                    color = tint,
                    style = stroke
                )
            }
            "gol" -> {
                drawCircle(color = tint, radius = 7f * scaleX, center = androidx.compose.ui.geometry.Offset(12f * scaleX, 12f * scaleY), style = stroke)
                drawCircle(color = tint, radius = 4f * scaleX, center = androidx.compose.ui.geometry.Offset(12f * scaleX, 12f * scaleY), style = stroke)
                drawCircle(color = tint, radius = 1.2f * scaleX, center = androidx.compose.ui.geometry.Offset(12f * scaleX, 12f * scaleY))
            }
            "sta" -> {
                drawPath(
                    path = Path().apply {
                        moveTo(5f * scaleX, 20f * scaleY); lineTo(5f * scaleX, 11f * scaleY)
                        moveTo(12f * scaleX, 20f * scaleY); lineTo(12f * scaleX, 5f * scaleY)
                        moveTo(19f * scaleX, 20f * scaleY); lineTo(19f * scaleX, 14f * scaleY)
                    },
                    color = tint,
                    style = stroke
                )
            }
            "awd" -> {
                drawPath(
                    path = Path().apply {
                        moveTo(8f * scaleX, 4f * scaleY)
                        lineTo(16f * scaleX, 4f * scaleY)
                        lineTo(16f * scaleX, 9f * scaleY)
                        cubicTo(16f * scaleX, 11.2f * scaleY, 14.2f * scaleX, 13f * scaleY, 12f * scaleX, 13f * scaleY)
                        cubicTo(9.8f * scaleX, 13f * scaleY, 8f * scaleX, 11.2f * scaleY, 8f * scaleX, 9f * scaleY)
                        close()
                    },
                    color = tint,
                    style = stroke
                )
                drawPath(
                    path = Path().apply {
                        moveTo(8f * scaleX, 6f * scaleY)
                        lineTo(5f * scaleX, 6f * scaleY)
                        cubicTo(3.3f * scaleX, 6f * scaleY, 3.3f * scaleX, 10f * scaleY, 8f * scaleX, 10f * scaleY)
                    },
                    color = tint,
                    style = stroke
                )
                drawPath(
                    path = Path().apply {
                        moveTo(16f * scaleX, 6f * scaleY)
                        lineTo(19f * scaleX, 6f * scaleY)
                        cubicTo(20.7f * scaleX, 6f * scaleY, 20.7f * scaleX, 10f * scaleY, 16f * scaleX, 10f * scaleY)
                    },
                    color = tint,
                    style = stroke
                )
                drawPath(
                    path = Path().apply {
                        moveTo(12f * scaleX, 13f * scaleY); lineTo(12f * scaleX, 17f * scaleY)
                        moveTo(8.5f * scaleX, 20f * scaleY); lineTo(15.5f * scaleX, 20f * scaleY)
                    },
                    color = tint,
                    style = stroke
                )
            }
        }
    }
}
