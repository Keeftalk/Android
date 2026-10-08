package tn.keeftalk.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.TodoListEntity
import tn.keeftalk.chat.data.local.entities.TodoTaskEntity

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_tasks WHERE deleted = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TodoTaskEntity>>

    @Query("SELECT * FROM todo_lists WHERE deleted = 0")
    fun getAllLists(): Flow<List<TodoListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TodoTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: TodoListEntity)

    @Query("UPDATE todo_tasks SET deleted = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: String, timestamp: Long)

    @Query("DELETE FROM todo_tasks WHERE completed = 1 AND deleted = 0")
    suspend fun clearCompletedTasks()

    @Query("SELECT * FROM todo_tasks WHERE syncState = 'PENDING'")
    suspend fun getPendingTasks(): List<TodoTaskEntity>

    @Query("UPDATE todo_tasks SET syncState = :syncState WHERE id = :id")
    suspend fun updateTaskSyncState(id: String, syncState: String)
}
