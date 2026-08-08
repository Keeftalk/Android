package com.keeftalk.chat.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.keeftalk.chat.data.local.entities.SmsMessageEntity
import com.keeftalk.chat.data.local.entities.SmsThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_threads ORDER BY isPinned DESC, timestamp DESC")
    fun getThreads(): Flow<List<SmsThreadEntity>>

    @Query("SELECT * FROM sms_threads WHERE isArchived = :archived ORDER BY isPinned DESC, timestamp DESC")
    fun getThreads(archived: Boolean): Flow<List<SmsThreadEntity>>

    @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY timestamp DESC")
    fun getMessagesPaging(threadId: Long): PagingSource<Int, SmsMessageEntity>

    @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessages(threadId: Long): Flow<List<SmsMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SmsMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<SmsThreadEntity>)

    @Query("UPDATE sms_threads SET unreadCount = 0 WHERE threadId = :threadId")
    suspend fun markAsRead(threadId: Long)

    @Query("DELETE FROM sms_threads WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: Long)

    @Query("DELETE FROM sms_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: Long)

    @Query("SELECT MAX(timestamp) FROM sms_messages")
    suspend fun getLastMessageTimestamp(): Long?

    @Transaction
    suspend fun clearAndInsertThreads(threads: List<SmsThreadEntity>) {
        // We might want to keep some local-only flags like isArchived or isPinned
        // For now just simple replace
        insertThreads(threads)
    }

    @Query("SELECT * FROM sms_threads WHERE address LIKE '%' || :query || '%' OR snippet LIKE '%' || :query || '%'")
    fun searchThreads(query: String): Flow<List<SmsThreadEntity>>
}
