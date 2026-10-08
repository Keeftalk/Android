package tn.keeftalk.chat.domain.model

import android.os.Environment
import kotlinx.serialization.Serializable

@Serializable
data class FolderBackupConfig(
    val id: String,
    val name: String,
    val path: String,
    val enabled: Boolean = true,
    val isPreset: Boolean = false,
    val fileTypes: List<String> = listOf("PHOTO", "VIDEO", "DOCUMENT", "OTHER")
)

fun defaultPresetFolders(): List<FolderBackupConfig> {
    val externalStorage = try {
        Environment.getExternalStorageDirectory().absolutePath
    } catch (_: Exception) {
        "/storage/emulated/0"
    }
    return listOf(
        FolderBackupConfig(
            id = "camera",
            name = "Camera",
            path = "$externalStorage/DCIM/Camera",
            enabled = true,
            isPreset = true,
            fileTypes = listOf("PHOTO", "VIDEO")
        ),
        FolderBackupConfig(
            id = "screenshots",
            name = "Screenshots",
            path = "$externalStorage/Pictures/Screenshots",
            enabled = true,
            isPreset = true,
            fileTypes = listOf("PHOTO", "VIDEO")
        ),
        FolderBackupConfig(
            id = "downloads",
            name = "Downloads",
            path = "$externalStorage/Download",
            enabled = false,
            isPreset = true,
            fileTypes = listOf("PHOTO", "VIDEO", "DOCUMENT", "OTHER")
        ),
        FolderBackupConfig(
            id = "whatsapp_media",
            name = "WhatsApp Media",
            path = "$externalStorage/Android/media/com.whatsapp/WhatsApp/Media",
            enabled = false,
            isPreset = true,
            fileTypes = listOf("PHOTO", "VIDEO", "DOCUMENT")
        )
    )
}

enum class BackupStatus {
    PENDING,
    UPLOADING,
    SUCCESS,
    FAILED,
    PAUSED,
    QUOTA_EXCEEDED
}

data class VaultBackupItem(
    val id: String,
    val folderId: String,
    val localPath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val fileHash: String,
    val mimeType: String?,
    val status: BackupStatus,
    val errorMessage: String? = null,
    val vaultItemId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val backedUpAt: Long? = null
)

data class FolderBackupStats(
    val config: FolderBackupConfig,
    val totalFilesDetected: Int = 0,
    val totalBytesDetected: Long = 0L,
    val backedUpCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val lastBackupTime: Long? = null,
    val statusText: String = "Idle"
)

data class OverallBackupStats(
    val isEnabled: Boolean = false,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalBackedUpCount: Int = 0,
    val totalBackedUpBytes: Long = 0L,
    val totalPendingCount: Int = 0,
    val totalPendingBytes: Long = 0L,
    val totalFailedCount: Int = 0,
    val lastBackupTimestamp: Long? = null,
    val currentUploadingFileName: String? = null,
    val currentProgress: Float = 0f,
    val quotaError: String? = null,
    val folderStats: List<FolderBackupStats> = emptyList()
)
