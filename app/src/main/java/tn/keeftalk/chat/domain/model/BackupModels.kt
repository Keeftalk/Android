package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BackupCategory(val displayName: String, val description: String, val isLocalOnly: Boolean = false) {
    BOOKS("My Books", "Reading progress, advancement, and bookmarks."),
    MY_DIN("My Din", "Dhikr counters, favorites, reading progress, and prayer settings."),
    HABITS("My Habits", "Habits, history, check-ins, and progress."),
    TODO("My Todo", "Tasks, lists, and completion status."),
    BROWSER("My Browser", "Bookmarks, history, and locally stored browser data."),
    FEED("My Feed", "Articles, posts, and locally stored feed data."),
    FINANCES("My Finances", "Financial records, transactions, budgets, and accounts."),
    CALENDAR("My Calendar", "Events, reminders, and associated calendar information."),
    NOTES("My Notes", "Notes and associated attachments."),
    DOCS("My Docs", "Documents, markdown content, and attachments."),
    SECRETS("My Secrets", "Encrypted secrets, vault items, and metadata."),
    CALLS("My Calls", "Locally stored call logs and metadata (Requires Call Log permission).", true),
    SMS("My SMS", "Locally stored SMS/MMS messages and threads (Requires SMS permission).", true)
}

@Serializable
enum class BackupSchedule(val title: String, val intervalMillis: Long) {
    DISABLED("Disabled", 0L),
    EVERY_12_HOURS("Every 12 hours", 12 * 60 * 60 * 1000L),
    EVERY_24_HOURS("Every 24 hours", 24 * 60 * 60 * 1000L),
    EVERY_3_DAYS("Every 3 days", 3 * 24 * 60 * 60 * 1000L)
}

@Serializable
data class BackupConfig(
    val selectedCategories: Set<String> = BackupCategory.entries.map { it.name }.toSet(),
    val schedule: BackupSchedule = BackupSchedule.DISABLED,
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val hasPasswordSet: Boolean = false
)

@Serializable
data class BackupManifest(
    val version: Int = 1,
    val backupId: String,
    val accountId: String,
    val timestamp: Long,
    val appVersion: String,
    val selectedCategories: List<String>,
    val recordCounts: Map<String, Int>,
    val schemaVersion: Int,
    val checksum: String
)

enum class KeeftalkBackupStatus {
    IDLE, BACKING_UP, RESTORING, SUCCESS, FAILED
}

data class BackupStats(
    val lastBackupTimestamp: Long? = null,
    val backupFileSize: Long = 0L,
    val status: KeeftalkBackupStatus = KeeftalkBackupStatus.IDLE,
    val lastError: String? = null,
    val nextScheduledTime: Long? = null,
    val selectedCategories: Set<String> = BackupCategory.entries.map { it.name }.toSet()
)
