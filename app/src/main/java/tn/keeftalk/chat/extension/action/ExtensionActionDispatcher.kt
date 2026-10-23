package tn.keeftalk.chat.extension.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import tn.keeftalk.chat.extension.model.ActionType
import tn.keeftalk.chat.extension.model.ExtensionAction
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager

class ExtensionActionDispatcher(
    private val context: Context,
    private val storageManager: ExtensionStorageManager = ExtensionStorageManager(context)
) {
    fun dispatch(
        extensionId: String,
        action: ExtensionAction,
        onNavigate: ((String) -> Unit)? = null,
        onOpenUrl: ((String) -> Unit)? = null,
        onRefresh: (() -> Unit)? = null,
        clipboardManager: ClipboardManager? = null
    ) {
        when (action.type) {
            ActionType.NAVIGATE -> {
                val target = action.screenId ?: action.targetUrl
                if (!target.isNullOrBlank()) {
                    onNavigate?.invoke(target)
                }
            }
            ActionType.OPEN_URL -> {
                val url = action.targetUrl
                if (!url.isNullOrBlank()) {
                    if (onOpenUrl != null) {
                        onOpenUrl(url)
                    } else {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to open link: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            ActionType.SET_PREFERENCE -> {
                val key = action.key
                val value = action.value
                if (!key.isNullOrBlank() && value != null) {
                    storageManager.setPreference(extensionId, key, value)
                    if (!action.message.isNullOrBlank()) {
                        Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ActionType.SHOW_TOAST -> {
                val msg = action.message ?: action.value
                if (!msg.isNullOrBlank()) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            ActionType.COPY_TO_CLIPBOARD -> {
                val text = action.value ?: action.message
                if (!text.isNullOrBlank()) {
                    clipboardManager?.setText(AnnotatedString(text))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
            ActionType.TRIGGER_REFRESH -> {
                onRefresh?.invoke()
            }
            ActionType.CLEAR_PREFERENCES -> {
                storageManager.clearPreferences(extensionId)
                val msg = action.message ?: "Extension preferences reset"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            ActionType.SCHEDULE_NOTIFICATION -> {
                val title = action.key ?: "Extension Reminder"
                val message = action.message ?: action.value ?: "Scheduled reminder triggered."
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val channelId = "keeftalk_extension_notifications"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(channelId, "Extension Notifications", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(channel)
                    }
                    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(extensionId.hashCode(), notification)
                    Toast.makeText(context, "Reminder notification scheduled/posted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to schedule notification: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            ActionType.CANCEL_NOTIFICATION -> {
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.cancel(extensionId.hashCode())
                    Toast.makeText(context, "Reminder notification cancelled", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        }
    }
}
