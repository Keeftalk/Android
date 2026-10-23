package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SecurityPrivacyCenterScreen(
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToTwoFactor: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { SettingsHeader(title = "Security & Privacy Center", onBack = onBack) },
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

            item {
                Text(
                    text = "Data Protection & Encryption",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingsSection(title = "Backup & Vault") {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restore",
                        subtitle = "Single encrypted .ktb backup for books, notes, secrets, and chat data.",
                        onClick = onNavigateToBackupRestore
                    )
                    SettingsItem(
                        icon = Icons.Default.Shield,
                        title = "End-to-End Encryption (E2EE)",
                        subtitle = "Manage conversation keys and hardware-backed Keystore.",
                        onClick = onNavigateToTwoFactor,
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Privacy & Sessions") {
                    SettingsItem(
                        icon = Icons.Default.Devices,
                        title = "Active Sessions & Security Activity",
                        subtitle = "Monitor authorized devices, active logins, and audit logs.",
                        onClick = {}
                    )
                    SettingsItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric App & Vault Lock",
                        subtitle = "Secure sensitive data with fingerprint or face recognition.",
                        onClick = {},
                        showDivider = false
                    )
                }
            }
        }
    }
}
