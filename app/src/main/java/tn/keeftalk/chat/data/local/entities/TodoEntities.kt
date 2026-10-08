package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_tasks")
data class TodoTaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val notes: String = "",
    val dueDate: String = "", // ISO date string yyyy-MM-dd or empty
    val priority: Int = 1, // 0: low, 1: medium, 2: high
    val listId: String = "personal",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L,
    val reminder: String = "", // datetime string or empty
    val reminderFired: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)

@Entity(tableName = "todo_lists")
data class TodoListEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val colorHex: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: String = "PENDING"
)
