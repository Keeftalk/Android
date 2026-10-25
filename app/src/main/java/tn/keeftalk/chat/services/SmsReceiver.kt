package tn.keeftalk.chat.services

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.SmsMessageEntity
import tn.keeftalk.chat.data.local.entities.SmsThreadEntity
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.SoundEffectManager
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload

private const val TAG = "KeeftalkSmsReceiver"

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val originatingAddress = messages[0].originatingAddress ?: "Unknown"
            val fullBody = messages.joinToString("") { it.messageBody ?: "" }
            val timestamp = messages[0].timestampMillis
            Log.d(TAG, "SMS received from $originatingAddress: $fullBody")

            SoundEffectManager.playMediaSound(context, SoundEffectManager.RES_MSG_RECEIVING_CHAT)

            scope.launch {
                val threadId = try {
                    Telephony.Threads.getOrCreateThreadId(context, originatingAddress)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating thread for address $originatingAddress", e)
                    timestamp
                }

                // 1. Save to system Telephony inbox
                var messageId = timestamp
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, originatingAddress)
                    put(Telephony.Sms.BODY, fullBody)
                    put(Telephony.Sms.DATE, timestamp)
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX) // 1
                    put(Telephony.Sms.THREAD_ID, threadId)
                }
                try {
                    val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    if (uri != null) {
                        messageId = ContentUris.parseId(uri)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting received SMS into system provider", e)
                }

                // 2. Insert into Room DB so UI updates instantly
                val db = AppModule.provideDatabase(context)
                val smsEntity = SmsMessageEntity(
                    id = messageId,
                    threadId = threadId,
                    address = originatingAddress,
                    body = fullBody,
                    timestamp = timestamp,
                    read = 0,
                    type = Telephony.Sms.MESSAGE_TYPE_INBOX, // 1
                    status = 0,
                    isMms = false,
                    attachmentsJson = null,
                    deliveryStatus = -1
                )
                db.smsDao().insertMessages(listOf(smsEntity))

                // 3. Update thread in Room DB
                val threadEntity = SmsThreadEntity(
                    threadId = threadId,
                    address = originatingAddress,
                    snippet = fullBody,
                    timestamp = timestamp,
                    unreadCount = 1
                )
                db.smsDao().insertThreads(listOf(threadEntity))

                // 4. Trigger sync manager refresh
                AppModule.provideTelephonySyncManager(context).syncThreads()

                // 5. Dispatch SMS notification
                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatch(
                    NotificationPayload(
                        feature = NotificationFeature.SMS_MMS,
                        type = NotificationType.MESSAGE,
                        priority = NotificationPriority.HIGH,
                        title = originatingAddress,
                        body = fullBody,
                        sourceId = "SMS_MMS:$threadId"
                    )
                )
            }
        }
    }
}
