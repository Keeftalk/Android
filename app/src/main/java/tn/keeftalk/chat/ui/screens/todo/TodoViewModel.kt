package tn.keeftalk.chat.ui.screens.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.todo.TodoList
import tn.keeftalk.chat.domain.model.todo.TodoTask
import tn.keeftalk.chat.domain.repository.TodoRepository
import tn.keeftalk.chat.util.SoundEffectManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class TodoListMeta(val id: String, val n: String, val c: String)

data class TodoStatistics(
    val totalCreated: Int = 0,
    val totalCompleted: Int = 0,
    val completionRate: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val priorityCounts: Map<Int, Pair<Int, Int>> = emptyMap(),
    val dayOfWeekHeights: List<Float> = listOf(0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f),
    val dayOfWeekCounts: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val timeOfDayPercentages: Map<String, Int> = mapOf("Morning" to 0, "Afternoon" to 0, "Evening" to 0, "Late night" to 0),
    val completedThisWeek: Int = 0,
    val overdueCount: Int = 0,
    val activeRemindersCount: Int = 0
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository = AppModule.provideTodoRepository(application)

    val defaultLists = listOf(
        TodoListMeta("personal", "Personal", "#7D5CFF"),
        TodoListMeta("work", "Work", "#00CCCC"),
        TodoListMeta("shop", "Shopping", "#FF8A5B"),
        TodoListMeta("health", "Health", "#2ED9A0")
    )

    private val _tasks = MutableStateFlow<List<TodoTask>>(emptyList())
    val tasks: StateFlow<List<TodoTask>> = _tasks.asStateFlow()

    private val _lists = MutableStateFlow<List<TodoList>>(emptyList())
    val lists: StateFlow<List<TodoList>> = _lists.asStateFlow()

    val allListMetas: StateFlow<List<TodoListMeta>> = combine(_lists) { userListsArray ->
        val customMetas = userListsArray.first().map {
            TodoListMeta(it.id, it.name, it.colorHex)
        }
        (defaultLists + customMetas).distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultLists)

    val showAddListDialog = MutableStateFlow(false)

    val currentView = MutableStateFlow("today") // today, tomorrow, up, all, done, stats
    val selectedList = MutableStateFlow("all")
    val searchQuery = MutableStateFlow("")
    val quickAddText = MutableStateFlow("")
    val showCompletedInAll = MutableStateFlow(false)
    val showSearchBox = MutableStateFlow(false)

    val editingTaskId = MutableStateFlow<String?>(null)
    val isSheetOpen = MutableStateFlow(false)

    val showDatePickerForUpcoming = MutableStateFlow(false)
    val pendingUpcomingTaskText = MutableStateFlow("")

    val statistics: StateFlow<TodoStatistics> = tasks.map { allTasks ->
        calculateStatistics(allTasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodoStatistics())

    init {
        viewModelScope.launch {
            repository.observeTasks().collect { _tasks.value = it }
        }
        viewModelScope.launch {
            repository.observeLists().collect { _lists.value = it }
        }
        viewModelScope.launch {
            repository.sync()
        }
    }

    fun setView(view: String) {
        currentView.value = view
    }

    fun setSelectedList(listId: String) {
        selectedList.value = listId
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleSearchBox() {
        showSearchBox.value = !showSearchBox.value
        if (showSearchBox.value) {
            searchQuery.value = ""
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val task = _tasks.value.find { it.id == taskId } ?: return@launch
            val newCompleted = !task.completed
            if (newCompleted) {
                SoundEffectManager.playMediaSound(getApplication<Application>(), SoundEffectManager.RES_GOAL_DONE)
            }
            val updated = task.copy(
                completed = newCompleted,
                completedAt = if (newCompleted) System.currentTimeMillis() else 0L,
                syncState = "PENDING"
            )
            repository.saveTask(updated)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    fun openSheet(taskId: String) {
        editingTaskId.value = taskId
        isSheetOpen.value = true
    }

    fun closeSheet() {
        isSheetOpen.value = false
        editingTaskId.value = null
    }

    fun updateEditingTask(updater: (TodoTask) -> TodoTask) {
        val id = editingTaskId.value ?: return
        val task = _tasks.value.find { it.id == id } ?: return
        val updated = updater(task)
        viewModelScope.launch {
            repository.saveTask(updated)
        }
    }

    fun addNewList(name: String, colorHex: String) {
        if (name.isBlank()) return
        val id = "list_" + System.currentTimeMillis()
        val list = TodoList(
            id = id,
            userId = "user",
            name = name.trim(),
            colorHex = colorHex.ifBlank { "#7D5CFF" }
        )
        viewModelScope.launch {
            repository.saveList(list)
            selectedList.value = id
            showAddListDialog.value = false
        }
    }

    fun addNewTask(rawText: String, explicitDueDate: String? = null) {
        if (rawText.isBlank()) return

        if (currentView.value == "up" && explicitDueDate == null) {
            val parsed = parseTaskInput(rawText)
            if (parsed.dueDate.isEmpty()) {
                pendingUpcomingTaskText.value = rawText
                showDatePickerForUpcoming.value = true
                return
            }
        }

        val parsed = parseTaskInput(rawText)
        val todayStr = LocalDate.now().toString()
        val tomorrowStr = LocalDate.now().plusDays(1).toString()

        val dueDate = when {
            explicitDueDate != null -> explicitDueDate
            parsed.dueDate.isNotEmpty() -> parsed.dueDate
            currentView.value == "today" -> todayStr
            currentView.value == "tomorrow" -> tomorrowStr
            else -> ""
        }

        val task = TodoTask(
            id = "t_" + System.currentTimeMillis(),
            userId = "user",
            title = parsed.title,
            dueDate = dueDate,
            priority = parsed.priority,
            listId = parsed.listId ?: if (selectedList.value != "all") selectedList.value else "personal",
            completed = false,
            createdAt = System.currentTimeMillis(),
            reminder = parsed.reminderTime
        )

        viewModelScope.launch {
            repository.saveTask(task)
            quickAddText.value = ""
            runCatching {
                SoundEffectManager.playMediaSound(getApplication<Application>(), SoundEffectManager.RES_MSG_SENDING)
            }
        }
    }

    fun confirmUpcomingTaskWithDate(selectedDate: String) {
        val text = pendingUpcomingTaskText.value
        showDatePickerForUpcoming.value = false
        pendingUpcomingTaskText.value = ""
        if (text.isNotBlank()) {
            addNewTask(text, explicitDueDate = selectedDate)
        }
    }

    data class ParsedTask(
        val title: String,
        val dueDate: String,
        val priority: Int,
        val listId: String?,
        val reminderTime: String
    )

    private fun parseTaskInput(s: String): ParsedTask {
        val words = s.trim().split(Regex("\\s+"))
        val keep = mutableListOf<String>()
        var due = ""
        var priority = 1
        var listId: String? = null
        var reminder = ""

        val availableLists = allListMetas.value

        val today = LocalDate.now()
        words.forEach { word ->
            val lower = word.lowercase()
            when {
                lower == "#today" || lower == "today" -> due = today.toString()
                lower == "#tomorrow" || lower == "tomorrow" || lower == "tmrw" -> due = today.plusDays(1).toString()
                lower == "!!!" || lower == "!urgent" -> priority = 3
                lower == "!!" || lower == "!high" -> priority = 2
                lower == "!low" -> priority = 0
                lower.startsWith("reminder@") || lower.startsWith("@") -> {
                    val timeStr = if (lower.startsWith("reminder@")) lower.removePrefix("reminder@") else lower.removePrefix("@")
                    val parsedTime = parseTimeToken(timeStr)
                    if (parsedTime != null) {
                        val (h, m) = parsedTime
                        val targetDate = if (due.isNotEmpty()) runCatching { LocalDate.parse(due) }.getOrDefault(today) else today
                        var ldt = LocalDateTime.of(targetDate, java.time.LocalTime.of(h, m))
                        if (due.isEmpty() && ldt.isBefore(LocalDateTime.now())) {
                            ldt = ldt.plusDays(1)
                            due = ldt.toLocalDate().toString()
                        }
                        reminder = ldt.toString()
                    } else {
                        keep.add(word)
                    }
                }
                lower.startsWith("#") && lower.length > 1 -> {
                    val tag = lower.slice(1 until lower.length)
                    val found = availableLists.find { it.id.startsWith(tag) || it.n.lowercase().startsWith(tag) }
                    if (found != null) {
                        listId = found.id
                    } else {
                        keep.add(word)
                    }
                }
                else -> keep.add(word)
            }
        }
        val title = keep.joinToString(" ").ifBlank { s.trim() }
        return ParsedTask(title, due, priority, listId, reminder)
    }

    private fun parseTimeToken(token: String): Pair<Int, Int>? {
        val parts = token.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    fun getListMeta(listId: String): TodoListMeta {
        val foundInAll = allListMetas.value.find { it.id == listId }
        if (foundInAll != null) return foundInAll
        return defaultLists.find { it.id == listId } ?: TodoListMeta(listId, listId, "#7D5CFF")
    }

    fun formatDueDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return runCatching {
            val date = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            val diff = ChronoUnit.DAYS.between(today, date)
            when (diff) {
                0L -> "Today"
                1L -> "Tomorrow"
                -1L -> "Yesterday"
                in -6L..-1L -> date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                in 2L..6L -> date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
            }
        }.getOrDefault(dateStr)
    }

    private fun calculateStatistics(allTasks: List<TodoTask>): TodoStatistics {
        val activeTasks = allTasks.filter { !it.deleted }
        val completedTasks = activeTasks.filter { it.completed }
        val totalCreated = activeTasks.size
        val totalCompleted = completedTasks.size
        val completionRate = if (totalCreated > 0) (totalCompleted * 100) / totalCreated else 0

        val priorityCounts = mutableMapOf<Int, Pair<Int, Int>>()
        listOf(0, 1, 2, 3).forEach { p ->
            val pTasks = activeTasks.filter { it.priority == p }
            val pDone = pTasks.count { it.completed }
            priorityCounts[p] = pDone to pTasks.size
        }

        val datesWithCompletions = completedTasks.mapNotNull {
            if (it.completedAt > 0L) {
                Instant.ofEpochMilli(it.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            } else null
        }.distinct().sorted()

        var currentStreak = 0
        var bestStreak = 0
        if (datesWithCompletions.isNotEmpty()) {
            val today = LocalDate.now()
            var tempStreak = 0
            val checkDate = datesWithCompletions.last()

            if (checkDate == today || checkDate == today.minusDays(1)) {
                var curr = checkDate
                while (datesWithCompletions.contains(curr)) {
                    currentStreak++
                    curr = curr.minusDays(1)
                }
            }

            for (i in datesWithCompletions.indices) {
                if (i == 0) {
                    tempStreak = 1
                } else {
                    if (datesWithCompletions[i] == datesWithCompletions[i - 1].plusDays(1)) {
                        tempStreak++
                    } else {
                        tempStreak = 1
                    }
                }
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak
                }
            }
        }

        val dayOfWeekCounts = MutableList(7) { 0 }
        completedTasks.forEach { t ->
            if (t.completedAt > 0L) {
                val dayIndex = Instant.ofEpochMilli(t.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .dayOfWeek.value - 1 // MONDAY=1 -> 0
                if (dayIndex in 0..6) {
                    dayOfWeekCounts[dayIndex]++
                }
            }
        }
        val maxDayCount = (dayOfWeekCounts.maxOrNull() ?: 1).coerceAtLeast(1)
        val dayOfWeekHeights = dayOfWeekCounts.map { count ->
            (count.toFloat() / maxDayCount.toFloat()).coerceIn(0.08f, 1.0f)
        }

        var morningCount = 0
        var afternoonCount = 0
        var eveningCount = 0
        var lateNightCount = 0

        completedTasks.forEach { t ->
            if (t.completedAt > 0L) {
                val hour = Instant.ofEpochMilli(t.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .hour
                when (hour) {
                    in 5..11 -> morningCount++
                    in 12..16 -> afternoonCount++
                    in 17..21 -> eveningCount++
                    else -> lateNightCount++
                }
            }
        }

        val todTotal = (morningCount + afternoonCount + eveningCount + lateNightCount).coerceAtLeast(1)
        val timeOfDayPercentages = mapOf(
            "Morning" to (morningCount * 100) / todTotal,
            "Afternoon" to (afternoonCount * 100) / todTotal,
            "Evening" to (eveningCount * 100) / todTotal,
            "Late night" to (lateNightCount * 100) / todTotal
        )

        val startOfWeek = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val completedThisWeek = completedTasks.count { t ->
            if (t.completedAt > 0L) {
                val completedDate = Instant.ofEpochMilli(t.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                !completedDate.isBefore(startOfWeek)
            } else false
        }

        val todayStr = LocalDate.now().toString()
        val overdueCount = activeTasks.count { !it.completed && it.dueDate.isNotEmpty() && it.dueDate < todayStr }
        val activeRemindersCount = activeTasks.count { !it.completed && (it.reminder.isNotEmpty() || it.dueDate.isNotEmpty()) }

        return TodoStatistics(
            totalCreated = totalCreated,
            totalCompleted = totalCompleted,
            completionRate = completionRate,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            priorityCounts = priorityCounts,
            dayOfWeekHeights = dayOfWeekHeights,
            dayOfWeekCounts = dayOfWeekCounts,
            timeOfDayPercentages = timeOfDayPercentages,
            completedThisWeek = completedThisWeek,
            overdueCount = overdueCount,
            activeRemindersCount = activeRemindersCount
        )
    }
}
