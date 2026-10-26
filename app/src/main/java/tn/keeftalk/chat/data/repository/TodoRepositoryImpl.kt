package tn.keeftalk.chat.data.repository

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
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
                tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelUndatedTaskReminder(context, task.id)
            } else {
                tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelTaskReminder(context, task.id)
                tn.keeftalk.chat.util.reminders.TodoReminderScheduler.scheduleUndatedTaskReminder(context, task.id, task.title, task.createdAt)
            }
        } else {
            tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelTaskReminder(context, task.id)
            tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelUndatedTaskReminder(context, task.id)
        }

        syncTaskToCalendar(entity)

        runCatching {
            remoteDataSource.upsertTask(TodoTaskDto.fromEntity(entity))
            todoDao.updateTaskSyncState(entity.id, "SYNCED")
        }
    }

    override suspend fun deleteTask(taskId: String) {
        val timestamp = System.currentTimeMillis()
        todoDao.softDeleteTask(taskId, timestamp)
        tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelTaskReminder(context, taskId)
        tn.keeftalk.chat.util.reminders.TodoReminderScheduler.cancelUndatedTaskReminder(context, taskId)

        try {
            val db = AppModule.provideDatabase(context)
            val itemDetails = db.calendarDao().getItemById(taskId)
            if (itemDetails != null) {
                db.calendarDao().deleteItem(itemDetails.item)
            }
        } catch (e: Exception) {
            android.util.Log.e("TodoRepositoryImpl", "Error removing calendar item on delete", e)
        }
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
            remoteTasks.forEach {
                val entity = it.toEntity()
                todoDao.upsertTask(entity)
                syncTaskToCalendar(entity)
            }
            val remoteLists = remoteDataSource.fetchLists(userId)
            remoteLists.forEach { todoDao.upsertList(it.toEntity()) }

            // Push pending
            val pending = todoDao.getPendingTasks()
            pending.forEach {
                remoteDataSource.upsertTask(TodoTaskDto.fromEntity(it))
                todoDao.updateTaskSyncState(it.id, "SYNCED")
                syncTaskToCalendar(it)
            }
        }
    }

    private fun parseDueDateToMillis(dueDate: String, reminder: String): Long? {
        if (reminder.isNotBlank()) {
            return runCatching {
                java.time.LocalDateTime.parse(reminder).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
        }
        if (dueDate.isNotBlank()) {
            return runCatching {
                java.time.LocalDate.parse(dueDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + (9 * 3600 * 1000)
            }.getOrNull()
        }
        return null
    }

    private suspend fun syncTaskToCalendar(task: TodoTaskEntity) {
        try {
            val db = AppModule.provideDatabase(context)
            val calendarDao = db.calendarDao()
            val cryptoManager = AppModule.provideCryptoManager(context)

            val existingItemDetails = calendarDao.getItemById(task.id)

            if (task.deleted) {
                if (existingItemDetails != null) {
                    calendarDao.deleteItem(existingItemDetails.item)
                }
                return
            }

            val dueDateMillis = parseDueDateToMillis(task.dueDate, task.reminder)
            val calPriority = when(task.priority) {
                0 -> tn.keeftalk.chat.domain.model.calendar.CalendarPriority.LOW
                1 -> tn.keeftalk.chat.domain.model.calendar.CalendarPriority.MEDIUM
                2 -> tn.keeftalk.chat.domain.model.calendar.CalendarPriority.HIGH
                3 -> tn.keeftalk.chat.domain.model.calendar.CalendarPriority.URGENT
                else -> tn.keeftalk.chat.domain.model.calendar.CalendarPriority.MEDIUM
            }
            val calStatus = if (task.completed) tn.keeftalk.chat.domain.model.calendar.CalendarStatus.COMPLETED else tn.keeftalk.chat.domain.model.calendar.CalendarStatus.PENDING

            if (existingItemDetails != null) {
                var currentTitle = existingItemDetails.item.title
                if (existingItemDetails.item.encryptedDescription != null && existingItemDetails.item.iv != null) {
                    runCatching {
                        val encryptedObj = tn.keeftalk.chat.security.crypto.EncryptedObject(
                            version = existingItemDetails.item.cryptoVersion,
                            keyId = "root",
                            iv = existingItemDetails.item.iv,
                            ciphertext = existingItemDetails.item.encryptedDescription
                        )
                        val decryptedBytes = cryptoManager.decryptFromStorage(encryptedObj, tn.keeftalk.chat.security.crypto.CryptoManager.StoragePurpose.AGENDA)
                        val json = Json.parseToJsonElement(String(decryptedBytes, Charsets.UTF_8)).jsonObject
                        currentTitle = json["title"]?.jsonPrimitive?.content ?: task.title
                    }
                }

                if (currentTitle == task.title &&
                    existingItemDetails.item.description == task.notes &&
                    existingItemDetails.item.startTime == dueDateMillis &&
                    existingItemDetails.item.priority == calPriority &&
                    existingItemDetails.item.status == calStatus
                ) {
                    return
                }
            }

            val sensitiveData = buildJsonObject {
                put("title", JsonPrimitive(task.title))
                put("description", JsonPrimitive(task.notes))
            }
            val encrypted = cryptoManager.encryptForStorage(
                sensitiveData.toString().toByteArray(Charsets.UTF_8),
                tn.keeftalk.chat.security.crypto.CryptoManager.StoragePurpose.AGENDA
            )

            val calendarEntity = tn.keeftalk.chat.data.local.entities.CalendarItemEntity(
                id = task.id,
                type = tn.keeftalk.chat.domain.model.calendar.CalendarItemType.TASK,
                title = "[Encrypted]",
                description = "",
                color = "#6C63FF",
                icon = null,
                location = null,
                startTime = dueDateMillis,
                endTime = dueDateMillis?.plus(3600000L),
                isAllDay = task.reminder.isBlank(),
                timezone = java.util.TimeZone.getDefault().id,
                recurrenceRule = null,
                priority = calPriority,
                status = calStatus,
                isPrivate = false,
                ownerId = task.userId,
                categoryId = null,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                meetingType = null,
                meetingLink = null,
                progress = if (task.completed) 100 else 0,
                pomodoroCount = 0,
                deadline = dueDateMillis,
                parentItemId = null,
                pendingSync = true,
                encryptedDescription = encrypted.ciphertext,
                iv = encrypted.iv,
                cryptoVersion = encrypted.version
            )

            calendarDao.upsertCalendarItem(
                calendarEntity,
                existingItemDetails?.attendees?.map { it.attendee } ?: emptyList(),
                existingItemDetails?.reminders ?: emptyList()
            )
        } catch (e: Exception) {
            android.util.Log.e("TodoRepositoryImpl", "Error syncing task to calendar", e)
        }
    }
}
