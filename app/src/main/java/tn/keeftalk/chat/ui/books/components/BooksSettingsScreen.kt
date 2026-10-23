package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.BookSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksSettingsScreen(
    settings: BookSettings,
    onBack: () -> Unit,
    onSettingsChanged: (BookSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSettings by remember(settings) { mutableStateOf(settings) }

    fun update(newSettings: BookSettings) {
        currentSettings = newSettings
        onSettingsChanged(newSettings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            tn.keeftalk.chat.ui.settings.components.FeatureCloudSyncCard(
                featureId = tn.keeftalk.chat.domain.model.entitlement.FeatureId.BOOKS,
                description = "Sync your books, reading progress, bookmarks, and reading data.",
                onEnableClick = { },
                modifier = Modifier.fillMaxWidth()
            )

            // Cloud Synchronization Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CLOUD SYNCHRONIZATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsSwitchRow(
                        title = "Sync books across devices",
                        caption = "Automatically back up and restore your reading progress, bookmarks, and settings",
                        checked = currentSettings.syncAcrossDevices,
                        onCheckedChange = { update(currentSettings.copy(syncAcrossDevices = it)) }
                    )
                }
            }
            // Read Marker Settings Group
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("READ MARKER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Show read marker toggle
                    SettingsSwitchRow(
                        title = "Show read marker",
                        caption = "Floating check button, green read pages and counter",
                        checked = currentSettings.showReadMarker,
                        onCheckedChange = { update(currentSettings.copy(showReadMarker = it)) }
                    )

                    SettingsSwitchRow(
                        title = "Auto-turn page when marked read",
                        caption = "Go to the next page right after you mark a page as read (green)",
                        checked = currentSettings.autoNextPageOnRead,
                        onCheckedChange = { update(currentSettings.copy(autoNextPageOnRead = it)) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Button Size Slider with Live Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Button size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tap = mark page read · drag = save spot · ${currentSettings.buttonSizePx}dp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .size(currentSettings.buttonSizePx.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .background(Color(0xFF6C63FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Read Marker",
                                tint = Color.White,
                                modifier = Modifier.size((currentSettings.buttonSizePx * 0.5f).dp)
                            )
                        }
                    }
                    Slider(
                        value = currentSettings.buttonSizePx.toFloat(),
                        onValueChange = { update(currentSettings.copy(buttonSizePx = it.toInt())) },
                        valueRange = 28f..64f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF6C63FF), activeTrackColor = Color(0xFF6C63FF))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val markerColor = parseHexColor(currentSettings.readMarkerColorHex, Color(0xFF2ECC71))

                    // Read Marker Opacity Slider with Live Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Read marker layer opacity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Colour intensity on pages marked read · ${currentSettings.greenOpacityPercent}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(markerColor.copy(alpha = currentSettings.greenOpacityPercent / 100f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        )
                    }
                    Slider(
                        value = currentSettings.greenOpacityPercent.toFloat(),
                        onValueChange = { update(currentSettings.copy(greenOpacityPercent = it.toInt())) },
                        valueRange = 10f..90f,
                        colors = SliderDefaults.colors(thumbColor = markerColor, activeTrackColor = markerColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Marker Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf("#2ECC71", "#10B981", "#A3E635", "#14B8A6", "#3B82F6", "#8B5CF6")
                        colors.forEach { hex ->
                            val color = parseHexColor(hex, Color(0xFF2ECC71))
                            val isSel = currentSettings.readMarkerColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { update(currentSettings.copy(readMarkerColorHex = hex)) }
                                    .border(if (isSel) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            )
                        }
                    }
                }
            }

            // Reader & Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("READER & CONTROLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsSwitchRow(
                        title = "Page margins",
                        caption = "Pad text edges for comfortable reading",
                        checked = currentSettings.pageMarginsEnabled,
                        onCheckedChange = { update(currentSettings.copy(pageMarginsEnabled = it)) }
                    )

                    SettingsSwitchRow(
                        title = "Volume keys turn page",
                        caption = "Press volume up/down to flip pages",
                        checked = currentSettings.volumeKeysTurnPage,
                        onCheckedChange = { update(currentSettings.copy(volumeKeysTurnPage = it)) }
                    )

                    SettingsSwitchRow(
                        title = "Keep screen on",
                        caption = "Prevent display sleep while reading",
                        checked = currentSettings.keepScreenOn,
                        onCheckedChange = { update(currentSettings.copy(keepScreenOn = it)) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("PDF DPI Quality", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Controls rendering sharpness and performance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf("lowest" to "Lowest", "middle" to "Middle (+50%)", "custom" to "Custom")
                        options.forEach { (key, label) ->
                            val isSel = currentSettings.pdfDpiOption == key
                            Surface(
                                color = if (isSel) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { update(currentSettings.copy(pdfDpiOption = key)) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (currentSettings.pdfDpiOption == "custom") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Custom DPI value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentSettings.pdfCustomDpi} DPI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                        }
                        Slider(
                            value = currentSettings.pdfCustomDpi.toFloat(),
                            onValueChange = { update(currentSettings.copy(pdfCustomDpi = it.toInt())) },
                            valueRange = 100f..750f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF6C63FF), activeTrackColor = Color(0xFF6C63FF))
                        )
                    }
                }
            }

            // Data & Goals
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DATA & GOALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsSwitchRow(
                        title = "Sync across devices",
                        caption = "Keep library & notes updated everywhere",
                        checked = currentSettings.syncAcrossDevices,
                        onCheckedChange = { update(currentSettings.copy(syncAcrossDevices = it)) }
                    )

                    SettingsSwitchRow(
                        title = "Encrypt vault files",
                        caption = "AES-256 encryption for imported documents",
                        checked = currentSettings.encryptFiles,
                        onCheckedChange = { update(currentSettings.copy(encryptFiles = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    caption: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(caption, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6C63FF))
        )
    }
}