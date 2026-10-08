package tn.keeftalk.chat.domain.prayer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Date

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val dispatcher = tn.keeftalk.chat.di.AppModule.provideNotificationDispatcher(context)
        dispatcher.dispatch(
            tn.keeftalk.chat.util.notifications.NotificationPayload(
                feature = tn.keeftalk.chat.util.notifications.NotificationFeature.FEED,
                type = tn.keeftalk.chat.domain.model.NotificationType.REMINDER,
                priority = tn.keeftalk.chat.domain.model.NotificationPriority.HIGH,
                title = "🕌 Time for $prayerName",
                body = "It is time for $prayerName prayer."
            )
        )
    }
}


object PrayerReminderScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminder(context: Context, prayerName: String, triggerTimeMs: Long) {
        if (triggerTimeMs <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            // Alarm permission handling
        }
    }

    fun cancelReminder(context: Context, prayerName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
