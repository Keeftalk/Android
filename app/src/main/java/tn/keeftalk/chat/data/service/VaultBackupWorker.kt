package tn.keeftalk.chat.data.service

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.util.NotesLogger
import java.util.concurrent.TimeUnit

class VaultBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            NotesLogger.i("VAULT_BACKUP", "Starting scheduled automatic folder backup scan & upload")
            val backupManager = AppModule.provideVaultBackupManager(applicationContext)
            val result = backupManager.performScanAndBackup()
            val dispatcher = AppModule.provideNotificationDispatcher(applicationContext)
            if (result.isSuccess) {
                dispatcher.dispatch(
                    tn.keeftalk.chat.util.notifications.NotificationPayload(
                        feature = tn.keeftalk.chat.util.notifications.NotificationFeature.VAULT,
                        type = tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                        priority = tn.keeftalk.chat.domain.model.NotificationPriority.LOW,
                        title = "Vault Backup Completed",
                        body = "Automatic folder backup scan & upload completed successfully.",
                        isSensitive = true
                    )
                )
                Result.success()
            } else {
                dispatcher.dispatch(
                    tn.keeftalk.chat.util.notifications.NotificationPayload(
                        feature = tn.keeftalk.chat.util.notifications.NotificationFeature.VAULT,
                        type = tn.keeftalk.chat.domain.model.NotificationType.SYSTEM,
                        priority = tn.keeftalk.chat.domain.model.NotificationPriority.NORMAL,
                        title = "Vault Backup Warning",
                        body = "Scheduled folder backup encountered an issue and will retry.",
                        isSensitive = true
                    )
                )
                Result.retry()
            }

        } catch (e: Exception) {
            NotesLogger.e("VAULT_BACKUP", "Vault backup worker execution failed", throwable = e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "tn.keeftalk.chat.VAULT_AUTOMATIC_BACKUP"

        fun schedule(context: Context, wifiOnly: Boolean = true, requiresCharging: Boolean = false) {
            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(requiresCharging)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<VaultBackupWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun triggerNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<VaultBackupWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
