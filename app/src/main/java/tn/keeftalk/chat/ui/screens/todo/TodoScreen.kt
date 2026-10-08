package tn.keeftalk.chat.ui.screens.todo

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.todo.TodoTask
import tn.keeftalk.chat.ui.theme.LucideIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val C1 = Color(0xFF00CCCC)
val C2 = Color(0xFF7D5CFF)
val Hi = Color(0xFFFF4D7A)
val CardBg = Color(0xFF111628)
val SoftBg = Color(0xFF18203a)
val ScrBg = Color(0xFF0a0e1e)
val TextColor = Color(0xFFeef1ff)
val MutedColor = Color(0xFF8d97bb)
val LineColor = Color(0xFF1e2742)

@OptIn(ExperimentalMaterial3Api::class)
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

    val listState = rememberLazyListState()
    val scrollOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    val headerShrink = remember(scrollOffset) { (scrollOffset.value / 80f).coerceIn(0f, 1f) }

    val todayStr = remember { LocalDate.now().toString() }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrBg)
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
                        if (isAllDone) Brush.linearGradient(listOf(C2, C1, Color(0xFF2ED9A0)))
                        else Brush.linearGradient(listOf(C1, C2))
                    )
                    .padding(heroPadding)
            ) {
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
                                onClick = { viewModel.setView("up") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = C2),
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

            // View Segment Switcher (seg4) - compact height
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
                ViewTab("Upcoming", currentView == "up") { viewModel.setView("up") }
                ViewTab("All", currentView == "all") { viewModel.setView("all") }
                ViewTab("Done", currentView == "done") { viewModel.setView("done") }
                ViewTab("Stats", currentView == "stats") { viewModel.setView("stats") }
            }

            // Chips & Search toggle
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
                viewModel.defaultLists.forEach { l ->
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
                        StatsView(activeTasks, viewModel)
                    }
                } else {
                    val filtered = activeTasks.filter { t ->
                        (selectedList == "all" || t.listId == selectedList) &&
                                (searchQuery.isBlank() || (t.title + " " + t.notes).contains(searchQuery, ignoreCase = true))
                    }

                    val overdue = filtered.filter { !it.completed && it.dueDate.isNotEmpty() && it.dueDate < todayStr }
                    val today = filtered.filter { !it.completed && it.dueDate == todayStr }
                    val upcoming = filtered.filter { !it.completed && it.dueDate.isNotEmpty() && it.dueDate > todayStr }
                    val noDate = filtered.filter { !it.completed && it.dueDate.isEmpty() }
                    val completedTasks = filtered.filter { it.completed }.sortedByDescending { it.completedAt }

                    if (currentView == "today") {
                        item { TaskGroup("Overdue", overdue, viewModel, isOverdue = true) }
                        item { TaskGroup("Today", today, viewModel) }
                    } else if (currentView == "up") {
                        item { TaskGroup("Upcoming", upcoming, viewModel) }
                        item { TaskGroup("No date", noDate, viewModel) }
                    } else if (currentView == "all") {
                        item { TaskGroup("Overdue", overdue, viewModel, isOverdue = true) }
                        item { TaskGroup("Today", today, viewModel) }
                        item { TaskGroup("Upcoming", upcoming, viewModel) }
                        item { TaskGroup("No date", noDate, viewModel) }
                        item {
                            Column {
                                TextButton(onClick = { /* toggle completed in all */ }) {
                                    Text("Completed (${completedTasks.size})", color = C2, fontWeight = FontWeight.Bold)
                                }
                                completedTasks.forEach { task ->
                                    TaskItem(task, viewModel)
                                }
                            }
                        }
                    } else if (currentView == "done") {
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

                    if (filtered.isEmpty() || (currentView == "today" && overdue.isEmpty() && today.isEmpty()) || (currentView == "done" && completedTasks.isEmpty())) {
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
                    placeholder = { Text("Add a task, try “Call Sam tomorrow #work !high”", color = MutedColor, fontSize = 14.sp) },
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
                    singleLine = true
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

                        Text("Due", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MutedColor)
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
                        }

                        Text("List", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            viewModel.defaultLists.forEach { l ->
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else MutedColor)
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

@Composable
fun TaskItem(task: TodoTask, viewModel: TodoViewModel) {
    val listMeta = viewModel.getListMeta(task.listId)
    val listColor = remember(listMeta) { Color(android.graphics.Color.parseColor(listMeta.c)) }
    val formattedDue = viewModel.formatDueDate(task.dueDate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, LineColor, RoundedCornerShape(18.dp))
            .clickable { viewModel.openSheet(task.id) }
            .padding(12.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox button reduced size (20x20 circle, 2px border)
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (task.priority == 2) Hi else C1, CircleShape)
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
                if (task.priority == 2 && !task.completed) {
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
fun StatsView(tasks: List<TodoTask>, viewModel: TodoViewModel) {
    val completedTasks = remember(tasks) { tasks.filter { it.completed } }
    val totalCount = tasks.size
    val completedCount = completedTasks.size
    val completionRate = if (totalCount > 0) (completedCount * 100) / totalCount else 0
    val overdueCount = remember(tasks) { tasks.filter { !it.completed && it.dueDate.isNotEmpty() && it.dueDate < LocalDate.now().toString() }.size }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grid KPI cards matching HTML stats grid (.kg, .kp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricBox("Completed", completedCount.toString(), Modifier.weight(1f))
            StatMetricBox("Completion rate", "$completionRate%", Modifier.weight(1f), isGradient = true)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricBox("Current streak", "0 days", Modifier.weight(1f))
            StatMetricBox("Best streak", "0 days", Modifier.weight(1f))
        }

        // By priority breakdown card matching HTML stats priority section
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBg,
            border = BorderStroke(1.dp, LineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("By priority", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)
                listOf(Triple(2, "High", Hi), Triple(1, "Medium", C1), Triple(0, "Low", MutedColor)).forEach { (p, name, color) ->
                    val pTasks = tasks.filter { it.priority == p }
                    val pDone = pTasks.filter { it.completed }.size
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextColor)
                        Text("$pDone/${pTasks.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                    }
                }
            }
        }

        // When you get things done card matching HTML stats chart
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBg,
            border = BorderStroke(1.dp, LineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("When you get things done", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)

                // Weekly activity bar chart (M, T, W, T, F, S, S)
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    val sampleHeights = listOf(0.2f, 0.3f, 0.25f, 0.4f, 0.15f, 0.9f, 0.3f)
                    days.forEachIndexed { idx, day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            val bgMod = if (idx == 5) {
                                Modifier.background(Brush.linearGradient(listOf(C1, C2)))
                            } else {
                                Modifier.background(SoftBg)
                            }
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(sampleHeights[idx])
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(bgMod)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(day, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                listOf("Morning" to "28%", "Afternoon" to "41%", "Evening" to "31%", "Late night" to "0%").forEach { (time, pct) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(time, fontSize = 12.sp, color = TextColor, fontWeight = FontWeight.Bold)
                        Text(pct, fontSize = 12.sp, color = MutedColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Insights card matching HTML stats insights
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBg,
            border = BorderStroke(1.dp, LineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Insights", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextColor)
                InsightRow("You completed $completedCount tasks this week.")
                InsightRow(if (overdueCount > 0) "$overdueCount overdue task(s) need attention." else "No overdue tasks. Nice! $totalCount open in total.")
                InsightRow("0 tasks with reminders, 0 already sent.")
            }
        }
    }
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
