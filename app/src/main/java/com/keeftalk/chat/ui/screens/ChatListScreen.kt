package com.keeftalk.chat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.domain.model.MessageStatus
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import java.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.delay
import com.keeftalk.chat.domain.model.ChatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import androidx.paging.PagingData
import com.keeftalk.chat.ui.theme.LucideIcons
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.FabGradient
import com.keeftalk.chat.ui.theme.fabGradientIcon
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.components.KeeftalkSearchTopBar
import com.keeftalk.chat.ui.components.NotificationsDropdown

import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.time.Duration.Companion.seconds

enum class ChatAction {
    PIN, MUTE, ARCHIVE, MARK_AS_READ, CLEAR_HISTORY, DELETE, FAVORITE
}

data class ChatContainer(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContainersRow(
    containers: List<ChatContainer>,
    selectedId: String,
    onContainerClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(containers) { container ->
            val isSelected = selectedId == container.id

            Surface(
                onClick = { onContainerClick(container.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        container.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .fabGradientIcon(),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = container.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (container.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                                    else Modifier.background(MaterialTheme.colorScheme.primary)
                                )
                                .padding(horizontal = 4.5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                container.badgeCount.toString(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListContent(
    chatsPager: Flow<androidx.paging.PagingData<com.keeftalk.chat.domain.model.ChatListItemUiModel>>,
    displayChats: List<com.keeftalk.chat.domain.model.ChatListItemUiModel>,
    currentUserId: String,
    onChatClick: (String) -> Unit,
    onMenuAction: (MenuAction) -> Unit,
    onChatAction: (com.keeftalk.chat.domain.model.ChatListItemUiModel, ChatAction) -> Unit = { _, _ -> },
    onLoadMore: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") highlightedChatId: String? = null,
    modifier: Modifier = Modifier,
    title: String = "Chats",
    onBackClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    showTopBar: Boolean = true,
    isSearching: Boolean = false,
    onIsSearchingChange: (Boolean) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onContainerChange: (String) -> Unit = {},
    startupUiReady: Boolean = true
) {
    if (!startupUiReady) {
        com.keeftalk.chat.util.PerformanceProfiler.logEvent("ChatListContent skeleton started", category = com.keeftalk.chat.util.PerformanceProfiler.Category.UI)
    }
    // STAGE 0: Render only first 10 items if startup is not ready
    val skeletonChats = remember(displayChats, startupUiReady) {
        if (!startupUiReady) displayChats.take(10) else displayChats
    }

    val icons = LocalAppIcons.current
    var showActionMenu by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    
    // NEW: State for context menu when using RecyclerView
    var selectedChatForMenu by remember { mutableStateOf<com.keeftalk.chat.domain.model.ChatListItemUiModel?>(null) }

    // Optimization: Skip menu animation state during skeleton
    val menuProgress = if (startupUiReady) {
        animateFloatAsState(
            targetValue = if (showActionMenu) 1f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "menuProgress"
        ).value
    } else 0f

    if (showActionMenu && startupUiReady) {
        BackHandler {
            showActionMenu = false
        }
    }

    val mainViewModel: com.keeftalk.chat.ui.MainViewModel = viewModel()
    val userPrefs by mainViewModel.userPreferences.collectAsState()
    val customization = userPrefs.appCustomization

    val notifications = if (startupUiReady) {
        mainViewModel.notifications.collectAsState().value
    } else emptyList()
    
    var selectedContainerId by remember { mutableStateOf("all") }
    var manualRevealContainers by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    val anyBadgeCount = remember(displayChats) {
        displayChats.any { it.unreadCount > 0 }
    }

    // REFIX: Logic for showing containers must be reactive and persistent
    val showContainersActual = (anyBadgeCount || manualRevealContainers || (selectedContainerId != "all"))

    LaunchedEffect(manualRevealContainers, lastInteractionTime) {
        if (manualRevealContainers && selectedContainerId == "all" && !anyBadgeCount) {
            delay(5.seconds) // Let user see it
            manualRevealContainers = false
        }
    }

    val containers = remember(displayChats, selectedContainerId, startupUiReady) {
        if (!startupUiReady) emptyList()
        else listOf(
            ChatContainer("all", "All", icons.chat),
            ChatContainer("unread", "Unread", icons.email, displayChats.count { it.unreadCount > 0 }),
            ChatContainer("groups", "Groups", icons.user, displayChats.count { it.type == ChatType.GROUP && it.unreadCount > 0 }),
            ChatContainer("archived", "Archived", icons.archive, displayChats.count { it.isArchived && it.unreadCount > 0 }),
            ChatContainer("favorites", "Favorites", icons.heart)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showTopBar) {
                if (isSearching && startupUiReady) {
                    KeeftalkSearchTopBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onCancel = {
                            onIsSearchingChange(false)
                            onSearchQueryChange("")
                        }
                    )
                } else {
                    KeeftalkTopBar(
                        title = title,
                        navigationIcon = {
                            if (onBackClick != null) {
                                IconButton(onClick = onBackClick) {
                                    Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                                }
                            }
                        },
                        actions = {
                            if (startupUiReady) {
                                IconButton(onClick = { onIsSearchingChange(true) }) {
                                    Icon(icons.search, contentDescription = "Search", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                                }
                                Box {
                                    IconButton(onClick = { showNotifications = true }) {
                                        BadgedBox(badge = {
                                            if (notifications.any { !it.isRead }) {
                                                Badge(
                                                    containerColor = Color(0xFFEF4444),
                                                    modifier = Modifier.size(8.dp)
                                                )
                                            }
                                        }) {
                                            Icon(icons.bell, contentDescription = "Notifications", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                                        }
                                    }
                                    NotificationsDropdown(
                                        notifications = notifications,
                                        onNotificationClick = {
                                            showNotifications = false
                                        },
                                        onMarkAllAsRead = { },
                                        onDismiss = { showNotifications = false },
                                        expanded = showNotifications
                                    )
                                }
                                IconButton(onClick = { onMenuAction(MenuAction.APPEARANCE) }) {
                                    Icon(icons.more, contentDescription = "Menu", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                                }
                            } else {
                                // Static Skeleton Icons
                                Icon(icons.search, null, modifier = Modifier.size(24.dp).padding(4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(icons.bell, null, modifier = Modifier.size(24.dp).padding(4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(icons.more, null, modifier = Modifier.size(24.dp).padding(4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showContainersActual && !isSearching && startupUiReady,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ChatContainersRow(
                    containers = containers,
                    selectedId = selectedContainerId,
                    onContainerClick = {
                        selectedContainerId = it
                        onContainerChange(it)
                        lastInteractionTime = System.currentTimeMillis()
                    }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (startupUiReady) {
                    val pullToRefreshState = rememberPullToRefreshState()
                    
                    PullToRefreshBox(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            if (!showContainersActual) {
                                // REFIX: Reveal containers on swipe
                                manualRevealContainers = true
                                lastInteractionTime = System.currentTimeMillis()
                            } else {
                                onRefresh()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ChatListRecyclerView(
                            chatsPager = chatsPager,
                            currentUserId = currentUserId,
                            onChatClick = onChatClick,
                            onChatLongClick = { selectedChatForMenu = it },
                            spacingMultiplier = customization.chatSpacingMultiplier
                        )
                    }
                } else {
                    // ULTRA-FAST SKELETON LIST
                    Column(modifier = Modifier.fillMaxSize()) {
                        skeletonChats.forEach { chat ->
                            ChatItem(
                                chat = chat,
                                currentUserId = currentUserId,
                                onClick = { onChatClick(chat.id) },
                                onChatAction = { _, _ -> },
                                startupUiReady = false,
                                modifier = Modifier.fillMaxWidth(),
                                spacingMultiplier = customization.chatSpacingMultiplier
                            )
                        }
                    }
                }
            }
        }

        // NEW: Context Menu for RecyclerView items
        if (selectedChatForMenu != null) {
            ChatContextMenu(
                chat = selectedChatForMenu!!,
                onAction = { 
                    onChatAction(selectedChatForMenu!!, it)
                    selectedChatForMenu = null
                },
                onDismiss = { selectedChatForMenu = null }
            )
        }

        if (menuProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f * menuProgress))
                    .pointerInput(Unit) { detectTapGestures { showActionMenu = false } }
            )
        }
    }
}

@Composable
fun ChatListRecyclerView(
    chatsPager: Flow<androidx.paging.PagingData<com.keeftalk.chat.domain.model.ChatListItemUiModel>>,
    currentUserId: String,
    onChatClick: (String) -> Unit,
    onChatLongClick: (com.keeftalk.chat.domain.model.ChatListItemUiModel) -> Unit,
    spacingMultiplier: Float = 0.5f
) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val adapter = remember(currentUserId, spacingMultiplier) {
        ChatListFragment.ChatPagingAdapter(
            onClick = { 
                com.keeftalk.chat.util.PerformanceProfiler.startSession("CHAT_OPEN", com.keeftalk.chat.util.PerformanceProfiler.Category.UI)
                onChatClick(it) 
            },
            onLongClick = { onChatLongClick(it) },
            currentUserId = currentUserId,
            spacingMultiplier = spacingMultiplier
        )
    }

    LaunchedEffect(chatsPager) {
        chatsPager.collectLatest { 
            adapter.submitData(lifecycle, it)
        }
    }

    AndroidView(
        factory = { ctx ->
            RecyclerView(ctx).apply {
                val lm = LinearLayoutManager(ctx)
                layoutManager = lm
                this.adapter = adapter
                isNestedScrollingEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                clipToPadding = false
                setPadding(0, 0, 0, (80 * resources.displayMetrics.density).toInt()) // Space for FAB
            }
        },
        modifier = Modifier.fillMaxSize().clip(androidx.compose.ui.graphics.RectangleShape),
        update = { it.adapter = adapter }
    )
}

@Composable
fun ChatItem(
    chat: com.keeftalk.chat.domain.model.ChatListItemUiModel,
    currentUserId: String,
    isTyping: Boolean = false,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    onChatAction: (com.keeftalk.chat.domain.model.ChatListItemUiModel, ChatAction) -> Unit,
    modifier: Modifier = Modifier,
    startupUiReady: Boolean = true,
    spacingMultiplier: Float = 0.5f
) {
    if (!startupUiReady) {
        // ABSOLUTE MINIMUM SKELETON ITEM: No Text Layout, No complex shapes
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(72.dp * spacingMultiplier / 0.5f) // Adjust based on spacing
                .padding(horizontal = 16.dp, vertical = 8.dp * spacingMultiplier / 0.5f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gray Circle Placeholder
                Spacer(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Gray Name Placeholder
                    Spacer(
                        modifier = Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Gray Message Placeholder
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
        return
    }

    var showContextMenu by remember { mutableStateOf(false) }
    val icons = LocalAppIcons.current
    val interactionSource = remember { MutableInteractionSource() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val animatedHighlightColor = if (isHighlighted) {
        val highlightColor = remember(primaryColor) { primaryColor.copy(alpha = 0.12f) }
        animateColorAsState(
            targetValue = highlightColor,
            animationSpec = repeatable(
                iterations = 2,
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "highlight"
        ).value
    } else Color.Transparent

    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val baseHeight = 80.dp
    val baseVerticalPadding = 12.dp
    
    // Apply multiplier (scaled relative to 1.0 being the OLD default)
    // The user wants 50% reduction by default (which I set as 0.5 multiplier)
    val scaledVerticalPadding = baseVerticalPadding * spacingMultiplier
    val scaledHeight = baseHeight - (baseVerticalPadding * 2) + (scaledVerticalPadding * 2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(scaledHeight)
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(88.dp.toPx(), size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true },
                interactionSource = interactionSource,
                indication = ripple(),
            )
            .background(
                if (chat.unreadCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                else Color.Transparent
            )
            .background(animatedHighlightColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = scaledVerticalPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeeftalkAvatar(
                avatarUrl = chat.avatarUrl,
                initials = chat.initials,
                seed = chat.peerId ?: chat.id,
                size = 56.dp,
                isOnline = chat.unreadCount > 0,
                isSkeleton = false
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.1).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (chat.isMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            icons.mute,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    if (chat.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            icons.pin,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (isTyping) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Typing",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TypingDots()
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val prefix = if (chat.lastMessageSenderId == currentUserId) "You: " else ""
                        Text(
                            text = "$prefix${chat.lastMessage ?: ""}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = chat.formattedTimestamp,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(FabGradient, CircleShape)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                            .widthIn(min = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            chat.unreadCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp
                            )
                        )
                    }
                }
                
                if (chat.lastMessageSenderId == currentUserId && chat.lastMessageStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusIndicator(chat, modifier = Modifier.padding(end = 4.dp))
                }
            }
        }

        if (showContextMenu) {
            ChatContextMenu(
                chat = chat,
                onAction = { onChatAction(chat, it) },
                onDismiss = { showContextMenu = false }
            )
        }
    }
}

@Composable
fun ChatContextMenu(
    chat: com.keeftalk.chat.domain.model.ChatListItemUiModel,
    onAction: (ChatAction) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = LocalAppIcons.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (chat.unreadCount > 0) "Mark as Read" else "Already Read") },
            leadingIcon = { Icon(Icons.Default.DoneAll, null) },
            enabled = chat.unreadCount > 0,
            onClick = { onDismiss(); onAction(ChatAction.MARK_AS_READ) }
        )
        DropdownMenuItem(
            text = { Text(if (chat.isFavorite) "Remove from Favorites" else "Add to Favorites") },
            leadingIcon = { Icon(if (chat.isFavorite) icons.heart else icons.heart, null) }, // heart usually has an outline/filled version but icons.heart is used here
            onClick = { onDismiss(); onAction(ChatAction.FAVORITE) }
        )
        DropdownMenuItem(
            text = { Text(if (chat.isPinned) "Unpin Chat" else "Pin Chat") },
            leadingIcon = { Icon(icons.pin, null) },
            onClick = { onDismiss(); onAction(ChatAction.PIN) }
        )
        DropdownMenuItem(
            text = { Text(if (chat.isMuted) "Unmute Notifications" else "Mute Notifications") },
            leadingIcon = { Icon(if (chat.isMuted) icons.unmute else icons.mute, null) },
            onClick = { onDismiss(); onAction(ChatAction.MUTE) }
        )
        DropdownMenuItem(
            text = { Text("Archive Chat") },
            leadingIcon = { Icon(icons.archive, null) },
            onClick = { onDismiss(); onAction(ChatAction.ARCHIVE) }
        )
        DropdownMenuItem(
            text = { Text("Clear History") },
            leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
            onClick = { onDismiss(); onAction(ChatAction.CLEAR_HISTORY) }
        )
        DropdownMenuItem(
            text = { Text("Delete Conversation") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error),
            onClick = { onDismiss(); onAction(ChatAction.DELETE) }
        )
    }
}


@Composable
fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.Bottom) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 600
                        0.3f at 0
                        1f at 300
                        0.3f at 600
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp, end = 1.dp)
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun StatusIndicator(chat: com.keeftalk.chat.domain.model.ChatListItemUiModel, modifier: Modifier = Modifier) {
    val status = chat.lastMessageStatus ?: return
    
    if (status == MessageStatus.SEEN) {
        KeeftalkAvatar(
            avatarUrl = chat.avatarUrl,
            initials = chat.initials,
            seed = chat.peerId ?: chat.id,
            size = 14.dp,
            modifier = modifier
        )
    } else {
        val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        val icon = when (status) {
            MessageStatus.SENDING -> LucideIcons.Send
            MessageStatus.SENT -> LucideIcons.EyeClosed
            MessageStatus.DELIVERED -> LucideIcons.EyeOff
            MessageStatus.FCM_RECEIVED -> LucideIcons.BellRing
            else -> LucideIcons.Send
        }
        Icon(icon, null, modifier.size(14.dp), tint)
    }
}
