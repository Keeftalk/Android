package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun VaultSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAutomaticBackupClick: () -> Unit = {}
) {
    val settings by viewModel.fullSettings.collectAsState()
    val vault = settings.vaultSettings

    Scaffold(
        topBar = { SettingsHeader(title = "My Vault", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsSection(title = "Automatic Backup") {
                    SettingsItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Automatic Folder Backup",
                        subtitle = if (vault.automaticBackupEnabled) "ON • ${vault.backupFolders.count { it.enabled }} folder(s) active" else "OFF • Tap to configure",
                        onClick = onAutomaticBackupClick
                    )
                }
            }
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.lock,
                        title = "Auto-lock Vault",
                        checked = vault.autoLockVault,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(autoLockVault = v) } }
                    )
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Require Biometric Auth",
                        checked = vault.requireBiometricAuth,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(requireBiometricAuth = v) } }
                    )
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Screenshot Protection",
                        checked = vault.screenshotProtection,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(screenshotProtection = v) } }
                    )
                }
            }
            item {
                SettingsSection(title = "Files") {
                    SettingsToggleItem(
                        icon = tn.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Generate Thumbnails",
                        checked = vault.generateThumbnails,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(generateThumbnails = v) } }
                    )
                }
            }
        }
    }
}
