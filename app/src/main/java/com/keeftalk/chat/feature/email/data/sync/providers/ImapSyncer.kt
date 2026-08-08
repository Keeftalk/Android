package com.keeftalk.chat.feature.email.data.sync.providers

import com.keeftalk.chat.data.local.dao.MailDao
import com.keeftalk.chat.data.local.entities.MailFolderEntity
import com.keeftalk.chat.data.local.entities.MailMessageEntity
import com.keeftalk.chat.data.local.entities.MailSyncStateEntity
import com.keeftalk.chat.feature.email.model.*
import com.sun.mail.imap.IMAPFolder
import javax.mail.*
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class ImapSyncer(
    private val mailDao: MailDao,
    private val secureStore: com.keeftalk.chat.feature.email.data.local.EmailSecureStore
) : EmailSyncer {

    private suspend fun getImapStore(accountId: String): Store = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val account = mailDao.getAccountSync(accountId) ?: throw Exception("Account not found")
        val password = secureStore.getPassword(accountId) ?: throw Exception("Password not found")
        
        val props = java.util.Properties()
        props["mail.store.protocol"] = "imap"
        props["mail.imap.host"] = account.imapHost
        props["mail.imap.port"] = account.imapPort.toString()
        
        if (account.securityType == "SSL_TLS") {
            props["mail.imap.ssl.enable"] = "true"
            props["mail.imap.socketFactory.port"] = account.imapPort.toString()
            props["mail.imap.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        }

        val session = Session.getInstance(props)
        val store = session.getStore("imap")
        store.connect(account.emailAddress, password)
        store
    }

    override suspend fun syncFolders(accountId: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val store = getImapStore(accountId)
        val defaultFolder = store.defaultFolder
        val list = defaultFolder.list("*")
        
        val folderEntities = list.map { folder ->
            MailFolderEntity(
                id = "${accountId}_${folder.fullName}",
                accountId = accountId,
                name = folder.name,
                type = mapImapToFolderType(folder.name),
                remoteId = folder.fullName
            )
        }
        mailDao.insertFolders(folderEntities)
        store.close()
    }

    private fun mapImapToFolderType(name: String): FolderType = when (name.uppercase()) {
        "INBOX" -> FolderType.INBOX
        "SENT", "SENT MESSAGES" -> FolderType.SENT
        "DRAFTS" -> FolderType.DRAFTS
        "SPAM", "JUNK" -> FolderType.SPAM
        "TRASH", "DELETED" -> FolderType.TRASH
        "ARCHIVE" -> FolderType.ARCHIVE
        else -> FolderType.CUSTOM
    }

    override suspend fun syncMessages(accountId: String, folderId: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        android.util.Log.d("ImapSyncer", "syncMessages for account $accountId folder $folderId")
        val account = mailDao.getAccountSync(accountId) ?: return@withContext
        val folderEntity = mailDao.getFoldersSync(accountId).find { it.id == folderId } ?: return@withContext
        val syncState = mailDao.getSyncState(accountId, folderId)
        
        try {
            val store = getImapStore(accountId)
            val folder = store.getFolder(folderEntity.remoteId) as IMAPFolder
            folder.open(Folder.READ_ONLY)
            
            val currentUidValidity = folder.uidValidity
            val lastUid = if (syncState?.uidValidity == currentUidValidity) syncState.lastUid ?: 0L else 0L
            
            if (syncState != null && syncState.uidValidity != currentUidValidity) {
                android.util.Log.w("ImapSyncer", "UIDValidity changed. Clearing folder $folderId to avoid UID mismatch")
                mailDao.deleteMessagesByFolder(folderId)
            }

            android.util.Log.d("ImapSyncer", "Fetching messages with UID > $lastUid for folder ${folder.fullName}")
            
            // Fetch messages with UID > lastUid
            val messages = folder.getMessagesByUID(lastUid + 1, Long.MAX_VALUE)
            android.util.Log.d("ImapSyncer", "[IMAP_SYNC] Found ${messages.size} new messages for ${folder.fullName}")
            
            // Batch processing: 50 at a time
            messages.toList().chunked(50).forEach { chunk ->
                val messageEntities = chunk.map { msg ->
                    val imapMsg = msg as MimeMessage
                    val content = try { extractTextContent(imapMsg) } catch (e: Exception) { "" }
                    val snippet = content.take(200)
                    val uid = folder.getUID(msg)
                    
                    MailMessageEntity(
                        id = "${accountId}_${folder.fullName}_$uid",
                        accountId = accountId,
                        folderId = folderId,
                        threadId = "", 
                        remoteId = uid.toString(),
                        senderName = (msg.from?.firstOrNull() as? javax.mail.internet.InternetAddress)?.personal ?: "Unknown",
                        senderEmail = (msg.from?.firstOrNull() as? javax.mail.internet.InternetAddress)?.address ?: "",
                        recipientsJson = "",
                        subject = msg.subject ?: "(No Subject)",
                        snippet = snippet,
                        content = content,
                        timestamp = msg.sentDate?.time ?: System.currentTimeMillis(),
                        isUnread = !msg.isSet(Flags.Flag.SEEN),
                        isStarred = msg.isSet(Flags.Flag.FLAGGED)
                    )
                }
                
                android.util.Log.d("ImapSyncer", "[IMAP_SYNC] Inserting batch of ${messageEntities.size} messages")
                mailDao.insertMessages(messageEntities)
            }

            if (messages.isNotEmpty()) {
                // Get the highest UID from the fetched messages
                val maxUid = messages.maxOfOrNull { folder.getUID(it) } ?: lastUid
                
                mailDao.updateSyncState(
                    MailSyncStateEntity(
                        accountId = accountId,
                        folderId = folderId,
                        lastUid = maxUid,
                        uidValidity = currentUidValidity
                    )
                )
            } else if (syncState?.uidValidity != currentUidValidity) {
                // Even if no messages, update the uidValidity
                mailDao.updateSyncState(
                    MailSyncStateEntity(
                        accountId = accountId,
                        folderId = folderId,
                        lastUid = 0L,
                        uidValidity = currentUidValidity
                    )
                )
            }

            folder.close(false)
            store.close()
        } catch (e: Exception) {
            android.util.Log.e("ImapSyncer", "Error during IMAP sync", e)
        }
    }

    private fun extractTextContent(p: Part): String {
        if (p.isMimeType("text/plain")) {
            return p.content as String
        }
        if (p.isMimeType("multipart/*")) {
            val mp = p.content as MimeMultipart
            for (i in 0 until mp.count) {
                val s = extractTextContent(mp.getBodyPart(i))
                if (s.isNotEmpty()) return s
            }
        }
        return ""
    }

    override suspend fun sendMessage(accountId: String, subject: String, content: String, to: List<EmailRecipient>, cc: List<EmailRecipient>, bcc: List<EmailRecipient>, attachments: List<EmailAttachment>) {
        // Implement SMTP send
    }

    override suspend fun deleteMessage(accountId: String, messageId: String) {
        // Implement IMAP STORE +DELETED
    }

    override suspend fun markAsRead(accountId: String, messageId: String, isRead: Boolean) {
        // Implement IMAP STORE \Seen
    }

    override suspend fun toggleStar(accountId: String, messageId: String, isStarred: Boolean) {
        // Implement IMAP STORE \Flagged
    }

    override suspend fun downloadAttachment(accountId: String, attachment: EmailAttachment): String? {
        return null
    }
}
