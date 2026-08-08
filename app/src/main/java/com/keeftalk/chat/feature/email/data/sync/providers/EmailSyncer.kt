package com.keeftalk.chat.feature.email.data.sync.providers

import com.keeftalk.chat.feature.email.model.*

interface EmailSyncer {
    suspend fun syncFolders(accountId: String)
    suspend fun syncMessages(accountId: String, folderId: String)
    suspend fun sendMessage(accountId: String, subject: String, content: String, to: List<EmailRecipient>, cc: List<EmailRecipient>, bcc: List<EmailRecipient>, attachments: List<EmailAttachment>)
    suspend fun deleteMessage(accountId: String, messageId: String)
    suspend fun markAsRead(accountId: String, messageId: String, isRead: Boolean)
    suspend fun toggleStar(accountId: String, messageId: String, isStarred: Boolean)
    suspend fun downloadAttachment(accountId: String, attachment: EmailAttachment): String?
}
