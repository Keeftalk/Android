package tn.keeftalk.chat.util.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tn.keeftalk.chat.MainActivity
import tn.keeftalk.chat.R
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.domain.model.AppNotification
import tn.keeftalk.chat.domain.model.NotificationData
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.domain.model.UserNotificationSettings
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.util.SoundEffectManager
import java.util.UUID

private const val TAG = "KEEFTALK_DISPATCHER"

enum class NotificationFeature {
    CHATS,
    CALLS,
    SMS_MMS,
    NOTES,
    VAULT,
    SECRETS,
    CALENDAR,
    WALLET,
    FINANCES,
    EMAIL,
    FEED,
    BOOKS,
    MARKET,
    HABITS,
    BROWSER,
    SYSTEM,
    TODO
}

data class NotificationActionSpec(
    val title: String,
    val intent: PendingIntent
)

data class NotificationPayload(
    val feature: NotificationFeature,
    val type: NotificationType = NotificationType.SYSTEM,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val title: String,
    val body: String,
    val chatId: String? = null,
    val noteId: String? = null,
    val bookId: String? = null,
    val habitId: String? = null,
    val eventId: String? = null,
    val emailId: String? = null,
    val orderId: String? = null,
    val sourceId: String? = null,
    val actionUrl: String? = null,
    val avatarUrl: String? = null,
    val isSensitive: Boolean = false,
    val actions: List<NotificationActionSpec> = emptyList()
)

class NotificationDispatcher(
    private val context: Context,
    private val userPrefsRepo: UserPreferencesRepository,
    private val chatRepository: ChatRepository
) {
    private val dispatcherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Central dispatch method that processes all notification requests in Keeftalk.
     */
    fun dispatch(payload: NotificationPayload) {
        dispatcherScope.launch {
            dispatchDirect(payload)
        }
    }

    /**
     * Direct suspend version of dispatch for use within BroadcastReceivers holding goAsync().
     */
    suspend fun dispatchDirect(payload: NotificationPayload) {
        try {
            val fullSettings = kotlinx.coroutines.withTimeoutOrNull(1500) {
                userPrefsRepo.fullSettingsFlow.first()
            }
            val settings = fullSettings?.notificationSettings ?: UserNotificationSettings()

            // Step 1: Global Master Switch
            if (fullSettings != null && !settings.allEnabled) {
                Log.d(TAG, "Suppressed [${payload.feature}]: Master notification switch is OFF")
                return
            }

            // Step 2: Per-Feature Switch Check
            if (fullSettings != null && !isFeatureEnabled(payload.feature, settings)) {
                Log.d(TAG, "Suppressed [${payload.feature}]: Feature notifications disabled in Settings")
                return
            }

            // Step 3: Privacy Sanitization
            val sanitizedBody = sanitizeBody(payload, settings)
            val sanitizedTitle = sanitizeTitle(payload, settings)

            // Step 4: Check Foreground App State
            val prefs = context.getSharedPreferences("keeftalk_prefs", Context.MODE_PRIVATE)
            val isAppInForeground = prefs.getBoolean("is_app_in_foreground", false)
            val currentOpenChatId = prefs.getString("current_open_chat_id", null)

            val isTargetActive = isAppInForeground && payload.chatId != null && payload.chatId == currentOpenChatId

            // Step 5: Layer 1 - In-App Notification Delivery
            if (settings.inAppEnabled) {
                try {
                    deliverInAppNotification(payload, sanitizedTitle, sanitizedBody)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to deliver in-app notification", e)
                }
            }

            // Step 6: Layer 2 - System Notification Delivery (Fail-open for push/FCM)
            if (!isTargetActive) {
                deliverSystemNotification(payload, sanitizedTitle, sanitizedBody)
            } else {
                Log.d(TAG, "Suppressed system notification: Chat/Screen ${payload.chatId} actively open in foreground")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching notification for feature ${payload.feature}, falling back to direct delivery", e)
            try {
                deliverSystemNotification(payload, payload.title, payload.body)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback notification delivery also failed", fallbackError)
            }
        }
    }

    private fun isFeatureEnabled(feature: NotificationFeature, settings: UserNotificationSettings): Boolean {
        return when (feature) {
            NotificationFeature.CHATS -> settings.chatsEnabled && settings.messagesEnabled
            NotificationFeature.CALLS -> settings.callsEnabled
            NotificationFeature.SMS_MMS -> settings.smsMmsEnabled
            NotificationFeature.NOTES -> settings.notesEnabled
            NotificationFeature.VAULT -> settings.vaultEnabled
            NotificationFeature.SECRETS -> settings.secretsEnabled
            NotificationFeature.CALENDAR -> settings.calendarEnabled
            NotificationFeature.WALLET -> settings.walletEnabled
            NotificationFeature.FINANCES -> settings.financesEnabled
            NotificationFeature.EMAIL -> settings.emailEnabled
            NotificationFeature.FEED -> settings.feedEnabled
            NotificationFeature.BOOKS -> settings.booksEnabled
            NotificationFeature.MARKET -> settings.marketEnabled
            NotificationFeature.HABITS -> settings.habitsEnabled
            NotificationFeature.BROWSER -> settings.browserEnabled
            NotificationFeature.SYSTEM -> settings.systemNudgesEnabled
            NotificationFeature.TODO -> settings.habitsEnabled || settings.systemNudgesEnabled
        }
    }

    private fun sanitizeTitle(payload: NotificationPayload, settings: UserNotificationSettings): String {
        if (isSensitiveFeature(payload.feature)) {
            return when (payload.feature) {
                NotificationFeature.SECRETS -> "Secrets Security"
                NotificationFeature.WALLET -> "Wallet Alert"
                else -> payload.title
            }
        }
        return payload.title
    }

    private fun sanitizeBody(payload: NotificationPayload, settings: UserNotificationSettings): String {
        if (isSensitiveFeature(payload.feature)) {
            return when (payload.feature) {
                NotificationFeature.SECRETS -> "[Encrypted Secret Alert]"
                NotificationFeature.WALLET -> "[Private Wallet Alert]"
                else -> payload.body
            }
        }

        return payload.body
    }

    private fun isSensitiveFeature(feature: NotificationFeature): Boolean {
        return feature == NotificationFeature.SECRETS || feature == NotificationFeature.WALLET
    }

    private suspend fun deliverInAppNotification(
        payload: NotificationPayload,
        title: String,
        body: String
    ) {
        val effectiveSourceId = when {
            !payload.sourceId.isNullOrBlank() && !payload.sourceId.startsWith(payload.feature.name, ignoreCase = true) -> "${payload.feature.name}:${payload.sourceId}"
            !payload.sourceId.isNullOrBlank() -> payload.sourceId
            else -> payload.feature.name
        }

        val appNotification = AppNotification(
            id = UUID.randomUUID().toString(),
            type = payload.type,
            priority = payload.priority,
            title = title,
            message = body,
            timestamp = System.currentTimeMillis(),
            data = NotificationData(
                chatId = payload.chatId,
                noteId = payload.noteId,
                contactName = payload.title,
                fullMessage = payload.body
            ),
            sourceId = effectiveSourceId,
            imageUrl = payload.avatarUrl,
            actionUrl = payload.actionUrl
        )
        chatRepository.insertNotification(appNotification)
        Log.d(TAG, "Delivered In-App Notification [${payload.feature} / $effectiveSourceId]: $title")
    }

    private fun deliverSystemNotification(
        payload: NotificationPayload,
        title: String,
        body: String
    ) {
        val channelId = NotificationChannelManager.getChannelIdForFeature(payload.feature.name)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("feature", payload.feature.name)
            putExtra("chatId", payload.chatId)
            putExtra("noteId", payload.noteId)
            putExtra("bookId", payload.bookId)
            putExtra("habitId", payload.habitId)
            putExtra("eventId", payload.eventId)
            putExtra("emailId", payload.emailId)
            putExtra("orderId", payload.orderId)
            putExtra("sourceId", payload.sourceId)
        }

        val notificationId = (payload.chatId ?: payload.habitId ?: payload.bookId ?: payload.eventId ?: payload.feature.name).hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        for (action in payload.actions) {
            builder.addAction(0, action.title, action.intent)
        }

        when (payload.priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH -> {
                builder.priority = NotificationCompat.PRIORITY_HIGH
                builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            }
            NotificationPriority.LOW -> {
                builder.priority = NotificationCompat.PRIORITY_LOW
            }
            else -> {
                builder.priority = NotificationCompat.PRIORITY_DEFAULT
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, builder.build())

        when (payload.feature) {
            NotificationFeature.SYSTEM -> {
                if (payload.title.contains("Nudge", ignoreCase = true) || payload.title.contains("Profile", ignoreCase = true)) {
                    SoundEffectManager.playNotificationSound(context, SoundEffectManager.RES_PROFILE_BELL)
                } else {
                    SoundEffectManager.playNotificationSound(context, SoundEffectManager.RES_ANY_NOTIFICATION)
                }
            }
            NotificationFeature.CHATS, NotificationFeature.SMS_MMS -> {
                SoundEffectManager.playNotificationSound(context, SoundEffectManager.RES_MSG_RECEIVING_APP)
            }
            else -> {
                SoundEffectManager.playNotificationSound(context, SoundEffectManager.RES_ANY_NOTIFICATION)
            }
        }

        Log.d(TAG, "Delivered System Notification [${payload.feature} / $channelId]: $title")
    }

    private fun hasPostNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
