package tn.keeftalk.chat.data.repository

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tn.keeftalk.chat.data.local.dao.TodoDao
import tn.keeftalk.chat.data.local.entities.TodoListEntity
import tn.keeftalk.chat.data.local.entities.TodoTaskEntity
import tn.keeftalk.chat.data.remote.TodoTaskDto
import tn.keeftalk.chat.data.remote.TodoListDto
import tn.keeftalk.chat.data.remote.TodoRemoteDataSource
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.todo.TodoList
import tn.keeftalk.chat.domain.model.todo.TodoTask
import tn.keeftalk.chat.domain.repository.TodoRepository

class TodoRepositoryImpl(
    private val context: Context,
    private val todoDao: TodoDao,
    private val remoteDataSource: TodoRemoteDataSource
) : TodoRepository {

    private suspend fun getUserId(): String {
        return runCatching {
            val supabase: SupabaseClient = AppModule.provideSupabaseClientAsync(context)
            supabase.auth.currentUserOrNull()?.id ?: "local_user"
        }.getOrDefault("local_user")
    }

    override fun observeTasks(): Flow<List<TodoTask>> {
        return todoDao.getAllTasks().map { entities ->
            entities.map { e ->
                TodoTask(
                    id = e.id,
                    userId = e.userId,
                    title = e.title,
                    notes = e.notes,
                    dueDate = e.dueDate,
                    priority = e.priority,
                    listId = e.listId,
                    completed = e.completed,
                    createdAt = e.createdAt,
                    completedAt = e.completedAt,
                    reminder = e.reminder,
                    reminderFired = e.reminderFired,
                    updatedAt = e.updatedAt,
                    deleted = e.deleted,
                    syncState = e.syncState
                )
            }
        }
    }

    override fun observeLists(): Flow<List<TodoList>> {
        return todoDao.getAllLists().map { entities ->
            entities.map { e ->
                TodoList(
                    id = e.id,
                    userId = e.userId,
                    name = e.name,
                    colorHex = e.colorHex,
                    updatedAt = e.updatedAt,
                    deleted = e.deleted,
                    syncState = e.syncState
                )
            }
        }
    }

    override suspend fun saveTask(task: TodoTask) {
        val userId = getUserId()
        val entity = TodoTaskEntity(
            id = task.id,
            userId = userId,
            title = task.title,
            notes = task.notes,
            dueDate = task.dueDate,
            priority = task.priority,
            listId = task.listId,
            completed = task.completed,
            createdAt = task.createdAt,
            completedAt = task.completedAt,
            reminder = task.reminder,
            reminderFired = task.reminderFired,
            updatedAt = System.currentTimeMillis(),
            deleted = task.deleted,
            syncState = "PENDING"
        )
        todoDao.upsertTask(entity)

        if (!task.deleted && !task.completed) {
            var triggerTime = 0L
            if (task.reminder.isNotBlank()) {
                triggerTime = runCatching {
                    java.time.LocalDateTime.parse(task.reminder).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrDefault(0L)
            } else if (task.dueDate.isNotBlank()) {
                triggerTime = runCatching {
                    java.time.LocalDate.parse(task.dueDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + (9 * 3600 * 1000)
                }.getOrDefault(0L)
            }
            if (triggerTime > 0L) {
                tn.keeftalk.chat.util.reminders.TodoReminderScheduler.scheduleAllTaskAlarms(context, task.id, task.title, triggerTime)
            } else {
                tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelTaskReminder(context, task.id)
            }
        } else {
            tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelTaskReminder(context, task.id)
        }

        runCatching {
            remoteDataSource.upsertTask(TodoTaskDto.fromEntity(entity))
            todoDao.updateTaskSyncState(entity.id, "SYNCED")
        }
    }

    override suspend fun deleteTask(taskId: String) {
        val timestamp = System.currentTimeMillis()
        todoDao.softDeleteTask(taskId, timestamp)
        tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelTaskReminder(context, taskId)
    }

    override suspend fun clearCompleted() {
        todoDao.clearCompletedTasks()
    }

    override suspend fun saveList(list: TodoList) {
        val userId = getUserId()
        val entity = TodoListEntity(
            id = list.id,
            userId = userId,
            name = list.name,
            colorHex = list.colorHex,
            updatedAt = System.currentTimeMillis(),
            deleted = list.deleted,
            syncState = "PENDING"
        )
        todoDao.upsertList(entity)
        runCatching {
            remoteDataSource.upsertList(TodoListDto.fromEntity(entity))
        }
    }

    override suspend fun sync() {
        val userId = getUserId()
        runCatching {
            val remoteTasks = remoteDataSource.fetchTasks(userId)
            remoteTasks.forEach { todoDao.upsertTask(it.toEntity()) }
            val remoteLists = remoteDataSource.fetchLists(userId)
            remoteLists.forEach { todoDao.upsertList(it.toEntity()) }

            // Push pending
            val pending = todoDao.getPendingTasks()
            pending.forEach {
                remoteDataSource.upsertTask(TodoTaskDto.fromEntity(it))
                todoDao.updateTaskSyncState(it.id, "SYNCED")
            }
        }
    }
}
