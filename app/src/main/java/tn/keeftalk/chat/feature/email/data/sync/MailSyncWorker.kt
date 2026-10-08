package tn.keeftalk.chat.feature.email.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import tn.keeftalk.chat.di.AppModule
import kotlinx.coroutines.flow.first

class MailSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val emailRepository = AppModule.provideEmailRepository(applicationContext)
            val accounts = emailRepository.getAccounts().first()
            
            accounts.forEach { account ->
                emailRepository.syncAccount(account.id)
            }
            if (accounts.isNotEmpty()) {
                val dispatcher = AppModule.provideNotificationDispatcher(applicationContext)
                dispatcher.dispatch(
                    tn.keeftalk.chat.util.notifications.NotificationPayload(
                        feature = tn.keeftalk.chat.util.notifications.NotificationFeature.EMAIL,
                        type = tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                        priority = tn.keeftalk.chat.domain.model.NotificationPriority.LOW,
                        title = "My E-Mail Synced",
                        body = "Inbox accounts synced successfully."
                    )
                )
            }
            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}
