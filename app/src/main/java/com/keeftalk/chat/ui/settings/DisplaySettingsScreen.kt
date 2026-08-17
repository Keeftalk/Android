package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.keeftalk.chat.ui.MainViewModel
import com.keeftalk.chat.ui.theme.AppTheme
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()

    Scaffold(
        topBar = {
            SettingsHeader(title = "Display", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val scope = rememberCoroutineScope()
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Theme") {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        ThemeOption(
                            title = "System Default",
                            selected = prefs.themeMode == "SYSTEM",
                            onClick = { scope.launch { viewModel.userPreferencesRepository.updateThemeMode("SYSTEM") } }
                        )
                        ThemeOption(
                            title = "Light",
                            selected = prefs.themeMode == "LIGHT",
                            onClick = { scope.launch { viewModel.userPreferencesRepository.updateThemeMode("LIGHT") } }
                        )
                        ThemeOption(
                            title = "Dark",
                            selected = prefs.themeMode == "DARK",
                            onClick = { scope.launch { viewModel.userPreferencesRepository.updateThemeMode("DARK") } }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Night Tools") {
                    SettingsToggleItem(
                        icon = Icons.Default.Brightness4,
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
                                steps = 2
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

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
