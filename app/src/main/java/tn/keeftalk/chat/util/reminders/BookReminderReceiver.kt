package tn.keeftalk.chat.util.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload

class BookReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.intent.action.MY_PACKAGE_REPLACED"
        ) {
            val store = tn.keeftalk.chat.util.KeeftalkStore.getInstance()
            val reminderEnabled = store.getBoolean("goals_reminder_enabled", true)
            if (reminderEnabled) {
                val reminderTime = store.getString("goals_reminder_time", "08:13") ?: "08:13"
                val dailyGoal = store.getInt("goals_daily_minutes", 30)
                val parts = reminderTime.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                BookReminderScheduler.scheduleDailyReadingGoalReminder(
                    context = context,
                    bookId = "daily_reading_goal",
                    bookTitle = "Daily Reading Goal",
                    targetAmount = dailyGoal,
                    targetUnit = "min",
                    hour = hour,
                    minute = minute
                )
            }
            return
        }

        val bookId = intent.getStringExtra("book_id")
        val bookTitle = intent.getStringExtra("book_title") ?: "My Books"
        val goalTarget = intent.getIntExtra("goal_target", 15)
        val goalUnit = intent.getStringExtra("goal_unit") ?: "min"

        val title = "Daily Reading Goal: $bookTitle"
        val body = "Time to read! Target: $goalTarget $goalUnit today."

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Reschedule for next day
                runCatching {
                    val store = tn.keeftalk.chat.util.KeeftalkStore.getInstance()
                    val reminderEnabled = store.getBoolean("goals_reminder_enabled", true)
                    if (reminderEnabled) {
                        val reminderTime = store.getString("goals_reminder_time", "08:13") ?: "08:13"
                        val dailyGoal = store.getInt("goals_daily_minutes", 30)
                        val parts = reminderTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        BookReminderScheduler.scheduleDailyReadingGoalReminder(
                            context = context,
                            bookId = "daily_reading_goal",
                            bookTitle = "Daily Reading Goal",
                            targetAmount = dailyGoal,
                            targetUnit = "min",
                            hour = hour,
                            minute = minute
                        )
                    }
                }

                val actions = mutableListOf<tn.keeftalk.chat.util.notifications.NotificationActionSpec>()
                val openIntent = Intent(context, tn.keeftalk.chat.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("feature", "BOOKS")
                    if (bookId != null) putExtra("bookId", bookId)
                }
                val openPendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    ("book_open_" + (bookId ?: "default")).hashCode(),
                    openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(tn.keeftalk.chat.util.notifications.NotificationActionSpec("📖 Read Now", openPendingIntent))

                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    NotificationPayload(
                        feature = NotificationFeature.BOOKS,
                        type = NotificationType.REMINDER,
                        priority = NotificationPriority.HIGH,
                        title = title,
                        body = body,
                        bookId = bookId,
                        actions = actions
                    )
                )
            } catch (e: Exception) {
                Log.e("BookReminderReceiver", "Error dispatching book reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
