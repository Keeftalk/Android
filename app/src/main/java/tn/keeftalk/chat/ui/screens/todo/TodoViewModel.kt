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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class TodoListMeta(val id: String, val n: String, val c: String)

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

    val currentView = MutableStateFlow("today") // today, up, all, done, stats
    val selectedList = MutableStateFlow("all")
    val searchQuery = MutableStateFlow("")
    val quickAddText = MutableStateFlow("")
    val showCompletedInAll = MutableStateFlow(false)
    val showSearchBox = MutableStateFlow(false)

    val editingTaskId = MutableStateFlow<String?>(null)
    val isSheetOpen = MutableStateFlow(false)

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

    fun addNewTask(rawText: String) {
        if (rawText.isBlank()) return
        val parsed = parseTaskInput(rawText)
        val todayStr = LocalDate.now().toString()
        val dueDate = if (parsed.dueDate.isNotEmpty()) parsed.dueDate else if (currentView.value == "today") todayStr else ""
        val task = TodoTask(
            id = "t_" + System.currentTimeMillis(),
            userId = "user",
            title = parsed.title,
            dueDate = dueDate,
            priority = parsed.priority,
            listId = parsed.listId ?: if (selectedList.value != "all") selectedList.value else "personal",
            completed = false,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveTask(task)
            quickAddText.value = ""
        }
    }

    data class ParsedTask(val title: String, val dueDate: String, val priority: Int, val listId: String?)

    private fun parseTaskInput(s: String): ParsedTask {
        val words = s.trim().split(Regex("\\s+"))
        val keep = mutableListOf<String>()
        var due = ""
        var priority = 1
        var listId: String? = null

        val today = LocalDate.now()
        words.forEach { word ->
            val lower = word.lowercase()
            when {
                lower == "today" -> due = today.toString()
                lower == "tomorrow" || lower == "tmrw" -> due = today.plusDays(1).toString()
                lower == "!!" || lower == "!high" -> priority = 2
                lower == "!low" -> priority = 0
                lower.startsWith("#") && lower.length > 1 -> {
                    val tag = lower.slice(1 until lower.length)
                    val found = defaultLists.find { it.id.startsWith(tag) || it.n.lowercase().startsWith(tag) }
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
        return ParsedTask(title, due, priority, listId)
    }

    fun getListMeta(listId: String): TodoListMeta {
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
}
