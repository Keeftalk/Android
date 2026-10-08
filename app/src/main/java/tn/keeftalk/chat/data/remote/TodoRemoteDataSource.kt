package tn.keeftalk.chat.data.remote

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import tn.keeftalk.chat.di.AppModule

interface TodoRemoteDataSource {
    suspend fun fetchTasks(userId: String): List<TodoTaskDto>
    suspend fun upsertTask(task: TodoTaskDto)
    suspend fun fetchLists(userId: String): List<TodoListDto>
    suspend fun upsertList(list: TodoListDto)
}

class TodoRemoteDataSourceImpl(private val context: Context) : TodoRemoteDataSource {
    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    override suspend fun fetchTasks(userId: String): List<TodoTaskDto> {
        return runCatching {
            val supabase = getSupabase()
            supabase.postgrest["todo_tasks"].select {
                filter { eq("user_id", userId) }
            }.decodeList<TodoTaskDto>()
        }.getOrDefault(emptyList())
    }

    override suspend fun upsertTask(task: TodoTaskDto) {
        runCatching {
            val supabase = getSupabase()
            supabase.postgrest["todo_tasks"].upsert(task)
        }
    }

    override suspend fun fetchLists(userId: String): List<TodoListDto> {
        return runCatching {
            val supabase = getSupabase()
            supabase.postgrest["todo_lists"].select {
                filter { eq("user_id", userId) }
            }.decodeList<TodoListDto>()
        }.getOrDefault(emptyList())
    }

    override suspend fun upsertList(list: TodoListDto) {
        runCatching {
            val supabase = getSupabase()
            supabase.postgrest["todo_lists"].upsert(list)
        }
    }
}
