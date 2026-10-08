package tn.keeftalk.chat.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharedFlow
import tn.keeftalk.chat.domain.model.AppNotification
import tn.keeftalk.chat.domain.model.habit.Habit
import tn.keeftalk.chat.domain.model.habit.ScheduleVersion
import tn.keeftalk.chat.ui.components.FabActionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HabitIconView(iconKey: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val vector = LUCIDE_ICONS[iconKey]
    if (vector != null) {
        Icon(imageVector = vector, contentDescription = iconKey, tint = tint, modifier = modifier)
    } else {
        Text(text = iconKey, style = MaterialTheme.typography.titleMedium)
    }
}

fun repLabel(schedule: ScheduleVersion?): String {
    val mask = schedule?.daysMask ?: emptySet()
    if (mask.size == 7) return "Daily"
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    if (mask == weekdays) return "Weekdays"
    val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    if (mask == weekends) return "Weekends"
    if (mask.isEmpty()) return "Never"
    val daysOrdered = listOf(
        DayOfWeek.SUNDAY to "Sun",
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat"
    )
    return daysOrdered.filter { mask.contains(it.first) }.joinToString(", ") { it.second }
}

fun nextDueLabel(schedule: ScheduleVersion?, fromDate: LocalDate): String {
    val mask = schedule?.daysMask ?: emptySet()
    if (mask.isEmpty()) return "—"
    for (i in 1..7) {
        val nextDate = fromDate.plusDays(i.toLong())
        if (mask.contains(nextDate.dayOfWeek)) {
            return if (i == 1) "Tomorrow" else nextDate.format(DateTimeFormatter.ofPattern("EEE"))
        }
    }
    return "—"
}

fun formatTime(time: LocalTime?): String {
    if (time == null) return ""
    return time.format(DateTimeFormatter.ofPattern("h:mm a"))
}

const val CHEER_MESSAGES = "Every rep rewires your brain. This is how automaticity gets built."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    notifications: List<AppNotification> = emptyList(),
    onNotificationClick: (AppNotification) -> Unit = {},
    onMarkAllNotificationsAsRead: () -> Unit = {},
    fabActionFlow: SharedFlow<FabActionType>? = null,
    onNavigateToBooks: (String?) -> Unit = {},
    viewModel: HabitsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMastered by remember { mutableStateOf(false) }
    var isEditHabitsSheetOpen by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    var showHelpSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.triggerInitialRailAutoExpand()
    }

    LaunchedEffect(fabActionFlow) {
        fabActionFlow?.collect { action ->
            if (action == FabActionType.NEW_HABIT) {
                viewModel.openAddSheet()
            } else if (action == FabActionType.EDIT_HABITS) {
                isEditHabitsSheetOpen = true
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.selectedTab == HabitsTab.STATS) {
                StatsScreen(
                    habits = uiState.habits,
                    selectedRange = uiState.statsRange,
                    gracePoolRemaining = uiState.gracePoolRemaining,
                    onRangeSelected = { viewModel.setStatsRange(it) },
                    selectedTab = uiState.selectedTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    isRailExpanded = uiState.isRailExpanded,
                    onToggleRail = { viewModel.toggleRailExpanded() },
                    onOpenHelp = { showHelpSheet = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Hero Banner Card with Rail (.hero)
                    item {
                        tn.keeftalk.chat.ui.habits.components.HabitsHeroRowWithRail(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = { viewModel.selectTab(it) },
                            isRailExpanded = uiState.isRailExpanded,
                            onToggleRail = { viewModel.toggleRailExpanded() }
                        ) {
                            HabitsHeroCard(
                                uiState = uiState,
                                onOpenHelp = { showHelpSheet = true }
                            )
                        }
                    }

                    // 2. Swipable Days Strip under the Card (.week)
                    item {
                        WeekStrip(
                            selectedDate = uiState.selectedDate,
                            today = uiState.today,
                            habits = uiState.habits,
                            onDateSelected = { viewModel.selectDate(it) }
                        )
                    }

                    // 3. Preview Mode Banner (.pvBar)
                    if (uiState.isPreviewMode) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Previewing ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))} — read only",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    TextButton(onClick = { viewModel.selectDate(uiState.today) }) {
                                        Text("Back to today", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Filter chips (.chips)
                    item {
                        val slots = listOf("All", "Early Morning", "Morning", "Midday", "Afternoon", "Evening", "Late Night")
                        val slotIcons = mapOf(
                            "All" to Icons.Default.GridOn,
                            "Early Morning" to Icons.Default.WbSunny,
                            "Morning" to Icons.Default.WbSunny,
                            "Midday" to Icons.Default.AutoAwesome,
                            "Afternoon" to Icons.Default.Cloud,
                            "Evening" to Icons.Default.WbTwilight,
                            "Late Night" to Icons.Default.DarkMode
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(slots) { slot ->
                                val isSelected = uiState.slotFilter == slot
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSlotFilter(slot) },
                                    label = { Text(slot) },
                                    leadingIcon = {
                                        Icon(slotIcons[slot] ?: Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }

                    // Grace Days Bar (.graceBar)
                    item {
                        val left = uiState.gracePoolRemaining
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(
                                            text = if (left > 0) "$left grace day${if (left == 1) "" else "s"} left this month" else "All grace days used this month",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (left > 0) "Forgive a missed day completely — no target increase, streak stays." else "They reset on the 1st of next month. Recovery Streaks still apply.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "$left/2",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Active Recovery Challenge Alert (#alert)
                    item {
                        val activeRec = uiState.habits.filter { !it.status.isMastered && it.status.recoveryActive }
                        val topRec = activeRec.maxByOrNull { it.status.recoveryProgress.toFloat() / maxOf(1, it.status.recoveryRequired).toFloat() }
                        if (!uiState.isPreviewMode && topRec != null) {
                            val status = topRec.status
                            val leftDays = maxOf(0, status.recoveryRequired - status.recoveryProgress)
                            val progPct = if (status.recoveryRequired > 0) status.recoveryProgress.toFloat() / status.recoveryRequired.toFloat() else 0f

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Grace Challenge · ${topRec.habit.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("You missed a day, but your progress is safe. Complete $leftDays more day${if (leftDays == 1) "" else "s"} in a row to earn your grace.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { progPct },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${status.recoveryProgress} / ${status.recoveryRequired} days · target becomes ${status.effTarget} days (+${status.effMissed} miss${if (status.effMissed == 1) "" else "es"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Section Header
                    val slotFilteredHabits = uiState.habits.filter {
                        uiState.slotFilter == "All" || it.habit.slot.equals(uiState.slotFilter, ignoreCase = true)
                    }

                    val dueList = slotFilteredHabits.filter { model ->
                        val dayStatus = model.recent7Days.find { it.date == uiState.selectedDate }
                        val isDue = dayStatus?.isDue ?: (uiState.selectedDate >= model.habit.createdDate && model.schedule?.daysMask?.contains(uiState.selectedDate.dayOfWeek) == true)
                        isDue && !model.status.isMastered
                    }

                    val offList = slotFilteredHabits.filter { model ->
                        val dayStatus = model.recent7Days.find { it.date == uiState.selectedDate }
                        val isDue = dayStatus?.isDue ?: (uiState.selectedDate >= model.habit.createdDate && model.schedule?.daysMask?.contains(uiState.selectedDate.dayOfWeek) == true)
                        !isDue && !model.status.isMastered
                    }

                    val masteredList = slotFilteredHabits.filter { it.status.isMastered }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.isPreviewMode) uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) else "Today's habits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${dueList.size} habit${if (dueList.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Due Habits List
                    if (dueList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Spa,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (!uiState.isPreviewMode) "Nothing scheduled today. Enjoy the rest." else "Nothing was scheduled that day.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(dueList, key = { it.habit.id }) { model ->
                            HabitCard(
                                model = model,
                                selectedDate = uiState.selectedDate,
                                isPreview = uiState.isPreviewMode,
                                gracePoolRemaining = uiState.gracePoolRemaining,
                                onToggle = { completed -> viewModel.toggleCheckIn(model.habit.id, completed) },
                                onUseGrace = { date -> viewModel.useGrace(model.habit.id, date) },
                                onNavigateToBooks = onNavigateToBooks
                            )
                        }
                    }

                    // Not Scheduled / Rest Section (#restBlock)
                    if (offList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (!uiState.isPreviewMode) "Not scheduled today" else "Not scheduled that day",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Rest day",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(offList, key = { "rest_${it.habit.id}" }) { model ->
                            RestHabitCard(model = model, selectedDate = uiState.selectedDate)
                        }
                    }

                    // Mastered Habits Section (#masterBlock)
                    if (masteredList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMastered = !showMastered },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Text("Mastered habits", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${masteredList.size} retired", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(
                                        imageVector = if (showMastered) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showMastered) {
                            items(masteredList, key = { "mastered_${it.habit.id}" }) { model ->
                                MasteredHabitCard(model = model)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isAddEditSheetOpen) {
            AddEditHabitSheet(
                habitToEdit = uiState.editingHabit,
                availableBooks = uiState.availableBooks,
                onDismiss = { viewModel.closeSheet() },
                onSave = { id, name, icon, category, baseDays, slot, reminder, mask, type, amount, unit, colorHex, isReading, autoCount, linkedBooks ->
                    viewModel.saveHabit(id, name, icon, category, baseDays, slot, reminder, mask, type, amount, unit, colorHex, isReading, autoCount, linkedBooks)
                }
            )
        }

        if (isEditHabitsSheetOpen) {
            EditHabitsSheet(
                habits = uiState.habits,
                onDismiss = { isEditHabitsSheetOpen = false },
                onEditHabit = { habit ->
                    isEditHabitsSheetOpen = false
                    viewModel.openEditSheet(habit)
                },
                onDeleteHabit = { habit ->
                    habitToDelete = habit
                }
            )
        }

        if (habitToDelete != null) {
            AlertDialog(
                onDismissRequest = { habitToDelete = null },
                title = { Text("Delete Habit") },
                text = { Text("Are you sure you want to delete \"${habitToDelete?.name}\"? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            habitToDelete?.let { viewModel.deleteHabit(it.id) }
                            habitToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { habitToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (!uiState.introShown) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = "MY HABITS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Build your life,\none habit at a time.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "My Habits is here to help you stay motivated, disciplined, and consistent.\n\nSmall actions repeated every day can become meaningful long-term progress.\n\nAnd because we believe in rewarding positive habits, Keeftalk gives you more than just motivation. Consistent users can benefit from additional free storage and free tools as part of Keeftalk's reward philosophy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.markIntroShown() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Begin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val milestonePopup = uiState.currentMilestonePopup
        if (milestonePopup != null) {
            Dialog(onDismissRequest = { viewModel.dismissMilestonePopup() }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Text(
                            text = "🎉",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "${milestonePopup.title} UNLOCKED!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = milestonePopup.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (milestonePopup.nextTitle != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("NEXT MILESTONE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(milestonePopup.nextTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Button(
                            onClick = { viewModel.dismissMilestonePopup() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showHelpSheet) {
            tn.keeftalk.chat.ui.habits.components.HabitsHelpSheet(
                onDismiss = { showHelpSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitsSheet(
    habits: List<HabitDisplayModel>,
    onDismiss: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Edit Habits",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (habits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active habits to edit yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(habits, key = { it.habit.id }) { model ->
                        val habit = model.habit
                        val habitColor = parseHexColor(model.colorHex)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(habitColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        HabitIconView(iconKey = habit.icon, modifier = Modifier.size(22.dp), tint = habitColor)
                                    }
                                    Column {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${repLabel(model.schedule)} · ${habit.slot}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onEditHabit(habit) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteHabit(habit) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun HabitsHeroCard(
    uiState: HabitsUiState,
    onOpenHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dueForDate = uiState.habits.filter { model ->
        val dayStatus = model.recent7Days.find { it.date == uiState.selectedDate }
        dayStatus?.isDue ?: (uiState.selectedDate >= model.habit.createdDate && model.schedule?.daysMask?.contains(uiState.selectedDate.dayOfWeek) == true)
    }
    val doneForDate = dueForDate.count { model ->
        val dayStatus = model.recent7Days.find { it.date == uiState.selectedDate }
        dayStatus?.isCompleted == true || dayStatus?.isForgiven == true
    }
    val totalForDate = dueForDate.size
    val isWin = !uiState.isPreviewMode && totalForDate > 0 && doneForDate == totalForDate
    val bestStreak = uiState.habits.maxOfOrNull { it.status.streak } ?: 0
    val dayName = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE"))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp),
        shape = RoundedCornerShape(26.dp),
        color = if (isWin) Color(0xFF00CCCC) else Color(0xFF5A52D9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Section: Ring + Hero Headline + Quick Stats Badges + Help ? Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ring
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (totalForDate > 0) doneForDate.toFloat() / totalForDate.toFloat() else 0f
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            style = Stroke(width = 5.5.dp.toPx())
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 5.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Text(
                        text = if (isWin) "✓" else "$doneForDate/$totalForDate",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.isPreviewMode) "Preview · $dayName" else "Today · $dayName",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (uiState.isPreviewMode) (if (totalForDate == 0) "Rest day" else if (isWin) "Perfect day!" else "$doneForDate of $totalForDate done")
                        else if (isWin) "All $totalForDate done! 🎉"
                        else if (totalForDate == 0) "Rest day"
                        else "${totalForDate - doneForDate} to go",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(11.dp))
                                Text(
                                    text = "$bestStreak day streak",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        if (totalForDate > 0) {
                            val pct = ((doneForDate.toFloat() / totalForDate.toFloat()) * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "$pct% done",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Help ? Icon Button
                Surface(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onOpenHelp() },
                    color = Color.White.copy(alpha = 0.22f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Habits Help",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Section: Continuous 00:00 - 23:59 Timeline Bar
            ContinuousDayTimelineBar(
                dueHabits = dueForDate,
                selectedDate = uiState.selectedDate,
                isPreview = uiState.isPreviewMode
            )
        }
    }
}

@Composable
fun ContinuousDayTimelineBar(
    dueHabits: List<HabitDisplayModel>,
    selectedDate: LocalDate,
    isPreview: Boolean,
    modifier: Modifier = Modifier
) {
    val now = LocalTime.now()
    val nowMins = if (isPreview) 1440 else (now.hour * 60 + now.minute)
    val nowFormatted = if (isPreview) "End of day" else now.format(DateTimeFormatter.ofPattern("h:mm a"))

    // Define 00:00 - 23:59 time slots with distinct colors
    val slots = listOf(
        TimelineSlot("Early Morning", 240, 480, Color(0xFFF59E0B)), // 4:00 - 8:00
        TimelineSlot("Morning", 480, 690, Color(0xFFEAB308)),      // 8:00 - 11:30
        TimelineSlot("Midday", 690, 840, Color(0xFF06B6D4)),       // 11:30 - 14:00
        TimelineSlot("Afternoon", 840, 1050, Color(0xFF3B82F6)),   // 14:00 - 17:30
        TimelineSlot("Evening", 1050, 1260, Color(0xFF8B5CF6)),    // 17:30 - 21:00
        TimelineSlot("Late Night", 1260, 1440, Color(0xFF4C1D95))   // 21:00 - 24:00
    )

    // Current active slot ("Open now")
    val activeSlot = slots.find { nowMins in it.startMins until it.endMins }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Slot Legend Row with colored indicators above timeline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slots.forEach { slot ->
                    val isSlotActive = activeSlot?.name == slot.name && !isPreview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSlotActive) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(slot.color)
                        )
                        Text(
                            text = slot.name.take(3),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (isSlotActive) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSlotActive) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Right side active slot status label
            if (activeSlot != null && !isPreview) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7CFFB2))
                    )
                    Text(
                        text = "${activeSlot.name} (Open)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFF7CFFB2)
                    )
                }
            }
        }

        // 24-Hour Continuous Bar (00:00 to 23:59) Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(11.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalWidth = size.width
                val totalHeight = size.height

                // 1. Draw segment fills for 00:00 - 23:59 slots
                val night0End = (240f / 1440f) * totalWidth
                drawRect(
                    color = Color(0xFF312E81).copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = Size(night0End, totalHeight)
                )

                slots.forEach { slot ->
                    val startX = (slot.startMins.toFloat() / 1440f) * totalWidth
                    val endX = (slot.endMins.toFloat() / 1440f) * totalWidth
                    val segmentWidth = endX - startX
                    val isSlotActive = activeSlot?.name == slot.name && !isPreview

                    drawRect(
                        color = slot.color.copy(alpha = if (isSlotActive) 0.85f else 0.45f),
                        topLeft = Offset(startX, 0f),
                        size = Size(segmentWidth, totalHeight)
                    )

                    // Draw separator line
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(startX, 0f),
                        end = Offset(startX, totalHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Draw current time progress needle
                if (!isPreview && nowMins in 0..1440) {
                    val currentX = (nowMins.toFloat() / 1440f) * totalWidth

                    drawLine(
                        color = Color.White,
                        start = Offset(currentX, 0f),
                        end = Offset(currentX, totalHeight),
                        strokeWidth = 2.5.dp.toPx()
                    )

                    drawCircle(
                        color = Color(0xFF7CFFB2),
                        radius = 4.dp.toPx(),
                        center = Offset(currentX, totalHeight / 2f)
                    )
                }

                // 3. Draw habit markers on the 24h timeline
                dueHabits.forEach { model ->
                    val habit = model.habit
                    val dayStatus = model.recent7Days.find { it.date == selectedDate }
                    val isDone = dayStatus?.isCompleted == true || dayStatus?.isForgiven == true

                    val habitMins = if (habit.reminderTime != null) {
                        habit.reminderTime.hour * 60 + habit.reminderTime.minute
                    } else {
                        when (habit.slot) {
                            "Early Morning" -> 360  // 6:00 AM
                            "Morning" -> 585        // 9:45 AM
                            "Midday" -> 765         // 12:45 PM
                            "Afternoon" -> 945      // 3:45 PM
                            "Evening" -> 1155       // 7:15 PM
                            else -> 1350            // 10:30 PM
                        }
                    }

                    val markerX = (habitMins.toFloat() / 1440f) * totalWidth
                    val markerColor = if (isDone) Color(0xFF00CCCC) else Color.White

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = 5.dp.toPx(),
                        center = Offset(markerX, totalHeight / 2f)
                    )
                    drawCircle(
                        color = markerColor,
                        radius = 3.5.dp.toPx(),
                        center = Offset(markerX, totalHeight / 2f)
                    )
                }
            }
        }

        // Timeline Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("00:00", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White.copy(alpha = 0.65f))
            Text(
                text = if (!isPreview) "NOW · $nowFormatted" else "Timeline (24h)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.9f)
            )
            Text("23:59", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White.copy(alpha = 0.65f))
        }
    }
}

data class TimelineSlot(
    val name: String,
    val startMins: Int,
    val endMins: Int,
    val color: Color
)

@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    habits: List<HabitDisplayModel>,
    onDateSelected: (LocalDate) -> Unit
) {
    val oldestDate = habits.map { it.habit.createdDate }.minOrNull()?.coerceAtMost(today.minusDays(180)) ?: today.minusDays(180)
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(oldestDate, today).toInt().coerceIn(7, 365)
    val days = (0..totalDays).map { today.minusDays(it.toLong()) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        reverseLayout = true
    ) {
        items(days, key = { it.toString() }) { date ->
            val isSelected = date == selectedDate
            val dayStatusList = habits.mapNotNull { m -> m.recent7Days.find { it.date == date } }
            val dueCount = dayStatusList.count { it.isDue }
            val doneCount = dayStatusList.count { it.isDue && (it.isCompleted || it.isForgiven) }
            val pct = if (dueCount > 0) (doneCount * 100) / dueCount else 0
            val isDayOne = date == oldestDate

            Surface(
                modifier = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onDateSelected(date) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else if (isDayOne) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEE")).take(3),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (dueCount > 0) pct / 100f else 0f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = if (dueCount > 0) "$pct%" else "—",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    model: HabitDisplayModel,
    selectedDate: LocalDate,
    isPreview: Boolean,
    gracePoolRemaining: Int,
    onToggle: (Boolean) -> Unit,
    onUseGrace: (LocalDate) -> Unit,
    onNavigateToBooks: (String?) -> Unit = {},
    viewModel: HabitsViewModel = viewModel()
) {
    val habit = model.habit
    val status = model.status
    val habitColor = parseHexColor(model.colorHex)
    val dayStatus = model.recent7Days.find { it.date == selectedDate }
    val isChecked = dayStatus?.isCompleted == true || dayStatus?.isForgiven == true

    var showSlotAlert by remember { mutableStateOf(false) }

    // Check if grace button should be displayed (today view, ungraced miss, grace available)
    val canGrace = !isPreview && !status.isMastered && status.hasUngracedMiss && gracePoolRemaining > 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header: Icon + Title + Streak Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(habitColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            HabitIconView(iconKey = habit.icon, modifier = Modifier.size(24.dp), tint = habitColor)
                        }

                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (status.streak > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = habitColor.copy(alpha = 0.18f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE07B00), modifier = Modifier.size(12.dp))
                                            Text(text = "${status.streak}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Subtitle metadata row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Text(repLabel(model.schedule), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (habit.amount != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AdsClick, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                        Text("${habit.amount} ${habit.unit ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Text(if (habit.reminderTime != null) formatTime(habit.reminderTime) else habit.slot, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 7-Day Dots Row (.dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        model.recent7Days.forEach { ds ->
                            val dotColor = when {
                                ds.isCompleted -> habitColor
                                ds.isForgiven -> Color(0xFF7D5CFF).copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    // Automaticity progress bar (.autoRow)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(status.automaticity.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(habitColor)
                            )
                        }

                        if (status.recoveryActive) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                                    Text("${status.recoveryProgress}/${status.recoveryRequired}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Text(
                                text = "${(status.automaticity * 100).toInt()}% auto",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Use a Grace Day Button (.graceUse)
                    if (canGrace) {
                        Surface(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { onUseGrace(selectedDate) },
                            shape = RoundedCornerShape(99.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text("Use a grace day", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Check Circle Button (.chk)
                IconButton(
                    onClick = {
                        if (!isPreview) {
                            if (viewModel.validateCheckIn(habit, selectedDate)) {
                                onToggle(!isChecked)
                            } else {
                                showSlotAlert = true
                            }
                        }
                    },
                    enabled = !isPreview,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isChecked) habitColor else Color.Transparent)
                        .border(2.dp, if (isChecked) habitColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    if (isChecked) {
                        Icon(Icons.Default.Check, contentDescription = "Checked", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            if (status.recoveryActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Missed day · Recovery challenge", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Your journey: ████████████░░░░", style = MaterialTheme.typography.labelSmall)
                        Text("Progress remains: ████████████████", style = MaterialTheme.typography.labelSmall)
                        Text("Momentum: Slightly affected", style = MaterialTheme.typography.labelSmall)
                        Text(viewModel.getMissedDayCostMessage(habit.category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Reading Habit Progress & Book Links
            val uiStateVal by viewModel.uiState.collectAsState()
            val readingSummary = uiStateVal.readingProgressMap[habit.id]
            val isReading = habit.isReadingHabit || readingSummary != null || habit.icon in listOf("book", "open-book", "menu-book")

            if (isReading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (readingSummary?.isCompleted == true || isChecked) "✓ Reading habit completed"
                                else if (readingSummary != null) "📖 Reading · ${readingSummary.currentAmount} / ${readingSummary.targetAmount} ${readingSummary.unit} · ${readingSummary.remainingAmount} ${readingSummary.unit} remaining"
                                else "📖 Reading habit active in My Books",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val linkedBooks = uiStateVal.availableBooks.filter {
                            habit.linkedBookIds.contains(it.id)
                        }

                        if (habit.linkedBookIds.isNotEmpty()) {
                            Text(
                                text = "Linked books:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            linkedBooks.forEach { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToBooks(book.id) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "• ${book.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Linked: Any book in My Books",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val firstBookId = habit.linkedBookIds.firstOrNull()
                                    onNavigateToBooks(firstBookId)
                                }
                            ) {
                                Text("Open My Books", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showSlotAlert) {
            AlertDialog(
                onDismissRequest = { showSlotAlert = false },
                title = { Text("⏰ Not available yet") },
                text = { Text("This habit is scheduled for your ${habit.slot} slot (${viewModel.getSlotWindowLabel(habit.slot)}). You can complete it during its scheduled time.") },
                confirmButton = {
                    TextButton(onClick = { showSlotAlert = false }) { Text("Got it") }
                }
            )
        }
    }
}

@Composable
fun RestHabitCard(model: HabitDisplayModel, selectedDate: LocalDate) {
    val habit = model.habit

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    HabitIconView(iconKey = habit.icon, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(repLabel(model.schedule), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (habit.amount != null) {
                            Text("· ${habit.amount} ${habit.unit ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Text(nextDueLabel(model.schedule, selectedDate), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MasteredHabitCard(model: HabitDisplayModel) {
    val habit = model.habit
    val done = model.status.doneCount
    val target = maxOf(1, model.status.effTarget)
    val times = (done.toFloat() / target.toFloat())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    HabitIconView(iconKey = habit.icon, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text("${String.format(java.util.Locale.getDefault(), "%.1f", times)}× target", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("$done reps — fully automatic", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Text("Retired", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
