package tn.keeftalk.chat.ui.theme.keef

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import tn.keeftalk.chat.ui.theme.LucideIcons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.AppScreen
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.PeriodOfDay
import tn.keeftalk.chat.domain.model.Profile
import tn.keeftalk.chat.ui.components.KeeftalkAvatar
import tn.keeftalk.chat.util.AvatarUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KeefHomeView(
    profile: Profile?,
    environmentalState: EnvironmentalState,
    unreadMessagesCount: Int,
    activeTasksCount: Int,
    streakDays: Int,
    onNavigate: (AppScreen) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: KeefThemeTokens = LocalKeefThemeTokens.current
) {
    val dateStr = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    val greeting = when (environmentalState.period) {
        PeriodOfDay.DAWN -> "Good morning"
        PeriodOfDay.DAY -> "Good day"
        PeriodOfDay.DUSK -> "Good evening"
        PeriodOfDay.NIGHT -> "Good night"
    }

    Box(modifier = modifier.fillMaxSize()) {
        KeefThemeBackground(
            environmentalState = environmentalState,
            tokens = tokens
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val name = profile?.fullName?.ifBlank { null } ?: profile?.username ?: "AM"
                        val initials = AvatarUtils.getInitials(name)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(tokens.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Column {
                            Text(
                                text = greeting,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.ink,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = dateStr,
                                fontSize = 12.5.sp,
                                color = tokens.ink2
                            )
                        }
                    }

                    KeefGlassIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                        tokens = tokens
                    )
                }
            }

            // 2. Search & Ask AI Bar
            item {
                KeefGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    tokens = tokens,
                    cornerRadius = 24.dp,
                    elevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = tokens.ink2,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Search or ask anything",
                            fontSize = 14.5.sp,
                            color = tokens.ink2,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tokens.soft)
                                .clickable { /* Voice input */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = tokens.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. Environmental Weather Widget
            item {
                KeefWeatherWidget(
                    envState = environmentalState,
                    tokens = tokens,
                    onWidgetClick = { onNavigate(AppScreen.Calendar) }
                )
            }

            // 4. Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeefStatTile(
                        value = unreadMessagesCount.toString(),
                        label = "Messages",
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                    KeefStatTile(
                        value = activeTasksCount.toString(),
                        label = "Tasks",
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                    KeefStatTile(
                        value = "$streakDays",
                        label = "Day streak",
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                }
            }

            // 5. Quick Actions Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeefQuickActionButton(
                        label = "Send",
                        icon = Icons.AutoMirrored.Filled.Send,
                        accentColor = Color(0xFF0A7CFF),
                        onClick = { onNavigate(AppScreen.ChatList) },
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                    KeefQuickActionButton(
                        label = "Call",
                        icon = Icons.Default.Phone,
                        accentColor = Color(0xFF18A999),
                        onClick = { onNavigate(AppScreen.ChatList) },
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                    KeefQuickActionButton(
                        label = "Camera",
                        icon = Icons.Default.CameraAlt,
                        accentColor = Color(0xFFE0447A),
                        onClick = { onNavigate(AppScreen.QrCode) },
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                    KeefQuickActionButton(
                        label = "More",
                        icon = Icons.Default.MoreHoriz,
                        accentColor = Color(0xFF7A5CFF),
                        onClick = { onNavigate(AppScreen.Discovery) },
                        modifier = Modifier.weight(1f),
                        tokens = tokens
                    )
                }
            }

            // 6. Categories & Module Navigation Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeefCategoryCard(
                            title = "Vault",
                            subtitle = "Encrypted storage",
                            icon = LucideIcons.FolderLock,
                            onClick = { onNavigate(AppScreen.Vault) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeefCategoryCard(
                            title = "My Books",
                            subtitle = "Library & reader",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            onClick = { onNavigate(AppScreen.Books) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                        KeefCategoryCard(
                            title = "My Docs",
                            subtitle = "Multi-format viewer",
                            icon = Icons.Default.Description,
                            onClick = { onNavigate(AppScreen.Docs) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeefCategoryCard(
                            title = "My Habits",
                            subtitle = "Streaks & goals",
                            icon = Icons.Default.CheckCircle,
                            onClick = { onNavigate(AppScreen.Habits) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                        KeefCategoryCard(
                            title = "My Browser",
                            subtitle = "Private web",
                            icon = Icons.Default.Language,
                            onClick = { onNavigate(AppScreen.Browser()) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeefCategoryCard(
                            title = "My Market",
                            subtitle = "Assets & trades",
                            icon = Icons.Default.Storefront,
                            onClick = { onNavigate(AppScreen.Market) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                        KeefCategoryCard(
                            title = "My Notes",
                            subtitle = "Notebooks & Sync",
                            icon = Icons.Default.Note,
                            onClick = { onNavigate(AppScreen.Notes()) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeefCategoryCard(
                            title = "My Maps",
                            subtitle = "Saved places & map",
                            icon = LucideIcons.MapPinned,
                            onClick = { onNavigate(AppScreen.MyMaps) },
                            modifier = Modifier.weight(1f),
                            tokens = tokens
                        )
                    }
                }
            }
        }
    }
}
