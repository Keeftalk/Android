package com.keeftalk.chat.data.sync

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import androidx.core.net.toUri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.keeftalk.chat.data.local.dao.SmsDao
import com.keeftalk.chat.data.local.entities.SmsMessageEntity
import com.keeftalk.chat.data.local.entities.SmsThreadEntity
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class TelephonySyncManager(
    private val context: Context,
    private val smsDao: SmsDao,
    private val chatRepository: ChatRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d("TelephonySyncManager", "Change detected in Telephony provider: $uri")
            syncThreads()
        }
    }

    fun startSync() {
        context.contentResolver.registerContentObserver(
            Telephony.MmsSms.CONTENT_URI,
            true,
            contentObserver
        )
        syncThreads()
    }

    fun stopSync() {
        context.contentResolver.unregisterContentObserver(contentObserver)
        scope.cancel()
    }

    fun syncThreads() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("TelephonySyncManager", "Missing READ_SMS permission, skipping sync")
            return
        }
        scope.launch {
            try {
                val threads = fetchThreadsFromProvider()
                val profiles = chatRepository.getSearchableProfiles().first()
                
                val threadEntities = threads.map { thread ->
                    val profile = profiles.find { it.phone == thread.address }
                    SmsThreadEntity(
                        threadId = thread.threadId,
                        address = thread.address,
                        snippet = thread.snippet,
                        timestamp = thread.timestamp,
                        unreadCount = thread.unreadCount,
                        recipientId = profile?.id
                    )
                }
                smsDao.clearAndInsertThreads(threadEntities)
                
                // Sync messages for each active thread if needed, or just sync on demand
                // For now, let's sync messages for all threads to have local search working
                threadEntities.forEach { syncMessages(it.threadId) }
            } catch (e: Exception) {
                Log.e("TelephonySyncManager", "Error syncing threads", e)
            }
        }
    }

    private suspend fun syncMessages(threadId: Long) {
        try {
            val messages = fetchMessagesFromProvider(threadId)
            val entities = messages.map { msg ->
                SmsMessageEntity(
                    id = msg.id,
                    threadId = threadId,
                    address = msg.address,
                    body = msg.body,
                    timestamp = msg.timestamp,
                    read = msg.read,
                    type = msg.type,
                    status = msg.status,
                    isMms = msg.isMms,
                    attachmentsJson = msg.attachmentsJson,
                    deliveryStatus = msg.deliveryStatus
                )
            }
            smsDao.insertMessages(entities)
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error syncing messages for thread $threadId", e)
        }
    }

    private fun fetchThreadsFromProvider(): List<ProviderThread> {
        val threads = mutableListOf<ProviderThread>()

        // Use the unified SMS/MMS conversations URI
        val uri = Uri.parse("content://mms-sms/conversations")
        val cursor = context.contentResolver.query(
            uri,
            null,
            null, null, "date DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Threads._ID)
            val dateIndex = it.getColumnIndex(Telephony.Threads.DATE)
            val snippetIndex = it.getColumnIndex(Telephony.Threads.SNIPPET)
            val readIndex = it.getColumnIndex(Telephony.Threads.READ)

            while (it.moveToNext()) {
                val threadId = it.getLong(idIndex)
                val date = it.getLong(dateIndex)
                val snippet = if (snippetIndex != -1) it.getString(snippetIndex) ?: "" else ""
                val read = if (readIndex != -1) it.getInt(readIndex) == 1 else true
                
                val address = getAddressForThread(threadId) ?: continue
                
                threads.add(
                    ProviderThread(
                        threadId = threadId,
                        address = address,
                        snippet = snippet,
                        timestamp = date,
                        unreadCount = if (read) 0 else 1
                    )
                )
            }
        }
        return threads
    }

    private fun getAddressForThread(threadId: Long): String? {
        val uri = "content://mms-sms/conversations/$threadId".toUri()
        // Try multiple columns common in various provider versions
        val cursor = context.contentResolver.query(uri, arrayOf("address", "recipient_ids"), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val addrIndex = it.getColumnIndex("address")
                if (addrIndex != -1) {
                    val addr = it.getString(addrIndex)
                    if (!addr.isNullOrBlank()) return addr
                }
                
                // Fallback to recipient IDs if address is null (common in multi-recipient threads)
                val recipIndex = it.getColumnIndex("recipient_ids")
                if (recipIndex != -1) {
                    val ids = it.getString(recipIndex)
                    if (!ids.isNullOrBlank()) return getAddressFromRecipientIds(ids)
                }
            }
        }
        return null
    }

    private fun getAddressFromRecipientIds(ids: String): String {
        // Simple implementation: resolve first ID
        val id = ids.split(" ").firstOrNull() ?: return "Unknown"
        val uri = Uri.parse("content://mms-sms/canonical-addresses/$id")
        val cursor = context.contentResolver.query(uri, arrayOf("address"), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0) ?: "Unknown"
            }
        }
        return "Unknown"
    }

    private fun fetchMessagesFromProvider(threadId: Long): List<ProviderMessage> {
        val messages = mutableListOf<ProviderMessage>()
        
        // Fetch SMS
        val smsCursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            Telephony.Sms.THREAD_ID + " = ?",
            arrayOf(threadId.toString()),
            Telephony.Sms.DATE + " ASC"
        )
        smsCursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
            val readIndex = it.getColumnIndex(Telephony.Sms.READ)
            val statusIndex = it.getColumnIndex(Telephony.Sms.STATUS)

            while (it.moveToNext()) {
                messages.add(
                    ProviderMessage(
                        id = it.getLong(idIndex),
                        address = it.getString(addressIndex) ?: "",
                        body = it.getString(bodyIndex) ?: "",
                        timestamp = it.getLong(dateIndex),
                        read = it.getInt(readIndex),
                        type = it.getInt(typeIndex),
                        status = if (statusIndex != -1) it.getInt(statusIndex) else -1,
                        isMms = false,
                        attachmentsJson = null,
                        deliveryStatus = -1
                    )
                )
            }
        }

        // Fetch MMS
        val mmsCursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            null,
            Telephony.Mms.THREAD_ID + " = ?",
            arrayOf(threadId.toString()),
            Telephony.Mms.DATE + " ASC"
        )
        mmsCursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Mms._ID)
            val dateIndex = it.getColumnIndex(Telephony.Mms.DATE)
            val readIndex = it.getColumnIndex(Telephony.Mms.READ)

            while (it.moveToNext()) {
                val mmsId = it.getLong(idIndex)
                val (body, attachments) = parseMmsParts(mmsId)
                
                messages.add(
                    ProviderMessage(
                        id = mmsId,
                        address = "", // Will be resolved by thread if needed
                        body = body,
                        timestamp = it.getLong(dateIndex) * 1000, // MMS timestamp is in seconds
                        read = it.getInt(readIndex),
                        type = 1, // Defaulting to received for now
                        status = 0,
                        isMms = true,
                        attachmentsJson = attachments,
                        deliveryStatus = -1
                    )
                )
            }
        }

        // Fetch SIM messages
        try {
            val iccUri = Uri.parse("content://sms/icc")
            val iccCursor = context.contentResolver.query(iccUri, null, null, null, null)
            iccCursor?.use {
                val addressIndex = it.getColumnIndex("address")
                val bodyIndex = it.getColumnIndex("body")
                val dateIndex = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: ""
                    val currentThreadId = Telephony.Threads.getOrCreateThreadId(context, address)
                    if (currentThreadId == threadId) {
                        messages.add(
                            ProviderMessage(
                                id = System.currentTimeMillis() + it.position, // SIM messages don't have stable IDs always
                                address = address,
                                body = it.getString(bodyIndex) ?: "",
                                timestamp = it.getLong(dateIndex),
                                read = 1,
                                type = 1,
                                status = 0,
                                isMms = false,
                                attachmentsJson = null,
                                deliveryStatus = -1
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        
        return messages.sortedBy { it.timestamp }
    }

    private fun parseMmsParts(mmsId: Long): Pair<String, String?> {
        val uri = Uri.parse("content://mms/part")
        val cursor = context.contentResolver.query(uri, null, "mid = ?", arrayOf(mmsId.toString()), null)
        var body = ""
        val attachments = mutableListOf<com.keeftalk.chat.domain.repository.SmsAttachment>()
        
        cursor?.use {
            val typeIndex = it.getColumnIndex("ct")
            val textIndex = it.getColumnIndex("text")
            val idIndex = it.getColumnIndex(Telephony.Mms.Part._ID)

            while (it.moveToNext()) {
                val contentType = it.getString(typeIndex) ?: ""
                if (contentType == "text/plain") {
                    body += it.getString(textIndex) ?: ""
                } else if (contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.startsWith("audio/")) {
                    val partId = it.getLong(idIndex)
                    attachments.add(
                        com.keeftalk.chat.domain.repository.SmsAttachment(
                            uri = "content://mms/part/$partId",
                            mimeType = contentType
                        )
                    )
                }
            }
        }
        
        val json = if (attachments.isNotEmpty()) {
            try {
                kotlinx.serialization.json.Json.encodeToString(attachments)
            } catch (e: Exception) {
                null
            }
        } else null
        
        return body to json
    }

    private data class ProviderThread(
        val threadId: Long,
        val address: String,
        val snippet: String,
        val timestamp: Long,
        val unreadCount: Int
    )

    private data class ProviderMessage(
        val id: Long,
        val address: String,
        val body: String,
        val timestamp: Long,
        val read: Int,
        val type: Int,
        val status: Int,
        val isMms: Boolean,
        val attachmentsJson: String?,
        val deliveryStatus: Int
    )
}
