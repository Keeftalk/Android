package com.keeftalk.chat.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.data.local.entities.SmsMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "KeeftalkSmsReceiver"

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody
                val address = sms.originatingAddress ?: "Unknown"
                val timestamp = sms.timestampMillis
                Log.d(TAG, "SMS received from $address: $body")
                
                scope.launch {
                    val db = AppModule.provideDatabase(context)
                    val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
                    
                    val smsEntity = SmsMessageEntity(
                        id = System.currentTimeMillis(), // We should ideally use system ID if possible, but for incoming it's tricky without a query
                        threadId = threadId,
                        address = address,
                        body = body,
                        timestamp = timestamp,
                        read = 0,
                        type = 1, // Inbox
                        status = 0,
                        isMms = false,
                        attachmentsJson = null,
                        deliveryStatus = -1
                    )
                    db.smsDao().insertMessages(listOf(smsEntity))
                    
                    // Trigger sync manager to refresh threads
                    AppModule.provideTelephonySyncManager(context).syncThreads()
                    
                    // TODO: Show notification using matched profile if available
                }
            }
        }
    }
}
