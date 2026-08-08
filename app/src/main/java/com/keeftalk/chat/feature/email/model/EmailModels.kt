package com.keeftalk.chat.feature.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmailAccount(
    val id: String,
    val emailAddress: String,
    val provider: EmailProvider,
    val displayName: String,
    val unreadCount: Int = 0,
    val storageUsed: Long = 0,
    val storageTotal: Long = 0,
    val isEnabled: Boolean = true,
    val accountColor: Int? = null,
    val profilePicUrl: String? = null,
    val lastSyncTimestamp: Long = 0
) : Parcelable

enum class EmailProvider {
    GMAIL, OUTLOOK, YAHOO, CUSTOM_IMAP
}

@Parcelize
data class EmailFolder(
    val id: String,
    val accountId: String,
    val name: String,
    val type: FolderType,
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val parentId: String? = null
) : Parcelable

enum class FolderType {
    INBOX, STARRED, SENT, DRAFTS, SPAM, TRASH, ARCHIVE, IMPORTANT, ALL_MAIL, CUSTOM, SHARED
}

@Parcelize
data class EmailMessage(
    val id: String,
    val accountId: String,
    val folderId: String,
    val threadId: String,
    val senderName: String,
    val senderEmail: String,
    val senderProfilePicUrl: String? = null,
    val subject: String,
    val snippet: String,
    val content: String,
    val htmlContent: String? = null,
    val timestamp: Long,
    val isImportant: Boolean = false,
    val isStarred: Boolean = false,
    val hasAttachments: Boolean = false,
    val isUnread: Boolean = true,
    val attachments: List<EmailAttachment> = emptyList(),
    val labels: List<EmailLabel> = emptyList()
) : Parcelable

@Parcelize
data class EmailAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val contentId: String? = null,
    val isInline: Boolean = false
) : Parcelable

@Parcelize
data class EmailLabel(
    val id: String,
    val accountId: String,
    val name: String,
    val color: Int? = null
) : Parcelable

data class EmailRecipient(
    val name: String?,
    val email: String
)
