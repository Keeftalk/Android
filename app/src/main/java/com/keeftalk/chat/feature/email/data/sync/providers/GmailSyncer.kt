package com.keeftalk.chat.feature.email.data.sync.providers

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.keeftalk.chat.data.local.dao.MailDao
import com.keeftalk.chat.data.local.entities.MailFolderEntity
import com.keeftalk.chat.data.local.entities.MailMessageEntity
import com.keeftalk.chat.data.local.entities.MailSyncStateEntity
import com.keeftalk.chat.feature.email.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest

class GmailSyncer(
    private val context: Context,
    private val mailDao: MailDao,
    private val secureStore: com.keeftalk.chat.feature.email.data.local.EmailSecureStore
) : EmailSyncer {

    private fun getGmailService(accountId: String, emailAddress: String): Gmail {
        val accessToken = secureStore.getAccessToken(accountId)
        
        val credential = if (accessToken != null) {
            android.util.Log.d("GmailSyncer", "Using stored access token for $emailAddress")
            com.google.api.client.http.HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $accessToken"
            }
        } else {
            android.util.Log.w("GmailSyncer", "No access token found in secure store, falling back to system credential")
            GoogleAccountCredential.usingOAuth2(
                context,
                listOf(GmailScopes.GMAIL_READONLY, GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_MODIFY)
            ).apply {
                selectedAccountName = emailAddress
            }
        }

        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("Keeftalk")
        .build()
    }

    override suspend fun syncFolders(accountId: String) = withContext(Dispatchers.IO) {
        try {
            val account = mailDao.getAccountSync(accountId) ?: return@withContext
            val service = getGmailService(accountId, account.emailAddress)
            
            android.util.Log.d("GmailSyncer", "Syncing folders for ${account.emailAddress}")
            val labelsResponse = service.users().labels().list("me").execute()
            val labels = labelsResponse.labels ?: emptyList()
            
            val folderEntities = labels.map { label ->
                MailFolderEntity(
                    id = "${accountId}_${label.id}",
                    accountId = accountId,
                    name = label.name,
                    type = mapGmailLabelToFolderType(label.id),
                    remoteId = label.id
                )
            }
            // Use IGNORE + explicit UPDATE for folders to prevent CASCADE DELETE of messages
            mailDao.insertFolders(folderEntities)
            mailDao.updateFolders(folderEntities)
            
            android.util.Log.d("GmailSyncer", "[GMAIL_SYNC] Synced ${folderEntities.size} folders")
        } catch (e: Exception) {
            android.util.Log.e("GmailSyncer", "Error syncing folders", e)
        }
    }

    private fun mapGmailLabelToFolderType(labelId: String): FolderType = when (labelId) {
        "INBOX" -> FolderType.INBOX
        "SENT" -> FolderType.SENT
        "DRAFT" -> FolderType.DRAFTS
        "SPAM" -> FolderType.SPAM
        "TRASH" -> FolderType.TRASH
        "STARRED" -> FolderType.STARRED
        "IMPORTANT" -> FolderType.IMPORTANT
        else -> FolderType.CUSTOM
    }

    override suspend fun syncMessages(accountId: String, folderId: String) = withContext(Dispatchers.IO) {
        val account = mailDao.getAccountSync(accountId) ?: return@withContext
        val service = getGmailService(accountId, account.emailAddress)
        val syncState = mailDao.getSyncState(accountId, "ACCOUNT")
        val lastHistoryId = syncState?.lastSyncHistoryId

        try {
            if (lastHistoryId == null) {
                performInitialSync(accountId, service)
            } else {
                android.util.Log.d("GmailSyncer", "Performing incremental sync for Gmail from historyId: $lastHistoryId")
                try {
                    val historyResponse = service.users().history().list("me")
                        .setStartHistoryId(java.math.BigInteger(lastHistoryId))
                        .execute()
                    
                    val histories = historyResponse.history ?: emptyList()
                    android.util.Log.d("GmailSyncer", "Found ${histories.size} history events")

                    histories.forEach { history ->
                        // Handle added messages
                        history.messagesAdded?.chunked(50)?.forEach { chunk ->
                            val entities = chunk.map { added ->
                                async { fetchMessageAsEntity(accountId, service, added.message.id) }
                            }.awaitAll().filterNotNull()
                            android.util.Log.d("GmailSyncer", "[GMAIL_SYNC] Incremental: Inserting batch of ${entities.size} messages")
                            mailDao.insertMessages(entities)
                        }
                        
                        // Handle label changes (read/unread, star, etc.)
                        history.labelsAdded?.forEach { labelChange ->
                            updateMessageLabels(accountId, service, labelChange.message.id)
                        }
                        history.labelsRemoved?.forEach { labelChange ->
                            updateMessageLabels(accountId, service, labelChange.message.id)
                        }

                        // Handle deletions
                        history.messagesDeleted?.forEach { deleted ->
                            mailDao.deleteMessage("${accountId}_${deleted.message.id}")
                        }
                    }

                    // Update historyId to the latest from the profile
                    val profile = service.users().getProfile("me").execute()
                    mailDao.updateSyncState(
                        MailSyncStateEntity(
                            accountId = accountId,
                            folderId = "ACCOUNT",
                            lastSyncHistoryId = profile.historyId.toString()
                        )
                    )
                } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                    if (e.statusCode == 404 || e.statusCode == 410) {
                        android.util.Log.w("GmailSyncer", "History ID expired, performing full sync")
                        performInitialSync(accountId, service)
                    } else {
                        throw e
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GmailSyncer", "Error during Gmail syncMessages", e)
        }
    }

    private suspend fun performInitialSync(accountId: String, service: Gmail) = coroutineScope {
        android.util.Log.d("GmailSyncer", "[GMAIL_SYNC] Performing initial sync for account: $accountId")
        val profile = service.users().getProfile("me").execute()
        val currentHistoryId = profile.historyId.toString()

        val messagesResponse = service.users().messages().list("me")
            .setMaxResults(100L)
            .execute()
        
        val messages = messagesResponse.messages ?: emptyList()
        android.util.Log.d("GmailSyncer", "[GMAIL_SYNC] Found ${messages.size} messages for initial sync")

        // Batch processing: 50 at a time
        messages.chunked(50).forEach { chunk ->
            val entities = chunk.map { msg ->
                async { fetchMessageAsEntity(accountId, service, msg.id) }
            }.awaitAll().filterNotNull()
            
            android.util.Log.d("GmailSyncer", "[GMAIL_SYNC] Initial sync: Mapping and inserting batch of ${entities.size} messages")
            entities.forEach { entity ->
                android.util.Log.v("GmailSyncer", "[GMAIL_SYNC] Message ${entity.remoteId} -> Folder: ${entity.folderId} (Subject: ${entity.subject})")
            }
            mailDao.insertMessages(entities)
        }

        mailDao.updateSyncState(
            MailSyncStateEntity(
                accountId = accountId,
                folderId = "ACCOUNT",
                lastSyncHistoryId = currentHistoryId
            )
        )
    }

    private suspend fun fetchMessageAsEntity(accountId: String, service: Gmail, gmailMsgId: String): MailMessageEntity? {
        return try {
            val fullMsg = service.users().messages().get("me", gmailMsgId).execute()
            val labels = fullMsg.labelIds ?: emptyList()
            val folders = mailDao.getFoldersSync(accountId)
            
            // Robust Mapping: Strictly prioritize INBOX.
            val priorityLabels = listOf("INBOX", "SENT", "DRAFT", "TRASH", "SPAM", "STARRED", "IMPORTANT")
            val primaryLabel = priorityLabels.firstOrNull { it in labels } 
                ?: labels.find { labelId -> folders.any { it.remoteId == labelId } } 
                ?: "INBOX"

            val folder = folders.find { it.remoteId == primaryLabel } ?: folders.find { it.type == FolderType.INBOX } 
            
            if (folder == null) {
                android.util.Log.e("GmailSyncer", "[GMAIL_SYNC] ERROR: No folder found for msg $gmailMsgId with labels $labels. Using default INBOX ID if available.")
                // Attempt to find ANY inbox folder for this account
                val fallbackFolder = folders.find { it.type == FolderType.INBOX } ?: return null
                return createMessageEntity(accountId, fullMsg, fallbackFolder.id, labels)
            }

            android.util.Log.d("GmailSyncer", "[GMAIL_SYNC] Mapped msg $gmailMsgId to folder ${folder.name} (ID: ${folder.id}) | Labels: $labels")
            return createMessageEntity(accountId, fullMsg, folder.id, labels)
        } catch (e: Exception) {
            android.util.Log.e("GmailSyncer", "[GMAIL_SYNC] Error fetching message $gmailMsgId", e)
            null
        }
    }

    private fun createMessageEntity(accountId: String, fullMsg: com.google.api.services.gmail.model.Message, folderId: String, labels: List<String>): MailMessageEntity {
        val senderEmail = extractEmail(getHeader(fullMsg, "From") ?: "")
        // MD5 Gravatar logic for consistent profile pictures
        val gravatarUrl = "https://www.gravatar.com/avatar/${md5(senderEmail.lowercase())}?d=identicon"
        
        return MailMessageEntity(
            id = "${accountId}_${fullMsg.id}",
            accountId = accountId,
            folderId = folderId,
            threadId = fullMsg.threadId,
            remoteId = fullMsg.id,
            senderName = getHeader(fullMsg, "From")?.substringBefore("<")?.trim() ?: "Unknown",
            senderEmail = senderEmail,
            senderProfilePicUrl = gravatarUrl, 
            recipientsJson = "", 
            subject = getHeader(fullMsg, "Subject") ?: "(No Subject)",
            snippet = fullMsg.snippet ?: "",
            content = extractBody(fullMsg),
            htmlContent = extractHtmlBody(fullMsg),
            timestamp = fullMsg.internalDate ?: System.currentTimeMillis(),
            isUnread = labels.contains("UNREAD"),
            isStarred = labels.contains("STARRED"),
            isImportant = labels.contains("IMPORTANT"),
            hasAttachments = hasAttachments(fullMsg)
        )
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }


    private suspend fun updateMessageLabels(accountId: String, service: Gmail, gmailMsgId: String) {
        // Just refetch the message metadata to update flags
        try {
            val fullMsg = service.users().messages().get("me", gmailMsgId).setFormat("minimal").execute()
            val messageId = "${accountId}_${gmailMsgId}"
            
            // Note: In a real app, we'd fetch the existing entity and update it.
            // Or use a targeted update query in DAO.
            // For simplicity here, we can refetch full if needed, but 'minimal' gives labels.
            val labels = fullMsg.labelIds ?: emptyList()
            
            // This is a bit inefficient without a specific 'updateFlags' DAO method
            // but it works for this overhaul.
            val existing = mailDao.getMessage(messageId).firstOrNull()
            if (existing != null) {
                mailDao.updateMessage(existing.copy(
                    isUnread = labels.contains("UNREAD"),
                    isStarred = labels.contains("STARRED"),
                    isImportant = labels.contains("IMPORTANT")
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("GmailSyncer", "Error updating labels for $gmailMsgId", e)
        }
    }

    private fun extractBody(message: com.google.api.services.gmail.model.Message): String {
        return parsePart(message.payload, "text/plain") ?: message.snippet ?: ""
    }

    private fun extractHtmlBody(message: com.google.api.services.gmail.model.Message): String? {
        return parsePart(message.payload, "text/html")
    }

    private fun parsePart(part: com.google.api.services.gmail.model.MessagePart?, mimeType: String): String? {
        if (part == null) return null
        
        if (part.mimeType == mimeType && part.body?.data != null) {
            val data = part.body.data.replace("-", "+").replace("_", "/")
            return try {
                String(android.util.Base64.decode(data, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
        
        part.parts?.forEach { subPart ->
            val content = parsePart(subPart, mimeType)
            if (content != null) return content
        }
        
        return null
    }

    private fun hasAttachments(message: com.google.api.services.gmail.model.Message): Boolean {
        return message.payload?.parts?.any { it.filename != null && it.filename.isNotEmpty() } ?: false
    }

    private fun getHeader(message: com.google.api.services.gmail.model.Message, name: String): String? {
        return message.payload?.headers?.find { it.name.equals(name, ignoreCase = true) }?.value
    }

    private fun extractEmail(from: String): String {
        val match = Regex("<(.*)>").find(from)
        return match?.groupValues?.get(1) ?: from
    }

    override suspend fun sendMessage(accountId: String, subject: String, content: String, to: List<EmailRecipient>, cc: List<EmailRecipient>, bcc: List<EmailRecipient>, attachments: List<EmailAttachment>) {
        // Implement Gmail API send
    }

    override suspend fun deleteMessage(accountId: String, messageId: String) {
        val account = mailDao.getAccountSync(accountId) ?: return
        val service = getGmailService(accountId, account.emailAddress)
        val remoteId = messageId.substringAfter("${accountId}_")
        service.users().messages().trash("me", remoteId).execute()
    }

    override suspend fun markAsRead(accountId: String, messageId: String, isRead: Boolean) {
        val account = mailDao.getAccountSync(accountId) ?: return
        val service = getGmailService(accountId, account.emailAddress)
        val remoteId = messageId.substringAfter("${accountId}_")
        
        val mods = com.google.api.services.gmail.model.ModifyMessageRequest()
        if (isRead) {
            mods.removeLabelIds = listOf("UNREAD")
        } else {
            mods.addLabelIds = listOf("UNREAD")
        }
        service.users().messages().modify("me", remoteId, mods).execute()
    }

    override suspend fun toggleStar(accountId: String, messageId: String, isStarred: Boolean) {
        val account = mailDao.getAccountSync(accountId) ?: return
        val service = getGmailService(accountId, account.emailAddress)
        val remoteId = messageId.substringAfter("${accountId}_")
        
        val mods = com.google.api.services.gmail.model.ModifyMessageRequest()
        if (isStarred) {
            mods.addLabelIds = listOf("STARRED")
        } else {
            mods.removeLabelIds = listOf("STARRED")
        }
        service.users().messages().modify("me", remoteId, mods).execute()
    }

    override suspend fun downloadAttachment(accountId: String, attachment: EmailAttachment): String? {
        return null
    }
}
