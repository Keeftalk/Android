package com.keeftalk.chat.feature.email.data.sync

import android.content.Context
import androidx.work.*
import com.keeftalk.chat.data.local.dao.MailDao
import com.keeftalk.chat.feature.email.data.sync.providers.*
import com.keeftalk.chat.feature.email.model.EmailProvider
import java.util.concurrent.TimeUnit

class MailSyncManager(
    private val context: Context,
    private val mailDao: MailDao,
    private val gmailSyncer: GmailSyncer,
    private val outlookSyncer: OutlookSyncer,
    private val imapSyncer: ImapSyncer
) {

    fun scheduleBackgroundSync() {
        // ... (existing implementation)
    }

    fun stopSync() {
        WorkManager.getInstance(context).cancelUniqueWork("MailSyncWork")
    }

    suspend fun syncAccount(accountId: String) {
        android.util.Log.d("MailSyncManager", "syncAccount start: $accountId")
        val account = mailDao.getAccountSync(accountId) ?: run {
            android.util.Log.e("MailSyncManager", "syncAccount: Account not found in DB!")
            return
        }
        val syncer = getSyncer(account.provider)
        
        android.util.Log.d("MailSyncManager", "Syncing folders for ${account.emailAddress}...")
        syncer.syncFolders(accountId)
        
        if (account.provider == EmailProvider.GMAIL) {
            // Gmail uses account-wide History API for incremental sync
            android.util.Log.d("MailSyncManager", "Syncing Gmail messages (account-wide)...")
            syncer.syncMessages(accountId, "ACCOUNT")
        } else {
            val folders = mailDao.getFoldersSync(accountId)
            android.util.Log.d("MailSyncManager", "Found ${folders.size} folders. Syncing messages per folder...")
            folders.forEach { folder ->
                try {
                    syncer.syncMessages(accountId, folder.id)
                } catch (e: Exception) {
                    android.util.Log.e("MailSyncManager", "Error syncing folder ${folder.name}", e)
                }
            }
        }
        
        // Update last sync timestamp via partial update to avoid CASCADE DELETE
        mailDao.updateAccountSyncTimestamp(accountId, System.currentTimeMillis())
        android.util.Log.d("MailSyncManager", "syncAccount finished")
    }

    fun getSyncer(provider: EmailProvider): EmailSyncer = when (provider) {
        EmailProvider.GMAIL -> gmailSyncer
        EmailProvider.OUTLOOK -> outlookSyncer
        EmailProvider.YAHOO, EmailProvider.CUSTOM_IMAP -> imapSyncer
    }
}
