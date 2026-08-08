package com.keeftalk.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.*
import com.keeftalk.chat.data.local.entities.*
import kotlinx.coroutines.flow.Flow

data class MessageWithReactions(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId",
    )
    val reactions: List<MessageReactionEntity>,

    @Relation(
        parentColumn = "replyToId",
        entityColumn = "id",
    )
    val replyTo: MessageEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            MessageAttachmentEntity::class,
            parentColumn = "message_id",
            entityColumn = "file_id"
        )
    )
    val attachments: List<FileEntity>
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestMessagesOnce(chatId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE decryptionState IN ('PENDING', 'RETRY_REQUIRED') AND retryCount < :maxRetries LIMIT :limit")
    suspend fun getMessagesForRepair(limit: Int, maxRetries: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE decryptionState IN ('PENDING', 'RETRY_REQUIRED') AND retryCount < :maxRetries")
    suspend fun countMessagesNeedingDecryption(maxRetries: Int): Int

    @Query("UPDATE messages SET decryptionState = :state, retryCount = :retryCount WHERE id = :id")
    suspend fun updateDecryptionState(id: String, state: com.keeftalk.chat.domain.model.DecryptionState, retryCount: Int)

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getMessagesForChatWithReactions(chatId: String, limit: Int, offset: Int): Flow<List<MessageWithReactions>>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForChatWithReactionsOnce(chatId: String, limit: Int, offset: Int): List<MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' ORDER BY timestamp DESC")
    fun getMessagesForChatWithReactionsPaging(chatId: String): androidx.paging.PagingSource<Int, MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' AND type IN ('IMAGE', 'VIDEO', 'VOICE', 'FILE', 'PDF') ORDER BY timestamp DESC")
    fun getMediaMessagesForChatPaging(chatId: String): androidx.paging.PagingSource<Int, MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE decryptionState = 'SUCCESS' AND type IN ('FILE', 'PDF') ORDER BY timestamp DESC")
    fun getAllSharedDocumentsPaging(): androidx.paging.PagingSource<Int, MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE decryptionState = 'SUCCESS' AND type IN ('FILE', 'PDF') ORDER BY timestamp DESC LIMIT 50")
    fun getAllSharedDocumentsOnce(): List<MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' AND (timestamp < :timestamp OR (timestamp = :timestamp AND id < :id)) ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun getMessagesBeforeOnce(chatId: String, timestamp: Long, id: String, limit: Int): List<MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' AND (timestamp > :timestamp OR (timestamp = :timestamp AND id > :id)) ORDER BY timestamp ASC, id ASC LIMIT :limit")
    suspend fun getMessagesAfterOnce(chatId: String, timestamp: Long, id: String, limit: Int): List<MessageWithReactions>

    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun getLatestMessagesWithReactionsOnce(chatId: String, limit: Int): List<MessageWithReactions>

    @Query("SELECT id FROM messages WHERE chatId = :chatId AND decryptionState = 'SUCCESS' AND type IN ('IMAGE', 'VIDEO', 'VOICE', 'FILE', 'PDF') ORDER BY timestamp DESC")
    suspend fun getMediaMessageIds(chatId: String): List<String>

    @Query("SELECT id FROM messages WHERE chatId = :chatId AND senderId = :senderId AND status = 'SEEN' ORDER BY timestamp DESC LIMIT 1")
    fun getLastSeenMessageIdFlow(chatId: String, senderId: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: MessageReactionEntity)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND userId = :userId")
    suspend fun deleteReaction(messageId: String, userId: String)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId")
    suspend fun deleteReactionsForMessage(messageId: String)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE messages SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateMessageTimestamp(id: String, timestamp: Long)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Transaction
    @Query("SELECT * FROM messages WHERE id = :id")
    fun getMessageWithReactionsById(id: String): Flow<MessageWithReactions?>

    @Query("SELECT MAX(timestamp) FROM messages")
    suspend fun getLastMessageTimestamp(): Long?

    @Query("SELECT MAX(timestamp) FROM messages WHERE chatId = :chatId")
    suspend fun getLastMessageTimestampForChat(chatId: String): Long?

    @Query("SELECT MIN(timestamp) FROM messages WHERE chatId = :chatId")
    suspend fun getOldestMessageTimestampForChat(chatId: String): Long?

    @Query("UPDATE messages SET status = 'SEEN' WHERE chatId = :chatId AND senderId != :currentUserId AND status != 'SEEN'")
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String)

    @Query("UPDATE messages SET status = 'SEEN' WHERE chatId = :chatId AND senderId = :senderId AND timestamp <= :timestamp AND status != 'SEEN'")
    suspend fun markEarlierMessagesAsRead(chatId: String, senderId: String, timestamp: Long)

    @Query("UPDATE messages SET ciphertext = :ciphertext, cryptoVersion = :version, envelopeType = :envelopeType, nonce = :nonce, decryptionState = 'SUCCESS' WHERE id = :id")
    suspend fun updateMessageCiphertext(id: String, ciphertext: String, version: Int, envelopeType: Int, nonce: String?)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String)

    @Query("UPDATE messages SET translatedContent = :translated, sourceLanguage = :sourceLang WHERE id = :id")
    suspend fun updateMessageTranslation(id: String, translated: String, sourceLang: String)

    @Query("UPDATE messages SET mediaLocked = :locked, mediaLockUpdatedAt = :updatedAt, mediaLockUpdatedBy = :updatedBy WHERE id = :id")
    suspend fun updateMediaLockStatus(id: String, locked: Boolean, updatedAt: Long, updatedBy: String)

    @Query("SELECT id FROM messages WHERE chatId = :chatId AND senderId != :currentUserId AND status != 'SEEN'")
    suspend fun getUnreadMessageIds(chatId: String, currentUserId: String): List<String>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND senderId != :currentUserId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPeerMessage(chatId: String, currentUserId: String): MessageEntity?

    @Query("UPDATE messages SET status = 'SEEN' WHERE chatId = :chatId AND senderId != :currentUserId AND timestamp <= :timestamp AND status != 'SEEN'")
    suspend fun markReceivedMessagesAsRead(chatId: String, currentUserId: String, timestamp: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
