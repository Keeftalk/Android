package tn.keeftalk.chat.util.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId
import java.time.ZonedDateTime

object BookReminderScheduler {

    fun scheduleDailyReadingGoalReminder(
        context: Context,
        bookId: String,
        bookTitle: String,
        targetAmount: Int,
        targetUnit: String,
        hour: Int = 20,
        minute: Int = 0
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, BookReminderReceiver::class.java).apply {
            putExtra("book_id", bookId)
            putExtra("book_title", bookTitle)
            putExtra("goal_target", targetAmount)
            putExtra("goal_unit", targetUnit)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("book_goal_" + bookId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var targetTime = now.withHour(hour).withMinute(minute).withSecond(0)
        if (targetTime.isBefore(now)) {
            targetTime = targetTime.plusDays(1)
        }

        val triggerAtMillis = targetTime.toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelDailyReadingGoalReminder(context: Context, bookId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BookReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("book_goal_" + bookId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
