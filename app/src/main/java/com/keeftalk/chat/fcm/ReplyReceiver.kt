package com.keeftalk.chat.fcm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.keeftalk.chat.R
import com.keeftalk.chat.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "KEEFTALK_REPLY"

class ReplyReceiver : BroadcastReceiver() {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val chatId = intent.getStringExtra("chatId")
        Log.d(TAG, "onReceive: action=$action, chatId=$chatId")

        if (chatId == null) {
            Log.e(TAG, "No chatId found in intent")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                when (action) {
                    "ACTION_REPLY" -> {
                        val remoteInput = RemoteInput.getResultsFromIntent(intent)
                        val replyText = remoteInput?.getCharSequence("key_text_reply")?.toString()
                        Log.d(TAG, "Reply text received: $replyText")
                        
                        if (!replyText.isNullOrBlank()) {
                            // Update notification to show progress
                            val progressNotification = NotificationCompat.Builder(context, "chat_messages")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentText("Sending...")
                                .setOngoing(true)
                                .build()
                            notificationManager.notify(chatId.hashCode(), progressNotification)

                            try {
                                val repo = AppModule.provideChatRepository(context.applicationContext)
                                // sendMessage is now a true suspend function that waits for Supabase
                                repo.sendMessage(chatId, replyText)
                                Log.d(TAG, "Message sent successfully from receiver")

                                // Clear notification after success
                                notificationManager.cancel(chatId.hashCode())
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to send message from receiver", e)
                                // Update notification to show failure
                                val errorNotification = NotificationCompat.Builder(context, "chat_messages")
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("Failed to send")
                                    .setContentText("Tap to open chat and retry")
                                    .setAutoCancel(true)
                                    .build()
                                notificationManager.notify(chatId.hashCode(), errorNotification)
                            }
                        } else {
                            Log.w(TAG, "Reply text was null or blank")
                        }
                    }
                    "ACTION_MARK_READ" -> {
                        Log.d(TAG, "Marking as read: $chatId")
                        try {
                            AppModule.provideChatRepository(context.applicationContext).markAsRead(chatId)
                            notificationManager.cancel(chatId.hashCode())
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to mark as read", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in receiverScope", e)
            } finally {
                Log.d(TAG, "Finishing pending result")
                pendingResult.finish()
            }
        }
    }
}
