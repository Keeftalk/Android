package tn.keeftalk.chat.data.service

import android.content.Context
import androidx.work.*
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.util.NotesLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FileCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            NotesLogger.i("CLEANUP", "Starting scheduled file cleanup")
            val fileRepository = AppModule.provideFileRepository(applicationContext)
            fileRepository.reconcileReferenceCounts()
            
            val referenceManager = AppModule.provideFileReferenceManager(applicationContext)
            referenceManager.deleteUnusedFiles()

            // Purge expired decrypted cache files and temporary scanner files
            val cacheDir = applicationContext.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.contains("decrypted_") || file.name.contains("temp_export_"))) {
                    val ageMs = System.currentTimeMillis() - file.lastModified()
                    if (ageMs > 24L * 60 * 60 * 1000) { // Older than 24h
                        file.delete()
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            NotesLogger.e("CLEANUP", "Cleanup worker failed", throwable = e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "tn.keeftalk.chat.FILE_CLEANUP"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<FileCleanupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
