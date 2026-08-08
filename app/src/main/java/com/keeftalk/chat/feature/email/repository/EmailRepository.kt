package com.keeftalk.chat.feature.email.repository

import androidx.paging.PagingData
import com.keeftalk.chat.data.local.entities.MailAccountEntity
import com.keeftalk.chat.feature.email.model.*
import kotlinx.coroutines.flow.Flow

interface EmailRepository {
    // --- Accounts ---
    fun getAccounts(): Flow<List<EmailAccount>>
    suspend fun addAccount(account: EmailAccount, refreshToken: String, accessToken: String, expiresAt: Long)
    suspend fun removeAccount(accountId: String)
    suspend fun updateAccountSyncTimestamp(accountId: String, timestamp: Long)
    suspend fun validateImapConnection(config: MailAccountEntity, password: String): Result<Unit>
    suspend fun addImapAccount(
        account: EmailAccount,
        password: String,
        imapHost: String,
        imapPort: Int,
        smtpHost: String,
        smtpPort: Int,
        securityType: String
    ): Result<Unit>

    // --- Folders & Labels ---
    fun getFolders(accountId: String): Flow<List<EmailFolder>>
    fun getLabels(accountId: String): Flow<List<EmailLabel>>

    // --- Messages ---
    fun getMessages(accountId: String, folderId: String): Flow<PagingData<EmailMessage>>
    fun getMessagesByType(accountId: String, folderType: FolderType): Flow<PagingData<EmailMessage>>
    fun getStarredMessages(accountId: String? = null): Flow<PagingData<EmailMessage>>
    fun getImportantMessages(accountId: String? = null): Flow<PagingData<EmailMessage>>
    fun getMergedInbox(): Flow<PagingData<EmailMessage>>
    fun getMessage(messageId: String): Flow<EmailMessage?>
    fun searchMessages(query: String): Flow<PagingData<EmailMessage>>

    // --- Actions ---
    suspend fun syncAccount(accountId: String)
    fun scheduleBackgroundSync()
    suspend fun sendMessage(accountId: String, subject: String, content: String, to: List<EmailRecipient>, cc: List<EmailRecipient> = emptyList(), bcc: List<EmailRecipient> = emptyList(), attachments: List<EmailAttachment> = emptyList())
    suspend fun deleteMessage(messageId: String)
    suspend fun markAsRead(messageId: String, isRead: Boolean)
    suspend fun toggleStar(messageId: String, isStarred: Boolean)
    suspend fun moveMessage(messageId: String, targetFolderId: String)
    suspend fun archiveMessage(messageId: String)
    suspend fun clearAllCache()
    fun getSharedEmails(): Flow<PagingData<EmailMessage>>
    suspend fun markEmailAsShared(messageId: String)

    // --- Attachments ---
    suspend fun downloadAttachment(attachment: EmailAttachment): String? // Returns local URI

    fun shutdown()
}
