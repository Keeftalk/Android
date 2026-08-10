package com.keeftalk.chat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.keeftalk.chat.domain.model.Chat
import com.keeftalk.chat.domain.model.ChatListItemUiModel
import com.keeftalk.chat.domain.model.ChatType
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.keeftalk.chat.util.PerformanceProfiler
import android.util.Log

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModel(private val repository: ChatRepository) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedContainerId = MutableStateFlow("all")
    val selectedContainerId = _selectedContainerId.asStateFlow()
    
    private val _isSyncing = MutableStateFlow(value = false)
    val isSyncing = _isSyncing.asStateFlow()

    val typingStatuses = repository.getAllTypingStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allChatsForBadges: StateFlow<List<Chat>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatsPager: Flow<androidx.paging.PagingData<ChatListItemUiModel>> = combine(
        _searchQuery,
        _selectedContainerId
    ) { query, containerId ->
        query to containerId
    }.flatMapLatest { (query, containerId) ->
        repository.getChatPager(containerId, query)
    }.map { pagingData ->
        pagingData.map { it.toUiModel() }
    }.flowOn(Dispatchers.Default)
    .cachedIn(viewModelScope)

    val activeChats: StateFlow<List<ChatListItemUiModel>> = flowOf(emptyList<ChatListItemUiModel>())
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val archivedChatsPager: Flow<androidx.paging.PagingData<ChatListItemUiModel>> = repository.getChatPager("archived", "")
        .map { pagingData ->
            pagingData.map { it.toUiModel() }
        }.flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)

    val archivedChats: StateFlow<List<ChatListItemUiModel>> = flowOf(emptyList<ChatListItemUiModel>())
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        PerformanceProfiler.logEvent("ChatListViewModel created", category = PerformanceProfiler.Category.DI)
        refresh()
    }

    // Load fast cache on IO immediately
    init {
        viewModelScope.launch(Dispatchers.IO) {
            // We can't access DAO directly here easily without dependency, 
            // but we can trust the repository's initial state if it's already hydrated 
            // OR we can just use the combine below which handles empty gracefully.
            // Actually, let's keep it simple and just make the combine faster.
        }
    }

    fun setContainerId(id: String) {
        _selectedContainerId.value = id
    }

    fun refresh() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // If query is empty, do a deep sync to ensure all old chats are present
                if (_searchQuery.value.isBlank()) {
                    repository.syncFullChatHistory()
                }
                repository.syncAllRecentContent()
            } catch (e: Exception) {
                Log.e("CHAT_LIST_VM", "Refresh failed", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun loadMoreChats() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Paging 3 handles local pagination. 
                // We might still want to trigger a network sync for more chats.
                repository.syncChats(30, 0) // Simplified for now
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun archiveChat(chatId: String, archive: Boolean) {
        viewModelScope.launch { repository.archiveChat(chatId, archive) }
    }

    fun markAsRead(chatId: String) {
        viewModelScope.launch { repository.markAsRead(chatId) }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch { repository.deleteChat(chatId) }
    }
}
