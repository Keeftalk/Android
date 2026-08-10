package com.keeftalk.chat.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.PowerManager
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
import com.keeftalk.chat.data.local.entities.MessageEntity
import com.keeftalk.chat.domain.model.DecryptionState
import com.keeftalk.chat.util.NotificationAvatarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL
import android.graphics.Bitmap

private const val TAG = "KEEFTALK_FCM"
private const val CHANNEL_ID = "chat_messages"
private const val CALL_CHANNEL_ID = "incoming_calls_v3"
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
        Log.d(TAG, "FCM Type: $type")

        if (type == "security_alert" || type == "profile_view" || type == "REACTION") {
            handleSpecialNotification(data)
            return
        }

        if (type == "call" || type == "CANCEL_CALL") {
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
        serviceScope.launch {
            try {
                if (messageId != null) {
                    Log.d(TAG, "Updating message $messageId status to FCM_RECEIVED")
                    repo.updateMessageStatus(messageId, com.keeftalk.chat.domain.model.MessageStatus.FCM_RECEIVED)
                }
                repo.syncChat(chatId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update status or sync chat $chatId on FCM message", e)
            }
        }
        
        serviceScope.launch {
            val userPrefs = userPrefsRepo.userPreferencesFlow.first()
            val globalNotifs = userPrefs.notifications
            
            if (!globalNotifs.allEnabled) return@launch

            val chat = repo.getChat(chatId).first()
            if (chat?.isMuted == true) {
                val muteUntil = chat.notificationSettings.muteUntil
                if (muteUntil == null || muteUntil > System.currentTimeMillis()) return@launch
            }

            if (chat?.type == ChatType.ONE_TO_ONE && !globalNotifs.messagesEnabled) return@launch
            if (chat?.type == ChatType.GROUP && !globalNotifs.groupsEnabled) return@launch

            val showPreview = chat?.notificationSettings?.showPreview ?: globalNotifs.showPreviewContent
            var finalBody = if (showPreview) body else "Message from $title"
            
            if (showPreview && (finalBody.isEmpty() || finalBody == "[Encrypted]") && messageId != null) {
                val ciphertext = data["ciphertext"]
                val nonce = data["nonce"]
                val cryptoVersion = data["cryptoVersion"]?.toIntOrNull() ?: 1
                val envelopeType = data["envelopeType"]?.toIntOrNull() ?: 100
                val senderId = data["senderId"] ?: ""

                if (!ciphertext.isNullOrEmpty() && !nonce.isNullOrEmpty()) {
                    Log.d(TAG, "[FCM_DECRYPT] Attempting instant decryption...")
                    try {
                        val decryptionManager = AppModule.provideMessageDecryptionManager(applicationContext)
                        val transientEntity = MessageEntity(
                            id = messageId,
                            chatId = chatId,
                            senderId = senderId,
                            content = "",
                            timestamp = System.currentTimeMillis(),
                            status = "SENT",
                            type = "TEXT",
                            ciphertext = ciphertext,
                            nonce = nonce,
                            cryptoVersion = cryptoVersion,
                            envelopeType = envelopeType,
                            decryptionState = DecryptionState.PENDING
                        )
                        
                        val result = decryptionManager.decrypt(transientEntity, updateDb = false, source = "FCM_INSTANT")
                        if (result is com.keeftalk.chat.security.crypto.MessageDecryptionManager.DecryptionResult.Success) {
                            finalBody = result.plaintext
                            Log.i(TAG, "[FCM_DECRYPT] SUCCESS")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[FCM_DECRYPT] FAILED", e)
                    }
                }

                // If still encrypted, wait for DB sync fallback
                if (finalBody == "[Encrypted]" || finalBody.isEmpty()) {
                    Log.d(TAG, "Notification body is encrypted, waiting for DB sync fallback... messageId=$messageId")
                    val decryptedMsg = withTimeoutOrNull(5000) {
                        repo.getMessage(messageId).filter { 
                            it != null && it.decryptionState == DecryptionState.SUCCESS
                        }.first()
                    }
                    if (decryptedMsg != null) {
                        finalBody = decryptedMsg.content
                    } else {
                        Log.w(TAG, "Decryption timeout. Showing fallback notification.")
                        finalBody = "New message"
                    }
                }
            }
            
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
        val type = data["type"] ?: "call"

        // Acquire WakeLock to ensure Activity has time to start
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Keeftalk:CallFCMWakeLock")
        wakeLock.acquire(15000L) // Hold for 15 seconds

        if (type == "CANCEL_CALL") {
            Log.d(TAG, "[FCM_CALL] CANCEL RECEIVED | id=$callId")
            val cancelIntent = Intent(this, com.keeftalk.chat.ui.call.IncomingCallActivity::class.java).apply {
                putExtra("extra_call_id", callId)
                action = "ACTION_CANCEL_CALL"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(cancelIntent)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(callId.hashCode())
            return
        }

        val callerName = data["callerName"] ?: data["caller_name"] ?: "Keeftalk Call"
        val avatarUrl = data["avatar"] ?: data["avatarUrl"]
        
        Log.d(TAG, "[FCM_CALL] RECEIVED | id=$callId | from=$callerName")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        // 1. Prepare Full-screen Intent (Reliable background Activity launch)
        val fullScreenIntent = Intent(this, com.keeftalk.chat.ui.call.IncomingCallActivity::class.java).apply {
            putExtra("extra_call_id", callId)
            action = "ACTION_INCOMING_CALL"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 
            callId.hashCode(), 
            fullScreenIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Build High-Priority Call Notification (Must have Ringtone & Vibration for Fullscreen)
        val ringtoneUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
        val builder = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText("Incoming Keeftalk Call...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLocalOnly(true)

        if (!avatarUrl.isNullOrBlank()) {
            serviceScope.launch {
                try {
                    val bitmap = BitmapFactory.decodeStream(URL(avatarUrl).openConnection().getInputStream())
                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                        notificationManager.notify(callId.hashCode(), builder.build())
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Add Answer/Decline Actions
        val answerIntent = Intent(this, com.keeftalk.chat.ui.call.IncomingCallActivity::class.java).apply {
            putExtra("extra_call_id", callId)
            putExtra("action", "answer")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val answerPendingIntent = PendingIntent.getActivity(this, callId.hashCode() + 1, answerIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(0, "Answer", answerPendingIntent)

        // 4. Post Notification
        notificationManager.notify(callId.hashCode(), builder.build())
        
        // Also try direct launch if possible (works better in foreground)
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

        serviceScope.launch {
            // 1. Get Avatar Bitmap (Always present, either URL or Initials)
            val avatarBitmap = NotificationAvatarHelper.getAvatarBitmap(applicationContext, avatarUrl, title)
            val icon = IconCompat.createWithBitmap(avatarBitmap)
            val user = Person.Builder()
                .setName(title)
                .setIcon(icon)
                .setKey(chatId)
                .setImportant(true)
                .build()

            // 2. Build Notification with MessagingStyle for proper sender-centric presentation
            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(avatarBitmap)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(NotificationCompat.MessagingStyle(user)
                    .addMessage(body, System.currentTimeMillis(), user)
                )

            if (chatId.isNotEmpty()) {
                builder.setShortcutId(chatId)
                
                // Reply Action
                val remoteInput = RemoteInput.Builder("key_text_reply").setLabel("Reply...").build()
                val replyIntent = Intent(applicationContext, ReplyReceiver::class.java).apply {
                    action = "ACTION_REPLY"
                    putExtra("chatId", chatId)
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    applicationContext, 
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
                val readIntent = Intent(applicationContext, ReplyReceiver::class.java).apply {
                    action = "ACTION_MARK_READ"
                    putExtra("chatId", chatId)
                }
                val readPendingIntent = PendingIntent.getBroadcast(
                    applicationContext, 
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

            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Notification posted for chat $chatId with sender $title")
        }
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

            val callChannel = NotificationChannel(CALL_CHANNEL_ID, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Urgent call alerts"
                setSound(android.provider.Settings.System.DEFAULT_RINGTONE_URI, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            manager.createNotificationChannel(callChannel)

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
            val screen = when (type) {
                "profile_view" -> "profile_view_history"
                "REACTION" -> "chat_detail"
                else -> "security_activity"
            }
            putExtra("screen", screen)
            if (type == "REACTION") {
                putExtra("chatId", data["chatId"])
            }
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
