package tn.keeftalk.chat.ui.habits.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import tn.keeftalk.chat.ui.habits.HabitsTab

data class HabitsRailItem(
    val tab: HabitsTab,
    val label: String,
    val icon: ImageVector
)

val HABITS_RAIL_ITEMS = listOf(
    HabitsRailItem(HabitsTab.HABITS, "Habits", Icons.Default.CheckCircle),
    HabitsRailItem(HabitsTab.STATS, "Stats", Icons.Default.BarChart)
)

@Composable
fun HabitsHeroRowWithRail(
    selectedTab: HabitsTab,
    onSelectTab: (HabitsTab) -> Unit,
    isRailExpanded: Boolean,
    onToggleRail: () -> Unit,
    modifier: Modifier = Modifier,
    heroCardContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
    ) {
        // Hero Card slot with 48dp end padding to make room for rail (40dp) + gap (8dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 48.dp)
                .height(156.dp)
        ) {
            heroCardContent()
        }

        // Section Rail overlay pinned to TopEnd
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(10f)
        ) {
            HabitsSectionRail(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                isExpanded = isRailExpanded,
                onToggleRail = onToggleRail
            )
        }
    }
}

@Composable
fun HabitsSectionRail(
    selectedTab: HabitsTab,
    onSelectTab: (HabitsTab) -> Unit,
    isExpanded: Boolean,
    onToggleRail: () -> Unit,
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
            .height(156.dp)
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HABITS_RAIL_ITEMS.forEach { item ->
                val isSelected = selectedTab == item.tab
                HabitsRailButton(
                    item = item,
                    isSelected = isSelected,
                    isRailExpanded = isExpanded,
                    onClick = {
                        if (item.tab != selectedTab) {
                            onSelectTab(item.tab)
                        } else {
                            onToggleRail()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HabitsRailButton(
    item: HabitsRailItem,
    isSelected: Boolean,
    isRailExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaText by animateFloatAsState(
        targetValue = if (isRailExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = if (isRailExpanded) 120 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "textAlpha"
    )

    val buttonBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = buttonBg,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )

            if (isRailExpanded || alphaText > 0f) {
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.alpha(alphaText)
                )
            }
        }
    }
}
