package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.BugReport

interface BugReportRepository {
    suspend fun submitReport(description: String, logs: String): Result<Unit>
}
