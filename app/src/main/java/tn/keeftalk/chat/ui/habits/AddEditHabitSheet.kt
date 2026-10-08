package tn.keeftalk.chat.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.domain.model.habit.Habit
import java.time.DayOfWeek
import java.time.LocalTime

val LUCIDE_ICONS = mapOf(
    "leaf" to Icons.Default.Eco,
    "pulse" to Icons.Default.MonitorHeart,
    "book" to Icons.Default.MenuBook,
    "pen" to Icons.Default.Edit,
    "dumbbell" to Icons.Default.FitnessCenter,
    "heart" to Icons.Default.Favorite,
    "bolt" to Icons.Default.Bolt,
    "target" to Icons.Default.AdsClick,
    "drop" to Icons.Default.WaterDrop,
    "moon" to Icons.Default.DarkMode,
    "spark" to Icons.Default.AutoAwesome,
    "clock" to Icons.Default.Schedule,
    "trophy" to Icons.Default.EmojiEvents,
    "medal" to Icons.Default.MilitaryTech,
    "calendar" to Icons.Default.CalendarToday,
    "trend" to Icons.Default.TrendingUp,
    "check" to Icons.Default.Check,
    "sun" to Icons.Default.WbSunny,
    "cloud" to Icons.Default.Cloud,
    "database" to Icons.Default.Storage,
    "gift" to Icons.Default.CardGiftcard,
    "archive" to Icons.Default.Archive,
    "shield" to Icons.Default.Security,
    "star" to Icons.Default.Star,
    "flame" to Icons.Default.LocalFireDepartment,
    "anchor" to Icons.Default.Anchor,
    "coffee" to Icons.Default.LocalCafe,
    "music" to Icons.Default.MusicNote,
    "camera" to Icons.Default.PhotoCamera,
    "code" to Icons.Default.Code,
    "eye" to Icons.Default.Visibility,
    "flag" to Icons.Default.Flag,
    "globe" to Icons.Default.Public,
    "key" to Icons.Default.VpnKey,
    "map" to Icons.Default.Map,
    "mic" to Icons.Default.Mic,
    "smile" to Icons.Default.SentimentSatisfied,
    "tag" to Icons.Default.LocalOffer,
    "tool" to Icons.Default.Build,
    "umbrella" to Icons.Default.BeachAccess,
    "zap" to Icons.Default.FlashOn,
    "smile2" to Icons.Default.SentimentVerySatisfied,
    "alarm" to Icons.Default.Alarm,
    "battery" to Icons.Default.BatteryFull,
    "bluetooth" to Icons.Default.Bluetooth,
    "brush" to Icons.Default.Brush,
    "cake" to Icons.Default.Cake,
    "car" to Icons.Default.DirectionsCar,
    "compass" to Icons.Default.Explore,
    "feather" to Icons.Default.Create,
    "headphones" to Icons.Default.Headphones,
    "image" to Icons.Default.Image,
    "life" to Icons.Default.Help,
    "lock" to Icons.Default.Lock,
    "package" to Icons.Default.Inventory,
    "phone" to Icons.Default.Phone,
    "rss" to Icons.Default.RssFeed,
    "scissors" to Icons.Default.ContentCut,
    "send" to Icons.Default.Send,
    "settings" to Icons.Default.Settings,
    "shopping" to Icons.Default.ShoppingBag,
    "terminal" to Icons.Default.Terminal,
    "volume" to Icons.Default.VolumeUp,
    "wifi" to Icons.Default.Wifi
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitSheet(
    habitToEdit: Habit? = null,
    availableBooks: List<Book> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        icon: String,
        category: String,
        baseDays: Int,
        slot: String,
        reminderTime: LocalTime?,
        daysMask: Set<DayOfWeek>,
        habitType: String,
        amount: Int?,
        unit: String?,
        colorHex: String,
        isReadingHabit: Boolean,
        autoCountReading: Boolean,
        linkedBookIds: List<String>
    ) -> Unit
) {
    val initialReminderTimeStr = habitToEdit?.reminderTime?.let {
        String.format("%02d:%02d", it.hour, it.minute)
    } ?: "07:00"

    val initialIsCustomTime = habitToEdit?.let { h ->
        h.reminderTime != null || (h.slot != null && h.slot !in listOf("Early Morning", "Morning", "Midday", "Afternoon", "Evening", "Late Night"))
    } ?: false

    var name by remember { mutableStateOf(habitToEdit?.name ?: "") }
    var icon by remember { mutableStateOf(habitToEdit?.icon ?: "leaf") }
    var selectedColorHex by remember { mutableStateOf(habitToEdit?.colorHex ?: HABIT_COLORS[0]) }
    var amount by remember { mutableStateOf(habitToEdit?.amount?.toString() ?: "") }
    var unit by remember { mutableStateOf(habitToEdit?.unit ?: "min") }
    var slot by remember { mutableStateOf(habitToEdit?.slot ?: "Morning") }
    var isCustomTime by remember { mutableStateOf(initialIsCustomTime) }
    var customTimeStr by remember { mutableStateOf(initialReminderTimeStr) }
    var category by remember { mutableStateOf(habitToEdit?.category ?: "drink") }
    var isCatManual by remember { mutableStateOf(false) }
    var baseDaysStr by remember { mutableStateOf(habitToEdit?.baseDays?.toString() ?: "18") }

    // Reading Habit Setup State
    var autoCountReading by remember { mutableStateOf(habitToEdit?.autoCountReading ?: true) }
    var selectedBookIds by remember { mutableStateOf(habitToEdit?.linkedBookIds?.toSet() ?: emptySet()) }
    var bookSelectionMode by remember {
        mutableStateOf(if (habitToEdit?.linkedBookIds.isNullOrEmpty()) "general" else "specific")
    }

    // Reminder state
    var remOn by remember { mutableStateOf(habitToEdit?.reminderTime != null) }
    var remTimeStr by remember { mutableStateOf(initialReminderTimeStr) }

    // Repeat state
    var repeatFreq by remember { mutableStateOf("daily") }
    var selectedDays by remember {
        mutableStateOf(
            habitToEdit?.let { DayOfWeek.entries.toSet() } ?: DayOfWeek.entries.toSet()
        )
    }

    var showCatInfo by remember { mutableStateOf(false) }
    var showAllIconsDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val units = listOf("min", "hr", "pages", "km", "mi", "reps", "steps", "glasses", "kcal", "L")
    val slots = listOf("Early Morning", "Morning", "Midday", "Afternoon", "Evening", "Late Night")
    val weekdays = listOf(
        DayOfWeek.SUNDAY to "Sun",
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat"
    )

    val detectedCat = detectCat(name)
    val activeCat = if (isCatManual) category else detectedCat

    val daysOk = repeatFreq != "custom" || selectedDays.isNotEmpty()
    val isValid = name.isNotBlank() && name.length <= 28 && daysOk

    // Default 12 icons for the 2 rows x 6 icons view + More button
    val defaultIcons = listOf("leaf", "pulse", "book", "pen", "dumbbell", "heart", "bolt", "target", "drop", "moon", "spark", "clock")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Grab handle (.grab)
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = if (habitToEdit == null) "New habit" else "Edit habit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Name
            Text("Name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 28) {
                        name = it
                        if (!isCatManual) category = detectCat(it)
                    }
                },
                placeholder = { Text("Read a book") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Color picker
            Text("Color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                lazyItems(HABIT_COLORS) { cHex ->
                    val c = parseHexColor(cHex)
                    val isSelected = selectedColorHex.equals(cHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = cHex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Icon picker: 2 rows of 6 icons + More button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Icon", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showAllIconsDialog = true }) {
                    Text("More icons (${LUCIDE_ICONS.size})")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) {
                    gridItems(defaultIcons) { key ->
                        val vector = LUCIDE_ICONS[key] ?: Icons.Default.Eco
                        val isSelected = icon == key
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = key,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Target & unit
            Text("Target & unit (optional)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = { Text("20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(92.dp),
                    shape = RoundedCornerShape(14.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    lazyItems(units) { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }
            }

            // Time of day
            Text("Time of day — pick a slot or set an exact time", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                lazyItems(slots) { s ->
                    FilterChip(
                        selected = !isCustomTime && slot == s,
                        onClick = {
                            isCustomTime = false
                            slot = s
                        },
                        label = { Text(s) }
                    )
                }
                item {
                    FilterChip(
                        selected = isCustomTime,
                        onClick = { isCustomTime = true },
                        label = { Text("Custom time") }
                    )
                }
            }

            if (isCustomTime) {
                val timePickerContext = androidx.compose.ui.platform.LocalContext.current
                OutlinedTextField(
                    value = customTimeStr,
                    onValueChange = { newVal ->
                        customTimeStr = newVal
                        remTimeStr = newVal
                    },
                    label = { Text("Custom time (HH:MM)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val parts = customTimeStr.split(":").mapNotNull { it.toIntOrNull() }
                            val h = parts.getOrNull(0) ?: 7
                            val m = parts.getOrNull(1) ?: 0
                            android.app.TimePickerDialog(timePickerContext, { _, selectedH, selectedM ->
                                val formatted = String.format("%02d:%02d", selectedH, selectedM)
                                customTimeStr = formatted
                                remTimeStr = formatted
                            }, h, m, true).show()
                        }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Reminder
            val reminderContext = androidx.compose.ui.platform.LocalContext.current
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.5.dp, if (remOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = if (remOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if (remOn) "On" else "Off", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Get a gentle nudge at the right moment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = remOn,
                            onCheckedChange = { checked ->
                                remOn = checked
                                if (checked && isCustomTime && customTimeStr.isNotBlank()) {
                                    remTimeStr = customTimeStr
                                }
                            }
                        )
                    }

                    if (remOn) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable {
                                    val parts = remTimeStr.split(":").mapNotNull { it.toIntOrNull() }
                                    val h = parts.getOrNull(0) ?: 7
                                    val m = parts.getOrNull(1) ?: 0
                                    android.app.TimePickerDialog(reminderContext, { _, selectedH, selectedM ->
                                        val formatted = String.format("%02d:%02d", selectedH, selectedM)
                                        remTimeStr = formatted
                                        if (isCustomTime) {
                                            customTimeStr = formatted
                                        }
                                    }, h, m, true).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Reminder time", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = remTimeStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Repeat
            Text("Repeat", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("daily" to "Daily", "weekdays" to "Weekdays", "weekends" to "Weekends", "custom" to "Custom").forEach { (freq, label) ->
                    FilterChip(
                        selected = repeatFreq == freq,
                        onClick = {
                            repeatFreq = freq
                            selectedDays = when (freq) {
                                "daily" -> DayOfWeek.entries.toSet()
                                "weekdays" -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                                "weekends" -> setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                                else -> selectedDays
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (repeatFreq == "custom") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekdays.forEach { (dayOfWeek, label) ->
                        val isSelected = selectedDays.contains(dayOfWeek)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) selectedDays - dayOfWeek else selectedDays + dayOfWeek
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Habit type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Habit type — sets your automaticity threshold", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { showCatInfo = !showCatInfo }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (showCatInfo) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Auto-detected from your habit name. You can override it by tapping another type.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        HABIT_CATEGORIES.values.forEach { cat ->
                            Text("• ${cat.label} · ${cat.range} — ${cat.examples}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Categories grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HABIT_CATEGORIES.values.chunked(2).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCats.forEach { cat ->
                            val isSelected = activeCat == cat.key
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        category = cat.key
                                        isCatManual = true
                                        baseDaysStr = cat.days.toString()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    if (!isCatManual && cat.key == detectedCat) {
                                        Surface(
                                            shape = RoundedCornerShape(99.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Text("Auto", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(99.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = cat.badge,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(cat.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(cat.range, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(cat.shortDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(cat.examples, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                        if (rowCats.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Reading Habit Setup Section
            val isReadingHabitDetected = habitToEdit?.isReadingHabit == true ||
                    name.contains("read", ignoreCase = true) ||
                    name.contains("book", ignoreCase = true) ||
                    name.contains("page", ignoreCase = true) ||
                    icon in listOf("book", "open-book", "menu-book") ||
                    (category == "eating" && (name.contains("read", ignoreCase = true) || name.contains("book", ignoreCase = true)))

            if (isReadingHabitDetected) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Reading Habit Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Should reading in My Books count toward completing this habit automatically?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = autoCountReading,
                                onClick = { autoCountReading = true },
                                label = { Text("Yes, automatically") },
                                leadingIcon = {
                                    if (autoCountReading) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !autoCountReading,
                                onClick = { autoCountReading = false },
                                label = { Text("Complete manually") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (autoCountReading) {
                            Text(
                                text = "Book Selection",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = bookSelectionMode == "general",
                                    onClick = {
                                        bookSelectionMode = "general"
                                        selectedBookIds = emptySet()
                                    },
                                    label = { Text("Any book (General)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = bookSelectionMode == "specific",
                                    onClick = { bookSelectionMode = "specific" },
                                    label = { Text("Specific book(s)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (availableBooks.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "Your My Books library is empty. You can add books in the My Books tab to link them directly.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                if (bookSelectionMode == "specific") {
                                    Text(
                                        text = "Select books to link to this habit:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        availableBooks.forEach { book ->
                                            val isSelected = selectedBookIds.contains(book.id)
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedBookIds = if (isSelected) {
                                                            selectedBookIds - book.id
                                                        } else {
                                                            selectedBookIds + book.id
                                                        }
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            selectedBookIds = if (checked) selectedBookIds + book.id else selectedBookIds - book.id
                                                        }
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(book.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Suggested books from your My Books library:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(availableBooks) { book ->
                                            SuggestionChip(
                                                onClick = {
                                                    bookSelectionMode = "specific"
                                                    selectedBookIds = setOf(book.id)
                                                },
                                                label = { Text(book.title) },
                                                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save button
            Button(
                onClick = {
                    val baseDays = baseDaysStr.toIntOrNull() ?: HABIT_CATEGORIES[activeCat]?.days ?: 18
                    val finalSlot = if (isCustomTime) slotFromTime(customTimeStr) else slot
                    val reminderTime = if (remOn) {
                        val stringToParse = if (isCustomTime && customTimeStr.isNotBlank()) customTimeStr else remTimeStr
                        val parts = stringToParse.split(":").mapNotNull { it.toIntOrNull() }
                        val h = parts.getOrNull(0)?.coerceIn(0, 23) ?: 7
                        val m = parts.getOrNull(1)?.coerceIn(0, 59) ?: 0
                        LocalTime.of(h, m)
                    } else null

                    val amtParsed = amount.toIntOrNull()
                    onSave(
                        habitToEdit?.id,
                        name.trim(),
                        icon,
                        activeCat,
                        baseDays,
                        finalSlot,
                        reminderTime,
                        selectedDays,
                        "AUTO",
                        if (amtParsed != null && amtParsed > 0) amtParsed else null,
                        if (amtParsed != null && amtParsed > 0) unit else null,
                        selectedColorHex,
                        isReadingHabitDetected,
                        autoCountReading,
                        if (bookSelectionMode == "specific") selectedBookIds.toList() else emptyList()
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (habitToEdit == null) "Create habit" else "Save changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Text("Only today's check-ins can be edited. Past days are read-only.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Full Lucide Icons Dialog (60+ icons)
    if (showAllIconsDialog) {
        Dialog(onDismissRequest = { showAllIconsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Choose Icon (${LUCIDE_ICONS.size} icons)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        gridItems(LUCIDE_ICONS.keys.toList()) { key ->
                            val vector = LUCIDE_ICONS[key] ?: Icons.Default.Eco
                            val isSelected = icon == key
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        icon = key
                                        showAllIconsDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = key,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAllIconsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
