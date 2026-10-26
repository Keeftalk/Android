package tn.keeftalk.chat.util.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tn.keeftalk.chat.MainActivity
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.NotificationPriority
import tn.keeftalk.chat.domain.model.NotificationType
import tn.keeftalk.chat.util.notifications.NotificationActionSpec
import tn.keeftalk.chat.util.notifications.NotificationFeature
import tn.keeftalk.chat.util.notifications.NotificationPayload
import java.time.LocalDate
import java.time.ZoneId

class TodoReminderReceiver : BroadcastReceiver() {
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
                    val tasks = db.todoDao().getAllTasks().firstOrNull() ?: emptyList()
                    val activeTasks = tasks.filter { !it.deleted && !it.completed }
                    val nowMillis = System.currentTimeMillis()
                    for (task in activeTasks) {
                        var triggerTime = 0L
                        if (task.reminder.isNotBlank()) {
                            triggerTime = runCatching {
                                java.time.LocalDateTime.parse(task.reminder).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }.getOrDefault(0L)
                        } else if (task.dueDate.isNotBlank()) {
                            triggerTime = runCatching {
                                LocalDate.parse(task.dueDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + (9 * 3600 * 1000)
                            }.getOrDefault(0L)
                        }
                        if (triggerTime > nowMillis - (30 * 60 * 1000)) {
                            TodoReminderScheduler.scheduleAllTaskAlarms(context, task.id, task.title, triggerTime)
                        } else if (task.dueDate.isBlank() && task.reminder.isBlank()) {
                            TodoReminderScheduler.scheduleUndatedTaskReminder(context, task.id, task.title, task.createdAt)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TodoReminderReceiver", "Error rescheduling todo alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val taskId = intent.getStringExtra("task_id") ?: return
        val taskTitle = intent.getStringExtra("task_title") ?: "Todo Task"
        val reminderType = intent.getStringExtra("reminder_type") ?: "DUE_NOW"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppModule.provideDatabase(context)
                val tasks = db.todoDao().getAllTasks().firstOrNull() ?: emptyList()
                val currentTask = tasks.find { it.id == taskId }

                if (currentTask == null || currentTask.deleted || currentTask.completed) {
                    TodoReminderScheduler.cancelTaskReminder(context, taskId)
                    return@launch
                }

                if (reminderType == "UNDATED_REMINDER") {
                    if (currentTask.dueDate.isNotBlank() || currentTask.reminder.isNotBlank()) {
                        TodoReminderScheduler.cancelUndatedTaskReminder(context, taskId)
                        return@launch
                    }
                    val title = "📋 Pending Task: $taskTitle"
                    val body = "Don't forget to review or schedule this undated task!"
                    
                    val actions = mutableListOf<NotificationActionSpec>()
                    val completeIntent = Intent(context, TodoActionReceiver::class.java).apply {
                        putExtra("task_id", taskId)
                    }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        context,
                        ("todo_complete_" + taskId).hashCode(),
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    actions.add(NotificationActionSpec("✅ Complete Task", completePendingIntent))

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("feature", "TODO")
                        putExtra("taskId", taskId)
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        ("todo_open_" + taskId).hashCode(),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    actions.add(NotificationActionSpec("📋 Open Todo", openPendingIntent))

                    val dispatcher = AppModule.provideNotificationDispatcher(context)
                    dispatcher.dispatchDirect(
                        NotificationPayload(
                            feature = NotificationFeature.TODO,
                            type = NotificationType.REMINDER,
                            priority = NotificationPriority.NORMAL,
                            title = title,
                            body = body,
                            sourceId = taskId,
                            actions = actions
                        )
                    )

                    // Reschedule for 24h later
                    TodoReminderScheduler.scheduleUndatedTaskReminder(context, taskId, taskTitle, System.currentTimeMillis())
                    return@launch
                }

                val (title, body) = when (reminderType) {
                    "DUE_SOON" -> "📋 Task Due Soon: $taskTitle ⏰" to "Your task is due in 30 minutes! Tap to review or complete."
                    "OVERDUE" -> "⚠️ Overdue Task: $taskTitle" to "This task has passed its time window! Tap to complete or update."
                    else -> "📋 Task Due Now: $taskTitle" to "Your task deadline is here! Tap to complete or view in My Todo."
                }

                val actions = mutableListOf<NotificationActionSpec>()

                val completeIntent = Intent(context, TodoActionReceiver::class.java).apply {
                    putExtra("task_id", taskId)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
                    context,
                    ("todo_complete_" + taskId).hashCode(),
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationActionSpec("✅ Complete Task", completePendingIntent))

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("feature", "TODO")
                    putExtra("taskId", taskId)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    ("todo_open_" + taskId).hashCode(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationActionSpec("📋 Open Todo", openPendingIntent))

                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    NotificationPayload(
                        feature = NotificationFeature.TODO,
                        type = NotificationType.REMINDER,
                        priority = NotificationPriority.HIGH,
                        title = title,
                        body = body,
                        sourceId = taskId,
                        actions = actions
                    )
                )
            } catch (e: Exception) {
                Log.e("TodoReminderReceiver", "Error dispatching todo reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
