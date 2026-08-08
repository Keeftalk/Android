package com.keeftalk.chat.data.local

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.keeftalk.chat.domain.model.ChatListItemUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optimized PagingSource for the conversation list.
 * Fetches data using RawChatListDataSource on the SERIAL_EXECUTOR to prevent lock contention.
 */
class RawChatListPagingSource(
    private val dataSource: RawChatListDataSource,
    private val filter: String = "all",
) : PagingSource<Int, ChatListItemUiModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChatListItemUiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val position = params.key ?: 0
                val limit = params.loadSize
                
                val chats = dataSource.getChatList(limit, position, filter)
                
                LoadResult.Page(
                    data = chats,
                    prevKey = if (position == 0) null else position - limit,
                    nextKey = if (chats.isEmpty()) null else position + limit
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ChatListItemUiModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(state.config.pageSize)
        }
    }
}
