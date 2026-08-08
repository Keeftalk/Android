package com.keeftalk.chat.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.model.MessageType
import androidx.paging.compose.collectAsLazyPagingItems
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.KeeftalkSearchTopBar
import com.keeftalk.chat.ui.components.MessageContextMenu
import com.keeftalk.chat.ui.components.ReactionDetailsSheet
import com.keeftalk.chat.ui.emoji.EmojiPicker
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.theme.LocalChatThemeExtra
import androidx.compose.ui.platform.LocalHapticFeedback
import com.keeftalk.chat.ui.screens.chatdetail.*
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.theme.OnlineColor
import com.keeftalk.chat.ui.theme.fabGradientIcon
import com.keeftalk.chat.util.AvatarUtils
import com.keeftalk.chat.util.PerformanceProfiler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailTopBar(
    chatName: String?,
    avatarUrl: String?,
    peerId: String?,
    chatId: String?,
    isOnline: Boolean,
    typingUsers: Set<String>,
    lastSeenText: String,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onClearChat: () -> Unit,
    onNotificationsClick: () -> Unit,
    onThemeChange: (String?) -> Unit,
    onSearchClick: () -> Unit,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    showSearch: Boolean,
    onCloseSearch: () -> Unit,
    isGroup: Boolean = false,
    onAddMember: () -> Unit = {},
    onViewMembers: () -> Unit = {}
) {
    val chatTheme = LocalChatTheme.current
    val icons = LocalAppIcons.current
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    if (showSearch) {
        KeeftalkSearchTopBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
            onCancel = onCloseSearch
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().zIndex(2f),
            color = Color.Transparent,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            TopAppBar(
                modifier = if (chatTheme.id == "rosa") Modifier.padding(vertical = 4.dp) else Modifier,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onProfileClick() }
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "AvatarGlow")
                        val glowPulse by infiniteTransition.animateFloat(
                            initialValue = 20.dp.value,
                            targetValue = 35.dp.value,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "glowPulse"
                        )

                        KeeftalkAvatar(
                            avatarUrl = avatarUrl,
                            initials = AvatarUtils.getInitials(chatName),
                            seed = peerId ?: chatId ?: chatName,
                            size = 48.dp,
                            isOnline = isOnline,
                            modifier = when(chatTheme.id) {
                                "alpha" -> Modifier.border(2.dp, Color(0xFFC9A84C), CircleShape)
                                "rosa" -> Modifier
                                    .shadow(glowPulse.dp, CircleShape, spotColor = Color(0xFFFF6B9D))
                                    .background(Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFFD44AD6))), CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                else -> Modifier
                            }
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                chatName ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = when (chatTheme.id) {
                                        "rosa" -> Color.White
                                        "alpha" -> Color(0xFFE8E0D0)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOnline && chatTheme.id == "rosa") {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF2EFF9E), CircleShape)
                                            .shadow(8.dp, CircleShape, spotColor = Color(0xFF2EFF9E))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (typingUsers.isNotEmpty()) "typing..." else if (isOnline) "Online · Let's chat!" else lastSeenText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    color = if (typingUsers.isNotEmpty() || isOnline) {
                                        if (chatTheme.id == "rosa") Color(0xFFFFE0E6)
                                        else if (chatTheme.id == "alpha") Color(0xFFC9A84C)
                                        else OnlineColor
                                    } else {
                                        if (chatTheme.id == "rosa") Color(0xFFFFE0E6).copy(alpha = 0.9f)
                                        else if (chatTheme.id == "alpha") Color(0xFF8A8A9A)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(icons.back, contentDescription = "Back", tint = if (chatTheme.id == "rosa") Color.White else if (chatTheme.id == "alpha") Color(0xFFC9A84C) else MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = onVoiceCallClick) {
                        Icon(
                            icons.phone,
                            contentDescription = "Voice Call",
                            modifier = if (chatTheme.id != "rosa" && chatTheme.id != "alpha") Modifier.fabGradientIcon() else Modifier,
                            tint = if (chatTheme.id == "rosa") Color.White else if (chatTheme.id == "alpha") Color(0xFFC9A84C) else Color.Unspecified
                        )
                    }
                    IconButton(onClick = onVideoCallClick) {
                        Icon(
                            icons.camera,
                            contentDescription = "Video Call",
                            modifier = if (chatTheme.id != "rosa" && chatTheme.id != "alpha") Modifier.fabGradientIcon() else Modifier,
                            tint = if (chatTheme.id == "rosa") Color.White else if (chatTheme.id == "alpha") Color(0xFFC9A84C) else Color.Unspecified
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showSettingsMenu = true },
                            modifier = if (chatTheme.id == "rosa") Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            else Modifier
                        ) {
                            Icon(
                                icons.more,
                                contentDescription = "More options",
                                tint = if (chatTheme.id == "rosa") Color(0xFFFFB6C1) else if (chatTheme.id == "alpha") Color(0xFFC9A84C) else MaterialTheme.colorScheme.onSurface,
                                modifier = if (chatTheme.id == "rosa") Modifier.size(20.dp) else Modifier
                            )
                        }
                        DropdownMenu(expanded = showSettingsMenu, onDismissRequest = { showSettingsMenu = false }) {
                            DropdownMenuItem(text = { Text("View Profile") }, leadingIcon = { Icon(icons.user, null) }, onClick = { showSettingsMenu = false; onProfileClick() })
                            if (isGroup) {
                                DropdownMenuItem(text = { Text("View Members") }, leadingIcon = { Icon(Icons.Default.Group, null) }, onClick = { showSettingsMenu = false; onViewMembers() })
                                DropdownMenuItem(text = { Text("Add Participant") }, leadingIcon = { Icon(Icons.Default.PersonAdd, null) }, onClick = { showSettingsMenu = false; onAddMember() })
                            }
                            DropdownMenuItem(text = { Text("Theme") }, leadingIcon = { Icon(Icons.Default.Palette, null) }, onClick = { showSettingsMenu = false; showThemePicker = true })
                            DropdownMenuItem(text = { Text("Notifications") }, leadingIcon = { Icon(icons.bell, null) }, onClick = { showSettingsMenu = false; onNotificationsClick() })
                            DropdownMenuItem(text = { Text("Archive Chat") }, leadingIcon = { Icon(icons.archive, null) }, onClick = { showSettingsMenu = false; /* archive chat */ })
                            DropdownMenuItem(text = { Text("Search in Chat") }, leadingIcon = { Icon(icons.search, null) }, onClick = { showSettingsMenu = false; onSearchClick() })
                            DropdownMenuItem(text = { Text("Clear Messages") }, leadingIcon = { Icon(icons.delete, null) }, colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error), onClick = { showSettingsMenu = false; onClearChat() })
                        }
                    }
                }
            )
        }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentThemeId = (LocalChatTheme.current.takeIf { it.id != "default" }?.id),
            onDismiss = { showThemePicker = false },
            onThemeSelect = { onThemeChange(it); showThemePicker = false }
        )
    }
}

@Composable
fun ChatMessageList(
    modifier: Modifier = Modifier,
    currentUserId: String,
    peerId: String?,
    peerName: String,
    avatarUrl: String?,
    chatSettings: com.keeftalk.chat.domain.model.UserChatSettings,
    chatTheme: com.keeftalk.chat.ui.theme.ChatTheme,
    messagesFlow: Flow<androidx.paging.PagingData<ChatItem>>,
    readReceipts: Map<String, Pair<String, Long>>,
    transferProgress: Map<String, Float>,
    expandedMessageId: String?,
    initialScrollPosition: Int,
    initialScrollOffset: Int,
    onScrollStateChange: (Int, Int) -> Unit,
    onMessageClick: (Message, Long) -> Unit,
    onPdfClick: (String) -> Unit,
    onEmailClick: (String) -> Unit = {},
    onVaultClick: (String) -> Unit = {},
    onAgendaClick: (String) -> Unit = {},
    onCodeClick: (String) -> Unit = {},
    onAddReaction: (String, String) -> Unit,
    onRemoveReaction: (String) -> Unit,
    onToggleMediaLock: (String, Boolean) -> Unit,
    onDownloadMessage: (Message) -> Unit,
    onShowContextMenu: (Message) -> Unit,
    onReplySwipe: (Message) -> Unit,
    onToggleTimestamp: (String) -> Unit,
    unreadCount: Int,
    onUnreadCountChange: (Int) -> Unit,
    onFirstVisibleItemChange: (Int) -> Unit,
    onMessageVisible: (Message) -> Unit = {},
    onEncryptionNoticeClick: () -> Unit = {},
    isGroup: Boolean = false,
    getUserProfile: (String) -> com.keeftalk.chat.domain.model.User? = { null },
    userProfiles: Map<String, com.keeftalk.chat.domain.model.User> = emptyMap()
) {
    val recyclerViewState = remember { mutableStateOf<RecyclerView?>(null) }
    val bubblePositions = remember { mutableStateMapOf<String, Pair<Offset, IntSize>>() }
    val scope = rememberCoroutineScope()

    val adapter = remember(currentUserId, peerId, isGroup) {
        MessagePagingAdapter(
            currentUserId = currentUserId,
            initialChatSettings = chatSettings,
            initialChatTheme = chatTheme,
            peerId = peerId,
            initialPeerName = peerName,
            initialPeerAvatarUrl = avatarUrl,
            initialReadReceipts = readReceipts,
            onMessageClick = onMessageClick,
            onPdfClick = onPdfClick,
            onEmailClick = onEmailClick,
            onVaultClick = onVaultClick,
            onAgendaClick = onAgendaClick,
            onCodeClick = onCodeClick,
            onDoubleTap = { msg ->
                val myReaction = msg.reactions.find { it.userId == currentUserId }
                if (myReaction != null) onRemoveReaction(msg.id) else onAddReaction(msg.id, "❤️")
            },
            onReactionClick = { msg, emoji ->
                val myReaction = msg.reactions.find { it.userId == currentUserId }
                if (myReaction?.emoji == emoji) onRemoveReaction(msg.id) else onAddReaction(msg.id, emoji)
            },
            onToggleMediaLock = onToggleMediaLock,
            onDownloadMessage = onDownloadMessage,
            onShowContextMenu = onShowContextMenu,
            onReplySwipe = onReplySwipe,
            onEncryptionNoticeClick = onEncryptionNoticeClick,
            initialIsGroup = isGroup,
            getUserProfile = getUserProfile
        )
    }

    LaunchedEffect(chatSettings, chatTheme, peerName, avatarUrl, isGroup, userProfiles) {
        adapter.updateSettings(chatSettings, chatTheme, peerName, avatarUrl, isGroup, userProfiles)
    }

    LaunchedEffect(readReceipts) {
        adapter.readReceipts = readReceipts
        recyclerViewState.value?.post { updateBubblePositions(recyclerViewState.value!!, bubblePositions) }
    }

    LaunchedEffect(transferProgress) { adapter.transferProgress = transferProgress }
    LaunchedEffect(expandedMessageId) { adapter.expandedMessageId = expandedMessageId }
    adapter.onToggleTimestamp = onToggleTimestamp

    LaunchedEffect(adapter.itemCount) {
        if (adapter.itemCount > 0 && initialScrollPosition != 0) {
            (recyclerViewState.value?.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(initialScrollPosition, initialScrollOffset)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                RecyclerView(ctx).apply {
                    itemAnimator = null
                    setItemViewCacheSize(10) // Increased cache
                    layoutManager = SafeLinearLayoutManager(ctx).apply {
                        stackFromEnd = true
                        reverseLayout = true
                    }
                    this.adapter = adapter
                    recyclerViewState.value = this
                    setHasFixedSize(false) // Still dynamic due to text, but cache helps

                    adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                            val lm = layoutManager as? LinearLayoutManager ?: return
                            if (positionStart == 0) {
                                if (lm.findFirstVisibleItemPosition() <= 1) {
                                    post {
                                        scrollToPosition(0)
                                    }
                                    onUnreadCountChange(0)
                                    
                                    // Mark newly arrived visible messages as read
                                    val item = adapter.peek(0)
                                    if (item is ChatItem.MessageItem) {
                                        onMessageVisible(item.message)
                                    }
                                } else {
                                    onUnreadCountChange(unreadCount + itemCount)
                                }
                            }
                        }
                    })

                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            updateBubblePositions(recyclerView, bubblePositions)
                            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                            val pos = lm.findFirstVisibleItemPosition()
                            onFirstVisibleItemChange(pos)
                            
                            if (pos != RecyclerView.NO_POSITION) {
                                val item = adapter.peek(pos)
                                if (item is ChatItem.MessageItem) {
                                    onMessageVisible(item.message)
                                }
                            }
                            
                            onScrollStateChange(pos, lm.findViewByPosition(pos)?.top ?: 0)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(messagesFlow) {
            messagesFlow.collectLatest { adapter.submitData(lifecycle, it) }
        }

        ReadReceiptOverlay(
            receipts = readReceipts,
            bubblePositions = bubblePositions,
            peerAvatarUrl = avatarUrl,
            peerName = peerName,
            peerId = peerId ?: "",
            modifier = Modifier.fillMaxSize()
        )

        val showScrollToBottom = remember(unreadCount) { 
            unreadCount > 0 || (recyclerViewState.value?.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0 > 5 
        }

        if (showScrollToBottom) {
            ScrollToBottomFAB(
                visible = true,
                unreadCount = unreadCount,
                onClick = {
                    scope.launch {
                        recyclerViewState.value?.smoothScrollToPosition(0)
                        onUnreadCountChange(0)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp).zIndex(100f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailDialogs(
    showAutoDeleteDialog: Boolean,
    onDismissAutoDelete: () -> Unit,
    autoDeleteTimer: Long?,
    onAutoDeleteChange: (Long?) -> Unit,
    showForwardDialog: Boolean,
    onDismissForward: () -> Unit,
    selectedMessage: Message?,
    onForwardMessage: (String, List<String>) -> Unit,
    showReactionDetails: Boolean,
    onDismissReactionDetails: () -> Unit,
    reactions: List<com.keeftalk.chat.domain.model.MessageReaction>,
    getUserProfile: (String) -> com.keeftalk.chat.domain.model.User?,
    currentUserId: String,
    showPollDialog: Boolean,
    onDismissPoll: () -> Unit,
    onSendMessage: (String) -> Unit,
    failedMessage: Message?,
    onDismissFailure: () -> Unit,
    onRetryMessage: (Message) -> Unit,
    onCancelMessage: (Message) -> Unit
) {
    if (showAutoDeleteDialog) {
        AutoDeleteDialog(currentTimer = autoDeleteTimer, onDismiss = onDismissAutoDelete, onConfirm = onAutoDeleteChange)
    }

    if (showForwardDialog && selectedMessage != null) {
        ForwardDialog(onDismiss = onDismissForward, onForward = { targetChatIds ->
            onForwardMessage(selectedMessage.id, targetChatIds)
        })
    }

    if (showReactionDetails) {
        ReactionDetailsSheet(reactions = reactions, onDismiss = onDismissReactionDetails, getUserProfile = getUserProfile, currentUserId = currentUserId)
    }

    if (showPollDialog) {
        PollDialog(onDismiss = onDismissPoll, onPollCreated = { q, opts ->
            onSendMessage("POLL: $q\n" + opts.joinToString("\n") { "- $it" })
        })
    }

    if (failedMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissFailure,
            title = { Text("Message Failed") },
            text = { Text("Your message could not be sent. Would you like to try again?") },
            confirmButton = { TextButton(onClick = { onRetryMessage(failedMessage) }) { Text("Retry") } },
            dismissButton = { TextButton(onClick = { onCancelMessage(failedMessage) }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailBottomSheets(
    showAttachmentSheet: Boolean,
    onDismissAttachment: () -> Unit,
    sheetState: SheetState,
    onAttachmentClick: (AttachmentType) -> Unit,
    mediaItems: androidx.paging.compose.LazyPagingItems<com.keeftalk.chat.domain.model.MediaItem>,
    onMediaClick: (com.keeftalk.chat.domain.model.MediaItem) -> Unit,
    availableAlbums: List<com.keeftalk.chat.domain.model.MediaAlbum>,
    selectedAlbum: com.keeftalk.chat.domain.model.MediaAlbum?,
    onAlbumClick: (com.keeftalk.chat.domain.model.MediaAlbum?) -> Unit,
    selectedMediaList: List<com.keeftalk.chat.domain.model.MediaItem>,
    onToggleMediaSelection: (com.keeftalk.chat.domain.model.MediaItem) -> Unit,
    onNextClick: () -> Unit,
    onGooglePhotosClick: () -> Unit,
    onGooglePhotosAppClick: () -> Unit,
    showDocumentPicker: Boolean,
    onDismissDocumentPicker: () -> Unit,
    onUnlockFullAccess: () -> Unit,
    onBrowseSystemDocuments: () -> Unit,
    onReviewDocuments: (List<com.keeftalk.chat.domain.model.DocumentModel>) -> Unit,
    onSendDocuments: (List<android.net.Uri>) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = onDismissAttachment, sheetState = sheetState) {
            AttachmentMenu(
                onTypeClick = { type ->
                    onAttachmentClick(type)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissAttachment() }
                },
                mediaItems = mediaItems,
                onMediaClick = { item ->
                    onMediaClick(item)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissAttachment() }
                },
                albums = availableAlbums,
                selectedAlbum = selectedAlbum,
                onAlbumClick = onAlbumClick,
                selectedItems = selectedMediaList,
                onToggleSelection = onToggleMediaSelection,
                onNextClick = {
                    onNextClick()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissAttachment() }
                },
                onGooglePhotosClick = {
                    onGooglePhotosClick()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissAttachment() }
                },
                onGooglePhotosAppClick = {
                    onGooglePhotosAppClick()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissAttachment() }
                }
            )
        }
    }

    if (showDocumentPicker) {
        val context = LocalContext.current
        val docViewModel: DocumentPickerViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
            val db = com.keeftalk.chat.di.AppModule.provideDatabase(context)
            DocumentPickerViewModel(
                repository = com.keeftalk.chat.data.repository.DocumentRepositoryImpl(context, db.messageDao()),
                chatRepository = com.keeftalk.chat.di.AppModule.provideChatRepository(context),
                vaultRepository = com.keeftalk.chat.di.AppModule.provideVaultRepository(context)
            )
        }
        DocumentPickerBottomSheet(
            viewModel = docViewModel,
            onDismiss = onDismissDocumentPicker,
            onUnlockFullAccess = onUnlockFullAccess,
            onBrowseSystem = onBrowseSystemDocuments,
            onNext = { onReviewDocuments(it.toList()) },
            onSend = { onSendDocuments(listOf(it.uri)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatDetailContent(
    chatName: String?,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    peerId: String? = null,
    chatId: String? = null,
    currentUserId: String,
    peerLastSeen: Long = 0L,
    isMuted: Boolean = false,
    isPeerBlocked: Boolean = false,
    isGroup: Boolean = false,
    screenshotProtectionEnabled: Boolean = false,
    autoDeleteTimer: Long? = null,
    autoTranslateEnabled: Boolean = false,
    messagesFlow: Flow<androidx.paging.PagingData<ChatItem>>,
    readReceipts: Map<String, Pair<String, Long>> = emptyMap(),
    typingUsers: Set<String> = emptySet(),
    onSendMessage: (String) -> Unit,
    onImageCaptured: (android.net.Uri) -> Unit,
    onVideoCaptureCaptured: (android.net.Uri) -> Unit = {},
    onSendDocuments: (List<android.net.Uri>) -> Unit = {},
    onReviewDocuments: (List<com.keeftalk.chat.domain.model.DocumentModel>) -> Unit = {},
    onUnlockFullAccess: () -> Unit = {},
    onBrowseSystemDocuments: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {},
    onAttachmentClick: (AttachmentType) -> Unit = {},
    onMicStart: () -> Unit = {},
    onMicStop: (Boolean) -> Unit = {},
    onMicCancel: () -> Unit = {},
    onGooglePhotosClick: () -> Unit = {},
    onGooglePhotosAppClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMarkAsRead: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTypingStatusChange: (Boolean) -> Unit = {},
    onAutoDeleteChange: (Long?) -> Unit = {},
    onAutoTranslateToggle: (Boolean) -> Unit = {},
    onMuteToggle: (Boolean) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBlockToggle: (Boolean) -> Unit = {},
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onThemeChange: (String?) -> Unit = {},
    onBumpChat: () -> Unit = {},
    onAddReaction: (String, String) -> Unit = { _, _ -> },
    onRemoveReaction: (String) -> Unit = { _ -> },
    onForwardMessage: (String, List<String>) -> Unit = { _, _ -> },
    onReportMessage: (String, String) -> Unit = { _, _ -> },
    onDeleteMessage: (String) -> Unit = { _ -> },
    onTranslateMessage: (String) -> Unit = { _ -> },
    onReactionBubbleClick: (List<com.keeftalk.chat.domain.model.MessageReaction>) -> Unit = {},
    onMessageVisible: (Message) -> Unit = {},
    getUserProfile: (String) -> com.keeftalk.chat.domain.model.User? = { null },
    chatSettings: com.keeftalk.chat.domain.model.UserChatSettings = com.keeftalk.chat.domain.model.UserChatSettings(""),
    onNoteClick: (String) -> Unit = {},
    checkNoteAvailability: suspend (String) -> Boolean = { true },
    onMediaClick: (String, Long) -> Unit = { _, _ -> },
    onPdfClick: (String) -> Unit = {},
    onEmailClick: (String) -> Unit = {},
    onVaultClick: (String) -> Unit = {},
    onAgendaClick: (String) -> Unit = {},
    onCodeClick: (String) -> Unit = {},
    onToggleMediaLock: (String, Boolean) -> Unit = { _, _ -> },
    onDownloadMessage: (Message) -> Unit = {},
    mediaItemsFlow: Flow<androidx.paging.PagingData<com.keeftalk.chat.domain.model.MediaItem>> = flowOf(androidx.paging.PagingData.empty()),
    availableAlbums: List<com.keeftalk.chat.domain.model.MediaAlbum> = emptyList(),
    selectedAlbum: com.keeftalk.chat.domain.model.MediaAlbum? = null,
    onAlbumClick: (com.keeftalk.chat.domain.model.MediaAlbum?) -> Unit = {},
    selectedMediaList: List<com.keeftalk.chat.domain.model.MediaItem> = emptyList(),
    onToggleMediaSelection: (com.keeftalk.chat.domain.model.MediaItem) -> Unit = {},
    mediaQualityMap: Map<Long, com.keeftalk.chat.ui.screens.chatdetail.MediaQuality> = emptyMap(),
    onQualityChange: (Long, com.keeftalk.chat.ui.screens.chatdetail.MediaQuality) -> Unit = { _, _ -> },
    onClearMediaSelection: () -> Unit = {},
    onSendMediaWithCaption: (com.keeftalk.chat.domain.model.MediaItem, String) -> Unit = { _, _ -> },
    onSendEditedMedia: (List<com.keeftalk.chat.domain.model.EditorModel>) -> Unit = {},
    recordingState: VoiceRecordingState = VoiceRecordingState.IDLE,
    recordingDuration: Long = 0L,
    amplitudeHistory: List<Float> = emptyList(),
    onSendVoice: () -> Unit = {},
    initialScrollPosition: Int = 0,
    initialScrollOffset: Int = 0,
    onScrollStateChange: (Int, Int) -> Unit = { _, _ -> },
    transferProgress: Map<String, Float> = emptyMap(),
    failedMessage: Message? = null,
    expandedMessageId: String? = null,
    onToggleTimestamp: (String) -> Unit = {},
    onRetryMessage: (Message) -> Unit = {},
    onCancelMessage: (Message) -> Unit = {},
    onDismissFailure: () -> Unit = {},
    userProfiles: Map<String, com.keeftalk.chat.domain.model.User> = emptyMap(),
    initialShowAttachmentSheet: Boolean = false
) {
    val themeExtra = LocalChatThemeExtra.current
    val chatTheme = LocalChatTheme.current
    val context = LocalContext.current
    val androidContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val mediaPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val mediaPermissionState = rememberMultiplePermissionsState(mediaPermissions)
    val mediaItems = mediaItemsFlow.collectAsLazyPagingItems()

    var itemsToPreview by remember { mutableStateOf<List<com.keeftalk.chat.domain.model.EditorModel>>(emptyList()) }
    var currentPreviewIndex by remember { mutableIntStateOf(0) }
    
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(initialShowAttachmentSheet) }
    var showDocumentPicker by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showEncryptionExplanation by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedMessageForInteractions by remember { mutableStateOf<Message?>(null) }
    var showMessageContextMenu by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showReactionDetails by remember { mutableStateOf(false) }
    var reactionsForDetails by remember { mutableStateOf<List<com.keeftalk.chat.domain.model.MessageReaction>>(emptyList()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var messageText by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    var isCurrentlyTyping by remember { mutableStateOf(false) }

    LaunchedEffect(currentPreviewIndex, itemsToPreview) {
        if (itemsToPreview.isNotEmpty() && currentPreviewIndex in itemsToPreview.indices) {
            messageText = itemsToPreview[currentPreviewIndex].caption
        }
    }

    LaunchedEffect(isCurrentlyTyping) { onTypingStatusChange(isCurrentlyTyping) }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            isCurrentlyTyping = true
            delay(3.seconds)
            isCurrentlyTyping = false
        } else {
            isCurrentlyTyping = false
        }
    }

    val isOnline = remember(peerLastSeen) { (System.currentTimeMillis() - peerLastSeen) < 60_000 }
    val lastSeenText = remember(peerLastSeen, isOnline) {
        if (isOnline) "Active now" else formatLastSeenInternal(peerLastSeen)
    }

    DisposableEffect(screenshotProtectionEnabled) {
        val activity = androidContext as? android.app.Activity
        if (screenshotProtectionEnabled) activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }

    var unreadCount by remember { mutableIntStateOf(0) }
    var firstVisibleItemIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(chatId) { 
        if (chatId != null) onMarkAsRead() 
    }

    LaunchedEffect(replyingToMessage) {
        if (replyingToMessage != null) {
            delay(300.milliseconds) // Increased delay to ensure UI stability
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                android.util.Log.w("ChatDetailScreen", "Failed to request focus for reply: ${e.message}")
            }
        }
    }

    if (showCamera) {
        com.keeftalk.chat.ui.camera.CameraPreview(
            onImageCaptured = { uri -> onImageCaptured(uri); showCamera = false },
            onVideoCaptured = { uri -> onVideoCaptureCaptured(uri); showCamera = false },
            onError = { showCamera = false }
        )
        return
    }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        ChatDetailTopBar(
            chatName = chatName,
            avatarUrl = avatarUrl,
            peerId = peerId,
            chatId = chatId,
            isOnline = isOnline,
            typingUsers = typingUsers,
            lastSeenText = lastSeenText,
            onBackClick = onBackClick,
            onProfileClick = onProfileClick,
            onVoiceCallClick = onVoiceCallClick,
            onVideoCallClick = onVideoCallClick,
            onClearChat = onClearChat,
            onNotificationsClick = onNotificationsClick,
            onThemeChange = onThemeChange,
            onSearchClick = { showSearch = true },
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            showSearch = showSearch,
            onCloseSearch = { showSearch = false; searchQuery = "" },
            isGroup = isGroup,
            onAddMember = { /* TODO */ },
            onViewMembers = { /* TODO */ }
        )

        Box(modifier = Modifier.weight(1f).clipToBounds()) {
            LocalChatTheme.current.background()

            if (itemsToPreview.isNotEmpty()) {
                MediaPreviewOverlay(
                    mediaItems = itemsToPreview,
                    onDismiss = { itemsToPreview = emptyList() },
                    qualityMap = mediaQualityMap,
                    onQualityChange = onQualityChange,
                    onExecuteCommand = { index, command ->
                        if (index in itemsToPreview.indices) {
                            itemsToPreview = itemsToPreview.toMutableList().apply {
                                this[index] = command.execute(this[index])
                            }
                        }
                    },
                    onPageChange = { currentPreviewIndex = it }
                )
            } else {
                ChatMessageList(
                    currentUserId = currentUserId,
                    peerId = peerId,
                    peerName = chatName ?: "User",
                    avatarUrl = avatarUrl,
                    chatSettings = chatSettings,
                    chatTheme = LocalChatTheme.current,
                    messagesFlow = messagesFlow,
                    readReceipts = readReceipts,
                    transferProgress = transferProgress,
                    expandedMessageId = expandedMessageId,
                    initialScrollPosition = initialScrollPosition,
                    initialScrollOffset = initialScrollOffset,
                    onScrollStateChange = onScrollStateChange,
                    onMessageClick = { msg, pos -> onMediaClick(msg.id, pos) },
                    onPdfClick = onPdfClick,
                    onEmailClick = onEmailClick,
                    onVaultClick = onVaultClick,
                    onAgendaClick = onAgendaClick,
                    onCodeClick = onCodeClick,
                    onAddReaction = onAddReaction,
                    onRemoveReaction = onRemoveReaction,
                    onToggleMediaLock = onToggleMediaLock,
                    onDownloadMessage = onDownloadMessage,
                    onShowContextMenu = {
                        selectedMessageForInteractions = it
                        showMessageContextMenu = true
                    },
                    onReplySwipe = {
                        replyingToMessage = it
                    },
                    onToggleTimestamp = onToggleTimestamp,
                    unreadCount = unreadCount,
                    onUnreadCountChange = { unreadCount = it },
                    onFirstVisibleItemChange = { firstVisibleItemIndex = it },
                    onMessageVisible = onMessageVisible,
                    onEncryptionNoticeClick = { showEncryptionExplanation = true },
                    isGroup = isGroup,
                    getUserProfile = getUserProfile,
                    userProfiles = userProfiles
                )
            }

            val typingNames = remember(typingUsers) {
                typingUsers.map { id -> getUserProfile(id)?.name ?: "Someone" }.filter { it != "Someone" }
            }
            if (typingNames.isNotEmpty()) {
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp)) {
                    TypingIndicator(typingNames = typingNames)
                }
            }
        }

        if (showMessageContextMenu && selectedMessageForInteractions != null) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            MessageContextMenu(
                onDismiss = { showMessageContextMenu = false },
                onCopy = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedMessageForInteractions!!.content)) },
                onForward = { showForwardDialog = true },
                onReply = { replyingToMessage = selectedMessageForInteractions },
                onTranslate = { onTranslateMessage(selectedMessageForInteractions!!.id) },
                onReport = { onReportMessage(selectedMessageForInteractions!!.id, "Reported") },
                onBump = { onBumpChat() },
                onDelete = { onDeleteMessage(selectedMessageForInteractions!!.id) },
                onDownload = { onDownloadMessage(selectedMessageForInteractions!!) },
                onCreateEvent = { showMessageContextMenu = false },
                onInfo = { android.widget.Toast.makeText(androidContext, "Status: ${selectedMessageForInteractions!!.status}\nTime: ${formatTimestampInternal(selectedMessageForInteractions!!.timestamp)}", android.widget.Toast.LENGTH_LONG).show() },
                isOwnMessage = selectedMessageForInteractions!!.senderId == currentUserId,
                hasText = selectedMessageForInteractions!!.content.isNotBlank(),
                isMedia = selectedMessageForInteractions!!.type != MessageType.TEXT,
                isLocked = selectedMessageForInteractions!!.mediaLocked
            )
        }

        ChatInput(
            messageText = messageText,
            onMessageChange = {
                messageText = it
                if (itemsToPreview.isNotEmpty() && currentPreviewIndex in itemsToPreview.indices) {
                    itemsToPreview = itemsToPreview.toMutableList().apply {
                        this[currentPreviewIndex] = this[currentPreviewIndex].copy(caption = it)
                    }
                }
            },
            onSendMessage = {
                if (itemsToPreview.isNotEmpty()) {
                    onSendEditedMedia(itemsToPreview)
                    itemsToPreview = emptyList()
                    onClearMediaSelection()
                    messageText = ""
                } else if (messageText.isNotBlank()) {
                    onSendMessage(messageText)
                    messageText = ""
                    isCurrentlyTyping = false
                    showEmojiPicker = false
                    replyingToMessage = null
                }
            },
            onCameraClick = { showCamera = true },
            onCameraLongClick = { showCamera = true },
            onAttachmentClick = { showAttachmentSheet = true },
            onEmojiClick = { showEmojiPicker = !showEmojiPicker },
            onMicStart = onMicStart,
            onMicStop = onMicStop,
            onMicCancel = onMicCancel,
            autoDeleteTimer = autoDeleteTimer,
            focusRequester = focusRequester,
            replyingToMessage = replyingToMessage,
            onCancelReply = { replyingToMessage = null },
            recordingState = recordingState,
            recordingDuration = recordingDuration,
            amplitudeHistory = amplitudeHistory,
            onSendVoice = onSendVoice,
            hasMicPermission = micPermissionState.status.isGranted,
            onRequestMicPermission = { micPermissionState.launchPermissionRequest() },
            isCaptioning = itemsToPreview.isNotEmpty()
        )

        ChatDetailDialogs(
            showAutoDeleteDialog = showAutoDeleteDialog,
            onDismissAutoDelete = { showAutoDeleteDialog = false },
            autoDeleteTimer = autoDeleteTimer,
            onAutoDeleteChange = onAutoDeleteChange,
            showForwardDialog = showForwardDialog,
            onDismissForward = { showForwardDialog = false },
            selectedMessage = selectedMessageForInteractions,
            onForwardMessage = onForwardMessage,
            showReactionDetails = showReactionDetails,
            onDismissReactionDetails = { showReactionDetails = false },
            reactions = reactionsForDetails,
            getUserProfile = getUserProfile,
            currentUserId = currentUserId,
            showPollDialog = showPollDialog,
            onDismissPoll = { showPollDialog = false },
            onSendMessage = onSendMessage,
            failedMessage = failedMessage,
            onDismissFailure = onDismissFailure,
            onRetryMessage = onRetryMessage,
            onCancelMessage = onCancelMessage
        )

        ChatDetailBottomSheets(
            showAttachmentSheet = showAttachmentSheet,
            onDismissAttachment = { showAttachmentSheet = false },
            sheetState = sheetState,
            onAttachmentClick = { type ->
                if (type == AttachmentType.FILE) {
                    showDocumentPicker = true
                } else {
                    onAttachmentClick(type)
                }
            },
            mediaItems = mediaItems,
            onMediaClick = { item ->
                itemsToPreview = listOf(com.keeftalk.chat.domain.model.EditorModel(item))
                currentPreviewIndex = 0
            },
            availableAlbums = availableAlbums,
            selectedAlbum = selectedAlbum,
            onAlbumClick = onAlbumClick,
            selectedMediaList = selectedMediaList,
            onToggleMediaSelection = onToggleMediaSelection,
            onNextClick = {
                itemsToPreview = selectedMediaList.map { com.keeftalk.chat.domain.model.EditorModel(it) }
                currentPreviewIndex = 0
            },
            onGooglePhotosClick = onGooglePhotosClick,
            onGooglePhotosAppClick = onGooglePhotosAppClick,
            showDocumentPicker = showDocumentPicker,
            onDismissDocumentPicker = { showDocumentPicker = false },
            onUnlockFullAccess = onUnlockFullAccess,
            onBrowseSystemDocuments = onBrowseSystemDocuments,
            onReviewDocuments = onReviewDocuments,
            onSendDocuments = onSendDocuments
        )

        if (showEncryptionExplanation) {
            com.keeftalk.chat.ui.screens.chatdetail.EncryptionExplanationBottomSheet(
                onDismiss = { showEncryptionExplanation = false }
            )
        }

        if (showEmojiPicker) {
            EmojiPicker(
                onEmojiSelected = { messageText += it },
                onDismiss = { showEmojiPicker = false },
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
        }
    }
}

/**
 * A LinearLayoutManager that catches IndexOutOfBoundsException to prevent crashes
 * during RecyclerView layout passes, which can happen with Paging 3 and reverse layouts.
 */
private class SafeLinearLayoutManager(context: android.content.Context) : LinearLayoutManager(context) {
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            android.util.Log.e("SafeLLM", "Inconsistency detected in layout pass", e)
        }
    }

    override fun supportsPredictiveItemAnimations(): Boolean = false
}
