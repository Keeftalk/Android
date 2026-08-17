package com.keeftalk.chat.data.repository

import android.content.Context
import android.os.Build
import com.keeftalk.chat.domain.model.BugReport
import com.keeftalk.chat.domain.model.SystemInfo
import com.keeftalk.chat.domain.repository.BugReportRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class BugReportRepositoryImpl(
    private val context: Context
) : BugReportRepository {

    private suspend fun getSupabase(): SupabaseClient {
        return com.keeftalk.chat.di.AppModule.provideSupabaseClientAsync(context)
    }

    override suspend fun submitReport(description: String, logs: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabaseClient = getSupabase()
            val user = supabaseClient.auth.currentUserOrNull()
            val userId = user?.id ?: throw Exception("User not authenticated")
            
            val reportId = UUID.randomUUID().toString()
            val logFileName = "logs_$reportId.txt"
            
            // 1. Upload logs to Supabase Storage
            val bucket = supabaseClient.storage["bug_logs"]
            bucket.upload(path = logFileName, data = logs.toByteArray())
            val logUrl = bucket.publicUrl(logFileName)

            // 2. Create system info
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val systemInfo = SystemInfo(
                appVersion = packageInfo.versionName ?: "unknown",
                deviceModel = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                sdkVersion = Build.VERSION.SDK_INT
            )

            // 3. Save report to Postgrest
            val report = BugReport(
                id = reportId,
                userId = userId,
                description = description,
                systemInfo = systemInfo,
                logUrl = logUrl
            )

            supabaseClient.postgrest["bug_reports"].insert(report)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
