package tn.keeftalk.chat.ui.screens

import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import tn.keeftalk.chat.domain.model.Message
import tn.keeftalk.chat.domain.model.MessageStatus
import tn.keeftalk.chat.domain.model.MessageType
import tn.keeftalk.chat.domain.repository.SmsMessage
import tn.keeftalk.chat.ui.screens.chatdetail.DateSeparator
import tn.keeftalk.chat.ui.screens.chatdetail.MessageBubble
import tn.keeftalk.chat.ui.theme.KeeftalkTheme

class SmsMessagePagingAdapter(
    private val currentUserId: String,
    private val onMessageClick: (SmsMessage) -> Unit,
    private val onShowContextMenu: (SmsMessage) -> Unit,
    private val peerName: String,
    private val peerAvatarUrl: String?
) : PagingDataAdapter<SmsChatItem, RecyclerView.ViewHolder>(SmsChatItemDiffCallback()) {

    var expandedMessageId by mutableStateOf<String?>(null)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SmsChatItem.MessageItem -> 1
            is SmsChatItem.DateSeparatorItem -> 2
            null -> 1
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
            1 -> SmsMessageViewHolder(composeView)
            2 -> SmsSeparatorViewHolder(composeView)
            else -> SmsMessageViewHolder(composeView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        when (holder) {
            is SmsMessageViewHolder -> {
                val message = (item as SmsChatItem.MessageItem).message
                holder.bind(message)
            }
            is SmsSeparatorViewHolder -> {
                val date = (item as SmsChatItem.DateSeparatorItem).date
                holder.bind(date)
            }
        }
    }

    class SmsSeparatorViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(date: String) {
            composeView.setContent {
                KeeftalkTheme {
                    DateSeparator(date = date)
                }
            }
        }
    }

    inner class SmsMessageViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        fun bind(smsMessage: SmsMessage) {
            val message = smsMessage.toKeeftalkMessage()
            composeView.setContent {
                KeeftalkTheme {
                    MessageBubble(
                        message = message,
                        currentUserId = currentUserId,
                        chatSettings = tn.keeftalk.chat.domain.model.UserChatSettings(""),
                        isLastSeenByPeer = false,
                        seenUsers = emptyList(),
                        seenCount = 0,
                        peerAvatarUrl = peerAvatarUrl,
                        peerName = peerName,
                        peerId = smsMessage.address,
                        onLongClick = { onShowContextMenu(smsMessage) },
                        onDoubleTap = { },
                        onReactionClick = { },
                        onReactionSelect = { },
                        onMediaClick = { _, _ -> onMessageClick(smsMessage) },
                        onPdfClick = { },
                        onToggleMediaLock = { _, _ -> },
                        showReactionsPopup = false,
                        onDismissReactions = { },
                        onMoreClick = { onShowContextMenu(smsMessage) },
                        isStackRoot = false,
                        stackMessages = emptyList(),
                        onReplySwipe = { },
                        transferProgress = null,
                        onDownloadClick = { },
                        isLatest = true,
                        isTimestampExpanded = expandedMessageId == smsMessage.id.toString(),
                        onToggleTimestamp = {
                            expandedMessageId = if (expandedMessageId == smsMessage.id.toString()) null else smsMessage.id.toString()
                        },
                        isGroup = false,
                        senderProfile = null,
                        isSms = true
                    )
                }
            }
        }
    }
}

private fun SmsMessage.toKeeftalkMessage(): Message {
    val isMe = type == 2 || type == 4 || type == 5 || type == 6
    val msgStatus = when {
        type == 5 || status == 64 -> MessageStatus.FAILED
        type == 4 || type == 6 || status == 32 -> MessageStatus.SENDING
        deliveryStatus == 0 || status == 0 -> MessageStatus.DELIVERED
        isMe -> MessageStatus.SENT
        else -> MessageStatus.DELIVERED
    }

    return Message(
        id = id.toString(),
        chatId = threadId.toString(),
        senderId = if (isMe) "me" else address,
        content = body,
        timestamp = timestamp,
        status = msgStatus,
        type = if (isMms) MessageType.IMAGE else MessageType.TEXT
    )
}

class SmsChatItemDiffCallback : DiffUtil.ItemCallback<SmsChatItem>() {
    override fun areItemsTheSame(oldItem: SmsChatItem, newItem: SmsChatItem): Boolean {
        return when {
            oldItem is SmsChatItem.MessageItem && newItem is SmsChatItem.MessageItem -> 
                oldItem.message.id == newItem.message.id
            oldItem is SmsChatItem.DateSeparatorItem && newItem is SmsChatItem.DateSeparatorItem -> 
                oldItem.date == newItem.date
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: SmsChatItem, newItem: SmsChatItem): Boolean {
        return oldItem == newItem
    }
}
