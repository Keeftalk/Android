package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import com.keeftalk.chat.feature.email.model.EmailProvider
import com.keeftalk.chat.feature.email.model.FolderType

@Entity(tableName = "mail_accounts")
data class MailAccountEntity(
    @PrimaryKey val id: String,
    val emailAddress: String,
    val provider: EmailProvider,
    val displayName: String,
    val unreadCount: Int = 0,
    val storageUsed: Long = 0,
    val storageTotal: Long = 0,
    val refreshToken: String? = null,
    val accessToken: String? = null,
    val tokenExpiration: Long = 0,
    val lastSyncTimestamp: Long = 0,
    val isEnabled: Boolean = true,
    val accountColor: Int? = null,
    val profilePicUrl: String? = null,
    val imapHost: String? = null,
    val imapPort: Int? = null,
    val smtpHost: String? = null,
    val smtpPort: Int? = null,
    val securityType: String? = "SSL_TLS", // SSL_TLS, STARTTLS, NONE
    val encryptedPassword: String? = null
)

@Entity(
    tableName = "mail_folders",
    foreignKeys = [
        ForeignKey(
            entity = MailAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class MailFolderEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val type: FolderType,
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
    val parentId: String? = null,
    val remoteId: String? = null
)

@Entity(
    tableName = "mail_messages",
    foreignKeys = [
        ForeignKey(
            entity = MailAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MailFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("folderId"),
        Index("timestamp"),
        Index("threadId"),
        Index("remoteId")
    ]
)
data class MailMessageEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val folderId: String,
    val threadId: String,
    val remoteId: String,
    val senderName: String,
    val senderEmail: String,
    val senderProfilePicUrl: String? = null,
    val recipientsJson: String, // JSON for TO, CC, BCC
    val replyTo: String? = null,
    val subject: String,
    val snippet: String,
    val content: String,
    val htmlContent: String? = null,
    val timestamp: Long,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isImportant: Boolean = false,
    val isStarred: Boolean = false,
    val hasAttachments: Boolean = false,
    val isUnread: Boolean = true,
    val isDraft: Boolean = false,
    val isDeleted: Boolean = false,
    val isShared: Boolean = false,
    val syncState: String = "SYNCED" // SYNCED, PENDING_SEND, DELETING, ERROR
)

@Entity(
    tableName = "mail_threads",
    foreignKeys = [
        ForeignKey(
            entity = MailAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("lastMessageTimestamp")]
)
data class MailThreadEntity(
    @PrimaryKey val id: String, // threadId from provider or local UUID
    val accountId: String,
    val subject: String,
    val snippet: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int,
    val messageCount: Int,
    val isStarred: Boolean = false,
    val isImportant: Boolean = false,
    val participantNames: String // Comma separated list for list view
)

@Entity(
    tableName = "mail_attachments",
    foreignKeys = [
        ForeignKey(
            entity = MailMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class MailAttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val contentId: String? = null,
    val isInline: Boolean = false
)

@Entity(
    tableName = "mail_labels",
    foreignKeys = [
        ForeignKey(
            entity = MailAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class MailLabelEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val color: Int? = null,
    val remoteId: String? = null
)

@Entity(
    tableName = "mail_message_labels",
    primaryKeys = ["messageId", "labelId"],
    foreignKeys = [
        ForeignKey(
            entity = MailMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MailLabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("labelId")]
)
data class MailMessageLabelCrossRef(
    val messageId: String,
    val labelId: String
)

@Entity(
    tableName = "mail_sync_state",
    primaryKeys = ["accountId", "folderId"],
    foreignKeys = [
        ForeignKey(
            entity = MailAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class MailSyncStateEntity(
    val accountId: String,
    val folderId: String, // Use "ACCOUNT" for account-wide sync state (e.g. Gmail)
    val lastSyncHistoryId: String? = null, // For Gmail
    val deltaToken: String? = null,        // For Outlook
    val lastUid: Long? = null,             // For IMAP
    val uidValidity: Long? = null          // For IMAP
)
