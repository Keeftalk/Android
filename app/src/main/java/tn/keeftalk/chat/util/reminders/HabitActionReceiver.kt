package tn.keeftalk.chat.util.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.entities.CheckInEntity
import tn.keeftalk.chat.di.AppModule
import java.time.LocalDate

class HabitActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppModule.provideDatabase(context)
                val today = LocalDate.now()
                val checkIn = CheckInEntity(
                    id = "${habitId}_${today.toEpochDay()}",
                    habitId = habitId,
                    epochDay = today.toEpochDay(),
                    completed = true,
                    updatedAt = System.currentTimeMillis()
                )
                db.nativeHabitDao().upsertCheckIn(checkIn)

                val notificationId = habitId.hashCode()
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(notificationId)

                val dispatcher = AppModule.provideNotificationDispatcher(context)
                dispatcher.dispatchDirect(
                    tn.keeftalk.chat.util.notifications.NotificationPayload(
                        feature = tn.keeftalk.chat.util.notifications.NotificationFeature.HABITS,
                        type = tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                        priority = tn.keeftalk.chat.domain.model.NotificationPriority.NORMAL,
                        title = "🎉 Habit Checked In!",
                        body = "Great job keeping your momentum going today!",
                        habitId = habitId
                    )
                )
            } catch (e: Exception) {
                Log.e("HabitActionReceiver", "Error checking in habit from notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
