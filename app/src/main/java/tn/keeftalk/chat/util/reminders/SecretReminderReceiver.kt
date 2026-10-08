package tn.keeftalk.chat.util.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload

class SecretReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED"
        ) {
            return
        }

        val secretId = intent.getStringExtra("secret_id")
        val reminderKind = intent.getStringExtra("secret_kind") ?: "EXPIRATION"

        val (title, body) = when (reminderKind) {
            "EXPIRATION" -> "Secret Expiration Alert" to "A protected secret item is set to expire soon."
            "SECURITY" -> "Secrets Security Warning" to "Vault security check or auth timeout event."
            else -> "Secret Reminder" to "Scheduled reminder for a protected secret item."
        }

        val dispatcher = AppModule.provideNotificationDispatcher(context)
        dispatcher.dispatch(
            NotificationPayload(
                feature = NotificationFeature.SECRETS,
                type = NotificationType.REMINDER,
                priority = NotificationPriority.HIGH,
                title = title,
                body = body,
                sourceId = secretId,
                isSensitive = true
            )
        )
    }
}
