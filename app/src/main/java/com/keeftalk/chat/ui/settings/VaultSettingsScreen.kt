package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun VaultSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val vault = settings.vaultSettings

    Scaffold(
        topBar = { SettingsHeader(title = "My Vault", onBack = onBack) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsSection(title = "Security") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.lock,
                        title = "Auto-lock Vault",
                        checked = vault.autoLockVault,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(autoLockVault = v) } }
                    )
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Require Biometric Auth",
                        checked = vault.requireBiometricAuth,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(requireBiometricAuth = v) } }
                    )
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Screenshot Protection",
                        checked = vault.screenshotProtection,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(screenshotProtection = v) } }
                    )
                }
            }
            item {
                SettingsSection(title = "Files") {
                    SettingsToggleItem(
                        icon = com.keeftalk.chat.ui.theme.LocalAppIcons.current.help,
                        title = "Generate Thumbnails",
                        checked = vault.generateThumbnails,
                        onCheckedChange = { val v = it; viewModel.updateVaultSettings { it.copy(generateThumbnails = v) } }
                    )
                }
            }
        }
    }
}
