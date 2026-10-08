package tn.keeftalk.chat.data.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tn.keeftalk.chat.data.local.dao.VaultBackupDao
import tn.keeftalk.chat.data.local.entities.VaultBackupItemEntity
import tn.keeftalk.chat.data.local.entities.toDomain
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.FileRepository
import tn.keeftalk.chat.domain.repository.VaultRepository
import tn.keeftalk.chat.util.ConnectivityObserver
import tn.keeftalk.chat.util.NotesLogger
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class VaultBackupManager(
    private val context: Context,
    private val vaultBackupDao: VaultBackupDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val vaultRepository: VaultRepository,
    private val fileRepository: FileRepository,
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    private val tag = "VaultBackupManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentUploadingFile = MutableStateFlow<String?>(null)
    private val _currentProgress = MutableStateFlow(0f)
    private val _quotaError = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val overallStatsFlow: Flow<OverallBackupStats> = combine(
        userPreferencesRepository.fullSettingsFlow,
        vaultBackupDao.getAllBackupItemsFlow(),
        _isPaused,
        _isProcessing,
        _currentUploadingFile,
        _currentProgress,
        _quotaError
    ) { flows ->
        val settings = flows[0] as UserSettings
        val items = flows[1] as List<VaultBackupItemEntity>
        val paused = flows[2] as Boolean
        val processing = flows[3] as Boolean
        val currentFile = flows[4] as String?
        val progress = flows[5] as Float
        val quotaErr = flows[6] as String?

        val vaultSettings = settings.vaultSettings
        val domainItems = items.map { it.toDomain() }

        val backedUpItems = domainItems.filter { it.status == BackupStatus.SUCCESS }
        val pendingItems = domainItems.filter { it.status == BackupStatus.PENDING || it.status == BackupStatus.UPLOADING }
        val failedItems = domainItems.filter { it.status == BackupStatus.FAILED }

        val folderStats = vaultSettings.backupFolders.map { folderConfig ->
            val folderItems = domainItems.filter { it.folderId == folderConfig.id }
            val backedUp = folderItems.filter { it.status == BackupStatus.SUCCESS }
            val pending = folderItems.filter { it.status == BackupStatus.PENDING || it.status == BackupStatus.UPLOADING }
            val failed = folderItems.filter { it.status == BackupStatus.FAILED }
            val totalDetectedSize = folderItems.sumOf { it.fileSize }
            val lastBackup = backedUp.maxOfOrNull { it.backedUpAt ?: 0L }

            val statusText = when {
                !folderConfig.enabled -> "Disabled"
                pending.isNotEmpty() && processing -> "Backing Up (${pending.size} pending)"
                pending.isNotEmpty() -> "Pending (${pending.size})"
                failed.isNotEmpty() -> "Failed (${failed.size})"
                backedUp.isNotEmpty() -> "Up to date"
                else -> "Idle"
            }

            FolderBackupStats(
                config = folderConfig,
                totalFilesDetected = folderItems.size,
                totalBytesDetected = totalDetectedSize,
                backedUpCount = backedUp.size,
                pendingCount = pending.size,
                failedCount = failed.size,
                lastBackupTime = if (lastBackup != null && lastBackup > 0) lastBackup else null,
                statusText = statusText
            )
        }

        OverallBackupStats(
            isEnabled = vaultSettings.automaticBackupEnabled,
            isRunning = processing,
            isPaused = paused,
            totalBackedUpCount = backedUpItems.size,
            totalBackedUpBytes = backedUpItems.sumOf { it.fileSize },
            totalPendingCount = pendingItems.size,
            totalPendingBytes = pendingItems.sumOf { it.fileSize },
            totalFailedCount = failedItems.size,
            lastBackupTimestamp = backedUpItems.maxOfOrNull { it.backedUpAt ?: 0L },
            currentUploadingFileName = currentFile,
            currentProgress = progress,
            quotaError = quotaErr,
            folderStats = folderStats
        )
    }

    suspend fun performScanAndBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext Result.success(Unit)
        _isProcessing.value = true
        _quotaError.value = null

        try {
            val settings = userPreferencesRepository.fullSettingsFlow.first().vaultSettings
            if (!settings.automaticBackupEnabled) {
                Log.d(tag, "Automatic backup is disabled in settings")
                _isProcessing.value = false
                return@withContext Result.success(Unit)
            }

            // 1. Scan Local Folders
            scanFolders(settings)

            // 2. Process Pending Queue
            if (!_isPaused.value) {
                processQueue(settings)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Backup execution failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            _isProcessing.value = false
            _currentUploadingFile.value = null
            _currentProgress.value = 0f
        }
    }

    private suspend fun scanFolders(settings: UserVaultSettings) {
        val enabledFolders = settings.backupFolders.filter { it.enabled }
        Log.i(tag, "Scanning ${enabledFolders.size} enabled folders for automatic backup")

        for (folderConfig in enabledFolders) {
            val folderDir = File(folderConfig.path)
            if (!folderDir.exists() || !folderDir.isDirectory) {
                Log.w(tag, "Folder path does not exist or is not directory: ${folderConfig.path}")
                continue
            }

            val files = folderDir.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile && !it.isHidden && it.length() > 0 }
                .toList()

            for (file in files) {
                try {
                    val localPath = file.absolutePath
                    val existingByPath = vaultBackupDao.getByLocalPath(localPath)

                    if (existingByPath != null) {
                        // Check if file was modified since last record
                        if (existingByPath.lastModified < file.lastModified()) {
                            // File modified - reset status to PENDING
                            vaultBackupDao.updateItem(
                                existingByPath.copy(
                                    fileSize = file.length(),
                                    lastModified = file.lastModified(),
                                    fileHash = calculateFileHash(file),
                                    status = BackupStatus.PENDING.name,
                                    errorMessage = null
                                )
                            )
                        }
                        continue
                    }

                    // Calculate stable content hash
                    val hash = calculateFileHash(file)

                    // Check if file already exists in Vault or backup records by content hash
                    val existingByHash = vaultBackupDao.getByFileHash(hash)
                    val existingInVault = fileRepository.getFileByHash(hash)

                    if (existingByHash?.status == BackupStatus.SUCCESS.name || existingInVault != null) {
                        // Already backed up or present in Vault - mark as SUCCESS to prevent duplicate upload
                        val entity = VaultBackupItemEntity(
                            id = existingByHash?.id ?: UUID.randomUUID().toString(),
                            folderId = folderConfig.id,
                            localPath = localPath,
                            fileName = file.name,
                            fileSize = file.length(),
                            lastModified = file.lastModified(),
                            fileHash = hash,
                            mimeType = getMimeType(file),
                            status = BackupStatus.SUCCESS.name,
                            vaultItemId = existingInVault?.id,
                            backedUpAt = System.currentTimeMillis()
                        )
                        vaultBackupDao.insertItem(entity)
                    } else {
                        // New file - insert as PENDING
                        val entity = VaultBackupItemEntity(
                            id = UUID.randomUUID().toString(),
                            folderId = folderConfig.id,
                            localPath = localPath,
                            fileName = file.name,
                            fileSize = file.length(),
                            lastModified = file.lastModified(),
                            fileHash = hash,
                            mimeType = getMimeType(file),
                            status = BackupStatus.PENDING.name
                        )
                        vaultBackupDao.insertItem(entity)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error processing file ${file.name}: ${e.message}")
                }
            }
        }
    }

    private suspend fun processQueue(settings: UserVaultSettings) {
        // Check Network Rule
        if (settings.backupWifiOnly && !settings.backupAllowMobileData && !connectivityObserver.isWifiConnected()) {
            Log.i(tag, "Wi-Fi required for backup, currently disconnected. Uploads paused.")
            return
        }

        // Check Power Rule
        if (settings.backupOnlyWhileCharging && !isDeviceCharging()) {
            Log.i(tag, "Charging required for backup, currently unplugged. Uploads paused.")
            return
        }

        val pendingItems = vaultBackupDao.getPendingAndFailedItems()
        if (pendingItems.isEmpty()) {
            Log.d(tag, "No pending files to upload")
            return
        }

        Log.i(tag, "Processing upload queue: ${pendingItems.size} items pending")

        val profile = authRepository.currentUserProfile.first()
        val limit = profile?.storageLimit ?: SubscriptionPlan.FREE.storageLimit
        val used = profile?.storageUsed ?: 0L
        var availableStorage = limit - used

        for (itemEntity in pendingItems) {
            if (_isPaused.value) break

            val file = File(itemEntity.localPath)
            if (!file.exists()) {
                // Local file was removed or deleted by user
                vaultBackupDao.updateStatus(itemEntity.id, BackupStatus.FAILED.name, "Local file no longer exists")
                continue
            }

            // Quota Enforcement Check
            if (file.length() > availableStorage) {
                val reqMb = file.length() / (1024 * 1024)
                val availMb = availableStorage / (1024 * 1024)
                _quotaError.value = "Insufficient My Vault storage limit. Required: ${reqMb}MB, Available: ${availMb}MB."
                Log.w(tag, "Upload halted due to storage quota: file size=${file.length()}, available=$availableStorage")
                vaultBackupDao.updateStatus(itemEntity.id, BackupStatus.QUOTA_EXCEEDED.name, "Storage quota exceeded")
                break
            }

            _currentUploadingFile.value = file.name
            _currentProgress.value = 0f
            vaultBackupDao.updateStatus(itemEntity.id, BackupStatus.UPLOADING.name)

            val result = vaultRepository.uploadFile(file, parentId = null) { uploaded, total ->
                if (total > 0) {
                    _currentProgress.value = uploaded.toFloat() / total
                }
            }

            if (result.isSuccess) {
                val vaultItem = result.getOrThrow()
                vaultBackupDao.updateStatus(
                    id = itemEntity.id,
                    status = BackupStatus.SUCCESS.name,
                    errorMessage = null,
                    vaultItemId = vaultItem.id,
                    backedUpAt = System.currentTimeMillis()
                )
                availableStorage -= file.length()
                Log.i(tag, "Backup upload successful for ${file.name} -> VaultItem ${vaultItem.id}")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Upload failed"
                vaultBackupDao.updateStatus(itemEntity.id, BackupStatus.FAILED.name, err)
                Log.e(tag, "Backup upload failed for ${file.name}: $err")
            }
        }
    }

    fun triggerImmediateBackup() {
        scope.launch {
            _isPaused.value = false
            performScanAndBackup()
        }
    }

    fun pauseBackup() {
        _isPaused.value = true
        NotesLogger.i(tag, "Automatic backup paused by user")
    }

    fun resumeBackup() {
        _isPaused.value = false
        triggerImmediateBackup()
    }

    fun retryFailedItems() {
        scope.launch {
            _isPaused.value = false
            performScanAndBackup()
        }
    }

    private fun isDeviceCharging(): Boolean {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) { true }
    }

    private fun calculateFileHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val length = file.length()
            val lastModified = file.lastModified()
            digest.update("$length:$lastModified:${file.name}".toByteArray())

            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                var totalRead = 0L
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                    totalRead += read
                    if (length > 10 * 1024 * 1024 && totalRead > 2 * 1024 * 1024) {
                        break
                    }
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "${file.name}_${file.length()}_${file.lastModified()}"
        }
    }

    private fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
