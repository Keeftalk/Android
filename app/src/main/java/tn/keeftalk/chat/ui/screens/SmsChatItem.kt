package tn.keeftalk.chat.ui.screens

import tn.keeftalk.chat.domain.repository.SmsMessage

sealed class SmsChatItem {
    data class MessageItem(val message: SmsMessage) : SmsChatItem()
    data class DateSeparatorItem(val date: String) : SmsChatItem()
}
