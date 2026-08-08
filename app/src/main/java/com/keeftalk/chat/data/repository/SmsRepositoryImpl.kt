package com.keeftalk.chat.data.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.keeftalk.chat.data.local.dao.SmsDao
import com.keeftalk.chat.data.local.entities.SmsMessageEntity
import com.keeftalk.chat.data.local.entities.SmsThreadEntity
import com.keeftalk.chat.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SmsRepositoryImpl(
    private val context: Context,
    private val smsDao: SmsDao
) : SmsRepository {

    override fun getConversations(showArchived: Boolean): Flow<List<SmsConversation>> {
        return smsDao.getThreads(showArchived).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getMessages(threadId: Long): Flow<PagingData<SmsMessage>> {
        return Pager(
            config = PagingConfig(pageSize = 30, enablePlaceholders = false),
            pagingSourceFactory = { smsDao.getMessagesPaging(threadId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getMessagesList(threadId: Long): Flow<List<SmsMessage>> {
        return smsDao.getMessages(threadId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun sendSms(address: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val sentIntent = android.app.PendingIntent.getBroadcast(
                context, 0, android.content.Intent("SMS_SENT"), android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val deliveredIntent = android.app.PendingIntent.getBroadcast(
                context, 0, android.content.Intent("SMS_DELIVERED"), android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                val sentIntents = java.util.ArrayList<android.app.PendingIntent>().apply { repeat(parts.size) { add(sentIntent) } }
                val deliveredIntents = java.util.ArrayList<android.app.PendingIntent>().apply { repeat(parts.size) { add(deliveredIntent) } }
                smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveredIntents)
            } else {
                smsManager.sendTextMessage(address, null, body, sentIntent, deliveredIntent)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMms(address: String, body: String, attachments: List<SmsAttachment>): Result<Unit> = withContext(Dispatchers.IO) {
        // MMS implementation using MmsManager
        Result.failure(Exception("Not implemented yet"))
    }

    override suspend fun markAsRead(threadId: Long) {
        withContext(Dispatchers.IO) {
            smsDao.markAsRead(threadId)
            // Also update system provider
            val values = android.content.ContentValues()
            values.put(Telephony.Sms.READ, 1)
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )
        }
    }

    override suspend fun deleteConversation(threadId: Long) {
        withContext(Dispatchers.IO) {
            smsDao.deleteThread(threadId)
            smsDao.deleteMessagesForThread(threadId)
            context.contentResolver.delete(
                Uri.parse("content://sms/conversations/$threadId"),
                null, null
            )
        }
    }

    override suspend fun toggleArchive(threadId: Long, archived: Boolean) {
        // Implementation for local archive flag
    }

    override suspend fun togglePin(threadId: Long, pinned: Boolean) {
        // Implementation for local pin flag
    }

    override suspend fun toggleMute(threadId: Long, muted: Boolean) {
        // Implementation for local mute flag
    }

    override fun searchConversations(query: String): Flow<List<SmsConversation>> {
        return smsDao.searchThreads(query).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }
}

private fun SmsMessageEntity.toDomain(): SmsMessage {
    val attachments = try {
        attachmentsJson?.let { Json.decodeFromString<List<SmsAttachment>>(it) } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    return SmsMessage(
        id = id,
        threadId = threadId,
        address = address,
        body = body,
        timestamp = timestamp,
        type = type,
        read = read,
        status = status,
        isMms = isMms,
        attachments = attachments,
        deliveryStatus = deliveryStatus
    )
}

private fun SmsThreadEntity.toDomain(): SmsConversation {
    return SmsConversation(
        threadId = threadId,
        address = address,
        contactName = null, // Enrichment should happen in the ViewModel or via a Flow
        lastMessage = snippet,
        timestamp = timestamp,
        unreadCount = unreadCount,
        isArchived = isArchived,
        isMuted = isMuted,
        isPinned = isPinned,
        draft = draft,
        recipientId = recipientId
    )
}
