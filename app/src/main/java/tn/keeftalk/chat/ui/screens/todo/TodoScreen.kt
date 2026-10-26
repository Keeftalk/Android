package tn.keeftalk.chat.ui.screens.todo

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.todo.TodoTask
import tn.keeftalk.chat.ui.theme.FabGradient
import tn.keeftalk.chat.ui.theme.FabPurple
import tn.keeftalk.chat.ui.theme.LucideIcons
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

val C1 @Composable get() = MaterialTheme.colorScheme.primary
val C2 @Composable get() = MaterialTheme.colorScheme.secondary
val Hi @Composable get() = MaterialTheme.colorScheme.error
val CardBg @Composable get() = MaterialTheme.colorScheme.surface
val SoftBg @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val ScrBg @Composable get() = MaterialTheme.colorScheme.background
val TextColor @Composable get() = MaterialTheme.colorScheme.onBackground
val MutedColor @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val LineColor @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

val UrgentColor = Color(0xFFFF2A4B)

class TodoInputVisualTransformation(private val availableLists: List<TodoListMeta>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val builder = AnnotatedString.Builder(originalText)

        val words = originalText.split(Regex("(?<=\\s)|(?=\\s)"))
        var currentIndex = 0
        for (word in words) {
            val trimmed = word.trim()
            val lower = trimmed.lowercase()
            val isRecognized = lower == "#tomorrow" || lower == "tomorrow" || lower == "tmrw" ||
                    lower == "#today" || lower == "today" ||
                    lower == "!urgent" || lower == "!!!" ||
                    lower == "!high" || lower == "!!" ||
                    lower == "!low" ||
                    (lower.startsWith("reminder@") && isValidTimeToken(lower.removePrefix("reminder@"))) ||
                    (lower.startsWith("@") && isValidTimeToken(lower.removePrefix("@"))) ||
                    (lower.startsWith("#") && lower.length > 1 && availableLists.any { it.id.startsWith(lower.removePrefix("#")) || it.n.lowercase().startsWith(lower.removePrefix("#")) })

            if (isRecognized && trimmed.isNotEmpty()) {
                val start = originalText.indexOf(trimmed, currentIndex)
                if (start >= 0) {
                    builder.addStyle(
                        style = SpanStyle(color = Color.Gray.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold),
                        start = start,
                        end = start + trimmed.length
                    )
                }
            }
            currentIndex += word.length
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun isValidTimeToken(token: String): Boolean {
        val parts = token.split(":")
        if (parts.size != 2) return false
        val h = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        return h in 0..23 && m in 0..59
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    onBack: () -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentView by viewModel.currentView.collectAsState()
    val selectedList by viewModel.selectedList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val quickAddText by viewModel.quickAddText.collectAsState()
    val showSearchBox by viewModel.showSearchBox.collectAsState()
    val isSheetOpen by viewModel.isSheetOpen.collectAsState()
    val editingTaskId by viewModel.editingTaskId.collectAsState()

    val allListMetas by viewModel.allListMetas.collectAsState()
    val showAddListDialog by viewModel.showAddListDialog.collectAsState()

    val showDatePickerForUpcoming by viewModel.showDatePickerForUpcoming.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scrollOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    val headerShrink = remember(scrollOffset) { (scrollOffset.value / 80f).coerceIn(0f, 1f) }

    val todayStr = remember { LocalDate.now().toString() }
    val tomorrowStr = remember { LocalDate.now().plusDays(1).toString() }

    val activeTasks = remember(tasks) { tasks.filter { !it.deleted } }
    val incompleteTasks = remember(activeTasks) { activeTasks.filter { !it.completed } }

    val todaySet = remember(activeTasks, todayStr) {
        activeTasks.filter { t ->
            t.dueDate == todayStr || (!t.completed && t.dueDate.isNotEmpty() && t.dueDate < todayStr) || (t.completed && t.completedAt > 0 && LocalDate.ofEpochDay(t.completedAt / 86400000).toString() == todayStr)
        }
    }
    val todayCompleted = remember(todaySet) { todaySet.filter { it.completed }.size }
    val progressPercent = remember(todaySet, todayCompleted) {
        if (todaySet.isEmpty()) 0 else (todayCompleted * 100) / todaySet.size
    }

    val overdueCount = remember(incompleteTasks, todayStr) { incompleteTasks.filter { t -> t.dueDate.isNotEmpty() && t.dueDate < todayStr }.size }
    val dueTodayCount = remember(incompleteTasks, todayStr) { incompleteTasks.filter { t -> t.dueDate == todayStr }.size }
    val upcomingCount = remember(incompleteTasks, todayStr) { incompleteTasks.filter { t -> t.dueDate.isNotEmpty() && t.dueDate > todayStr }.size }

    val isAllDone = todaySet.isNotEmpty() && progressPercent == 100

    if (showHelpDialog) {
        HelpSyntaxDialog(onDismiss = { showHelpDialog = false })
    }

    if (showAddListDialog) {
        CreateListDialog(
            onDismiss = { viewModel.showAddListDialog.value = false },
            onCreate = { name, colorHex ->
                viewModel.addNewList(name, colorHex)
            }
        )
    }

    if (showDatePickerForUpcoming) {
        DatePickerPromptDialog(
            onDismiss = { viewModel.showDatePickerForUpcoming.value = false },
            onDateSelected = { selectedDate ->
                viewModel.confirmUpcomingTaskWithDate(selectedDate)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp)
        ) {
            // Header Hero Section (shrinks on scroll)
            val heroPadding = 18.dp - (8.dp * headerShrink)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isAllDone) Brush.linearGradient(listOf(FabPurple, tn.keeftalk.chat.ui.theme.FabCyan, Color(0xFF2ED9A0)))
                        else FabGradient
                    )
                    .padding(heroPadding)
            ) {
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(LucideIcons.Sparkles, contentDescription = "Todo Syntax & Priorities", tint = Color.White)
                }

                if (isAllDone) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(LucideIcons.Trophy, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                            Column {
                                Text("All done for today!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${todaySet.size} of ${todaySet.size} tasks finished", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.setView("tomorrow") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = FabPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Plan tomorrow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.setView("stats") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.22f), contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("See my stats", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Circular Progress
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(68.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressPercent / 100f },
                                    modifier = Modifier.size(68.dp),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.28f),
                                    strokeWidth = 7.dp
                                )
                                Text("$progressPercent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column {
                                Text(
                                    text = if (todaySet.isNotEmpty()) "$todayCompleted of ${todaySet.size}" else "No tasks yet",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (todaySet.isEmpty()) "Add something for today" else if (progressPercent == 100) "All clear, great work!" else "${todaySet.size - todayCompleted} left to go",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        if (headerShrink < 0.5f) {
                            Spacer(modifier = Modifier.height(10.dp))
                            // Mini stats boxes
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniStatBox("Overdue", overdueCount, Modifier.weight(1f))
                                MiniStatBox("Due today", dueTodayCount, Modifier.weight(1f))
                                MiniStatBox("Upcoming", upcomingCount, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // View Segment Switcher in exact order: Today, Tomorrow, Upcoming, All, Done, Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .border(1.dp, LineColor, RoundedCornerShape(14.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ViewTab("Today", currentView == "today") { viewModel.setView("today") }
                ViewTab("Tomorrow", currentView == "tomorrow") { viewModel.setView("tomorrow") }
                ViewTab("Upcoming", currentView == "up") { viewModel.setView("up") }
                ViewTab("All", currentView == "all") { viewModel.setView("all") }
                ViewTab("Done", currentView == "done") { viewModel.setView("done") }
                ViewTab("Stats", currentView == "stats") { viewModel.setView("stats") }
            }

            // Chips & Search toggle & Custom Lists
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showSearchBox,
                    onClick = { viewModel.toggleSearchBox() },
                    label = { Icon(LucideIcons.Search, contentDescription = "Search", modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = CardBg, selectedContainerColor = C2)
                )
                FilterChip(
                    selected = selectedList == "all",
                    onClick = { viewModel.setSelectedList("all") },
                    label = { Text("All lists", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = CardBg, selectedContainerColor = C2)
                )
                allListMetas.forEach { l ->
                    FilterChip(
                        selected = selectedList == l.id,
                        onClick = { viewModel.setSelectedList(l.id) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(l.c))))
                                Text(l.n, fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(containerColor = CardBg, selectedContainerColor = C2)
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { viewModel.showAddListDialog.value = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "New list", modifier = Modifier.size(14.dp))
                            Text("New list", fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(containerColor = CardBg)
                )
            }

            if (showSearchBox) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search tasks", color = MutedColor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor,
                        focusedBorderColor = C2,
                        unfocusedBorderColor = LineColor
                    )
                )
            }

            // Main Content Scroll
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentView == "stats") {
                    item {
                        StatsView(viewModel)
                    }
                } else {
                    val filtered = activeTasks.filter { t ->
                        (selectedList == "all" || t.listId == selectedList) &&
                                (searchQuery.isBlank() || (t.title + " " + t.notes).contains(searchQuery, ignoreCase = true))
                    }

                    val overdue = filtered.filter { !it.completed && it.dueDate.isNotEmpty() && it.dueDate < todayStr }
                    val today = filtered.filter { !it.completed && it.dueDate == todayStr }
                    val tomorrow = filtered.filter { !it.completed && it.dueDate == tomorrowStr }
                    val upcoming = filtered.filter { !it.completed && it.dueDate.isNotEmpty() && it.dueDate > tomorrowStr }
                    val noDate = filtered.filter { !it.completed && it.dueDate.isEmpty() }
                    val completedTasks = filtered.filter { it.completed }.sortedByDescending { it.completedAt }

                    when (currentView) {
                        "today" -> {
                            item { TaskGroup("Overdue", overdue, viewModel, isOverdue = true) }
                            item { TaskGroup("Today", today, viewModel) }
                        }
                        "tomorrow" -> {
                            item { TaskGroup("Tomorrow", tomorrow, viewModel) }
                        }
                        "up" -> {
                            val upcomingGroups = groupUpcomingTasksByRange(upcoming)
                            upcomingGroups.forEach { group ->
                                item { TaskGroup(group.first, group.second, viewModel) }
                            }
                        }
                        "all" -> {
                            item { TaskGroup("Overdue", overdue, viewModel, isOverdue = true) }
                            item { TaskGroup("Today", today, viewModel) }
                            item { TaskGroup("Tomorrow", tomorrow, viewModel) }
                            val upcomingGroups = groupUpcomingTasksByRange(upcoming)
                            upcomingGroups.forEach { group ->
                                item { TaskGroup("Upcoming - ${group.first}", group.second, viewModel) }
                            }
                            item { TaskGroup("No date", noDate, viewModel) }
                            item {
                                Column {
                                    TextButton(onClick = { /* toggle completed */ }) {
                                        Text("Completed (${completedTasks.size})", color = C2, fontWeight = FontWeight.Bold)
                                    }
                                    completedTasks.forEach { task ->
                                        TaskItem(task, viewModel)
                                    }
                                }
                            }
                        }
                        "done" -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Completed", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 13.sp)
                                    TextButton(onClick = { viewModel.clearCompleted() }) {
                                        Text("Clear all", color = Hi, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            items(completedTasks, key = { it.id }) { task ->
                                TaskItem(task, viewModel)
                            }
                        }
                    }

                    val isSectionEmpty = when (currentView) {
                        "today" -> overdue.isEmpty() && today.isEmpty()
                        "tomorrow" -> tomorrow.isEmpty()
                        "up" -> upcoming.isEmpty()
                        "done" -> completedTasks.isEmpty()
                        else -> filtered.isEmpty()
                    }

                    if (isSectionEmpty) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(LucideIcons.Sparkles, contentDescription = null, tint = MutedColor, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("A fresh start", fontWeight = FontWeight.Bold, color = TextColor, fontSize = 16.sp)
                                    Text("Type a task below to get going", color = MutedColor, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Bar / Quick Add
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, ScrBg)))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(1.dp, LineColor, RoundedCornerShape(20.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickAddText,
                    onValueChange = { viewModel.quickAddText.value = it },
                    placeholder = { Text("Add a task, try “Call Sam tomorrow reminder@14:30 !urgent”", color = MutedColor, fontSize = 13.5.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor
                    ),
                    singleLine = true,
                    visualTransformation = TodoInputVisualTransformation(allListMetas)
                )
                Button(
                    onClick = { viewModel.addNewTask(quickAddText) },
                    enabled = quickAddText.isNotBlank(),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(C1, C2))),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(LucideIcons.Send, contentDescription = "Add task", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Edit Task Bottom Sheet
        if (isSheetOpen && editingTaskId != null) {
            val task = tasks.find { it.id == editingTaskId }
            if (task != null) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.closeSheet() },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = CardBg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = task.title,
                            onValueChange = { title -> viewModel.updateEditingTask { it.copy(title = title) } },
                            placeholder = { Text("Task title", color = MutedColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextColor,
                                unfocusedTextColor = TextColor
                            )
                        )
                        OutlinedTextField(
                            value = task.notes,
                            onValueChange = { notes -> viewModel.updateEditingTask { it.copy(notes = notes) } },
                            placeholder = { Text("Add notes", color = MutedColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SoftBg),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SoftBg,
                                unfocusedContainerColor = SoftBg,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextColor,
                                unfocusedTextColor = TextColor
                            ),
                            maxLines = 3
                        )

                        Text("Due Date", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DuePill("Today", task.dueDate == LocalDate.now().toString()) {
                                viewModel.updateEditingTask { it.copy(dueDate = LocalDate.now().toString()) }
                            }
                            DuePill("Tomorrow", task.dueDate == LocalDate.now().plusDays(1).toString()) {
                                viewModel.updateEditingTask { it.copy(dueDate = LocalDate.now().plusDays(1).toString()) }
                            }
                            DuePill("No date", task.dueDate.isEmpty()) {
                                viewModel.updateEditingTask { it.copy(dueDate = "") }
                            }
                        }

                        Text("Priority", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PriorityPill("Low", task.priority == 0) { viewModel.updateEditingTask { it.copy(priority = 0) } }
                            PriorityPill("Medium", task.priority == 1) { viewModel.updateEditingTask { it.copy(priority = 1) } }
                            PriorityPill("High", task.priority == 2) { viewModel.updateEditingTask { it.copy(priority = 2) } }
                            PriorityPill("Urgent", task.priority == 3) { viewModel.updateEditingTask { it.copy(priority = 3) } }
                        }

                        Text("List / Container", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allListMetas.forEach { l ->
                                FilterChip(
                                    selected = task.listId == l.id,
                                    onClick = { viewModel.updateEditingTask { it.copy(listId = l.id) } },
                                    label = { Text(l.n) },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = SoftBg, selectedContainerColor = C2)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.deleteTask(task.id)
                                    viewModel.closeSheet()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Hi.copy(alpha = 0.14f), contentColor = Hi),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delete", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.closeSheet() },
                                colors = ButtonDefaults.buttonColors(containerColor = C1),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateListDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = listOf("#7D5CFF", "#00CCCC", "#FF8A5B", "#2ED9A0", "#FF5722", "#E91E63", "#9C27B0", "#3F51B5", "#FFC107", "#4CAF50")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New List / Container", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("List name (e.g. Study, Fitness)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Pick Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = if (selectedColor == hex) TextColor else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Create List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun HelpSyntaxDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(LucideIcons.Sparkles, contentDescription = null, tint = C1)
                Text("Todo Syntax & Priorities", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Scheduling Shortcuts:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextColor)
                Text("• #Today or today → Schedules for today\n• #Tomorrow or tmrw → Schedules for tomorrow\n• reminder@14:30 or @09:00 → Sets reminder time", fontSize = 12.5.sp, color = MutedColor)

                Text("List Tags:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextColor)
                Text("• #work, #personal, #shop, #health → Assigns task to list", fontSize = 12.5.sp, color = MutedColor)

                Text("Priority Levels:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextColor)
                Text("• !low → Low priority\n• !high or !! → High priority\n• !urgent or !!! → Urgent priority (pulsing red border)", fontSize = 12.5.sp, color = MutedColor)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", fontWeight = FontWeight.Bold, color = C1)
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun DatePickerPromptDialog(onDismiss: () -> Unit, onDateSelected: (String) -> Unit) {
    var selectedDateStr by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date for Upcoming Task", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("When is this upcoming task due?", fontSize = 13.sp, color = MutedColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedDateStr == LocalDate.now().plusDays(2).toString(),
                        onClick = { selectedDateStr = LocalDate.now().plusDays(2).toString() },
                        label = { Text("In 2 Days") }
                    )
                    FilterChip(
                        selected = selectedDateStr == LocalDate.now().plusDays(7).toString(),
                        onClick = { selectedDateStr = LocalDate.now().plusDays(7).toString() },
                        label = { Text("Next Week") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDateStr) }) {
                Text("Confirm Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = CardBg
    )
}

@Composable
fun MiniStatBox(label: String, value: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.17f))
            .padding(4.dp, 6.dp)
    ) {
        Column {
            Text(value.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun ViewTab(title: String, selected: Boolean, onClick: () -> Unit) {
    val mod = if (selected) {
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(C1, C2)))
    } else {
        Modifier.clip(RoundedCornerShape(10.dp))
    }
    Box(
        modifier = mod
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else MutedColor)
    }
}

@Composable
fun TaskGroup(title: String, tasks: List<TodoTask>, viewModel: TodoViewModel, isOverdue: Boolean = false) {
    if (tasks.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isOverdue) Hi else TextColor)
            Text(tasks.size.toString(), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MutedColor)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tasks.forEach { task ->
                TaskItem(task, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(task: TodoTask, viewModel: TodoViewModel) {
    val context = LocalContext.current
    val listMeta = viewModel.getListMeta(task.listId)
    val listColor = remember(listMeta) { Color(android.graphics.Color.parseColor(listMeta.c)) }
    val formattedDue = viewModel.formatDueDate(task.dueDate)

    val accessibilityManager = remember { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager }
    val isTouchExploration = remember { accessibilityManager?.isTouchExplorationEnabled == true }

    val isUrgentIncomplete = task.priority == 3 && !task.completed

    val borderModifier = if (isUrgentIncomplete) {
        if (isTouchExploration) {
            Modifier.border(2.dp, UrgentColor, RoundedCornerShape(18.dp))
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "urgentPulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Modifier.border(2.dp, UrgentColor.copy(alpha = alpha), RoundedCornerShape(18.dp))
        }
    } else {
        Modifier.border(1.dp, LineColor, RoundedCornerShape(18.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .then(borderModifier)
            .combinedClickable(
                onClick = { viewModel.openSheet(task.id) },
                onLongClick = { viewModel.openSheet(task.id) }
            )
            .padding(12.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox button
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (task.priority == 3 && !task.completed) UrgentColor else if (task.priority == 2) Hi else C1, CircleShape)
                .clickable { viewModel.toggleTaskCompletion(task.id) },
            contentAlignment = Alignment.Center
        ) {
            if (task.completed) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(C1, C2))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (task.completed) MutedColor else TextColor,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.dueDate.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = SoftBg
                    ) {
                        Text(
                            text = formattedDue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedColor,
                            modifier = Modifier.padding(8.dp, 2.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = SoftBg
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp, 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(listColor))
                        Text(listMeta.n, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                    }
                }
                if (task.priority == 3 && !task.completed) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = UrgentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "Urgent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = UrgentColor,
                            modifier = Modifier.padding(8.dp, 2.dp)
                        )
                    }
                } else if (task.priority == 2 && !task.completed) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = Hi.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "High",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Hi,
                            modifier = Modifier.padding(8.dp, 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsView(viewModel: TodoViewModel) {
    val stats by viewModel.statistics.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grid KPI cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricBox("Completed", stats.totalCompleted.toString(), Modifier.weight(1f))
            StatMetricBox("Completion rate", "${stats.completionRate}%", Modifier.weight(1f), isGradient = true)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricBox("Current streak", "${stats.currentStreak} days", Modifier.weight(1f))
            StatMetricBox("Best streak", "${stats.bestStreak} days", Modifier.weight(1f))
        }

        // By priority breakdown
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBg,
            border = BorderStroke(1.dp, LineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("By priority", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)
                listOf(
                    Triple(3, "Urgent", UrgentColor),
                    Triple(2, "High", Hi),
                    Triple(1, "Medium", C1),
                    Triple(0, "Low", MutedColor)
                ).forEach { (p, name, color) ->
                    val counts = stats.priorityCounts[p] ?: (0 to 0)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                        Text("${counts.first}/${counts.second}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                    }
                }
            }
        }

        // Real data chart
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBg,
            border = BorderStroke(1.dp, LineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("When you get things done", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)

                // Real weekly activity bar chart (M, T, W, T, F, S, S)
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { idx, day ->
                        val heightFraction = stats.dayOfWeekHeights.getOrElse(idx) { 0.05f }
                        val rawCount = stats.dayOfWeekCounts.getOrElse(idx) { 0 }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (rawCount > 0) {
                                Text(rawCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val barModifier = if (rawCount > 0) {
                                Modifier.background(Brush.linearGradient(listOf(C1, C2)))
                            } else {
                                Modifier.background(SoftBg)
                            }
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(barModifier)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                stats.timeOfDayPercentages.forEach { (time, pct) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(time, fontSize = 12.sp, color = TextColor, fontWeight = FontWeight.Bold)
                        Text("$pct%", fontSize = 12.sp, color = MutedColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Real Insights card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBg,
            border = BorderStroke(1.dp, LineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Insights", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)
                InsightRow("You completed ${stats.completedThisWeek} task(s) this week.")
                InsightRow(if (stats.overdueCount > 0) "${stats.overdueCount} overdue task(s) need attention." else "No overdue tasks. Great job!")
                InsightRow("${stats.activeRemindersCount} task(s) have active reminders scheduled.")
            }
        }
    }
}

private fun groupUpcomingTasksByRange(tasks: List<TodoTask>): List<Pair<String, List<TodoTask>>> {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val endOfThisWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val startOfNextWeek = endOfThisWeek.plusDays(1)
    val endOfNextWeek = startOfNextWeek.plusDays(6)
    val endOfThisYear = today.with(TemporalAdjusters.lastDayOfYear())

    val thisWeekTasks = mutableListOf<TodoTask>()
    val nextWeekTasks = mutableListOf<TodoTask>()
    val thisYearTasks = mutableListOf<TodoTask>()
    val nextYearTasks = mutableListOf<TodoTask>()

    tasks.filter { t ->
        if (t.dueDate.isBlank()) return@filter false
        val date = runCatching { LocalDate.parse(t.dueDate) }.getOrNull() ?: return@filter false
        date.isAfter(tomorrow)
    }.sortedBy { it.dueDate }.forEach { t ->
        val date = LocalDate.parse(t.dueDate)
        when {
            !date.isAfter(endOfThisWeek) -> thisWeekTasks.add(t)
            !date.isBefore(startOfNextWeek) && !date.isAfter(endOfNextWeek) -> nextWeekTasks.add(t)
            !date.isAfter(endOfThisYear) -> thisYearTasks.add(t)
            else -> nextYearTasks.add(t)
        }
    }

    val result = mutableListOf<Pair<String, List<TodoTask>>>()
    if (thisWeekTasks.isNotEmpty()) result.add("This Week" to thisWeekTasks)
    if (nextWeekTasks.isNotEmpty()) result.add("Next Week" to nextWeekTasks)
    if (thisYearTasks.isNotEmpty()) result.add("This Year" to thisYearTasks)
    if (nextYearTasks.isNotEmpty()) result.add("Next Year" to nextYearTasks)

    return result
}

@Composable
fun InsightRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(LucideIcons.Sparkles, contentDescription = null, tint = C1, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Text(text, fontSize = 12.5.sp, color = MutedColor, lineHeight = 17.sp)
    }
}

@Composable
fun StatMetricBox(label: String, value: String, modifier: Modifier = Modifier, isGradient: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, LineColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGradient) C1 else TextColor
            )
            Text(label, fontSize = 11.sp, color = MutedColor)
        }
    }
}

@Composable
fun DuePill(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(containerColor = SoftBg, selectedContainerColor = C2)
    )
}

@Composable
fun PriorityPill(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(containerColor = SoftBg, selectedContainerColor = C2)
    )
}
