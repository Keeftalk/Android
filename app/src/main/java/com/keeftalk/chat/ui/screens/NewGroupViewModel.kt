package com.keeftalk.chat.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NewGroupUiState(
    val name: String = "",
    val avatarUri: Uri? = null,
    val searchQuery: String = "",
    val recentUsers: List<User> = emptyList(),
    val selectedUserIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdChatId: String? = null
)

class NewGroupViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewGroupUiState())
    val uiState: StateFlow<NewGroupUiState> = _uiState.asStateFlow()

    init {
        loadRecentUsers()
    }

    private fun loadRecentUsers() {
        chatRepository.getRecentChatUsers()
            .onEach { users ->
                _uiState.update { it.copy(recentUsers = users) }
            }
            .launchIn(viewModelScope)
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onAvatarChange(uri: Uri?) {
        _uiState.update { it.copy(avatarUri = uri) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleUserSelection(userId: String) {
        _uiState.update { state ->
            val selected = state.selectedUserIds.toMutableSet()
            if (selected.contains(userId)) {
                selected.remove(userId)
            } else {
                selected.add(userId)
            }
            state.copy(selectedUserIds = selected)
        }
    }

    fun createGroup() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Group name cannot be empty") }
            return
        }
        if (state.selectedUserIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one member") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            try {
                val chatId = chatRepository.createGroup(
                    name = state.name,
                    memberIds = state.selectedUserIds.toList(),
                    avatarUrl = state.avatarUri?.toString()
                )
                _uiState.update { it.copy(isCreating = false, createdChatId = chatId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = e.message ?: "Failed to create group") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
