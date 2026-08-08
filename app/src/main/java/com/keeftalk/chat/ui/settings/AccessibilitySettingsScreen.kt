package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.MainViewModel
import com.keeftalk.chat.ui.theme.LocalAppIcons
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = "Accessibility", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Night Tools") {
                    SettingsToggleItem(
                        icon = icons.moon ?: Icons.Default.Add, // Fallback if moon not in icons
                        title = "Extra Night Mode",
                        subtitle = "Reduces screen brightness with an overlay",
                        checked = prefs.extraNightModeEnabled,
                        onCheckedChange = { viewModel.toggleExtraNightMode() }
                    )
                    
                    if (prefs.extraNightModeEnabled) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Dim Intensity: ${(prefs.nightModeOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = prefs.nightModeOpacity,
                                onValueChange = { viewModel.updateNightModeOpacity(it) },
                                valueRange = 0.25f..0.70f,
                                steps = 2 // 25%, 40%, 55%, 70%
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "UI Scaling") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Current UI Scale", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${(prefs.uiScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            IconButton(onClick = { viewModel.resetUiScale() }) {
                                Icon(Icons.Default.Restore, contentDescription = "Reset")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FilledIconButton(
                                onClick = { viewModel.decreaseUiScale() },
                                modifier = Modifier.weight(1f),
                                enabled = prefs.uiScale > 0.80f
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            
                            FilledIconButton(
                                onClick = { viewModel.increaseUiScale() },
                                modifier = Modifier.weight(1f),
                                enabled = prefs.uiScale < 1.30f
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scaling affects fonts, icons, and layout spacing across the entire app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
