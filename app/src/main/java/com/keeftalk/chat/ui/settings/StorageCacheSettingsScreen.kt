package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons
import android.text.format.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCacheSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit
) {
    val cacheSize by viewModel.cacheSize.collectAsState()
    val icons = LocalAppIcons.current
    val context = androidx.compose.ui.platform.LocalContext.current

    fun formatSize(size: Long): String = Formatter.formatShortFileSize(context, size)

    Scaffold(
        topBar = {
            SettingsHeader(title = "Storage & Cache", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Storage Usage") {
                    UsageRow("Images", formatSize(cacheSize["images"] ?: 0), icons.image)
                    UsageRow("Videos", formatSize(cacheSize["videos"] ?: 0), icons.bell) // Using bell as placeholder for video
                    UsageRow("Documents", formatSize(cacheSize["files"] ?: 0), icons.note)
                    UsageRow("Total Cache", formatSize(cacheSize["total"] ?: 0), icons.sparkles, isTotal = true)
                }
            }

            item {
                SettingsSection(title = "Maintenance") {
                    SettingsItem(
                        icon = icons.delete,
                        title = "Clear Media Cache",
                        subtitle = "Thumbnails and temporary files",
                        onClick = { viewModel.clearCache(listOf("images", "videos")) }
                    )
                    SettingsItem(
                        icon = icons.delete,
                        title = "Clear Database Logs",
                        subtitle = "Optimizes local performance",
                        onClick = { viewModel.clearCache(listOf("others")) }
                    )
                    SettingsItem(
                        icon = icons.delete,
                        title = "Deep Reset Cache",
                        subtitle = "Redownloads everything from cloud",
                        onClick = { viewModel.clearCache(listOf("all")) },
                        contentColor = MaterialTheme.colorScheme.error,
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
fun UsageRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isTotal: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
            )
        }
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

