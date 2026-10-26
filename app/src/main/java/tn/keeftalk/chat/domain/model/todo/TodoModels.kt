package tn.keeftalk.chat.domain.model.todo

data class TodoTask(
    val id: String,
    val userId: String,
    val title: String,
    val notes: String = "",
    val dueDate: String = "",
    val priority: Int = 1, // 0: low, 1: medium, 2: high, 3: urgent
    val listId: String = "personal",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L,
    val reminder: String = "",
    val reminderFired: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)

data class TodoList(
    val id: String,
    val userId: String,
    val name: String,
    val colorHex: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)
