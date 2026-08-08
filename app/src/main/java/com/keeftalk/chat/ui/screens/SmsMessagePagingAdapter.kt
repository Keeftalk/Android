package com.keeftalk.chat.ui.screens

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.keeftalk.chat.domain.repository.SmsMessage
import androidx.compose.ui.platform.ComposeView
import com.keeftalk.chat.ui.theme.KeeftalkTheme
import com.keeftalk.chat.ui.theme.LocalChatTheme
import com.keeftalk.chat.ui.screens.chatdetail.MessageBubble
import com.keeftalk.chat.ui.screens.chatdetail.DateSeparator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.model.MessageType

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
                        chatSettings = com.keeftalk.chat.domain.model.UserChatSettings(""),
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
                        isLatest = false,
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
    return Message(
        id = id.toString(),
        chatId = threadId.toString(),
        senderId = if (type == 2) "me" else address, // type 2 is sent
        content = body,
        timestamp = timestamp,
        status = when (deliveryStatus) {
            0 -> com.keeftalk.chat.domain.model.MessageStatus.DELIVERED
            else -> if (type == 2) com.keeftalk.chat.domain.model.MessageStatus.SENT else com.keeftalk.chat.domain.model.MessageStatus.FCM_RECEIVED
        },
        type = if (isMms) MessageType.IMAGE else MessageType.TEXT // Simplified
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
