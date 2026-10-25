package tn.keeftalk.chat.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.data.local.dao.SmsDao
import tn.keeftalk.chat.data.local.entities.SmsMessageEntity
import tn.keeftalk.chat.data.local.entities.SmsThreadEntity
import tn.keeftalk.chat.domain.repository.*

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
            val timestamp = System.currentTimeMillis()
            val threadId = try {
                Telephony.Threads.getOrCreateThreadId(context, address)
            } catch (e: Exception) {
                Log.e("SmsRepositoryImpl", "Error creating threadId for address $address", e)
                timestamp // Fallback timestamp if thread creation fails
            }

            // 1. Save to system Telephony provider if permitted
            var messageId = timestamp
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT) // 2
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_NONE)
            }
            try {
                val uri = context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                if (uri != null) {
                    messageId = ContentUris.parseId(uri)
                }
            } catch (e: Exception) {
                Log.e("SmsRepositoryImpl", "Error saving sent message to system provider", e)
            }

            // 2. Insert message into Room DB so UI updates instantly
            val messageEntity = SmsMessageEntity(
                id = messageId,
                threadId = threadId,
                address = address,
                body = body,
                timestamp = timestamp,
                read = 1,
                type = Telephony.Sms.MESSAGE_TYPE_SENT, // 2
                status = Telephony.Sms.STATUS_NONE,
                isMms = false,
                attachmentsJson = null,
                deliveryStatus = -1
            )
            smsDao.insertMessages(listOf(messageEntity))

            // 3. Update thread in Room DB
            val threadEntity = SmsThreadEntity(
                threadId = threadId,
                address = address,
                snippet = body,
                timestamp = timestamp,
                unreadCount = 0
            )
            smsDao.insertThreads(listOf(threadEntity))

            // 4. Send message via SmsManager
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val reqCode = (messageId % Int.MAX_VALUE).toInt()
            val sentIntent = android.app.PendingIntent.getBroadcast(
                context,
                reqCode,
                android.content.Intent("SMS_SENT").apply {
                    putExtra("message_id", messageId)
                    putExtra("thread_id", threadId)
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val deliveredIntent = android.app.PendingIntent.getBroadcast(
                context,
                reqCode,
                android.content.Intent("SMS_DELIVERED").apply {
                    putExtra("message_id", messageId)
                    putExtra("thread_id", threadId)
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
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
            Log.e("SmsRepositoryImpl", "Failed to send SMS", e)
            Result.failure(e)
        }
    }

    override suspend fun sendMms(address: String, body: String, attachments: List<SmsAttachment>): Result<Unit> = withContext(Dispatchers.IO) {
        Result.failure(Exception("Not implemented yet"))
    }

    override suspend fun markAsRead(threadId: Long) {
        withContext(Dispatchers.IO) {
            smsDao.markAsRead(threadId)
            try {
                val values = ContentValues()
                values.put(Telephony.Sms.READ, 1)
                context.contentResolver.update(
                    Telephony.Sms.CONTENT_URI,
                    values,
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                    arrayOf(threadId.toString())
                )
            } catch (e: Exception) {
                Log.e("SmsRepositoryImpl", "Error marking SMS as read in system provider", e)
            }
        }
    }

    override suspend fun deleteConversation(threadId: Long) {
        withContext(Dispatchers.IO) {
            smsDao.deleteThread(threadId)
            smsDao.deleteMessagesForThread(threadId)
            try {
                context.contentResolver.delete(
                    Uri.parse("content://sms/conversations/$threadId"),
                    null, null
                )
            } catch (e: Exception) {
                Log.e("SmsRepositoryImpl", "Error deleting conversation from system provider", e)
            }
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
        contactName = null,
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
