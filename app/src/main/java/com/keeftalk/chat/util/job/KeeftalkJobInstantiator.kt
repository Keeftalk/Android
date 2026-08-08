package com.keeftalk.chat.util.job

import android.content.Context
import com.keeftalk.chat.di.AppModule

class KeeftalkJobInstantiator(private val context: Context) : JobInstantiator {
    override fun create(spec: JobSpec): Job {
        return when (spec.type) {
            "SYNC_MESSAGES" -> SyncMessagesJob(context, spec.data)
            // Add more job types here
            else -> throw IllegalArgumentException("Unknown job type: ${spec.type}")
        }
    }
}

class SyncMessagesJob(private val context: Context, private val chatId: String) : Job {
    override fun onRun(): JobResult {
        return try {
            val repository = AppModule.provideChatRepository(context)
            kotlinx.coroutines.runBlocking {
                if (chatId == "all") {
                    repository.syncAllRecentContent()
                } else {
                    repository.syncChat(chatId)
                }
            }
            JobResult.SUCCESS
        } catch (e: Exception) {
            JobResult.FAILURE
        }
    }
}
