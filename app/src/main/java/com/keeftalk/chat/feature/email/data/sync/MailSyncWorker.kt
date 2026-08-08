package com.keeftalk.chat.feature.email.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.keeftalk.chat.di.AppModule
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
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
