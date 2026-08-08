package com.keeftalk.chat.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DiscoveryViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    private val _networkResults = MutableStateFlow<List<Profile>>(emptyList())
    private val _searchError = MutableStateFlow<String?>(null)

    private val _allProfiles = MutableStateFlow<List<Profile>>(emptyList())
    private var networkSearchJob: Job? = null
    
    val uiState: StateFlow<DiscoveryUiState> = combine(
        _searchQuery, 
        _allProfiles, 
        _networkResults,
        _isSearching,
        _searchError
    ) { query, profiles, networkResults, isSearching, searchError ->
        if (query.isBlank()) {
            if (profiles.isEmpty()) DiscoveryUiState.Idle 
            else DiscoveryUiState.Results(profiles.take(50), isSearching = false)
        } else if (searchError != null && networkResults.isEmpty()) {
            DiscoveryUiState.Error(searchError)
        } else {
            val localFiltered = profiles.filter { profile ->
                profile.username.contains(query, ignoreCase = true) ||
                (profile.fullName?.contains(query, ignoreCase = true) == true) ||
                (profile.phone?.contains(query, ignoreCase = true) == true) ||
                (profile.email?.contains(query, ignoreCase = true) == true)
            }
            
            // Combine local and network results, prioritizing network/global results
            val combined = (networkResults + localFiltered)
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<Profile> { 
                        it.username.equals(query, ignoreCase = true) || 
                        it.fullName?.equals(query, ignoreCase = true) == true ||
                        it.phone?.equals(query, ignoreCase = true) == true ||
                        it.email?.equals(query, ignoreCase = true) == true
                    }.thenByDescending {
                        it.username.startsWith(query, ignoreCase = true) || 
                        it.fullName?.startsWith(query, ignoreCase = true) == true ||
                        it.phone?.startsWith(query, ignoreCase = true) == true ||
                        it.email?.startsWith(query, ignoreCase = true) == true
                    }
                )
            DiscoveryUiState.Results(combined, isSearching = isSearching)
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscoveryUiState.Idle)

    init {
        viewModelScope.launch {
            chatRepository.getSearchableProfiles().collect {
                _allProfiles.value = it
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _searchError.value = null // Clear error on new input
        
        // Background network search to refresh cache for new users not in local DB
        networkSearchJob?.cancel()
        if (query.length >= 1) {
            networkSearchJob = viewModelScope.launch {
                _isSearching.value = true
                try {
                    delay(300) // Debounce network requests
                    chatRepository.searchUsers(query)
                        .onSuccess { results ->
                            _networkResults.value = results
                            _searchError.value = null
                        }
                        .onFailure { exception ->
                            android.util.Log.e("DiscoveryVM", "Search failed for $query", exception)
                            if (exception is io.ktor.client.plugins.HttpRequestTimeoutException) {
                                _searchError.value = "Search timed out. Please check your connection."
                            } else {
                                _searchError.value = "Failed to fetch global results."
                            }
                        }
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        _searchError.value = "An unexpected error occurred."
                    }
                } finally {
                    _isSearching.value = false
                }
            }
        } else {
            _isSearching.value = false
            _networkResults.value = emptyList()
            _searchError.value = null
        }
    }

    fun onUserSelected(profile: Profile, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.getOrCreateOneToOneChat(profile.id)
                .onSuccess { chatId ->
                    onChatCreated(chatId)
                }
                .onFailure {
                    // Error handling could be added here
                }
        }
    }
}

sealed class DiscoveryUiState {
    data object Idle : DiscoveryUiState()
    data object Loading : DiscoveryUiState()
    data class Results(val users: List<Profile>, val isSearching: Boolean = false) : DiscoveryUiState()
    data class Error(val message: String) : DiscoveryUiState()
}
