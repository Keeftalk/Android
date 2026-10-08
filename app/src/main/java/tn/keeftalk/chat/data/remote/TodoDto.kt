package tn.keeftalk.chat.data.remote

import kotlinx.serialization.Serializable
import tn.keeftalk.chat.data.local.entities.TodoTaskEntity
import tn.keeftalk.chat.data.local.entities.TodoListEntity
import tn.keeftalk.chat.domain.model.todo.TodoTask
import tn.keeftalk.chat.domain.model.todo.TodoList

@Serializable
data class TodoTaskDto(
    val id: String,
    val user_id: String,
    val title: String,
    val notes: String = "",
    val due_date: String = "",
    val priority: Int = 1,
    val list_id: String = "personal",
    val completed: Boolean = false,
    val created_at: Long,
    val completed_at: Long = 0L,
    val reminder: String = "",
    val reminder_fired: Boolean = false,
    val updated_at: Long,
    val deleted: Boolean = false
) {
    fun toEntity() = TodoTaskEntity(
        id = id,
        userId = user_id,
        title = title,
        notes = notes,
        dueDate = due_date,
        priority = priority,
        listId = list_id,
        completed = completed,
        createdAt = created_at,
        completedAt = completed_at,
        reminder = reminder,
        reminderFired = reminder_fired,
        updatedAt = updated_at,
        deleted = deleted,
        syncState = "SYNCED"
    )

    companion object {
        fun fromEntity(e: TodoTaskEntity) = TodoTaskDto(
            id = e.id,
            user_id = e.userId,
            title = e.title,
            notes = e.notes,
            due_date = e.dueDate,
            priority = e.priority,
            list_id = e.listId,
            completed = e.completed,
            created_at = e.createdAt,
            completed_at = e.completedAt,
            reminder = e.reminder,
            reminder_fired = e.reminderFired,
            updated_at = e.updatedAt,
            deleted = e.deleted
        )
    }
}

@Serializable
data class TodoListDto(
    val id: String,
    val user_id: String,
    val name: String,
    val color_hex: String,
    val updated_at: Long,
    val deleted: Boolean = false
) {
    fun toEntity() = TodoListEntity(
        id = id,
        userId = user_id,
        name = name,
        colorHex = color_hex,
        updatedAt = updated_at,
        deleted = deleted,
        syncState = "SYNCED"
    )

    companion object {
        fun fromEntity(e: TodoListEntity) = TodoListDto(
            id = e.id,
            user_id = e.userId,
            name = e.name,
            color_hex = e.colorHex,
            updated_at = e.updatedAt,
            deleted = e.deleted
        )
    }
}
