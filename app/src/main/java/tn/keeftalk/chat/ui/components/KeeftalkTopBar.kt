package tn.keeftalk.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import tn.keeftalk.chat.domain.model.AppNotification
import tn.keeftalk.chat.ui.theme.KeeftalkDimensions
import tn.keeftalk.chat.ui.theme.LocalAppIcons
import tn.keeftalk.chat.ui.theme.fabGradientIcon

val LocalNotifications = compositionLocalOf<List<AppNotification>> { emptyList() }

data class NotificationActions(
    val onNotificationClick: (AppNotification) -> Unit,
    val onMarkAllAsRead: () -> Unit
)

val LocalNotificationActions = compositionLocalOf<NotificationActions?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeeftalkTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val finalOnBack = onBack ?: { backDispatcher?.onBackPressed() }

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 0.dp)
                    ) {
                        Text(
                            text = "Keeftalk",
                            modifier = @OptIn(ExperimentalFoundationApi::class) Modifier.combinedClickable(
                                onLongClick = {
                                    if (tn.keeftalk.chat.BuildConfig.DEBUG) {
                                        (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager?.let { fm ->
                                            fm.beginTransaction()
                                                .add(android.R.id.content, tn.keeftalk.chat.ui.debug.PerformanceDashboardFragment())
                                                .addToBackStack("performance_dashboard")
                                                .commit()
                                        }
                                    }
                                },
                                onClick = { finalOnBack() }
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = KeeftalkDimensions.brandTextSize,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = KeeftalkDimensions.sectionTextSize,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun KeeftalkFeatureTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    notifications: List<AppNotification>? = null,
    onNotificationClick: ((AppNotification) -> Unit)? = null,
    onMarkAllNotificationsAsRead: (() -> Unit)? = null,
    onSearch: () -> Unit = {},
    onSettings: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
) {
    val icons = LocalAppIcons.current
    var showNotifications by remember { mutableStateOf(false) }

    val mainViewModel: tn.keeftalk.chat.ui.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userPrefs by mainViewModel.userPreferences.collectAsState(initial = tn.keeftalk.chat.data.prefs.UserPreferences())
    val isMinimalMode = userPrefs.appCustomization.isMinimalMode

    val effectiveTitle = if (isMinimalMode) "" else title

    // Use provided values or consume from CompositionLocal
    val currentNotifications = notifications ?: LocalNotifications.current
    val currentActions = LocalNotificationActions.current
    
    val effectiveOnNotificationClick = onNotificationClick ?: { currentActions?.onNotificationClick?.invoke(it) }
    val effectiveOnMarkAllAsRead = onMarkAllNotificationsAsRead ?: { currentActions?.onMarkAllAsRead?.invoke() }

    KeeftalkTopBar(
        title = effectiveTitle,
        modifier = modifier,
        onBack = onBack,
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            } else if (onBack != null && !isMinimalMode) {
                IconButton(onClick = onBack) {
                    Icon(
                        icons.back,
                        contentDescription = "Back",
                        modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                    )
                }
            }
        },
        actions = {
            if (!isMinimalMode) {
                IconButton(onClick = onSearch) {
                    Icon(
                        icons.search,
                        contentDescription = "Search",
                        modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                    )
                }

                Box {
                    IconButton(onClick = { showNotifications = true }) {
                        val unreadCount = currentNotifications.count { !it.isRead }
                        if (unreadCount > 0) {
                            BadgedBox(
                                badge = { 
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(Color(0xFFEF4444), androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(unreadCount.toString(), fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(
                                    icons.bell,
                                    contentDescription = "Notifications",
                                    modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                                )
                            }
                        } else {
                            Icon(
                                icons.bell,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                            )
                        }
                    }

                    NotificationsDropdown(
                        notifications = currentNotifications,
                        onNotificationClick = {
                            showNotifications = false
                            effectiveOnNotificationClick?.invoke(it)
                        },
                        onMarkAllAsRead = {
                            effectiveOnMarkAllAsRead?.invoke()
                        },
                        onDismiss = { showNotifications = false },
                        expanded = showNotifications
                    )
                }

                actions()

                IconButton(onClick = onSettings) {
                    Icon(
                        icons.more,
                        contentDescription = "Settings",
                        modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                    )
                }
            }
        },
        windowInsets = windowInsets
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeeftalkSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
) {
    val icons = LocalAppIcons.current
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            icons.back, 
                            contentDescription = "Back", 
                            modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                        )
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Clear", 
                                modifier = Modifier.size(KeeftalkDimensions.topBarIconSize)
                            )
                        }
                    }
                },
                windowInsets = windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
