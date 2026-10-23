package tn.keeftalk.chat.data.service

import android.content.Context
import androidx.work.*
import tn.keeftalk.chat.di.AppModule
import java.util.concurrent.TimeUnit

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val password = inputData.getString(KEY_PASSWORD) ?: return Result.failure()
        val accountId = inputData.getString(KEY_ACCOUNT_ID) ?: "default_account"
        val categories = inputData.getStringArray(KEY_CATEGORIES)?.toSet()
            ?: tn.keeftalk.chat.domain.model.BackupCategory.entries.map { it.name }.toSet()

        val database = AppModule.provideDatabase(context)
        val backupEngine = BackupEngine(context, database)

        val result = backupEngine.performBackup(password, categories, accountId)
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val KEY_PASSWORD = "key_password"
        const val KEY_ACCOUNT_ID = "key_account_id"
        const val KEY_CATEGORIES = "key_categories"
        private const val WORK_NAME = "tn.keeftalk.chat.AUTOMATIC_BACKUP_WORK"

        fun schedule(context: Context, intervalHours: Long, password: String, accountId: String, categories: Set<String>) {
            if (intervalHours <= 0) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val inputData = workDataOf(
                KEY_PASSWORD to password,
                KEY_ACCOUNT_ID to accountId,
                KEY_CATEGORIES to categories.toTypedArray()
            )

            val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
