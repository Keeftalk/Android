package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppearanceSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.chatSettings.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Appearance", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ChatPreview(settings)
            }

            item {
                SettingsSection(title = "Color Themes") {
                    val themes = listOf(
                        Triple("SYSTEM", "System Default", icons.palette),
                        Triple("LIGHT", "Light Mode", icons.sparkles),
                        Triple("DARK", "Dark Mode", icons.lock),
                        Triple("AMOLED", "Pure Black", icons.shield),
                        Triple("DAY", "Day Blue", icons.sparkles),
                        Triple("PINKY", "Rosa (Pink)", icons.sparkles),
                        Triple("MASCULINE", "Alpha (Slate)", icons.shield)
                    )
                    
                    themes.forEachIndexed { index, (id, label, icon) ->
                        SettingsItem(
                            icon = icon,
                            title = label,
                            trailing = {
                                RadioButton(selected = settings.theme == id, onClick = { viewModel.updateSetting("theme", id) })
                            },
                            onClick = { viewModel.updateSetting("theme", id) },
                            showDivider = index != themes.size - 1
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Chat Bubbles") {
                    val styles = listOf("ROUNDED", "MODERN", "COMPACT")
                    styles.forEachIndexed { index, style ->
                        SettingsItem(
                            icon = icons.chat,
                            title = style.lowercase().replaceFirstChar { it.uppercase() },
                            trailing = {
                                RadioButton(selected = settings.bubbleStyle == style, onClick = { viewModel.updateSetting("bubble_style", style) })
                            },
                            onClick = { viewModel.updateSetting("bubble_style", style) },
                            showDivider = true
                        )
                    }
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bubble Corner Radius", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.bubbleRadius.toFloat(),
                            onValueChange = { viewModel.updateSetting("bubble_radius", it.toInt()) },
                            valueRange = 0f..28f,
                            steps = 7
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Animations") {
                    SettingsToggleItem(
                        icon = icons.sparkles,
                        title = "Enable Animations",
                        subtitle = "Smooth transitions and bubble effects",
                        checked = settings.animationsEnabled,
                        onCheckedChange = { viewModel.updateSetting("animations_enabled", it) },
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
fun ChatPreview(settings: com.keeftalk.chat.domain.model.UserChatSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(
            "Preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewBubble(
                text = "Hey! Check out this new redesign.",
                isMe = false,
                settings = settings
            )
            PreviewBubble(
                text = "Looks amazing! I love the smooth animations.",
                isMe = true,
                settings = settings
            )
        }
    }
}

@Composable
fun PreviewBubble(
    text: String,
    isMe: Boolean,
    settings: com.keeftalk.chat.domain.model.UserChatSettings
) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    
    val radius = settings.bubbleRadius.dp
    val shape = when (settings.bubbleStyle) {
        "ROUNDED" -> RoundedCornerShape(radius)
        "MODERN" -> RoundedCornerShape(
            topStart = if (isMe) radius else 4.dp,
            topEnd = if (isMe) 4.dp else radius,
            bottomStart = radius,
            bottomEnd = radius
        )
        else -> RoundedCornerShape(8.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = color,
            shape = shape,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}


@Composable
fun ColorPickerDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#F0F2F5", "#FFFFFF", "#E3F2FD", "#F1F8E9", "#FFF3E0",
        "#FCE4EC", "#F3E5F5", "#E8EAF6", "#E0F2F1", "#F9FBE7"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column {
                SettingsSection(title = "Presets") {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                        modifier = Modifier.height(120.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colors.size) { index ->
                            val colorHex = colors[index]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                                    .clickable { onColorSelected(colorHex) }
                                    .then(
                                        if (initialColor == colorHex)
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
