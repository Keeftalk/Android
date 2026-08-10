package com.keeftalk.chat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConnectionPathViewModel(
    private val relationshipRepository: RelationshipRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val targetUserId: String
) : ViewModel() {

    private val _paths = MutableStateFlow<List<List<Any>>>(emptyList()) // Any can be Profile or String "masked"
    val paths: StateFlow<List<List<Any>>> = _paths.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPaths()
    }

    private fun loadPaths() {
        viewModelScope.launch {
            _isLoading.value = true
            relationshipRepository.getRelationshipPaths(targetUserId)
                .onSuccess { rawPaths ->
                    // Resolve profiles for all non-masked IDs
                    val allIds = rawPaths.flatten().filter { it != "masked" }.distinct()
                    val profiles = chatRepository.getSearchableProfiles().first().filter { it.id in allIds }
                        .associateBy { it.id }
                    
                    val resolvedPaths = rawPaths.map { path ->
                        path.map { id ->
                            if (id == "masked") "masked"
                            else profiles[id] ?: id // Fallback to ID if profile not found
                        }
                    }
                    _paths.value = resolvedPaths
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }
}
