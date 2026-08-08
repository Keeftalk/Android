package com.keeftalk.chat.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.keeftalk.chat.MainActivity
import com.keeftalk.chat.R
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.ChatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL

private const val TAG = "KEEFTALK_FCM"
private const val CHANNEL_ID = "chat_messages"
private const val SECURITY_CHANNEL_ID = "security_alerts"

class KeeftalkFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "Message received from: ${message.from}")
        
        val data = message.data
        if (data.isEmpty()) {
            Log.w(TAG, "Received empty data payload")
            message.notification?.let {
                showNotification(it.title ?: "New Message", it.body ?: "", "", null)
            }
            return
        }

        val type = data["type"]
        if (type == "security_alert" || type == "profile_view") {
            handleSpecialNotification(data)
            return
        }

        if (type == "call") {
            handleCallNotification(data)
            return
        }

        val chatId = data["chatId"] ?: data["chat_id"]
        val messageId = data["messageId"] ?: data["message_id"] ?: data["id"] ?: data["msg_id"] ?: data["message_uuid"]
        if (chatId == null) {
            Log.w(TAG, "No chatId found in data payload keys: ${data.keys}")
            val title = data["title"] ?: data["name"] ?: "New Message"
            val body = data["body"] ?: data["content"] ?: ""
            showNotification(title, body, "", data["avatar"] ?: data["avatarUrl"])
            return
        }
        
        val prefs = applicationContext.getSharedPreferences("keeftalk_prefs", Context.MODE_PRIVATE)
        val currentOpenChatId = prefs.getString("current_open_chat_id", null)
        val isAppInForeground = prefs.getBoolean("is_app_in_foreground", false)

        if (isAppInForeground && currentOpenChatId == chatId) {
            Log.d(TAG, "App in foreground and chat $chatId open, skipping notification")
            // Still sync chat to get the new message
            serviceScope.launch {
                try {
                    val repo = AppModule.provideChatRepository(applicationContext)
                    repo.syncChat(chatId)
                    if (messageId != null) {
                        repo.updateMessageStatus(messageId, com.keeftalk.chat.domain.model.MessageStatus.FCM_RECEIVED)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Background sync failed", e)
                }
            }
            return
        }

        val title = data["title"] ?: data["name"] ?: "New Message"
        var body = data["body"] ?: data["content"] ?: ""
        val avatarUrl = data["avatar"] ?: data["avatarUrl"]

        val repo = AppModule.provideChatRepository(applicationContext)
        val userPrefsRepo = UserPreferencesRepository(applicationContext)

        // ALWAYS update status to FCM_RECEIVED if we got the message
        // This is a delivery confirmation. We do this first to be fast.
        serviceScope.launch {
            try {
                if (messageId != null) {
                    Log.d(TAG, "Updating message $messageId status to FCM_RECEIVED")
                    repo.updateMessageStatus(messageId, com.keeftalk.chat.domain.model.MessageStatus.FCM_RECEIVED)
                }
                // Then sync to ensure local DB has the message
                repo.syncChat(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update status or sync chat $chatId on FCM message", e)
            }
        }
        
        serviceScope.launch {
            val userPrefs = userPrefsRepo.userPreferencesFlow.first()
            val globalNotifs = userPrefs.notifications
            
            if (!globalNotifs.allEnabled) {
                Log.d(TAG, "All notifications disabled globally")
                return@launch
            }

            val chat = repo.getChat(chatId).first()
            if (chat?.isMuted == true) {
                val muteUntil = chat.notificationSettings.muteUntil
                if (muteUntil == null || muteUntil > System.currentTimeMillis()) {
                    Log.d(TAG, "Chat $chatId is muted")
                    return@launch
                }
            }

            if (chat?.type == ChatType.ONE_TO_ONE && !globalNotifs.messagesEnabled) return@launch
            if (chat?.type == ChatType.GROUP && !globalNotifs.groupsEnabled) return@launch

            val showPreview = chat?.notificationSettings?.showPreview ?: globalNotifs.showPreviewContent
            var finalBody = if (showPreview) body else "Message from $title"
            
            if (showPreview && (finalBody.isEmpty() || finalBody == "[Encrypted]") && messageId != null) {
                // Wait for sync and decryption
                Log.d(TAG, "Notification body is encrypted, waiting for decryption... messageId=$messageId")
                val decryptedMsg = withTimeoutOrNull(5000) { // Increased timeout
                    repo.getMessage(messageId).filter { 
                        it != null && it.decryptionState == com.keeftalk.chat.domain.model.DecryptionState.SUCCESS
                    }.first()
                }
                
                if (decryptedMsg != null) {
                    finalBody = decryptedMsg.content
                    Log.d(TAG, "Decryption success for notification: $messageId")
                } else {
                    Log.w(TAG, "Decryption timeout or failure for notification: $messageId. Hiding notification.")
                    return@launch // HIDE NOTIFICATION as per "NEVER display placeholder"
                }
            }
            
            // Internal notification system entry (only if notifications are enabled for this chat)
            try {
                repo.insertNotification(com.keeftalk.chat.domain.model.AppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    type = com.keeftalk.chat.domain.model.NotificationType.MESSAGE,
                    title = title,
                    message = finalBody,
                    timestamp = System.currentTimeMillis(),
                    data = com.keeftalk.chat.domain.model.NotificationData(chatId = chatId)
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert internal notification", e)
            }

            showNotification(title, finalBody, chatId, avatarUrl)
        }
    }

    private fun handleCallNotification(data: Map<String, String>) {
        val callId = data["callId"] ?: data["call_id"] ?: return
        val callerName = data["callerName"] ?: data["caller_name"] ?: "Keeftalk Call"
        
        Log.d(TAG, "Handling incoming call FCM: $callId from $callerName")

        // Launch IncomingCallActivity
        com.keeftalk.chat.ui.call.IncomingCallActivity.start(this, callId)
    }

    private fun showNotification(title: String, body: String, chatId: String, avatarUrl: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            if (chatId.isNotEmpty()) {
                putExtra("chatId", chatId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val notificationId = if (chatId.isNotEmpty()) chatId.hashCode() else System.currentTimeMillis().toInt()
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (chatId.isNotEmpty()) {
            builder.setShortcutId(chatId)
            
            // Reply Action
            val remoteInput = RemoteInput.Builder("key_text_reply").setLabel("Reply...").build()
            val replyIntent = Intent(this, ReplyReceiver::class.java).apply {
                action = "ACTION_REPLY"
                putExtra("chatId", chatId)
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                this, 
                chatId.hashCode(), 
                replyIntent, 
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send, 
                "Reply", 
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            // Mark as Read Action
            val readIntent = Intent(this, ReplyReceiver::class.java).apply {
                action = "ACTION_MARK_READ"
                putExtra("chatId", chatId)
            }
            val readPendingIntent = PendingIntent.getBroadcast(
                this, 
                chatId.hashCode() + 1, 
                readIntent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val readAction = NotificationCompat.Action.Builder(
                0, 
                "Mark as Read", 
                readPendingIntent
            ).build()
            
            builder.addAction(replyAction)
            builder.addAction(readAction)
        }

        if (!avatarUrl.isNullOrBlank()) {
            serviceScope.launch {
                try {
                    val bitmap = BitmapFactory.decodeStream(URL(avatarUrl).openConnection().getInputStream())
                    if (bitmap != null) {
                        val icon = IconCompat.createWithBitmap(bitmap)
                        val user = Person.Builder()
                            .setName(title)
                            .setIcon(icon)
                            .build()

                        val style = NotificationCompat.MessagingStyle(user)
                            .addMessage(body, System.currentTimeMillis(), user)
                        
                        builder.setStyle(style)
                        builder.setLargeIcon(bitmap)
                        
                        notificationManager.notify(notificationId, builder.build())
                        Log.d(TAG, "Notification updated with avatar for chat $chatId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load avatar", e)
                }
            }
        }
        
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Notification posted for chat $chatId")
    }

    private fun createChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Chat Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for new messages"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)

            val securityChannel = NotificationChannel(SECURITY_CHANNEL_ID, "Security Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Critical security and account alerts"
                setShowBadge(true)
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }
            manager.createNotificationChannel(securityChannel)
        }
    }

    private fun handleSpecialNotification(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"] ?: "Security Alert"
        val body = data["body"] ?: ""
        val avatarUrl = data["avatar"]
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("screen", if (type == "profile_view") "profile_view_history" else "security_activity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            type.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, SECURITY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (type == "profile_view" && !avatarUrl.isNullOrBlank()) {
            // Apply blur for profile view if needed (though typically server sends blurred image)
            // For now, we'll let the client handle it if they click.
            Log.d(TAG, "Profile view notification with avatar: $avatarUrl")
        }

        notificationManager.notify(type.hashCode(), builder.build())
        
        // Also insert into internal notifications
        serviceScope.launch {
            val repo = AppModule.provideChatRepository(applicationContext)
            repo.insertNotification(com.keeftalk.chat.domain.model.AppNotification(
                id = java.util.UUID.randomUUID().toString(),
                type = if (type == "profile_view") com.keeftalk.chat.domain.model.NotificationType.PROFILE_VIEW else com.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                title = title,
                message = body,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        com.keeftalk.chat.util.StartupOrchestrator.enqueue(com.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            try {
                AppModule.provideAuthRepository(applicationContext).updateFcmToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token", e)
            }
        }
    }
}
