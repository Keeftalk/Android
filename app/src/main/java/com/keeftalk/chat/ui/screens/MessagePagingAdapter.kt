package com.keeftalk.chat.ui.screens

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.keeftalk.chat.domain.model.Message
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.keeftalk.chat.ui.theme.KeeftalkTheme
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.screens.chatdetail.MessageBubble
import com.keeftalk.chat.ui.screens.chatdetail.DateSeparator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MessagePagingAdapter(
    private val currentUserId: String,
    initialChatSettings: com.keeftalk.chat.domain.model.UserChatSettings,
    initialChatTheme: com.keeftalk.chat.ui.theme.ChatTheme,
    private val peerId: String?,
    initialPeerName: String,
    initialPeerAvatarUrl: String?,
    initialReadReceipts: Map<String, Pair<String, Long>>,
    private val onMessageClick: (Message, Long) -> Unit,
    private val onPdfClick: (String) -> Unit = {},
    private val onEmailClick: (String) -> Unit = {},
    private val onVaultClick: (String) -> Unit = {},
    private val onAgendaClick: (String) -> Unit = {},
    private val onCodeClick: (String) -> Unit = {},
    private val onDoubleTap: (Message) -> Unit = {},
    private val onReactionClick: (Message, String) -> Unit = { _, _ -> },
    private val onToggleMediaLock: (String, Boolean) -> Unit = { _, _ -> },
    private val onDownloadMessage: (Message) -> Unit = {},
    private val onShowContextMenu: (Message) -> Unit,
    private val onReplySwipe: (Message) -> Unit = {},
    private val onEncryptionNoticeClick: () -> Unit = {},
    initialTransferProgress: Map<String, Float> = emptyMap(),
    initialIsGroup: Boolean = false,
    private val getUserProfile: (String) -> com.keeftalk.chat.domain.model.User? = { null }
) : PagingDataAdapter<ChatItem, RecyclerView.ViewHolder>(ChatItemDiffCallback()) {

    var chatSettings by mutableStateOf(initialChatSettings)
    var chatTheme by mutableStateOf(initialChatTheme)
    var peerName by mutableStateOf(initialPeerName)
    var peerAvatarUrl by mutableStateOf(initialPeerAvatarUrl)
    var isGroup by mutableStateOf(initialIsGroup)
    var readReceipts by mutableStateOf(initialReadReceipts)
    var transferProgress by mutableStateOf(initialTransferProgress)
    var userProfiles by mutableStateOf<Map<String, com.keeftalk.chat.domain.model.User>>(emptyMap())
    var messageIdForReactions by mutableStateOf<String?>(null)
    var expandedMessageId by mutableStateOf<String?>(null)
    var onToggleTimestamp: ((String) -> Unit)? = null

    fun updateSettings(
        newSettings: com.keeftalk.chat.domain.model.UserChatSettings,
        newTheme: com.keeftalk.chat.ui.theme.ChatTheme,
        newName: String,
        newAvatar: String?,
        isGroup: Boolean = false,
        profiles: Map<String, com.keeftalk.chat.domain.model.User> = emptyMap()
    ) {
        this.chatSettings = newSettings
        this.chatTheme = newTheme
        this.peerName = newName
        this.peerAvatarUrl = newAvatar
        this.isGroup = isGroup
        this.userProfiles = profiles
    }

    companion object {
        private const val TYPE_MESSAGE = 1
        private const val TYPE_SEPARATOR = 2
        private const val TYPE_ENCRYPTION_NOTICE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatItem.MessageItem -> TYPE_MESSAGE
            is ChatItem.DateSeparatorItem -> TYPE_SEPARATOR
            is ChatItem.EncryptionNoticeItem -> TYPE_ENCRYPTION_NOTICE
            null -> TYPE_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        return when (viewType) {
            TYPE_MESSAGE -> MessageViewHolder(composeView)
            TYPE_SEPARATOR -> SeparatorViewHolder(composeView)
            TYPE_ENCRYPTION_NOTICE -> EncryptionNoticeViewHolder(composeView)
            else -> MessageViewHolder(composeView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        
        when (holder) {
            is MessageViewHolder -> {
                // ... message binding logic
                val messageItem = item as ChatItem.MessageItem
                val message = messageItem.message
                android.util.Log.d("CHAT_DEBUG", "Adapter: onBindViewHolder pos=$position msgId=${message.id}")
                com.keeftalk.chat.util.PerformanceProfiler.logEvent("First Message Composed", category = com.keeftalk.chat.util.PerformanceProfiler.Category.UI)

                // Grouping logic for Media Stack
                val nextMsg = findNextMessage(position)
                val isNextImage = nextMsg?.type == com.keeftalk.chat.domain.model.MessageType.IMAGE && 
                                  nextMsg.senderId == message.senderId && 
                                  (message.timestamp - nextMsg.timestamp) < 60_000

                val prevMsg = findPrevMessage(position)
                val isPrevImage = prevMsg?.type == com.keeftalk.chat.domain.model.MessageType.IMAGE && 
                                  prevMsg.senderId == message.senderId && 
                                  (prevMsg.timestamp - message.timestamp) < 60_000

                // If part of a group, only the "first" (newest in reverse list, so lowest index) shows the stack
                val isStackRoot = message.type == com.keeftalk.chat.domain.model.MessageType.IMAGE && isNextImage && !isPrevImage
                val isStackChild = message.type == com.keeftalk.chat.domain.model.MessageType.IMAGE && isPrevImage

                // Collect all images in this stack for the root
                val stackMessages = mutableListOf<com.keeftalk.chat.domain.model.Message>()
                if (isStackRoot) {
                    stackMessages.add(message)
                    var i = position + 1
                    while (i < itemCount) {
                        val m = (getItem(i) as? ChatItem.MessageItem)?.message
                        if (m?.type == com.keeftalk.chat.domain.model.MessageType.IMAGE && m.senderId == message.senderId) {
                            stackMessages.add(m)
                            i++
                        } else if (getItem(i) is ChatItem.DateSeparatorItem) {
                            i++ // Skip separator in stack search
                        } else break
                    }
                }

                val seenUsers = readReceipts.filter { (_, pointer) ->
                    pointer.first == message.id
                }.keys.mapNotNull { userId -> userProfiles[userId] ?: getUserProfile(userId) }
                .sortedByDescending { it.isContact } // Contacts first

                val seenCount = if (isGroup) {
                    readReceipts.count { (_, pointer) ->
                        pointer.first == message.id || pointer.second >= message.timestamp
                    }
                } else 0

                val isLastSeenByPeer = !isGroup && readReceipts.values.any { it.first == message.id }

                val isLatest = isLatestMessage(position)
                holder.bind(message, isStackRoot, isStackChild, stackMessages, isLatest, seenUsers, isLastSeenByPeer, seenCount)
            }
            is SeparatorViewHolder -> {
                val separatorItem = item as ChatItem.DateSeparatorItem
                holder.bind(separatorItem.date)
            }
            is EncryptionNoticeViewHolder -> {
                holder.bind()
            }
        }
    }

    private fun findNextMessage(position: Int): Message? {
        var i = position + 1
        while (i < itemCount) {
            val item = getItem(i)
            if (item is ChatItem.MessageItem) return item.message
            i++
        }
        return null
    }

    private fun findPrevMessage(position: Int): Message? {
        var i = position - 1
        while (i >= 0) {
            val item = getItem(i)
            if (item is ChatItem.MessageItem) return item.message
            i--
        }
        return null
    }

    private fun isLatestMessage(position: Int): Boolean {
        for (i in 0 until position) {
            if (getItem(i) is ChatItem.MessageItem) return false
        }
        return true
    }

    inner class SeparatorViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(date: String) {
            composeView.setContent {
                KeeftalkTheme(chatSettings = chatSettings) {
                    DateSeparator(date = date)
                }
            }
        }
    }

    inner class EncryptionNoticeViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind() {
            composeView.setContent {
                KeeftalkTheme(chatSettings = chatSettings) {
                    CompositionLocalProvider(LocalChatTheme provides chatTheme) {
                        com.keeftalk.chat.ui.screens.chatdetail.EncryptionNotice(
                            onInfoClick = { onEncryptionNoticeClick() }
                        )
                    }
                }
            }
        }
    }

    inner class MessageViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(
            message: Message, 
            isStackRoot: Boolean = false, 
            isStackChild: Boolean = false, 
            stackMessages: List<Message> = emptyList(),
            isLatest: Boolean = false,
            seenUsers: List<com.keeftalk.chat.domain.model.User> = emptyList(),
            isLastSeenByPeer: Boolean = false,
            seenCount: Int = 0
        ) {
            val currentStatus = message.status
            val currentId = message.id
            val themeId = chatTheme.id
            val progress = transferProgress[message.id] ?: 0f
            val isExpanded = expandedMessageId == message.id
            val profile = userProfiles[message.senderId] ?: getUserProfile(message.senderId)

            val localPath = message.localFilePath ?: ""
            val decryptState = message.decryptionState.name
            val seenHash = seenUsers.hashCode()
            val settingsHash = chatSettings.hashCode()
            
            // OPTIMIZATION: Only include message-specific receipt info in the tag.
            // This prevents updates to OTHER messages from invalidating this bubble.
            val isSeenByMe = message.status == com.keeftalk.chat.domain.model.MessageStatus.SEEN
            val reactionHash = message.reactions.hashCode()
            
            val bindingTag = "msg_${currentId}_${currentStatus}_${themeId}_${isLatest}_${isStackRoot}_${stackMessages.size}_${progress}_${isExpanded}_${localPath.hashCode()}_${decryptState}_${isGroup}_${profile?.id}_${profile?.name}_${seenHash}_${isLastSeenByPeer}_${seenCount}_${settingsHash}_${isSeenByMe}_${reactionHash}"
            
            if (composeView.tag == bindingTag) {
                return
            }
            composeView.tag = bindingTag

            composeView.setContent {
                KeeftalkTheme(chatSettings = chatSettings) {
                    CompositionLocalProvider(LocalChatTheme provides chatTheme) {
                        if (isStackChild) {
                            Box(modifier = androidx.compose.ui.Modifier.size(0.dp))
                        } else {
                            MessageBubble(
                                message = message,
                                currentUserId = currentUserId,
                                chatSettings = chatSettings,
                                isLastSeenByPeer = isLastSeenByPeer,
                                seenUsers = seenUsers,
                                seenCount = seenCount,
                                peerAvatarUrl = peerAvatarUrl,
                                peerName = peerName,
                                peerId = peerId ?: "",
                                onLongClick = { messageIdForReactions = message.id },
                                onDoubleTap = { onDoubleTap(message) },
                                onReactionClick = { emoji -> 
                                    onReactionClick(message, emoji)
                                },
                                onReactionSelect = { emoji ->
                                    onReactionClick(message, emoji)
                                    messageIdForReactions = null
                                },
                                onMediaClick = { id, pos -> onMessageClick(message, pos) },
                                onPdfClick = { onPdfClick(it) },
                                onEmailClick = { onEmailClick(it) },
                                onVaultClick = { onVaultClick(it) },
                                onAgendaClick = { onAgendaClick(it) },
                                onCodeClick = { onCodeClick(it) },
                                onToggleMediaLock = onToggleMediaLock,
                                showReactionsPopup = message.id == messageIdForReactions,
                                onDismissReactions = { messageIdForReactions = null },
                                onMoreClick = { 
                                    onShowContextMenu(message)
                                    messageIdForReactions = null
                                },
                                isStackRoot = isStackRoot,
                                stackMessages = stackMessages,
                                onReplySwipe = { onReplySwipe(message) },
                                transferProgress = transferProgress[message.id],
                                onDownloadClick = { onDownloadMessage(message) },
                                isLatest = isLatest,
                                isTimestampExpanded = expandedMessageId == message.id,
                                onToggleTimestamp = {
                                    onToggleTimestamp?.invoke(message.id)
                                },
                                isGroup = isGroup,
                                senderProfile = profile
                            )
                        }
                    }
                }
            }
        }
    }
}

class ChatItemDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return when {
            oldItem is ChatItem.MessageItem && newItem is ChatItem.MessageItem -> 
                oldItem.message.id == newItem.message.id
            oldItem is ChatItem.DateSeparatorItem && newItem is ChatItem.DateSeparatorItem -> 
                oldItem.date == newItem.date
            oldItem is ChatItem.EncryptionNoticeItem && newItem is ChatItem.EncryptionNoticeItem ->
                true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem == newItem
    }
}
