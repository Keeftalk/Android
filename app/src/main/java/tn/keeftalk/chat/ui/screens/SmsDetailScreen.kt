package tn.keeftalk.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import tn.keeftalk.chat.ui.screens.chatdetail.ChatInput
import tn.keeftalk.chat.ui.theme.ChatThemes
import tn.keeftalk.chat.ui.theme.LocalChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsDetailScreen(
    viewModel: SmsDetailViewModel,
    onBack: () -> Unit
) {
    val contactInfo by viewModel.contactInfo.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val currentChatTheme = ChatThemes.getById("default")

    val adapter = remember(contactInfo) {
        SmsMessagePagingAdapter(
            currentUserId = "me",
            onMessageClick = { },
            onShowContextMenu = { },
            peerName = contactInfo.name,
            peerAvatarUrl = contactInfo.avatarUrl
        )
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel.messages) {
        viewModel.messages.collectLatest { adapter.submitData(lifecycle, it) }
    }

    LaunchedEffect(viewModel.threadId) {
        viewModel.markAsRead()
    }

    CompositionLocalProvider(LocalChatTheme provides currentChatTheme) {
        Scaffold(
            topBar = {
                ChatDetailTopBar(
                    chatName = contactInfo.name,
                    avatarUrl = contactInfo.avatarUrl,
                    peerId = viewModel.address,
                    chatId = viewModel.threadId.toString(),
                    isOnline = false,
                    typingUsers = emptySet(),
                    lastSeenText = "SMS/MMS",
                    onBackClick = onBack,
                    onProfileClick = { },
                    onVoiceCallClick = { },
                    onVideoCallClick = { },
                    onClearChat = { },
                    onNotificationsClick = { },
                    onThemeChange = { },
                    onSearchClick = { },
                    searchQuery = "",
                    onQueryChange = { },
                    showSearch = false,
                    onCloseSearch = { }
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    ChatInput(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSendMessage = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        onCameraClick = { },
                        onCameraLongClick = { },
                        onAttachmentClick = { },
                        onEmojiClick = { },
                        onMicStart = { },
                        onMicStop = { },
                        onMicCancel = { },
                        focusRequester = focusRequester,
                        autoDeleteTimer = null
                    )
                }
            },
            contentWindowInsets = WindowInsets.statusBars
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                currentChatTheme.background()

                AndroidView(
                    factory = { ctx ->
                        RecyclerView(ctx).apply {
                            layoutManager = LinearLayoutManager(ctx).apply {
                                stackFromEnd = true
                                reverseLayout = true
                            }
                            this.adapter = adapter
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
