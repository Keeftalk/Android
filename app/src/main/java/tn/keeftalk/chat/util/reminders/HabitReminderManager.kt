package tn.keeftalk.chat.util.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import tn.keeftalk.chat.domain.model.habit.Habit
import java.time.ZoneId
import java.time.ZonedDateTime

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

object HabitReminderManager {

    fun scheduleAllHabitAlarms(context: Context, habit: Habit) {
        if (habit.deleted) {
            cancelReminder(context, habit.id)
            return
        }

        scheduleReminder(context, habit)

        val (startH, startM, endH, endM) = when (habit.slot) {
            "Early Morning" -> Quadruple(4, 0, 8, 0)
            "Morning" -> Quadruple(8, 0, 11, 30)
            "Midday" -> Quadruple(11, 30, 14, 0)
            "Afternoon" -> Quadruple(14, 0, 17, 30)
            "Evening" -> Quadruple(17, 30, 22, 0)
            "Late Night" -> Quadruple(22, 0, 1, 0)
            else -> Quadruple(8, 0, 20, 0)
        }

        scheduleWindowStartWarning(context, habit, startH, startM)
        scheduleSlotExpiryWarning(context, habit, endH, endM)
    }

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

    fun scheduleWindowStartWarning(context: Context, habit: Habit, slotStartHour: Int, slotStartMinute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habit_id", habit.id)
            putExtra("habit_name", habit.name)
            putExtra("habit_icon", habit.icon)
            putExtra("reminder_type", "WINDOW_START_WARNING")
        }

        val requestCode = (habit.id + "_start").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var warningTime = now.withHour(slotStartHour).withMinute(slotStartMinute).withSecond(0).minusMinutes(15)
        if (warningTime.isBefore(now)) {
            warningTime = warningTime.plusDays(1)
        }

        setAlarm(alarmManager, warningTime.toInstant().toEpochMilli(), pendingIntent)
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

        listOf(habitId.hashCode(), (habitId + "_start").hashCode(), (habitId + "_expiry").hashCode(), (habitId + "_grace").hashCode()).forEach { code ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
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
