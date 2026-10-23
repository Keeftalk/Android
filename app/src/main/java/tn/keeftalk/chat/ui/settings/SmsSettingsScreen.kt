package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.ui.theme.LocalAppIcons

@Composable
fun SmsSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val sms = settings.smsSettings
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = { SettingsHeader(title = "My SMS/MMS", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                tn.keeftalk.chat.ui.settings.components.FeatureCloudSyncCard(
                    featureId = tn.keeftalk.chat.domain.model.entitlement.FeatureId.SMS_MMS,
                    description = "Sync your SMS and MMS data across your devices.",
                    onEnableClick = { },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SettingsSection(title = "Messaging & Integration") {
                    SettingsToggleItem(
                        icon = icons.chat,
                        title = "SMS Bridge Integration",
                        subtitle = "Manage traditional SMS/MMS within secure ecosystem",
                        checked = sms.smsBridgeEnabled,
                        onCheckedChange = { v -> viewModel.updateSmsSettings { it.copy(smsBridgeEnabled = v) } }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Delivery Reports",
                        subtitle = "Request delivery confirmation for sent messages",
                        checked = sms.deliveryReportsEnabled,
                        onCheckedChange = { v -> viewModel.updateSmsSettings { it.copy(deliveryReportsEnabled = v) } }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Download,
                        title = "MMS Auto-Download",
                        subtitle = "Automatically fetch multimedia messages",
                        checked = sms.mmsAutoDownload,
                        onCheckedChange = { v -> viewModel.updateSmsSettings { it.copy(mmsAutoDownload = v) } },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Security & Storage") {
                    SettingsToggleItem(
                        icon = icons.lock,
                        title = "Encrypt SMS Threads",
                        subtitle = "Store messages with local encryption",
                        checked = sms.encryptSmsThreads,
                        onCheckedChange = { v -> viewModel.updateSmsSettings { it.copy(encryptSmsThreads = v) } },
                        showDivider = false
                    )
                }
            }
        }
    }
}
