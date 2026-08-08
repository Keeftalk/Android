package com.keeftalk.chat.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.keeftalk.chat.data.local.dao.MessageWithReactions
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.data.remote.MessageDto
import com.keeftalk.chat.util.TimestampSerializer
import com.keeftalk.chat.util.PerformanceProfiler
import com.keeftalk.chat.util.PerformanceProfiler.Category
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val chatId: String,
    private val database: KeeftalkDatabase,
    private val supabaseProvider: suspend () -> SupabaseClient,
    private val handleIncomingMessage: suspend (MessageDto, String) -> Unit
) : RemoteMediator<Int, MessageWithReactions>() {

    private val messageDao = database.messageDao()

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageWithReactions>
    ): MediatorResult {
        val sourceTag = if (loadType == LoadType.REFRESH) "PAGING_REFRESH" else "PAGING_APPEND"
        android.util.Log.d("CHAT_DEBUG", "Mediator: load type=$loadType chatId=$chatId")
        if (loadType == LoadType.APPEND) {
            PerformanceProfiler.logEvent("Pagination Triggered", category = Category.CHAT)
            PerformanceProfiler.startStage("Load Older Messages")
        }
        return try {
            val cursor = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()?.message
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    lastItem
                }
            }

            val limit = state.config.pageSize
            val supabase = supabaseProvider()
            
            val response = withContext(Dispatchers.IO) {
                supabase.postgrest["messages"].select(io.github.jan.supabase.postgrest.query.Columns.raw("*, message_attachment(*, files(*))")) {
                    filter {
                        eq("chat_id", chatId)
                        if (cursor != null) {
                            or {
                                lt("created_at", TimestampSerializer.formatTimestamp(cursor.timestamp))
                                and {
                                    eq("created_at", TimestampSerializer.formatTimestamp(cursor.timestamp))
                                    lt("id", cursor.id)
                                }
                            }
                        }
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }.decodeList<MessageDto>()
            }

            android.util.Log.d("CHAT_DEBUG", "Mediator: Supabase returned ${response.size} messages for chatId=$chatId")
            if (loadType == LoadType.APPEND) {
                PerformanceProfiler.logEvent("Older Messages Query End", info = "Count: ${response.size}", category = Category.DATABASE)
            }

            android.util.Log.d("CHAT_DEBUG", "Mediator: Decrypting ${response.size} messages before insertion")
            response.forEach { dto ->
                handleIncomingMessage(dto, sourceTag)
            }
            
            if (loadType == LoadType.APPEND) {
                PerformanceProfiler.endStage("Load Older Messages", info = "Count: ${response.size}", category = Category.CHAT)
            }

            MediatorResult.Success(endOfPaginationReached = response.isEmpty() || response.size < limit)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
