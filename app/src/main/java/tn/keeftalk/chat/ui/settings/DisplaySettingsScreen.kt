package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.EnvironmentalCalculators
import tn.keeftalk.chat.ui.MainViewModel
import tn.keeftalk.chat.ui.theme.keef.KeefAdaptiveFab
import tn.keeftalk.chat.ui.theme.keef.KeefGlassContainer
import tn.keeftalk.chat.ui.theme.keef.KeefGlassIconButton
import tn.keeftalk.chat.ui.theme.keef.KeefGlassPopupWindow
import tn.keeftalk.chat.ui.theme.keef.KeefQuickActionButton
import tn.keeftalk.chat.ui.theme.keef.KeefStatTile
import tn.keeftalk.chat.ui.theme.keef.KeefThemeBackground
import tn.keeftalk.chat.ui.theme.keef.LocalKeefThemeTokens
import java.util.Calendar
import java.util.Locale

@Composable
fun DisplaySettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val envState by viewModel.environmentalState.collectAsState()
    val scope = rememberCoroutineScope()
    var showDialogPreview by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val themeRegistry = remember(context) {
        try {
            AppModule.provideExtensionHost(context).registry.themeRegistry
        } catch (_: Exception) {
            null
        }
    }
    val installedThemes = remember(themeRegistry) {
        themeRegistry?.getInstalledThemes() ?: emptyList()
    }
    val globalAppThemes = remember(installedThemes) {
        installedThemes.filter { pack ->
            !pack.isReaderThemeOnly &&
            pack.id != "dark" &&
            pack.id != "twilight" &&
            pack.id != "paper" &&
            pack.id != "sepia" &&
            pack.id != "sepiap"
        }
    }
    val activeCustomTheme by (themeRegistry?.activeCustomTheme?.collectAsState() ?: remember { mutableStateOf(null) })

    Scaffold(
        topBar = {
            SettingsHeader(title = "Display", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSection(title = "Theme Mode") {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            ThemeOption(
                                title = "System Default",
                                selected = prefs.themeMode == "SYSTEM" && activeCustomTheme == null,
                                onClick = {
                                    themeRegistry?.setActiveTheme(null)
                                    scope.launch { viewModel.updateThemeMode("SYSTEM") }
                                }
                            )
                            ThemeOption(
                                title = "Light",
                                selected = prefs.themeMode == "LIGHT" && activeCustomTheme == null,
                                onClick = {
                                    themeRegistry?.setActiveTheme(null)
                                    scope.launch { viewModel.updateThemeMode("LIGHT") }
                                }
                            )
                            ThemeOption(
                                title = "Dark",
                                selected = prefs.themeMode == "DARK" && activeCustomTheme == null,
                                onClick = {
                                    themeRegistry?.setActiveTheme(null)
                                    scope.launch { viewModel.updateThemeMode("DARK") }
                                }
                            )
                            ThemeOption(
                                title = "Keef Theme (Dynamic Sky UI)",
                                selected = prefs.themeMode == "KEEF" && activeCustomTheme == null,
                                onClick = {
                                    themeRegistry?.setActiveTheme(null)
                                    scope.launch { viewModel.updateThemeMode("KEEF") }
                                }
                            )
                            if (globalAppThemes.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Installed Custom Themes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                globalAppThemes.forEach { themePack ->
                                    ThemeOption(
                                        title = themePack.name,
                                        selected = activeCustomTheme?.id == themePack.id,
                                        onClick = {
                                            themeRegistry?.setActiveTheme(themePack.id)
                                            scope.launch { viewModel.updateThemeMode(themePack.id) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (prefs.themeMode == "KEEF") {
                    // Live Interactive Preview Card
                    item {
                        SettingsSection(title = "Live Theme Preview") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            ) {
                                // Mini Sky Background (Ground down 50%)
                                KeefThemeBackground(
                                    environmentalState = envState
                                )

                                // Live Glass UI Elements Sample
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Keef Theme Live",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = LocalKeefThemeTokens.current.ink
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            KeefGlassIconButton(
                                                icon = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                onClick = { showDialogPreview = true }
                                            )
                                            KeefAdaptiveFab(
                                                onClick = { showDialogPreview = true },
                                                icon = Icons.Default.Add,
                                                label = "New",
                                                expanded = true
                                            )
                                        }
                                    }

                                    KeefGlassContainer(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Glass Container & Pop-ups",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = LocalKeefThemeTokens.current.ink
                                                )
                                                Text(
                                                    text = "Real-time Gliders for Blur & Opacity",
                                                    fontSize = 11.5.sp,
                                                    color = LocalKeefThemeTokens.current.ink2
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = LocalKeefThemeTokens.current.accent
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        KeefStatTile(
                                            value = "98%",
                                            label = "Custom",
                                            modifier = Modifier.weight(1f)
                                        )
                                        KeefQuickActionButton(
                                            label = "Send",
                                            icon = Icons.AutoMirrored.Filled.Send,
                                            accentColor = Color(0xFF0A7CFF),
                                            onClick = { showDialogPreview = true },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SettingsSection(title = "Sky & Weather Source") {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = prefs.keefWeatherMode == "REAL",
                                        onClick = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(mode = "REAL")
                                            }
                                        },
                                        label = { Text("🌦️ Real Weather") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = prefs.keefWeatherMode == "SIMULATED",
                                        onClick = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(mode = "SIMULATED")
                                            }
                                        },
                                        label = { Text("🎨 Simulated Sky") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (prefs.keefWeatherMode == "REAL") {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    "📍 Active Weather Context",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "${envState.locationName} · ${envState.temperatureCelsius.toInt()}°C",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "${envState.weatherDescription} · Wind ${envState.windSpeed.toInt()} km/h · Clouds ${envState.cloudCover}%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "Last Updated: ${formatTimeAgo(envState.timestamp)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Update Interval",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    formatUpdateInterval(prefs.keefUpdateIntervalMinutes),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = prefs.keefUpdateIntervalMinutes.toFloat(),
                                                onValueChange = {
                                                    val minutes = it.toInt()
                                                    scope.launch {
                                                        viewModel.userPreferencesRepository.updateKeefWeatherSettings(
                                                            updateIntervalMinutes = minutes
                                                        )
                                                    }
                                                },
                                                valueRange = 5f..1440f,
                                                steps = 10
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // UI Glass Gliders (Customization Sliders for Cards, Buttons, Surfaces, Pop-ups, FAB "+", Containers, Borders & Corners)
                    item {
                        SettingsSection(title = "UI Glass Elements Customizer (Gliders)") {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Adjust translucency, blur, borders and rounded corners for all UI elements.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 1. Cards Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cards Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefCardOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefCardOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(cardOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 2. Cards Blur Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cards Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefCardBlurPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefCardBlurPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(cardBlur = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 3. Pop-up Windows / Dialogs Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Pop-up Windows Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefDialogOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefDialogOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(dialogOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 4. Pop-up Windows / Dialogs Blur Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Pop-up Windows Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefDialogBlurPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefDialogBlurPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(dialogBlur = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 5. Adaptive FAB "+" Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Adaptive FAB \"+\" Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefFabOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefFabOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(fabOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 6. Adaptive FAB "+" Blur Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Adaptive FAB \"+\" Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefFabBlurPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefFabBlurPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(fabBlur = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 7. Glass Containers Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Glass Containers Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefContainerOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefContainerOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(containerOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 8. Glass Containers Blur Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Glass Containers Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefContainerBlurPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefContainerBlurPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(containerBlur = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 9. Buttons Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Buttons Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefButtonOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefButtonOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(buttonOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 10. Buttons Blur Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Buttons Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefButtonBlurPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefButtonBlurPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(buttonBlur = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 11. Surfaces & Bars Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Surfaces & Bars Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSurfaceOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSurfaceOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(surfaceOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 12. Surfaces & Bars Blur Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Surfaces & Bars Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSurfaceBlurPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSurfaceBlurPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(surfaceBlur = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 13. Border / Line Highlight Opacity Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Glass Borders Highlight", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefBorderOpacityPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefBorderOpacityPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(borderOpacity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // 14. Corner Radius Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("UI Corner Roundness", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefCornerRadiusDp} dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefCornerRadiusDp.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(cornerRadiusDp = it.toInt())
                                            }
                                        },
                                        valueRange = 8f..36f
                                    )
                                }
                            }
                        }
                    }

                    // Environment & Landscape Gliders
                    item {
                        SettingsSection(title = "Ground & Sky Controls (Gliders)") {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Ground Type Picker
                                Column {
                                    Text(
                                        "Ground Landscape Type",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        val types = listOf(
                                            "AUTO" to "Auto",
                                            "FOREST" to "Forest",
                                            "CITY" to "City",
                                            "SAHARA" to "Sahara",
                                            "GREEN" to "Meadow"
                                        )
                                        types.forEachIndexed { index, (value, label) ->
                                            SegmentedButton(
                                                selected = prefs.keefGroundType == value,
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.userPreferencesRepository.updateKeefWeatherSettings(groundType = value)
                                                    }
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                                            ) {
                                                Text(label, fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }

                                // Light Pollution Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Light Pollution Level", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefLightPollutionPercent}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefLightPollutionPercent.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(lightPollution = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                    Text(
                                        "Higher light pollution dims stars and adds urban skyglow.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Stars Density Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Stars Density", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSimStarsDensity}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimStarsDensity.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(starsDensity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // Cloud Density Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cloud Density", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSimCloudDensity}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimCloudDensity.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(cloudDensity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..400f
                                    )
                                }

                                // Cloud Smoothness Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cloud Smoothness", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${(prefs.keefSimCloudSmoothness * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimCloudSmoothness,
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(cloudSmoothness = it)
                                            }
                                        },
                                        valueRange = 0.2f..1.0f
                                    )
                                }

                                // Wind Speed Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Wind Speed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSimWindSpeed.toInt()} km/h", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimWindSpeed,
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(windSpeed = it)
                                            }
                                        },
                                        valueRange = 0f..300f
                                    )
                                }

                                // Rain / Particle Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Rain & Particle Density", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSimRainDensity}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimRainDensity.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(rainDensity = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // Thunder & Lightning Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Thunder & Lightning Frequency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${prefs.keefSimThunderFrequency}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimThunderFrequency.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(thunderFrequency = it.toInt())
                                            }
                                        },
                                        valueRange = 0f..100f
                                    )
                                }

                                // Time of Day (Sun & Moon Arc) Glider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Sun & Celestial Arc (Time)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(formatSimTime(prefs.keefSimTimeOfDay), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimTimeOfDay,
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(timeOfDay = it)
                                            }
                                        },
                                        valueRange = 0f..24f
                                    )
                                }

                                // Moon Phase Glider
                                val simMoonState = remember(prefs.keefSimDayOfMonth, prefs.keefSimTimeOfDay) {
                                    val totalMinutes = (prefs.keefSimTimeOfDay * 60f).toInt().coerceIn(0, 1439)
                                    val cal = Calendar.getInstance().apply {
                                        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                                        set(Calendar.DAY_OF_MONTH, prefs.keefSimDayOfMonth.coerceIn(1, maxDay))
                                        set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
                                        set(Calendar.MINUTE, totalMinutes % 60)
                                    }
                                    EnvironmentalCalculators.calculateMoonState(cal.timeInMillis, true)
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Moon Phase (Day of Month)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Day ${prefs.keefSimDayOfMonth} (${simMoonState.phaseName})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = prefs.keefSimDayOfMonth.toFloat(),
                                        onValueChange = {
                                            scope.launch {
                                                viewModel.userPreferencesRepository.updateKeefWeatherSettings(dayOfMonth = it.toInt())
                                            }
                                        },
                                        valueRange = 1f..31f,
                                        steps = 29
                                    )
                                }

                                // Reset Gliders Button
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            viewModel.userPreferencesRepository.updateKeefWeatherSettings(
                                                cardBlur = 20,
                                                buttonBlur = 20,
                                                surfaceBlur = 20,
                                                cardOpacity = 60,
                                                buttonOpacity = 50,
                                                surfaceOpacity = 70,
                                                borderOpacity = 50,
                                                cornerRadiusDp = 22,
                                                dialogBlur = 20,
                                                dialogOpacity = 80,
                                                fabBlur = 20,
                                                fabOpacity = 60,
                                                containerBlur = 20,
                                                containerOpacity = 60,
                                                groundType = "AUTO",
                                                lightPollution = 30
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset All Gliders to Recommended Defaults")
                                }
                            }
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

            // Dialog Pop-up Preview Modal
            if (showDialogPreview) {
                KeefGlassPopupWindow(
                    onDismissRequest = { showDialogPreview = false },
                    title = "Glass Pop-up Window Preview"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "This pop-up window uses the customized Pop-up Window Gliders (Blur & Opacity).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalKeefThemeTokens.current.ink
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showDialogPreview = false },
                                colors = ButtonDefaults.buttonColors(containerColor = LocalKeefThemeTokens.current.accent)
                            ) {
                                Text("Close Preview")
                            }
                        }
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

private fun formatUpdateInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes minutes"
        minutes == 60 -> "1 hour"
        minutes < 1440 -> "${minutes / 60} hours"
        else -> "24 hours (1 day)"
    }
}

private fun formatSimTime(timeHours: Float): String {
    val totalMinutes = (timeHours * 60f).toInt().coerceIn(0, 1439)
    val hh = totalMinutes / 60
    val mm = totalMinutes % 60
    val timeStr = String.format(Locale.US, "%02d:%02d", hh, mm)
    val periodStr = when {
        totalMinutes in 345..435 -> "Dawn"
        totalMinutes in 436..1065 -> "Day"
        totalMinutes in 1066..1155 -> "Dusk"
        else -> "Night"
    }
    return "$timeStr ($periodStr)"
}

private fun formatTimeAgo(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Unknown"
    val diffSec = (System.currentTimeMillis() - timestampMs) / 1000
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60} mins ago"
        diffSec < 86400 -> "${diffSec / 3600} hours ago"
        else -> "More than 1 day ago"
    }
}
