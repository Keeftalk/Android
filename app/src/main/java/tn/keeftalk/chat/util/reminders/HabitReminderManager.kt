package tn.keeftalk.chat.util.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import tn.keeftalk.chat.domain.model.habit.Habit
import java.time.ZoneId
import java.time.ZonedDateTime

object HabitReminderManager {

    fun scheduleReminder(context: Context, habit: Habit) {
        val reminderTime = habit.reminderTime ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habit_id", habit.id)
            putExtra("habit_name", habit.name)
            putExtra("habit_icon", habit.icon)
            putExtra("reminder_type", "REGULAR")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var targetTime = now.withHour(reminderTime.hour).withMinute(reminderTime.minute).withSecond(0)
        if (targetTime.isBefore(now)) {
            targetTime = targetTime.plusDays(1)
        }

        setAlarm(alarmManager, targetTime.toInstant().toEpochMilli(), pendingIntent)
    }

    fun scheduleSlotExpiryWarning(context: Context, habit: Habit, slotEndHour: Int, slotEndMinute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habit_id", habit.id)
            putExtra("habit_name", habit.name)
            putExtra("habit_icon", habit.icon)
            putExtra("reminder_type", "SLOT_EXPIRY_WARNING")
        }

        val requestCode = (habit.id + "_expiry").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var warningTime = now.withHour(slotEndHour).withMinute(slotEndMinute).withSecond(0).minusMinutes(30)
        if (warningTime.isBefore(now)) {
            warningTime = warningTime.plusDays(1)
        }

        setAlarm(alarmManager, warningTime.toInstant().toEpochMilli(), pendingIntent)
    }

    fun scheduleGraceTacticPrompt(context: Context, habit: Habit, slotEndHour: Int, slotEndMinute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habit_id", habit.id)
            putExtra("habit_name", habit.name)
            putExtra("habit_icon", habit.icon)
            putExtra("reminder_type", "GRACE_TACTIC_RECOVERY")
        }

        val requestCode = (habit.id + "_grace").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var promptTime = now.withHour(slotEndHour).withMinute(slotEndMinute).withSecond(0).plusMinutes(10)
        if (promptTime.isBefore(now)) {
            promptTime = promptTime.plusDays(1)
        }

        setAlarm(alarmManager, promptTime.toInstant().toEpochMilli(), pendingIntent)
    }

    fun cancelReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
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
}
