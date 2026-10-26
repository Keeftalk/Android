package tn.keeftalk.chat.ui.screens

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.repository.SmsMessage
import tn.keeftalk.chat.ui.components.MessageContextMenu
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
    var selectedSmsMessage by remember { mutableStateOf<SmsMessage?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<SmsMessage?>(null) }

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val currentChatTheme = ChatThemes.getById("default")

    val adapter = remember(contactInfo) {
        SmsMessagePagingAdapter(
            currentUserId = "me",
            onMessageClick = { },
            onShowContextMenu = { smsMsg ->
                selectedSmsMessage = smsMsg
                showContextMenu = true
            },
            peerName = contactInfo.name,
            peerAvatarUrl = contactInfo.avatarUrl,
            onReplySwipe = { smsMsg ->
                replyingToMessage = smsMsg
            }
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
                                replyingToMessage = null
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
                        autoDeleteTimer = null,
                        replyingToMessage = replyingToMessage?.toKeeftalkMessage(),
                        onCancelReply = { replyingToMessage = null }
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
                            val lm = LinearLayoutManager(ctx).apply {
                                stackFromEnd = true
                                reverseLayout = true
                            }
                            layoutManager = lm
                            this.adapter = adapter

                            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                                    if (positionStart == 0) {
                                        post {
                                            lm.scrollToPositionWithOffset(0, 0)
                                        }
                                    }
                                }
                            })
                        }
                    },
                    update = { recyclerView ->
                        val lm = recyclerView.layoutManager as? LinearLayoutManager
                        if (adapter.itemCount > 0 && lm != null) {
                            recyclerView.post {
                                lm.scrollToPositionWithOffset(0, 0)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (showContextMenu && selectedSmsMessage != null) {
                    val msg = selectedSmsMessage!!
                    val textToCopy = msg.body.ifBlank {
                        msg.attachments.firstOrNull()?.name
                            ?: msg.attachments.firstOrNull()?.uri
                            ?: ""
                    }
                    val isMe = msg.type == 2 || msg.type == 4 || msg.type == 5 || msg.type == 6

                    MessageContextMenu(
                        onDismiss = { showContextMenu = false },
                        onCopy = {
                            if (textToCopy.isNotBlank()) {
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", textToCopy)))
                                }
                                Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            showContextMenu = false
                        },
                        onForward = {
                            if (textToCopy.isNotBlank()) {
                                messageText = textToCopy
                            }
                            showContextMenu = false
                        },
                        onReply = {
                            replyingToMessage = msg
                            showContextMenu = false
                        },
                        onTranslate = { showContextMenu = false },
                        onReport = { showContextMenu = false },
                        onBump = { showContextMenu = false },
                        onDelete = {
                            viewModel.deleteConversation()
                            showContextMenu = false
                        },
                        onInfo = {
                            val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                            val infoText = "Address: ${msg.address}\nType: ${if (isMe) "Sent" else "Received"}\nTime: $timeStr"
                            Toast.makeText(context, infoText, Toast.LENGTH_LONG).show()
                            showContextMenu = false
                        },
                        onCreateEvent = { showContextMenu = false },
                        isOwnMessage = isMe,
                        hasText = textToCopy.isNotBlank(),
                        isMedia = msg.isMms,
                        isLocked = false
                    )
                }
            }
        }
    }
}
