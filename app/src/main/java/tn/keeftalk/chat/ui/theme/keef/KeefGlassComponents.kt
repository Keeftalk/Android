package tn.keeftalk.chat.ui.theme.keef

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.PeriodOfDay

@Composable
fun KeefGlassCard(
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current,
    cornerRadius: Dp = tokens.cornerRadius,
    backgroundColor: Color = tokens.card,
    borderColor: Color = tokens.cardLine,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardOpacity = (tokens.cardOpacityPercent / 100f).coerceIn(0.05f, 1.0f)
    val borderOpacity = (tokens.borderOpacityPercent / 100f).coerceIn(0.05f, 1.0f)

    // Frosted glass background translucency & border highlight adjustment
    val frostedBgColor = backgroundColor.copy(
        alpha = (backgroundColor.alpha * cardOpacity).coerceIn(0.05f, 0.98f)
    )
    val frostedBorderColor = borderColor.copy(
        alpha = (borderColor.alpha * borderOpacity).coerceIn(0.05f, 0.98f)
    )

    Surface(
        modifier = modifier
            .shadow(elevation, shape = shape, clip = false)
            .clip(shape)
            .background(frostedBgColor, shape)
            .border(BorderStroke(1.2.dp, frostedBorderColor), shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        color = Color.Transparent,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f * cardOpacity))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun KeefGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    val buttonOpacity = (tokens.buttonOpacityPercent / 100f).coerceIn(0.05f, 1.0f)
    val borderOpacity = (tokens.borderOpacityPercent / 100f).coerceIn(0.05f, 1.0f)

    val frostedSoftColor = tokens.soft.copy(
        alpha = (tokens.soft.alpha * buttonOpacity * 2.5f).coerceIn(0.08f, 0.95f)
    )
    val frostedBorderColor = tokens.cardLine.copy(
        alpha = (tokens.cardLine.alpha * borderOpacity).coerceIn(0.08f, 0.95f)
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(frostedSoftColor)
            .border(BorderStroke(1.dp, frostedBorderColor), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tokens.ink,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun KeefStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    KeefGlassCard(
        modifier = modifier,
        tokens = tokens,
        cornerRadius = 18.dp,
        elevation = 4.dp
    ) {
        Column {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.ink,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.ink2
            )
        }
    }
}

@Composable
fun KeefQuickActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    KeefGlassCard(
        modifier = modifier,
        tokens = tokens,
        cornerRadius = 18.dp,
        elevation = 4.dp,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.ink
            )
        }
    }
}

@Composable
fun KeefCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    KeefGlassCard(
        modifier = modifier,
        tokens = tokens,
        cornerRadius = 16.dp,
        elevation = 4.dp,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.soft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tokens.accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.ink
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = tokens.ink2
                )
            }
        }
    }
}

@Composable
fun KeefWeatherWidget(
    envState: EnvironmentalState,
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current,
    onWidgetClick: (() -> Unit)? = null
) {
    KeefGlassCard(
        modifier = modifier.fillMaxWidth(),
        tokens = tokens,
        cornerRadius = 24.dp,
        elevation = 8.dp,
        onClick = onWidgetClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${envState.temperatureCelsius.toInt()}°",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.ink,
                        letterSpacing = (-2).sp,
                        lineHeight = 52.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = envState.weatherDescription,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "H ${envState.tempMax.toInt()}° · L ${envState.tempMin.toInt()}° · ${envState.locationName}",
                        fontSize = 12.5.sp,
                        color = tokens.ink2
                    )
                }

                Crossfade(targetState = envState.period == PeriodOfDay.NIGHT, label = "weatherIcon") { isNight ->
                    Icon(
                        imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                        contentDescription = if (isNight) "Night" else "Day",
                        tint = tokens.accent,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            if (envState.hourlyTemps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = tokens.cardLine, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val temps = envState.hourlyTemps.take(6)
                    temps.forEachIndexed { index, temp ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (index == 0) "Now" else "+${index}h",
                                fontSize = 11.5.sp,
                                color = tokens.ink2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${temp.toInt()}°",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeefGlassContainer(
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current,
    cornerRadius: Dp = tokens.cornerRadius,
    backgroundColor: Color = tokens.card,
    borderColor: Color = tokens.cardLine,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val opacity = (tokens.containerOpacityPercent / 100f).coerceIn(0.05f, 1.0f)
    val borderOpacity = (tokens.borderOpacityPercent / 100f).coerceIn(0.05f, 1.0f)

    val frostedBgColor = backgroundColor.copy(
        alpha = (backgroundColor.alpha * opacity).coerceIn(0.05f, 0.98f)
    )
    val frostedBorderColor = borderColor.copy(
        alpha = (borderColor.alpha * borderOpacity).coerceIn(0.05f, 0.98f)
    )

    Surface(
        modifier = modifier
            .shadow(elevation, shape = shape, clip = false)
            .clip(shape)
            .background(frostedBgColor, shape)
            .border(BorderStroke(1.2.dp, frostedBorderColor), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.Transparent,
        shape = shape
    ) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.06f * opacity))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun KeefAdaptiveFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    label: String? = null,
    expanded: Boolean = false,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    val fabOpacity = (tokens.fabOpacityPercent / 100f).coerceIn(0.05f, 1.0f)
    val borderOpacity = (tokens.borderOpacityPercent / 100f).coerceIn(0.05f, 1.0f)

    val shape = RoundedCornerShape(18.dp)
    val frostedFabColor = tokens.accent.copy(
        alpha = (0.35f + 0.60f * fabOpacity).coerceIn(0.15f, 0.98f)
    )
    val frostedBorderColor = tokens.cardLine.copy(
        alpha = (tokens.cardLine.alpha * borderOpacity).coerceIn(0.10f, 0.95f)
    )

    Surface(
        modifier = modifier
            .shadow(8.dp, shape = shape, clip = false)
            .clip(shape)
            .background(frostedFabColor, shape)
            .border(BorderStroke(1.2.dp, frostedBorderColor), shape)
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (expanded && !label.isNullOrBlank()) 18.dp else 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label ?: "Add",
                tint = tokens.onAccent,
                modifier = Modifier.size(24.dp)
            )
            if (expanded && !label.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = tokens.onAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
            }
        }
    }
}

@Composable
fun KeefGlassPopupWindow(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        val shape = RoundedCornerShape(tokens.cornerRadius + 4.dp)
        val dialogOpacity = (tokens.dialogOpacityPercent / 100f).coerceIn(0.05f, 1.0f)
        val borderOpacity = (tokens.borderOpacityPercent / 100f).coerceIn(0.05f, 1.0f)

        val frostedBgColor = tokens.card.copy(
            alpha = (tokens.card.alpha * dialogOpacity * 1.3f).coerceIn(0.20f, 0.98f)
        )
        val frostedBorderColor = tokens.cardLine.copy(
            alpha = (tokens.cardLine.alpha * borderOpacity * 1.5f).coerceIn(0.20f, 0.98f)
        )

        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .shadow(16.dp, shape = shape, clip = false)
                .clip(shape)
                .background(frostedBgColor, shape)
                .border(BorderStroke(1.5.dp, frostedBorderColor), shape),
            color = Color.Transparent,
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f * dialogOpacity))
                    .padding(20.dp)
            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.ink
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                content()
            }
        }
    }
}
