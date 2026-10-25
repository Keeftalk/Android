package tn.keeftalk.chat.util.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.toDomain
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppModule.provideDatabase(context)
                    val habits = db.nativeHabitDao().getAllHabits().firstOrNull() ?: emptyList()
                    val activeHabits = habits.filter { !it.deleted }
                    for (habitEntity in activeHabits) {
                        val habit = habitEntity.toDomain()
                        HabitReminderManager.scheduleAllHabitAlarms(context, habit)
                    }
                } catch (e: Exception) {
                    Log.e("HabitReminderReceiver", "Error rescheduling habit alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val habitId = intent.getStringExtra("habit_id")
        val habitName = intent.getStringExtra("habit_name") ?: "Habit Reminder"
        val reminderType = intent.getStringExtra("reminder_type") ?: "REGULAR"

        val (title, body) = when (reminderType) {
            "WINDOW_START_WARNING" -> {
                "$habitName · Check-in Window Opening 🌅" to "Your $habitName check-in window is opening in 15 minutes! Get ready to check in."
            }
            "SLOT_EXPIRY_WARNING" -> {
                "$habitName · Slot Closing Soon! ⏰" to "30 minutes left in this time slot! Check in now before it expires."
            }
            "GRACE_TACTIC_RECOVERY" -> {
                "Missed $habitName?" to "Use a Grace Tactic token from your monthly pool to save your streak!"
            }
            "NEXT_HABIT" -> {
                "Up Next: $habitName" to "Your next habit is ready. Keep your momentum going!"
            }
            else -> {
                "$habitName Reminder" to "Time to check in on $habitName today!"
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (habitId != null) {
                    val db = AppModule.provideDatabase(context)
                    val habitEntity = db.nativeHabitDao().getAllHabits().firstOrNull()?.find { it.id == habitId }
                    if (habitEntity != null && !habitEntity.deleted) {
                        val habit = habitEntity.toDomain()
                        if (habit.reminderTime != null) {
                            HabitReminderManager.scheduleReminder(context, habit)
                        }
                    }
                }

                val actions = mutableListOf<tn.keeftalk.chat.util.notifications.NotificationActionSpec>()
                if (habitId != null) {
                    val checkInIntent = Intent(context, HabitActionReceiver::class.java).apply {
                        putExtra("habit_id", habitId)
                    }
                    val checkInPendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        (habitId + "_checkin").hashCode(),
                        checkInIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    actions.add(tn.keeftalk.chat.util.notifications.NotificationActionSpec("✅ Check In", checkInPendingIntent))

                    val openIntent = Intent(context, tn.keeftalk.chat.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("feature", "HABITS")
                        putExtra("habitId", habitId)
                    }
                    val openPendingIntent = android.app.PendingIntent.getActivity(
                        context,
                        (habitId + "_open").hashCode(),
                        openIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    actions.add(tn.keeftalk.chat.util.notifications.NotificationActionSpec("🎯 Open Habits", openPendingIntent))
                }

                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    NotificationPayload(
                        feature = NotificationFeature.HABITS,
                        type = NotificationType.REMINDER,
                        priority = NotificationPriority.HIGH,
                        title = title,
                        body = body,
                        habitId = habitId,
                        actions = actions
                    )
                )
            } catch (e: Exception) {
                Log.e("HabitReminderReceiver", "Error dispatching habit reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
