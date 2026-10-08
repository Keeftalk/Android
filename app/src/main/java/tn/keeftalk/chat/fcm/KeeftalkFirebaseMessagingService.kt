package tn.keeftalk.chat.fcm

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
import tn.keeftalk.chat.MainActivity
import tn.keeftalk.chat.R
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.domain.model.ChatType
import tn.keeftalk.chat.data.local.entities.MessageEntity
import tn.keeftalk.chat.domain.model.DecryptionState
import tn.keeftalk.chat.util.NotificationAvatarHelper
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
                        repo.updateMessageStatus(messageId, tn.keeftalk.chat.domain.model.MessageStatus.FCM_RECEIVED)
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
                    repo.updateMessageStatus(messageId, tn.keeftalk.chat.domain.model.MessageStatus.FCM_RECEIVED)
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
                        if (result is tn.keeftalk.chat.security.crypto.MessageDecryptionManager.DecryptionResult.Success) {
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
                repo.insertNotification(tn.keeftalk.chat.domain.model.AppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    type = tn.keeftalk.chat.domain.model.NotificationType.MESSAGE,
                    title = title,
                    message = finalBody,
                    timestamp = System.currentTimeMillis(),
                    data = tn.keeftalk.chat.domain.model.NotificationData(chatId = chatId)
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
            val cancelIntent = Intent(this, tn.keeftalk.chat.ui.call.IncomingCallActivity::class.java).apply {
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


        // 1. Prepare Full-screen Intent (Reliable background Activity launch)
        val fullScreenIntent = Intent(this, tn.keeftalk.chat.ui.call.IncomingCallActivity::class.java).apply {
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
        val answerIntent = Intent(this, tn.keeftalk.chat.ui.call.IncomingCallActivity::class.java).apply {
            putExtra("extra_call_id", callId)
            putExtra("action", "answer")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val answerPendingIntent = PendingIntent.getActivity(this, callId.hashCode() + 1, answerIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(0, "Answer", answerPendingIntent)

        // 4. Post Notification
        notificationManager.notify(callId.hashCode(), builder.build())
        
        // Also try direct launch if possible (works better in foreground)
        tn.keeftalk.chat.ui.call.IncomingCallActivity.start(this, callId)
    }

    private fun showNotification(title: String, body: String, chatId: String, avatarUrl: String?) {
        val dispatcher = AppModule.provideNotificationDispatcher(applicationContext)
        dispatcher.dispatch(
            tn.keeftalk.chat.util.notifications.NotificationPayload(
                feature = tn.keeftalk.chat.util.notifications.NotificationFeature.CHATS,
                type = tn.keeftalk.chat.domain.model.NotificationType.MESSAGE,
                priority = tn.keeftalk.chat.domain.model.NotificationPriority.HIGH,
                title = title,
                body = body,
                chatId = chatId,
                avatarUrl = avatarUrl
            )
        )
    }

    private fun handleSpecialNotification(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"] ?: "Security Alert"
        val body = data["body"] ?: ""
        
        val feature = when (type) {
            "profile_view" -> tn.keeftalk.chat.util.notifications.NotificationFeature.SYSTEM
            "security_alert" -> tn.keeftalk.chat.util.notifications.NotificationFeature.SECRETS
            else -> tn.keeftalk.chat.util.notifications.NotificationFeature.CHATS
        }

        val dispatcher = AppModule.provideNotificationDispatcher(applicationContext)
        dispatcher.dispatch(
            tn.keeftalk.chat.util.notifications.NotificationPayload(
                feature = feature,
                type = if (type == "profile_view") tn.keeftalk.chat.domain.model.NotificationType.PROFILE_VIEW else tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                priority = tn.keeftalk.chat.domain.model.NotificationPriority.HIGH,
                title = title,
                body = body,
                chatId = data["chatId"],
                isSensitive = (type == "security_alert")
            )
        )
    }


    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        tn.keeftalk.chat.util.StartupOrchestrator.enqueue(tn.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            try {
                AppModule.provideAuthRepository(applicationContext).updateFcmToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token", e)
            }
        }
    }
}
