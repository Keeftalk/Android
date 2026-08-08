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
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextAccessibilitySettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.chatSettings.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Text & Accessibility", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Font Customization") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sample Message Text",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                                fontSize = settings.fontSize.sp
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Text("Font Size: ${settings.fontSize}sp", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.fontSize.toFloat(),
                            onValueChange = { viewModel.updateSetting("font_size", it.toInt()) },
                            valueRange = 12f..24f,
                            steps = 3
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Font Scale: ${String.format(Locale.getDefault(), "%.2f", settings.fontScale)}x", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.fontScale,
                            onValueChange = { viewModel.updateSetting("font_scale", it) },
                            valueRange = 0.8f..1.4f
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Reading Experience") {
                    SettingsToggleItem(
                        icon = icons.edit,
                        title = "Bold Text",
                        subtitle = "Increase weight of all text",
                        checked = settings.boldText,
                        onCheckedChange = { viewModel.updateSetting("bold_text", it) }
                    )
                    SettingsToggleItem(
                        icon = icons.palette,
                        title = "High Contrast Mode",
                        subtitle = "Stronger color distinctions",
                        checked = settings.highContrast,
                        onCheckedChange = { viewModel.updateSetting("high_contrast", it) }
                    )
                    SettingsToggleItem(
                        icon = icons.sparkles,
                        title = "Reduced Motion",
                        subtitle = "Minimize transitions and effects",
                        checked = settings.reducedMotion,
                        onCheckedChange = { viewModel.updateSetting("reduced_motion", it) },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "External Links") {
                    val previewSizes = listOf("COMPACT", "NORMAL")
                    previewSizes.forEachIndexed { index, size ->
                        SettingsItem(
                            icon = icons.info,
                            title = "${size.lowercase().replaceFirstChar { it.uppercase() }} Previews",
                            trailing = {
                                RadioButton(selected = settings.linkPreviewSize == size, onClick = { viewModel.updateSetting("link_preview_size", size) })
                            },
                            onClick = { viewModel.updateSetting("link_preview_size", size) },
                            showDivider = index != previewSizes.size - 1
                        )
                    }
                }
            }
        }
    }
}

