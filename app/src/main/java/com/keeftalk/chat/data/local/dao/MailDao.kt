package com.keeftalk.chat.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.keeftalk.chat.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MailDao {

    // --- Accounts ---
    @Query("SELECT * FROM mail_accounts")
    fun getAccounts(): Flow<List<MailAccountEntity>>

    @Query("SELECT * FROM mail_accounts WHERE id = :accountId")
    suspend fun getAccountSync(accountId: String): MailAccountEntity?

    @Query("SELECT * FROM mail_folders WHERE accountId = :accountId")
    suspend fun getFoldersSync(accountId: String): List<MailFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: MailAccountEntity)

    @Update
    suspend fun updateAccount(account: MailAccountEntity)

    @Query("UPDATE mail_accounts SET lastSyncTimestamp = :timestamp WHERE id = :accountId")
    suspend fun updateAccountSyncTimestamp(accountId: String, timestamp: Long)

    @Delete
    suspend fun deleteAccount(account: MailAccountEntity)

    // --- Folders ---
    @Query("SELECT * FROM mail_folders WHERE accountId = :accountId")
    fun getFolders(accountId: String): Flow<List<MailFolderEntity>>

    @Query("SELECT * FROM mail_folders WHERE accountId = :accountId AND type = :type LIMIT 1")
    suspend fun getFolderByType(accountId: String, type: com.keeftalk.chat.feature.email.model.FolderType): MailFolderEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolders(folders: List<MailFolderEntity>)

    @Update
    suspend fun updateFolders(folders: List<MailFolderEntity>)

    // --- Messages ---
    @Query("SELECT * FROM mail_messages WHERE accountId = :accountId AND folderId = :folderId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getMessagesPagingSource(accountId: String, folderId: String): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE isStarred = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getStarredMessagesPagingSource(): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE accountId = :accountId AND isStarred = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getStarredMessagesPagingSource(accountId: String): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE isImportant = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getImportantMessagesPagingSource(): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE accountId = :accountId AND isImportant = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getImportantMessagesPagingSource(accountId: String): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getMergedInboxPagingSource(): PagingSource<Int, MailMessageEntity>

    @Query("SELECT m.* FROM mail_messages m JOIN mail_folders f ON m.folderId = f.id WHERE m.accountId = :accountId AND f.type = :folderType AND m.isDeleted = 0 ORDER BY m.timestamp DESC")
    fun getMessagesByFolderTypePagingSource(accountId: String, folderType: com.keeftalk.chat.feature.email.model.FolderType): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE id = :messageId")
    fun getMessage(messageId: String): Flow<MailMessageEntity?>

    @Query("SELECT * FROM mail_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: String): Flow<List<MailMessageEntity>>

    @Query("SELECT * FROM mail_threads WHERE accountId = :accountId ORDER BY lastMessageTimestamp DESC")
    fun getThreadsPagingSource(accountId: String): PagingSource<Int, MailThreadEntity>

    @Query("SELECT * FROM mail_threads ORDER BY lastMessageTimestamp DESC")
    fun getAllThreadsPagingSource(): PagingSource<Int, MailThreadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MailMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<MailThreadEntity>)

    @Update
    suspend fun updateThread(thread: MailThreadEntity)

    @Update
    suspend fun updateMessage(message: MailMessageEntity)

    @Query("UPDATE mail_messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("UPDATE mail_messages SET isDeleted = 1 WHERE id IN (:messageIds)")
    suspend fun deleteMessages(messageIds: List<String>)

    @Query("UPDATE mail_messages SET isUnread = :isUnread WHERE id IN (:messageIds)")
    suspend fun markMessagesRead(messageIds: List<String>, isUnread: Boolean)

    @Query("UPDATE mail_messages SET isStarred = :isStarred WHERE id IN (:messageIds)")
    suspend fun starMessages(messageIds: List<String>, isStarred: Boolean)

    @Query("DELETE FROM mail_messages WHERE folderId = :folderId")
    suspend fun deleteMessagesByFolder(folderId: String)

    @Query("DELETE FROM mail_messages WHERE accountId = :accountId")
    suspend fun deleteMessagesByAccount(accountId: String)

    // --- Attachments ---
    @Query("SELECT * FROM mail_attachments WHERE messageId = :messageId")
    fun getAttachments(messageId: String): Flow<List<MailAttachmentEntity>>

    @Query("SELECT * FROM mail_attachments WHERE id = :id")
    suspend fun getAttachmentById(id: String): MailAttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<MailAttachmentEntity>)

    @Update
    suspend fun updateAttachment(attachment: MailAttachmentEntity)

    // --- Labels ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<MailLabelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLabelToMessage(crossRef: MailMessageLabelCrossRef)

    @Query("DELETE FROM mail_message_labels WHERE messageId = :messageId AND labelId = :labelId")
    suspend fun removeLabelFromMessage(messageId: String, labelId: String)

    // --- Sync State ---
    @Query("SELECT * FROM mail_sync_state WHERE accountId = :accountId AND folderId = :folderId")
    suspend fun getSyncState(accountId: String, folderId: String): MailSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSyncState(state: MailSyncStateEntity)

    // --- Search ---
    @Query("SELECT * FROM mail_messages WHERE (subject LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MailMessageEntity>>

    @Query("SELECT * FROM mail_messages WHERE (subject LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%') AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchMessagesPagingSource(query: String): PagingSource<Int, MailMessageEntity>

    @Query("SELECT * FROM mail_messages WHERE isDeleted = 0 AND isShared = 1 ORDER BY timestamp DESC")
    fun getSharedMessagesPagingSource(): PagingSource<Int, MailMessageEntity>

    @Query("UPDATE mail_messages SET isShared = 1 WHERE id = :messageId")
    suspend fun markMessageAsShared(messageId: String)
}
