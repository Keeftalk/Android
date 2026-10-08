package tn.keeftalk.chat.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.prayer.AsrMadhhab
import tn.keeftalk.chat.domain.prayer.CalculationMethod

data class CardDef(
    val id: String,
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val brush: Brush
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSettingsSheet(
    cardOrder: List<String>,
    hiddenCards: List<String>,
    layoutMode: String,
    calculationMethod: String,
    asrMadhhab: String,
    fajrReminder: Boolean,
    dhuhrReminder: Boolean,
    asrReminder: Boolean,
    maghribReminder: Boolean,
    ishaReminder: Boolean,
    onDismiss: () -> Unit,
    onMoveCardUp: (String) -> Unit,
    onMoveCardDown: (String) -> Unit,
    onToggleCardHidden: (String, Boolean) -> Unit,
    onChangeLayoutMode: (String) -> Unit,
    onChangeCalculationMethod: (String) -> Unit,
    onChangeAsrMadhhab: (String) -> Unit,
    onTogglePrayerReminder: (String, Boolean) -> Unit
) {
    val cardDefs = remember {
        listOf(
            CardDef("weather", "Weather", "Current conditions & forecast", Icons.Default.WbSunny, Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFFA855F7)))),
            CardDef("verse", "Verse of the day", "Daily Quranic verse", Icons.Default.Favorite, Brush.linearGradient(listOf(Color(0xFF131A30), Color(0xFF2C1A3E)))),
            CardDef("prayer", "Prayer times", "Daily salah schedule", Icons.Default.Mosque, Brush.linearGradient(listOf(Color(0xFF0F1530), Color(0xFF2E1650)))),
            CardDef("markets", "Markets", "Crypto, gold & currency", Icons.Default.ShowChart, Brush.linearGradient(listOf(Color(0xFF12B76A), Color(0xFF059669))))
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFD6D9E3))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize feed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1020),
                    letterSpacing = (-0.5).sp
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEEF0F6),
                        contentColor = Color(0xFF4F46E5)
                    ),
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
                ) {
                    Text("Done", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Text(
                text = "Turn cards on or off, and reorder them to match your day.",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B90A4),
                letterSpacing = (-0.1).sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Cards Reorder / Toggle Section
            cardOrder.forEachIndexed { idx, cardId ->
                val def = cardDefs.find { it.id == cardId } ?: return@forEachIndexed
                val isVisible = !hiddenCards.contains(cardId)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F8FC))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(def.brush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = def.icon,
                            contentDescription = def.name,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = def.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1020),
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = def.desc,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9AA0B4)
                        )
                    }

                    // Arrows
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onMoveCardUp(cardId) },
                            enabled = idx > 0,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(12.dp))
                        }
                        IconButton(
                            onClick = { onMoveCardDown(cardId) },
                            enabled = idx < cardOrder.size - 1,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(12.dp))
                        }
                    }

                    // Toggle Switch
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { checked -> onToggleCardHidden(cardId, !checked) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF5B5BF5)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Layout Section Header
            Text(
                text = "LAYOUT",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF8B90A4),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Layout Choice: Stacked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF7F8FC))
                    .clickable { onChangeLayoutMode("STACKED") }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                RadioButton(
                    selected = layoutMode == "STACKED",
                    onClick = { onChangeLayoutMode("STACKED") }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stacked view", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1020))
                    Text("Vertical list of cards", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF9AA0B4))
                }
            }

            // Layout Choice: Swipe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF7F8FC))
                    .clickable { onChangeLayoutMode("SWIPE") }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                RadioButton(
                    selected = layoutMode == "SWIPE",
                    onClick = { onChangeLayoutMode("SWIPE") }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Swipe cards sideways", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1020))
                    Text("Swipe left/right through cards", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF9AA0B4))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prayer Calculation Settings Section
            Text(
                text = "PRAYER & SALAH SETTINGS",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF8B90A4),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Calculation Method Selector
            var showMethodMenu by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF7F8FC))
                    .clickable { showMethodMenu = true }
                    .padding(12.dp)
            ) {
                Column {
                    Text("Calculation Method", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1020))
                    Text(
                        CalculationMethod.fromName(calculationMethod).title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5B5BF5)
                    )
                }

                DropdownMenu(
                    expanded = showMethodMenu,
                    onDismissRequest = { showMethodMenu = false }
                ) {
                    CalculationMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.title) },
                            onClick = {
                                onChangeCalculationMethod(method.name)
                                showMethodMenu = false
                            }
                        )
                    }
                }
            }

            // Asr Madhhab Selector
            var showMadhhabMenu by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF7F8FC))
                    .clickable { showMadhhabMenu = true }
                    .padding(12.dp)
            ) {
                Column {
                    Text("Asr Madhhab", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1020))
                    Text(
                        AsrMadhhab.fromName(asrMadhhab).title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5B5BF5)
                    )
                }

                DropdownMenu(
                    expanded = showMadhhabMenu,
                    onDismissRequest = { showMadhhabMenu = false }
                ) {
                    AsrMadhhab.values().forEach { madhhab ->
                        DropdownMenuItem(
                            text = { Text(madhhab.title) },
                            onClick = {
                                onChangeAsrMadhhab(madhhab.name)
                                showMadhhabMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prayer Reminders Switches
            Text(
                text = "PRAYER REMINDERS",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF8B90A4),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            listOf(
                "Fajr" to fajrReminder,
                "Dhuhr" to dhuhrReminder,
                "Asr" to asrReminder,
                "Maghrib" to maghribReminder,
                "Isha" to ishaReminder
            ).forEach { (prayerName, enabled) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF7F8FC))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prayerName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1020))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked -> onTogglePrayerReminder(prayerName, checked) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
