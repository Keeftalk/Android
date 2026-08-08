package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBehaviorSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.chatSettings.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Chat Behavior", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Messaging") {
                    val behaviors = listOf("SEND" to "Send Message", "NEW_LINE" to "Add New Line")
                    behaviors.forEachIndexed { index, (value, label) ->
                        SettingsItem(
                            icon = icons.chat,
                            title = "Enter Key: $label",
                            trailing = {
                                RadioButton(selected = settings.enterKeyBehavior == value, onClick = { viewModel.updateSetting("enter_key_behavior", value) })
                            },
                            onClick = { viewModel.updateSetting("enter_key_behavior", value) },
                            showDivider = index != behaviors.size - 1
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Gestures") {
                    var showSwipeLeftDialog by remember { mutableStateOf(false) }
                    var showSwipeRightDialog by remember { mutableStateOf(false) }
                    var showDoubleTapDialog by remember { mutableStateOf(false) }
                    var showReactionDialog by remember { mutableStateOf(false) }

                    SettingsItem(
                        icon = icons.back,
                        title = "Swipe Left",
                        subtitle = settings.swipeLeftAction.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showSwipeLeftDialog = true }
                    )
                    SettingsItem(
                        icon = icons.back,
                        title = "Swipe Right",
                        subtitle = settings.swipeRightAction.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showSwipeRightDialog = true }
                    )
                    SettingsItem(
                        icon = icons.heart,
                        title = "Double Tap",
                        subtitle = settings.doubleTapAction.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showDoubleTapDialog = true }
                    )
                    SettingsItem(
                        icon = icons.emoji,
                        title = "Quick Reaction",
                        subtitle = settings.defaultReaction,
                        onClick = { showReactionDialog = true },
                        showDivider = false
                    )

                    if (showSwipeLeftDialog) {
                        ActionPickerDialog(
                            title = "Swipe Left Action",
                            options = listOf("ARCHIVE", "DELETE", "MUTE"),
                            selected = settings.swipeLeftAction,
                            onDismiss = { showSwipeLeftDialog = false },
                            onSelect = { viewModel.updateSetting("swipe_left_action", it); showSwipeLeftDialog = false }
                        )
                    }
                    if (showSwipeRightDialog) {
                        ActionPickerDialog(
                            title = "Swipe Right Action",
                            options = listOf("REPLY", "MARK_UNREAD", "NONE"),
                            selected = settings.swipeRightAction,
                            onDismiss = { showSwipeRightDialog = false },
                            onSelect = { viewModel.updateSetting("swipe_right_action", it); showSwipeRightDialog = false }
                        )
                    }
                    if (showDoubleTapDialog) {
                        ActionPickerDialog(
                            title = "Double Tap Action",
                            options = listOf("REACT", "REPLY", "COPY", "NONE"),
                            selected = settings.doubleTapAction,
                            onDismiss = { showDoubleTapDialog = false },
                            onSelect = { viewModel.updateSetting("double_tap_action", it); showDoubleTapDialog = false }
                        )
                    }
                    if (showReactionDialog) {
                        ActionPickerDialog(
                            title = "Quick Reaction Emoji",
                            options = listOf("❤️", "👍", "🔥", "😂", "😮", "😢"),
                            selected = settings.defaultReaction,
                            onDismiss = { showReactionDialog = false },
                            onSelect = { viewModel.updateSetting("default_reaction", it); showReactionDialog = false }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Automatic Features") {
                    SettingsToggleItem(
                        icon = icons.info,
                        title = "Link Previews",
                        subtitle = "Fetch info for shared links",
                        checked = settings.linkPreviewsEnabled,
                        onCheckedChange = { viewModel.updateSetting("link_previews_enabled", it) }
                    )
                    
                    var showTranslationDialog by remember { mutableStateOf(false) }
                    SettingsItem(
                        icon = Icons.Default.Translate,
                        title = "Auto Translation",
                        subtitle = settings.autoTranslateMode.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showTranslationDialog = true },
                        showDivider = false
                    )

                    if (showTranslationDialog) {
                        ActionPickerDialog(
                            title = "Translation Mode",
                            options = listOf("DISABLED", "INCOMING", "OUTGOING", "BOTH"),
                            selected = settings.autoTranslateMode,
                            onDismiss = { showTranslationDialog = false },
                            onSelect = { viewModel.updateSetting("auto_translate_mode", it); showTranslationDialog = false }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ActionPickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(option.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
