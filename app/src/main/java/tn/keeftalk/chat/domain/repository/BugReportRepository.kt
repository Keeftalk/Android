package tn.keeftalk.chat.domain.repository

import tn.keeftalk.chat.domain.model.BugReport

interface BugReportRepository {
    suspend fun submitReport(description: String, logs: String): Result<Unit>
}
