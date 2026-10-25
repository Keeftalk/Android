package tn.keeftalk.chat.data.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import tn.keeftalk.chat.data.local.dao.SmsDao
import tn.keeftalk.chat.data.local.entities.SmsMessageEntity
import tn.keeftalk.chat.data.local.entities.SmsThreadEntity
import tn.keeftalk.chat.domain.repository.ChatRepository

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
        try {
            context.contentResolver.registerContentObserver(
                Telephony.MmsSms.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error registering content observer", e)
        }
        syncThreads()
    }

    fun stopSync() {
        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error unregistering content observer", e)
        }
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
                
                // Sync messages for each active thread
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
        val processedThreadIds = mutableSetOf<Long>()

        // 1. Query unified MMS/SMS conversations
        try {
            val uri = Uri.parse("content://mms-sms/conversations?simple=true")
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
                val recipIndex = it.getColumnIndex("recipient_ids")

                while (it.moveToNext()) {
                    val threadId = if (idIndex != -1) it.getLong(idIndex) else continue
                    val date = if (dateIndex != -1) it.getLong(dateIndex) else System.currentTimeMillis()
                    val snippet = if (snippetIndex != -1) it.getString(snippetIndex) ?: "" else ""
                    val read = if (readIndex != -1) it.getInt(readIndex) == 1 else true
                    val recipientIds = if (recipIndex != -1) it.getString(recipIndex) else null

                    val address = getAddressForThread(threadId, recipientIds) ?: continue

                    threads.add(
                        ProviderThread(
                            threadId = threadId,
                            address = address,
                            snippet = snippet,
                            timestamp = date,
                            unreadCount = if (read) 0 else 1
                        )
                    )
                    processedThreadIds.add(threadId)
                }
            }
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error querying mms-sms conversations", e)
        }

        // 2. Scan Telephony.Sms.CONTENT_URI to discover any missing threads
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ),
                null, null, "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                val threadIdIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)

                val extraThreads = mutableMapOf<Long, ProviderThread>()

                while (it.moveToNext()) {
                    val threadId = if (threadIdIdx != -1) it.getLong(threadIdIdx) else 0L
                    if (threadId <= 0L || processedThreadIds.contains(threadId) || extraThreads.containsKey(threadId)) continue

                    val address = if (addressIdx != -1) it.getString(addressIdx) else null
                    if (address.isNullOrBlank()) continue

                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                    val read = if (readIdx != -1) it.getInt(readIdx) == 1 else true

                    extraThreads[threadId] = ProviderThread(
                        threadId = threadId,
                        address = address,
                        snippet = body,
                        timestamp = date,
                        unreadCount = if (read) 0 else 1
                    )
                }
                threads.addAll(extraThreads.values)
            }
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error querying extra threads from SMS provider", e)
        }

        return threads
    }

    private fun getAddressForThread(threadId: Long, recipientIds: String?): String? {
        if (!recipientIds.isNullOrBlank()) {
            val addr = getAddressFromRecipientIds(recipientIds)
            if (addr != "Unknown" && addr.isNotBlank()) return addr
        }

        // Query direct SMS provider for thread's address
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.ADDRESS} IS NOT NULL AND ${Telephony.Sms.ADDRESS} != ''",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val addr = it.getString(0)
                    if (!addr.isNullOrBlank()) return addr
                }
            }
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error getting address from SMS for thread $threadId", e)
        }

        // Fallback: check MMS address table
        try {
            val uri = Uri.parse("content://mms/$threadId/addr")
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("address"),
                null,
                null,
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val addr = it.getString(0)
                    if (!addr.isNullOrBlank() && addr != "insert-address-token") return addr
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun getAddressFromRecipientIds(ids: String): String {
        val id = ids.split(" ").firstOrNull { it.isNotBlank() } ?: return "Unknown"
        try {
            val uri = Uri.parse("content://mms-sms/canonical-addresses/$id")
            val cursor = context.contentResolver.query(uri, arrayOf("address"), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0) ?: "Unknown"
                }
            }
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error getting canonical address for id $id", e)
        }
        return "Unknown"
    }

    private fun fetchMessagesFromProvider(threadId: Long): List<ProviderMessage> {
        val messages = mutableListOf<ProviderMessage>()
        
        // Fetch SMS
        try {
            val smsCursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC"
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
                            id = if (idIndex != -1) it.getLong(idIndex) else System.currentTimeMillis(),
                            address = if (addressIndex != -1) it.getString(addressIndex) ?: "" else "",
                            body = if (bodyIndex != -1) it.getString(bodyIndex) ?: "" else "",
                            timestamp = if (dateIndex != -1) it.getLong(dateIndex) else System.currentTimeMillis(),
                            read = if (readIndex != -1) it.getInt(readIndex) else 1,
                            type = if (typeIndex != -1) it.getInt(typeIndex) else 1,
                            status = if (statusIndex != -1) it.getInt(statusIndex) else -1,
                            isMms = false,
                            attachmentsJson = null,
                            deliveryStatus = -1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error querying SMS messages for thread $threadId", e)
        }

        // Fetch MMS
        try {
            val mmsCursor = context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                null,
                "${Telephony.Mms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Mms.DATE} ASC"
            )
            mmsCursor?.use {
                val idIndex = it.getColumnIndex(Telephony.Mms._ID)
                val dateIndex = it.getColumnIndex(Telephony.Mms.DATE)
                val readIndex = it.getColumnIndex(Telephony.Mms.READ)

                while (it.moveToNext()) {
                    val mmsId = if (idIndex != -1) it.getLong(idIndex) else continue
                    val (body, attachments) = parseMmsParts(mmsId)
                    val rawDate = if (dateIndex != -1) it.getLong(dateIndex) else 0L
                    val timestamp = if (rawDate > 0) rawDate * 1000 else System.currentTimeMillis()

                    messages.add(
                        ProviderMessage(
                            id = mmsId,
                            address = "",
                            body = body,
                            timestamp = timestamp,
                            read = if (readIndex != -1) it.getInt(readIndex) else 1,
                            type = 1,
                            status = 0,
                            isMms = true,
                            attachmentsJson = attachments,
                            deliveryStatus = -1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("TelephonySyncManager", "Error querying MMS messages for thread $threadId", e)
        }

        return messages.sortedBy { it.timestamp }
    }

    private fun parseMmsParts(mmsId: Long): Pair<String, String?> {
        val uri = Uri.parse("content://mms/part")
        val cursor = context.contentResolver.query(uri, null, "mid = ?", arrayOf(mmsId.toString()), null)
        var body = ""
        val attachments = mutableListOf<tn.keeftalk.chat.domain.repository.SmsAttachment>()
        
        cursor?.use {
            val typeIndex = it.getColumnIndex("ct")
            val textIndex = it.getColumnIndex("text")
            val idIndex = it.getColumnIndex(Telephony.Mms.Part._ID)

            while (it.moveToNext()) {
                val contentType = if (typeIndex != -1) it.getString(typeIndex) ?: "" else ""
                if (contentType == "text/plain") {
                    body += if (textIndex != -1) it.getString(textIndex) ?: "" else ""
                } else if (contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.startsWith("audio/")) {
                    val partId = if (idIndex != -1) it.getLong(idIndex) else continue
                    attachments.add(
                        tn.keeftalk.chat.domain.repository.SmsAttachment(
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
