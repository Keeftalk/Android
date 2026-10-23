package tn.keeftalk.chat.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.service.BackupEngine
import tn.keeftalk.chat.domain.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRestoreScreen(
    backupEngine: BackupEngine,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isRestoreMode by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showPermissionExplanationDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember {
        mutableStateOf(BackupCategory.entries.map { it.name }.toSet())
    }
    var backupSchedule by remember { mutableStateOf(BackupSchedule.DISABLED) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "import_backup.ktb")
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    pendingRestoreFile = tempFile
                    isRestoreMode = true
                    showPasswordDialog = true
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open backup file: ${e.message}")
                }
            }
        }
    }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Handled via standard permissions on older SDKs
        }
    }

    Scaffold(
        topBar = { SettingsHeader(title = "Backup & Restore", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. Dashboard Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Single Encrypted .ktb Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Badge(
                                containerColor = if (backupEngine.currentBackupFile.exists()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ) {
                                Text(if (backupEngine.currentBackupFile.exists()) "Ready" else "No Backup")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                        val lastTime = if (backupEngine.currentBackupFile.exists()) dateFormat.format(Date(backupEngine.currentBackupFile.lastModified())) else "Never"
                        val fileSize = if (backupEngine.currentBackupFile.exists()) "${backupEngine.currentBackupFile.length() / 1024} KB" else "0 KB"

                        Text("Last Backup: $lastTime", style = MaterialTheme.typography.bodyMedium)
                        Text("File Size: $fileSize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Storage Folder: Storage/Keeftalk/backups/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("Active Categories: ${selectedCategories.size} / ${BackupCategory.entries.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 2. Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasStoragePermission()) {
                                showPermissionExplanationDialog = true
                            } else {
                                isRestoreMode = false
                                showPasswordDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back Up Now")
                    }

                    OutlinedButton(
                        onClick = {
                            if (!hasStoragePermission()) {
                                showPermissionExplanationDialog = true
                            } else {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore")
                    }
                }
            }

            // 3. Backup Settings Section
            item {
                Text(
                    text = "Backup Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingsSection(title = "Configuration") {
                    SettingsItem(
                        icon = Icons.Default.Category,
                        title = "Select Categories",
                        subtitle = "${selectedCategories.size} categories included",
                        onClick = { showCategoriesDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Schedule,
                        title = "Automatic Backup Schedule",
                        subtitle = backupSchedule.title,
                        onClick = {
                            backupSchedule = when (backupSchedule) {
                                BackupSchedule.DISABLED -> BackupSchedule.EVERY_12_HOURS
                                BackupSchedule.EVERY_12_HOURS -> BackupSchedule.EVERY_24_HOURS
                                BackupSchedule.EVERY_24_HOURS -> BackupSchedule.EVERY_3_DAYS
                                BackupSchedule.EVERY_3_DAYS -> BackupSchedule.DISABLED
                            }
                        },
                        showDivider = false
                    )
                }
            }
        }
    }

    // Permission Explanation Dialog
    if (showPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanationDialog = false },
            title = { Text("📁 Storage Access Permission Required") },
            text = {
                Text(
                    "Keeftalk needs 'All Files Access' permission to save, import, and restore your encrypted .ktb backup files directly in Storage/Keeftalk/backups/.\n\n" +
                    "This ensures your backups are stored in ordinary storage so you can easily find, share, and manage them anytime.\n\n" +
                    "Please tap 'Grant Permission' below to allow access in the system settings screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanationDialog = false
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isFirstBackup = !backupEngine.currentBackupFile.exists()

    // Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { 
                Text(
                    when {
                        isRestoreMode -> "Enter Backup Password"
                        isFirstBackup -> "🔒 Establish Backup Password (Identity Verification)"
                        else -> "🔐 Authorize Backup Password"
                    }
                ) 
            },
            text = {
                Column {
                    Text(
                        when {
                            isRestoreMode -> "Enter the password used to encrypt this .ktb backup."
                            isFirstBackup -> "This is your first backup. To verify your identity and protect your personal data from unauthorized access or theft, please establish a secure backup password. This password will be required for all future backups and restorations."
                            else -> "Please enter your backup password to verify your identity and authorize the new backup."
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pwd = passwordInput
                        passwordInput = ""
                        showPasswordDialog = false
                        scope.launch {
                            if (isRestoreMode && pendingRestoreFile != null) {
                                val res = backupEngine.restoreBackup(pendingRestoreFile!!, pwd, "current_user_account")
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Backup restored successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("Restore failed: ${res.exceptionOrNull()?.message}")
                                }
                                pendingRestoreFile = null
                            } else {
                                val res = backupEngine.performBackup(pwd, selectedCategories, "current_user_account")
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Backup created successfully in Storage/Keeftalk/backups/!")
                                } else {
                                    snackbarHostState.showSnackbar("Backup failed: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    },
                    enabled = passwordInput.isNotEmpty()
                ) {
                    Text(if (isRestoreMode) "Restore" else "Confirm & Back Up")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Categories Dialog
    if (showCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showCategoriesDialog = false },
            title = { Text("Select Backup Categories") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(BackupCategory.entries.size) { index ->
                        val cat = BackupCategory.entries[index]
                        val isSelected = selectedCategories.contains(cat.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + cat.name
                                    } else {
                                        selectedCategories - cat.name
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCategoriesDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
