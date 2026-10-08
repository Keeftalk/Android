package tn.keeftalk.chat.util.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload

class CalendarReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED"
        ) {
            return
        }

        val eventId = intent.getStringExtra("event_id")
        val eventTitle = intent.getStringExtra("event_title") ?: "Calendar Event"
        val eventLocation = intent.getStringExtra("event_location") ?: ""

        val title = "📅 $eventTitle"
        val body = if (eventLocation.isNotBlank()) "Upcoming event at $eventLocation" else "Upcoming event scheduled in My Calendar"

        val dispatcher = AppModule.provideNotificationDispatcher(context)
        dispatcher.dispatch(
            NotificationPayload(
                feature = NotificationFeature.CALENDAR,
                type = NotificationType.REMINDER,
                priority = NotificationPriority.HIGH,
                title = title,
                body = body,
                eventId = eventId
            )
        )
    }
}
