package tn.keeftalk.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.todo.TodoList
import tn.keeftalk.chat.domain.model.todo.TodoTask

interface TodoRepository {
    fun observeTasks(): Flow<List<TodoTask>>
    fun observeLists(): Flow<List<TodoList>>
    suspend fun saveTask(task: TodoTask)
    suspend fun deleteTask(taskId: String)
    suspend fun clearCompleted()
    suspend fun saveList(list: TodoList)
    suspend fun sync()
}
