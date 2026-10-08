package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.data.service.VaultBackupManager
import tn.keeftalk.chat.domain.model.FolderBackupConfig
import tn.keeftalk.chat.domain.model.OverallBackupStats
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AutomaticBackupSettingsScreen(
    viewModel: SettingsViewModel,
    backupManager: VaultBackupManager,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val vaultSettings = settings.vaultSettings
    val backupStats by backupManager.overallStatsFlow.collectAsState(initial = OverallBackupStats())

    var showAddFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsHeader(title = "Automatic Backup", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. Master Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automatic Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automatically back up selected device folders to My Vault with zero-knowledge encryption.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = vaultSettings.automaticBackupEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.updateVaultSettings { it.copy(automaticBackupEnabled = enabled) }
                                    if (enabled) backupManager.triggerImmediateBackup()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Back Up All Photos & Videos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Conveniently include all common device photo and video folders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = vaultSettings.backupAllPhotosAndVideos,
                                onCheckedChange = { allMedia ->
                                    viewModel.updateVaultSettings { current ->
                                        val updatedFolders = current.backupFolders.map { f ->
                                            if (f.id in listOf("camera", "screenshots", "whatsapp_media")) {
                                                f.copy(enabled = allMedia)
                                            } else f
                                        }
                                        current.copy(
                                            backupAllPhotosAndVideos = allMedia,
                                            backupFolders = updatedFolders
                                        )
                                    }
                                    if (allMedia) backupManager.triggerImmediateBackup()
                                }
                            )
                        }
                    }
                }
            }

            // 2. Folders to Back Up
            item {
                Text(
                    text = "Folders to back up",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(vaultSettings.backupFolders) { folder ->
                val fStats = backupStats.folderStats.find { it.config.id == folder.id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (folder.id) {
                                "camera" -> Icons.Default.PhotoCamera
                                "screenshots" -> Icons.Default.Smartphone
                                "downloads" -> Icons.Default.Download
                                "whatsapp_media" -> Icons.Default.Chat
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = if (folder.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = folder.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (fStats != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${fStats.totalFilesDetected} files (${formatSize(fStats.totalBytesDetected)}) • ${fStats.statusText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (fStats.failedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                                if (fStats.lastBackupTime != null) {
                                    Text(
                                        text = "Last backup: ${formatBackupTimestamp(fStats.lastBackupTime)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        if (!folder.isPreset) {
                            IconButton(onClick = {
                                viewModel.updateVaultSettings { current ->
                                    current.copy(backupFolders = current.backupFolders.filter { it.id != folder.id })
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove folder", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Switch(
                            checked = folder.enabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateVaultSettings { current ->
                                    val updated = current.backupFolders.map {
                                        if (it.id == folder.id) it.copy(enabled = enabled) else it
                                    }
                                    current.copy(backupFolders = updated)
                                }
                                if (enabled) backupManager.triggerImmediateBackup()
                            }
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showAddFolderDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ Add folder")
                }
            }

            // 3. Backup Conditions
            item {
                Text(
                    text = "Backup conditions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wi-Fi only", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = vaultSettings.backupWifiOnly,
                                onCheckedChange = { wifiOnly ->
                                    viewModel.updateVaultSettings { it.copy(backupWifiOnly = wifiOnly) }
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Allow mobile data", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = vaultSettings.backupAllowMobileData,
                                onCheckedChange = { allowMobile ->
                                    viewModel.updateVaultSettings { it.copy(backupAllowMobileData = allowMobile) }
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Only while charging", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = vaultSettings.backupOnlyWhileCharging,
                                onCheckedChange = { chargingOnly ->
                                    viewModel.updateVaultSettings { it.copy(backupOnlyWhileCharging = chargingOnly) }
                                }
                            )
                        }
                    }
                }
            }

            // 4. Backup Status
            item {
                Text(
                    text = "Backup status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (backupStats.quotaError != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = backupStats.quotaError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Files backed up:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${backupStats.totalBackedUpCount} files (${formatSize(backupStats.totalBackedUpBytes)})",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pending:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${backupStats.totalPendingCount} files",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Failed:", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${backupStats.totalFailedCount} files",
                                    color = if (backupStats.totalFailedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                if (backupStats.totalFailedCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { backupManager.retryFailedItems() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last backup:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (backupStats.lastBackupTimestamp != null && backupStats.lastBackupTimestamp!! > 0) {
                                    formatBackupTimestamp(backupStats.lastBackupTimestamp!!)
                                } else "Never",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (backupStats.isRunning && backupStats.currentUploadingFileName != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Uploading: ${backupStats.currentUploadingFileName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { backupStats.currentProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 5. Actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { backupManager.triggerImmediateBackup() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back Up Now")
                    }

                    OutlinedButton(
                        onClick = {
                            if (backupStats.isPaused) backupManager.resumeBackup() else backupManager.pauseBackup()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (backupStats.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (backupStats.isPaused) "Resume" else "Pause")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { name, path ->
                val newConfig = FolderBackupConfig(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    path = path,
                    enabled = true,
                    isPreset = false
                )
                viewModel.updateVaultSettings { current ->
                    current.copy(backupFolders = current.backupFolders + newConfig)
                }
                showAddFolderDialog = false
                backupManager.triggerImmediateBackup()
            }
        )
    }
}

@Composable
private fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, path: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Folder to Back Up") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name (e.g. Telegram Media)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Local Folder Path (e.g. /storage/emulated/0/Pictures/Telegram)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && path.isNotBlank()) onConfirm(name, path) },
                enabled = name.isNotBlank() && path.isNotBlank()
            ) {
                Text("Add Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}

private fun formatBackupTimestamp(timeMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(timeMs))
    } catch (_: Exception) {
        "Unknown"
    }
}
