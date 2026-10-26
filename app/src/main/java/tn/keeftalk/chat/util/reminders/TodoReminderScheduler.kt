package tn.keeftalk.chat.util.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TodoReminderScheduler {

    fun scheduleAllTaskAlarms(
        context: Context,
        taskId: String,
        taskTitle: String,
        dueAtMillis: Long
    ) {
        val now = System.currentTimeMillis()
        if (dueAtMillis <= 0L) {
            cancelTaskReminder(context, taskId)
            return
        }

        val soonMillis = dueAtMillis - (30 * 60 * 1000)
        if (soonMillis > now) {
            scheduleSingleAlarm(context, taskId, taskTitle, soonMillis, "DUE_SOON", ("todo_task_soon_" + taskId).hashCode())
        }

        if (dueAtMillis > now) {
            scheduleSingleAlarm(context, taskId, taskTitle, dueAtMillis, "DUE_NOW", ("todo_task_" + taskId).hashCode())
        }

        val overdueMillis = dueAtMillis + (30 * 60 * 1000)
        if (overdueMillis > now) {
            scheduleSingleAlarm(context, taskId, taskTitle, overdueMillis, "OVERDUE", ("todo_task_overdue_" + taskId).hashCode())
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        taskId: String,
        taskTitle: String,
        triggerAtMillis: Long,
        reminderType: String,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_title", taskTitle)
            putExtra("reminder_type", reminderType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

    fun cancelTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TodoReminderReceiver::class.java)

        listOf(
            ("todo_task_" + taskId).hashCode(),
            ("todo_task_soon_" + taskId).hashCode(),
            ("todo_task_overdue_" + taskId).hashCode()
        ).forEach { requestCode ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        cancelUndatedTaskReminder(context, taskId)
    }

    fun scheduleUndatedTaskReminder(
        context: Context,
        taskId: String,
        taskTitle: String,
        createdAtMillis: Long
    ) {
        val now = System.currentTimeMillis()
        var nextTrigger = createdAtMillis + (24 * 3600 * 1000L)
        if (nextTrigger <= now) {
            nextTrigger = now + (24 * 3600 * 1000L)
        }
        scheduleSingleAlarm(
            context,
            taskId,
            taskTitle,
            nextTrigger,
            "UNDATED_REMINDER",
            ("todo_undated_" + taskId).hashCode()
        )
    }

    fun cancelUndatedTaskReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TodoReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("todo_undated_" + taskId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
