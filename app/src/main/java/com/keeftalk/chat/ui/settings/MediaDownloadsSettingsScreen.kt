package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDownloadsSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.chatSettings.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Media & Downloads", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Auto-Download") {
                    AutoDownloadItem(
                        title = "On Mobile Data",
                        selected = settings.autoDownloadMobile,
                        onSelectedChange = { viewModel.updateSetting("auto_download_mobile", it) }
                    )
                    AutoDownloadItem(
                        title = "On Wi-Fi",
                        selected = settings.autoDownloadWifi,
                        onSelectedChange = { viewModel.updateSetting("auto_download_wifi", it) }
                    )
                    AutoDownloadItem(
                        title = "When Roaming",
                        selected = settings.autoDownloadRoaming,
                        onSelectedChange = { viewModel.updateSetting("auto_download_roaming", it) },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Quality Controls") {
                    QualityItem(
                        title = "Photo Upload Quality",
                        subtitle = "Higher quality uses more data",
                        selected = settings.uploadPhotoQuality,
                        options = listOf("ORIGINAL", "HIGH", "MEDIUM", "COMPRESSED"),
                        onSelectedChange = { viewModel.updateSetting("upload_photo_quality", it) }
                    )
                    QualityItem(
                        title = "Video Upload Quality",
                        subtitle = "Compression levels for sending",
                        selected = settings.uploadVideoQuality,
                        options = listOf("ORIGINAL", "HD", "STANDARD", "COMPRESSED"),
                        onSelectedChange = { viewModel.updateSetting("upload_video_quality", it) },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Playback & Gallery") {
                    SettingsToggleItem(
                        icon = icons.sparkles, // Using sparkles for GIFs
                        title = "Auto-play GIFs",
                        checked = settings.autoplayGifs,
                        onCheckedChange = { viewModel.updateSetting("autoplay_gifs", it) }
                    )
                    SettingsToggleItem(
                        icon = icons.bell, // Using bell for videos
                        title = "Auto-play Videos",
                        checked = settings.autoplayVideos,
                        onCheckedChange = { viewModel.updateSetting("autoplay_videos", it) }
                    )
                    SettingsToggleItem(
                        icon = icons.user, // Using user for gallery
                        title = "Save to Gallery",
                        subtitle = "Media appears in your phone's photos app",
                        checked = settings.saveToGallery,
                        onCheckedChange = { viewModel.updateSetting("save_to_gallery", it) },
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
fun AutoDownloadItem(
    title: String,
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    showDivider: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val types = listOf("PHOTO", "VIDEO", "AUDIO", "DOCUMENT")
    val icons = com.keeftalk.chat.ui.theme.LocalAppIcons.current

    SettingsItem(
        icon = icons.note, // Using note for storage icon
        title = title,
        subtitle = if (selected.isEmpty()) "Never" else selected.joinToString(", ") { it.lowercase().replaceFirstChar { it.uppercase() } },
        onClick = { showDialog = true },
        showDivider = showDivider
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    types.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelected = if (selected.contains(type)) selected - type else selected + type
                                    onSelectedChange(newSelected)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Checkbox(checked = selected.contains(type), onCheckedChange = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(type.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun QualityItem(
    title: String,
    subtitle: String,
    selected: String,
    options: List<String>,
    onSelectedChange: (String) -> Unit,
    showDivider: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val icons = com.keeftalk.chat.ui.theme.LocalAppIcons.current

    SettingsItem(
        icon = icons.palette,
        title = title,
        subtitle = "$subtitle • ${selected.lowercase().replaceFirstChar { it.uppercase() }}",
        onClick = { showDialog = true },
        showDivider = showDivider
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectedChange(option)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = selected == option, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(option.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

