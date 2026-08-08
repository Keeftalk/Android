package com.keeftalk.chat.feature.email.data.sync.providers

import com.microsoft.graph.serviceclient.GraphServiceClient
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.MailFolder
import com.keeftalk.chat.data.local.dao.MailDao
import com.keeftalk.chat.data.local.entities.MailFolderEntity
import com.keeftalk.chat.data.local.entities.MailMessageEntity
import com.keeftalk.chat.data.local.entities.MailSyncStateEntity
import com.keeftalk.chat.feature.email.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import com.microsoft.kiota.authentication.AccessTokenProvider
import com.microsoft.kiota.authentication.AllowedHostsValidator
import com.microsoft.kiota.authentication.BaseBearerTokenAuthenticationProvider

class OutlookSyncer(
    private val mailDao: MailDao,
    private val secureStore: com.keeftalk.chat.feature.email.data.local.EmailSecureStore
) : EmailSyncer {

    private fun getGraphClient(accountId: String): GraphServiceClient {
        val accessToken = secureStore.getAccessToken(accountId) ?: throw Exception("No access token")
        val tokenProvider = object : AccessTokenProvider {
            override fun getAuthorizationToken(uri: URI, additionalAuthenticationContext: Map<String, Any>?): String {
                return accessToken
            }
            override fun getAllowedHostsValidator(): AllowedHostsValidator {
                return AllowedHostsValidator("graph.microsoft.com")
            }
        }
        val authProvider = BaseBearerTokenAuthenticationProvider(tokenProvider)
        return GraphServiceClient(authProvider)
    }

    override suspend fun syncFolders(accountId: String): Unit = withContext(Dispatchers.IO) {
        try {
            val client = getGraphClient(accountId)
            val foldersResponse = client.me().mailFolders().get()
            
            val folderEntities = foldersResponse?.value?.map { folder ->
                MailFolderEntity(
                    id = "${accountId}_${folder.id}",
                    accountId = accountId,
                    name = folder.displayName ?: "",
                    type = mapOutlookFolderToType(folder.displayName),
                    remoteId = folder.id
                )
            } ?: emptyList()
            mailDao.insertFolders(folderEntities)
        } catch (e: Exception) {
            android.util.Log.e("OutlookSyncer", "Error syncing folders", e)
        }
    }

    private fun mapOutlookFolderToType(name: String?): FolderType = when (name?.uppercase()) {
        "INBOX" -> FolderType.INBOX
        "SENT ITEMS" -> FolderType.SENT
        "DRAFTS" -> FolderType.DRAFTS
        "JUNK EMAIL" -> FolderType.SPAM
        "DELETED ITEMS" -> FolderType.TRASH
        else -> FolderType.CUSTOM
    }

    override suspend fun syncMessages(accountId: String, folderId: String): Unit = withContext(Dispatchers.IO) {
        try {
            val client = getGraphClient(accountId)
            val syncState = mailDao.getSyncState(accountId, folderId)
            val folder = mailDao.getFoldersSync(accountId).find { it.id == folderId } ?: return@withContext
            val remoteFolderId = folder.remoteId ?: return@withContext

            var deltaResponse = if (syncState?.deltaToken != null) {
                // Using deltatoken query parameter for incremental sync
                val deltaUrl = "https://graph.microsoft.com/v1.0/me/mailFolders/$remoteFolderId/messages/delta?deltatoken=${syncState.deltaToken}"
                client.me().mailFolders().byMailFolderId(remoteFolderId).messages().delta()
                    .withUrl(deltaUrl)
                    .get()
            } else {
                client.me().mailFolders().byMailFolderId(remoteFolderId).messages().delta().get()
            }

            while (deltaResponse != null) {
                val messages = deltaResponse.value ?: emptyList()
                android.util.Log.d("OutlookSyncer", "[OUTLOOK_SYNC] Processing batch of ${messages.size} messages")
                
                messages.chunked(50).forEach { chunk ->
                    val messageEntities = chunk.mapNotNull { msg ->
                        if (msg.additionalData?.containsKey("@removed") == true) {
                            android.util.Log.d("OutlookSyncer", "[OUTLOOK_SYNC] Message deleted on server: ${msg.id}")
                            mailDao.deleteMessage("${accountId}_${msg.id}")
                            null
                        } else {
                            MailMessageEntity(
                                id = "${accountId}_${msg.id}",
                                accountId = accountId,
                                folderId = folderId,
                                threadId = msg.conversationId ?: "",
                                remoteId = msg.id ?: "",
                                senderName = msg.from?.emailAddress?.name ?: "Unknown",
                                senderEmail = msg.from?.emailAddress?.address ?: "",
                                recipientsJson = "", 
                                subject = msg.subject ?: "(No Subject)",
                                snippet = msg.bodyPreview ?: "",
                                content = msg.body?.content ?: "",
                                htmlContent = if (msg.body?.contentType?.name == "html") msg.body?.content else null,
                                timestamp = msg.receivedDateTime?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis(),
                                isUnread = msg.isRead == false,
                                isStarred = msg.importance?.name == "high",
                                hasAttachments = msg.hasAttachments ?: false
                            )
                        }
                    }
                    mailDao.insertMessages(messageEntities)
                }

                val odataNextLink = deltaResponse.odataNextLink
                val odataDeltaLink = deltaResponse.odataDeltaLink

                if (odataNextLink != null) {
                    deltaResponse = client.me().mailFolders().byMailFolderId(remoteFolderId).messages().delta()
                        .withUrl(odataNextLink)
                        .get()
                } else if (odataDeltaLink != null) {
                    val deltaToken = odataDeltaLink.substringAfter("deltatoken=").substringBefore("&")
                    mailDao.updateSyncState(
                        MailSyncStateEntity(
                            accountId = accountId,
                            folderId = folderId,
                            deltaToken = deltaToken
                        )
                    )
                    deltaResponse = null
                } else {
                    deltaResponse = null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OutlookSyncer", "Error during Outlook syncMessages", e)
        }
    }

    override suspend fun sendMessage(accountId: String, subject: String, content: String, to: List<EmailRecipient>, cc: List<EmailRecipient>, bcc: List<EmailRecipient>, attachments: List<EmailAttachment>) {
    }

    override suspend fun deleteMessage(accountId: String, messageId: String) {
    }

    override suspend fun markAsRead(accountId: String, messageId: String, isRead: Boolean) {
    }

    override suspend fun toggleStar(accountId: String, messageId: String, isStarred: Boolean) {
    }

    override suspend fun downloadAttachment(accountId: String, attachment: EmailAttachment): String? {
        return null
    }
}
