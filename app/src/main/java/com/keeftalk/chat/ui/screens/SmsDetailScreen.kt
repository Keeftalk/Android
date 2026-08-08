package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keeftalk.chat.ui.screens.chatdetail.ChatInput
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.theme.ChatThemes
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsDetailScreen(
    viewModel: SmsDetailViewModel,
    onBack: () -> Unit
) {
    val contact by viewModel.contact.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val currentChatTheme = ChatThemes.getById("default")

    val adapter = remember(contact) {
        SmsMessagePagingAdapter(
            currentUserId = "me",
            onMessageClick = { },
            onShowContextMenu = { },
            peerName = contact?.fullName ?: viewModel.address,
            peerAvatarUrl = contact?.avatarUrl
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
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ChatDetailTopBar(
                chatName = contact?.fullName ?: contact?.username ?: viewModel.address,
                avatarUrl = contact?.avatarUrl,
                peerId = contact?.id,
                chatId = viewModel.threadId.toString(),
                isOnline = false,
                typingUsers = emptySet(),
                lastSeenText = "SMS/MMS",
                onBackClick = onBack,
                onProfileClick = { /* View contact profile */ },
                onVoiceCallClick = { /* Start GSM Call */ },
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

            Box(modifier = Modifier.weight(1f)) {
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
    }
}
