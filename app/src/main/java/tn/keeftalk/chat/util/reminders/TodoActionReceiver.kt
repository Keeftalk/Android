package tn.keeftalk.chat.util.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule

class TodoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppModule.provideDatabase(context)
                val tasks = db.todoDao().getAllTasks().firstOrNull() ?: emptyList()
                val task = tasks.find { it.id == taskId }
                if (task != null) {
                    val updated = task.copy(
                        completed = true,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    db.todoDao().upsertTask(updated)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(("todo_task_" + taskId).hashCode())

                val chatRepo = AppModule.provideChatRepository(context)
                chatRepo.markFeatureNotificationsAsRead("TODO")

                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    tn.keeftalk.chat.util.notifications.NotificationPayload(
                        feature = tn.keeftalk.chat.util.notifications.NotificationFeature.TODO,
                        type = tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                        priority = tn.keeftalk.chat.domain.model.NotificationPriority.NORMAL,
                        title = "🎉 Task Completed!",
                        body = "Great job finishing your task!",
                        sourceId = taskId
                    )
                )
            } catch (e: Exception) {
                Log.e("TodoActionReceiver", "Error completing task from notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
