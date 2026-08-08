package com.keeftalk.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeeftalkTopAppBar(
    screenTitle: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
) {
    val icons = LocalAppIcons.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryGradientColor = Color(0xFF8B83FF)
    
    Surface(
        color = Color.Transparent,
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 0.dp)
                ) {
                    // Icon from design
                    Icon(
                        imageVector = icons.chat,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp)
                    )

                    // Brand: Keeftalk
                    Text(
                        text = "Keeftalk",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryColor, secondaryGradientColor)
                            )
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Screen Section with Animation and Underline
                    AnimatedContent(
                        targetState = screenTitle,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { height -> height / 2 }).togetherWith(
                                fadeOut() + slideOutVertically { height -> -height / 2 }
                            )
                        },
                        label = "ScreenTitleAnimation"
                    ) { title ->
                        Column(
                            modifier = Modifier.width(IntrinsicSize.Min),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1
                            )
                            
                            // Accent indicator (underline)
                            Box(
                                modifier = Modifier
                                    .padding(top = 0.dp)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(
                                        color = primaryColor.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            windowInsets = WindowInsets.statusBars,
            modifier = Modifier.height(74.dp)
        )
    }
}
