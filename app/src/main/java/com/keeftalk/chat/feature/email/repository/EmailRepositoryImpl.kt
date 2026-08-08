package com.keeftalk.chat.feature.email.repository

import androidx.paging.*
import com.keeftalk.chat.data.local.dao.MailDao
import com.keeftalk.chat.data.local.entities.*
import com.keeftalk.chat.feature.email.data.sync.MailSyncManager
import com.keeftalk.chat.feature.email.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class EmailRepositoryImpl(
    private val context: android.content.Context,
    private val mailDao: MailDao,
    private val syncManager: MailSyncManager,
    private val secureStore: com.keeftalk.chat.feature.email.data.local.EmailSecureStore
) : EmailRepository {

    companion object {
        private const val IDENTITY_TOOLKIT_API_KEY = "AIzaSyAQUUeXA-P2_jnc-F10KdVCLs4YH-EyQ5Y"
    }

    override fun getAccounts(): Flow<List<EmailAccount>> = mailDao.getAccounts()
        .distinctUntilChanged()
        .map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addAccount(account: EmailAccount, refreshToken: String, accessToken: String, expiresAt: Long) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("EmailRepository", "addAccount start: ${account.emailAddress}")
            
            var finalRefreshToken = refreshToken
            var finalAccessToken = accessToken
            var finalExpiresAt = expiresAt

            // If it's Gmail and we have a serverAuthCode (passed as refreshToken initially in the call from ViewModel)
            if (account.provider == EmailProvider.GMAIL && refreshToken.isNotEmpty() && refreshToken.length < 100) {
                android.util.Log.d("EmailRepository", "Exchanging serverAuthCode for tokens...")
                try {
                    val tokenResponse = com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest(
                        com.google.api.client.http.javanet.NetHttpTransport(),
                        com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                        "https://oauth2.googleapis.com/token",
                        "263449157700-5jmpabi45c5m8qkgmb3vsjiu4smfulo1.apps.googleusercontent.com",
                        "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", 
                        refreshToken, // auth code
                        "" // redirect uri - usually empty for installed apps
                    ).execute()

                    finalAccessToken = tokenResponse.accessToken
                    finalRefreshToken = tokenResponse.refreshToken ?: refreshToken
                    finalExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresInSeconds ?: 3600) * 1000
                    android.util.Log.d("EmailRepository", "Token exchange success")
                } catch (e: Exception) {
                    android.util.Log.e("EmailRepository", "Token exchange failed: ${e.message}", e)
                }
            }

            val entity = account.toEntity(finalRefreshToken, finalAccessToken, finalExpiresAt)
            mailDao.insertAccount(entity)
            
            secureStore.saveTokens(account.id, finalRefreshToken, finalAccessToken, finalExpiresAt)
            syncAccount(account.id)
            android.util.Log.d("EmailRepository", "addAccount finished")
        }
    }

    override suspend fun removeAccount(accountId: String) {
        val account = mailDao.getAccounts().first().find { it.id == accountId }
        account?.let { 
            android.util.Log.d("EmailRepository", "[REMOVE_ACCOUNT] Removing account: ${it.emailAddress}")
            mailDao.deleteAccount(it) 
            mailDao.deleteMessagesByAccount(accountId)
        }
        secureStore.removeCredentials(accountId)
    }

    override suspend fun validateImapConnection(config: MailAccountEntity, password: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val props = java.util.Properties()
            props["mail.store.protocol"] = "imap"
            props["mail.imap.host"] = config.imapHost
            props["mail.imap.port"] = config.imapPort.toString()
            
            if (config.securityType == "SSL_TLS") {
                props["mail.imap.ssl.enable"] = "true"
                props["mail.imap.socketFactory.port"] = config.imapPort.toString()
                props["mail.imap.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                props["mail.imap.socketFactory.fallback"] = "false"
            } else if (config.securityType == "STARTTLS") {
                props["mail.imap.starttls.enable"] = "true"
            }

            val session = javax.mail.Session.getInstance(props)
            val store = session.getStore("imap")
            store.connect(config.emailAddress, password)
            store.close()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("EmailRepository", "IMAP Validation failed", e)
            Result.failure(e)
        }
    }

    override suspend fun addImapAccount(
        account: EmailAccount,
        password: String,
        imapHost: String,
        imapPort: Int,
        smtpHost: String,
        smtpPort: Int,
        securityType: String
    ): Result<Unit> {
        val entity = account.toEntity("", "", 0).copy(
            imapHost = imapHost,
            imapPort = imapPort,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            securityType = securityType
        )
        mailDao.insertAccount(entity)
        secureStore.savePassword(account.id, password)
        syncAccount(account.id)
        return Result.success(Unit)
    }

    override suspend fun updateAccountSyncTimestamp(accountId: String, timestamp: Long) {
        val account = mailDao.getAccounts().first().find { it.id == accountId }
        account?.let {
            mailDao.insertAccount(it.copy(lastSyncTimestamp = timestamp))
        }
    }

    override fun getFolders(accountId: String): Flow<List<EmailFolder>> = mailDao.getFolders(accountId).map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getLabels(accountId: String): Flow<List<EmailLabel>> = emptyFlow() // TODO: Implement Labels

    override fun getMessages(accountId: String, folderId: String): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { mailDao.getMessagesPagingSource(accountId, folderId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getMessagesByType(accountId: String, folderType: FolderType): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { mailDao.getMessagesByFolderTypePagingSource(accountId, folderType) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getStarredMessages(accountId: String?): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                if (accountId == null) mailDao.getStarredMessagesPagingSource()
                else mailDao.getStarredMessagesPagingSource(accountId)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getImportantMessages(accountId: String?): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                if (accountId == null) mailDao.getImportantMessagesPagingSource()
                else mailDao.getImportantMessagesPagingSource(accountId)
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getMergedInbox(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { mailDao.getMergedInboxPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMessage(messageId: String): Flow<EmailMessage?> = 
        mailDao.getMessage(messageId).flatMapLatest { messageEntity ->
            if (messageEntity == null) return@flatMapLatest flowOf(null)
            mailDao.getAttachments(messageId).map { attachmentEntities ->
                messageEntity.toDomain(attachmentEntities.map { it.toDomain() })
            }
        }

    override fun searchMessages(query: String): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { mailDao.searchMessagesPagingSource(query) }
        ).flow.map { pagingData: PagingData<MailMessageEntity> ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun syncAccount(accountId: String) {
        // Optional: Check if token is expired and refresh if needed for Gmail
        val account = mailDao.getAccountSync(accountId)
        if (account?.provider == EmailProvider.GMAIL) {
            val expiration = secureStore.getTokenExpiration(accountId)
            if (expiration < System.currentTimeMillis() + 300000) { // Refresh 5 mins before expiry
                android.util.Log.d("EmailRepository", "Gmail token expired or expiring soon, refreshing...")
                refreshGmailToken(accountId)
            }
        }
        syncManager.syncAccount(accountId)
    }

    private suspend fun refreshGmailToken(accountId: String) {
        val refreshToken = secureStore.getRefreshToken(accountId) ?: return
        try {
            val tokenResponse = com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                refreshToken,
                "263449157700-5jmpabi45c5m8qkgmb3vsjiu4smfulo1.apps.googleusercontent.com",
                "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"
            ).execute()

            val newAccessToken = tokenResponse.accessToken
            val expiresAt = System.currentTimeMillis() + (tokenResponse.expiresInSeconds ?: 3600) * 1000
            secureStore.saveTokens(accountId, refreshToken, newAccessToken, expiresAt)
            
            val account = mailDao.getAccountSync(accountId)
            account?.let {
                mailDao.insertAccount(it.copy(accessToken = newAccessToken, tokenExpiration = expiresAt))
            }
            android.util.Log.d("EmailRepository", "Gmail token refreshed successfully")
        } catch (e: Exception) {
            android.util.Log.e("EmailRepository", "Failed to refresh Gmail token", e)
        }
    }

    override fun scheduleBackgroundSync() {
        syncManager.scheduleBackgroundSync()
    }

    override suspend fun sendMessage(accountId: String, subject: String, content: String, to: List<EmailRecipient>, cc: List<EmailRecipient>, bcc: List<EmailRecipient>, attachments: List<EmailAttachment>) {
        // Use provider-specific sender logic via syncManager or syncer
    }

    override suspend fun deleteMessage(messageId: String) {
        val message = mailDao.getMessage(messageId).firstOrNull()
        if (message != null) {
            val trashFolder = mailDao.getFolderByType(message.accountId, FolderType.TRASH)
            if (trashFolder != null && message.folderId != trashFolder.id) {
                mailDao.updateMessage(message.copy(folderId = trashFolder.id))
            } else {
                mailDao.deleteMessage(messageId)
            }
        }
    }

    override suspend fun markAsRead(messageId: String, isRead: Boolean) {
        val message = mailDao.getMessage(messageId).firstOrNull()
        message?.let {
            mailDao.updateMessage(it.copy(isUnread = !isRead))
        }
    }

    override suspend fun toggleStar(messageId: String, isStarred: Boolean) {
        val message = mailDao.getMessage(messageId).firstOrNull()
        message?.let {
            mailDao.updateMessage(it.copy(isStarred = isStarred))
        }
    }

    override suspend fun moveMessage(messageId: String, targetFolderId: String) {
        val message = mailDao.getMessage(messageId).firstOrNull()
        message?.let {
            mailDao.updateMessage(it.copy(folderId = targetFolderId))
        }
    }

    override suspend fun archiveMessage(messageId: String) {
        val message = mailDao.getMessage(messageId).firstOrNull()
        if (message != null) {
            val archiveFolder = mailDao.getFolderByType(message.accountId, FolderType.ARCHIVE)
                ?: mailDao.getFolderByType(message.accountId, FolderType.ALL_MAIL)
            
            if (archiveFolder != null) {
                mailDao.updateMessage(message.copy(folderId = archiveFolder.id))
            } else {
                // If no archive folder, just mark as deleted or keep in inbox?
                // Most providers have archive. For now, mark as deleted if no archive.
                mailDao.deleteMessage(messageId)
            }
        }
    }

    override suspend fun clearAllCache() {
        mailDao.getAccounts().first().forEach { account ->
            mailDao.deleteMessagesByAccount(account.id)
        }
    }

    override fun getSharedEmails(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { mailDao.getSharedMessagesPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun markEmailAsShared(messageId: String) {
        mailDao.markMessageAsShared(messageId)
    }

    override suspend fun downloadAttachment(attachment: EmailAttachment): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val attachmentEntity = mailDao.getAttachmentById(attachment.id) ?: return@withContext null
        if (attachmentEntity.localUri != null) return@withContext attachmentEntity.localUri

        val message = mailDao.getMessage(attachmentEntity.messageId).first() ?: return@withContext null
        val syncer = syncManager.getSyncer(mailDao.getAccountSync(message.accountId)?.provider ?: return@withContext null)
        
        val localPath = syncer.downloadAttachment(message.accountId, attachment)
        if (localPath != null) {
            mailDao.updateAttachment(attachmentEntity.copy(localUri = localPath))
        }
        localPath
    }

    override fun shutdown() {
        android.util.Log.i("EmailRepository", "Shutting down EmailRepository")
        syncManager.stopSync()
    }

    // --- Mappers ---
    private fun MailAccountEntity.toDomain() = EmailAccount(id, emailAddress, provider, displayName, unreadCount, storageUsed, storageTotal, isEnabled, accountColor, profilePicUrl, lastSyncTimestamp)
    private fun EmailAccount.toEntity(refreshToken: String, accessToken: String, expiresAt: Long) = MailAccountEntity(id, emailAddress, provider, displayName, unreadCount, storageUsed, storageTotal, refreshToken, accessToken, expiresAt, lastSyncTimestamp, isEnabled, accountColor, profilePicUrl)
    private fun MailFolderEntity.toDomain() = EmailFolder(id, accountId, name, type, unreadCount, totalCount, parentId)
    private fun MailMessageEntity.toDomain(attachments: List<EmailAttachment> = emptyList()) = EmailMessage(id, accountId, folderId, threadId, senderName, senderEmail, senderProfilePicUrl, subject, snippet, content, htmlContent, timestamp, isImportant, isStarred, hasAttachments, isUnread, attachments)
    private fun MailAttachmentEntity.toDomain() = EmailAttachment(id, fileName, mimeType, size, localUri, remoteUrl, contentId, isInline)
}
