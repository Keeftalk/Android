package tn.keeftalk.chat.ui.docs.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.domain.model.DocSettings

private data class ThemeOption(
    val key: String,
    val label: String,
    val bg: Color,
    val fg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocSettingsSheet(
    settings: DocSettings,
    onUpdateSettings: (DocSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Document Reader Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reading Theme Selection
            Text(
                text = "Reading Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val themes = listOf(
                    ThemeOption("original", "Light", Color.White, Color.Black),
                    ThemeOption("dark", "Dark", Color(0xFF1E1E1E), Color.White),
                    ThemeOption("sepia", "Sepia", Color(0xFFFBF0D9), Color(0xFF5F4B32)),
                    ThemeOption("night", "Night", Color(0xFF0D1117), Color(0xFFC9D1D9))
                )

                themes.forEach { option ->
                    val isSelected = settings.theme == option.key

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(option.bg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUpdateSettings(settings.copy(theme = option.key)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            color = option.fg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font Size Slider
            Text(
                text = "Font Size (${settings.fontSizeSp} sp)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onUpdateSettings(settings.copy(fontSizeSp = it.toInt())) },
                valueRange = 12f..28f,
                steps = 15
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Keep Screen On", style = MaterialTheme.typography.bodyLarge)
                    Text("Prevent screen from dimming while reading", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onUpdateSettings(settings.copy(keepScreenOn = it)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Show Line Numbers", style = MaterialTheme.typography.bodyLarge)
                    Text("Display line numbers for code and text docs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.showLineNumbers,
                    onCheckedChange = { onUpdateSettings(settings.copy(showLineNumbers = it)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Cloud Sync Pro Across Devices", style = MaterialTheme.typography.bodyLarge)
                    Text("Sync reading progress and bookmarks via Supabase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.syncAcrossDevices,
                    onCheckedChange = { onUpdateSettings(settings.copy(syncAcrossDevices = it)) }
                )
            }
        }
    }
}
