package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NewSmsViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    initialQuery: String? = null
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow(initialQuery ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Profile>> = _searchQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                chatRepository.getSearchableProfiles()
            } else {
                chatRepository.getSearchableProfiles().map { profiles ->
                    profiles.filter { 
                        it.fullName?.contains(query, ignoreCase = true) == true ||
                        it.username.contains(query, ignoreCase = true) ||
                        it.phone?.contains(query) == true
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isQueryValidNumber = _searchQuery.map { query ->
        query.matches(Regex("""^\+?[0-9]{7,15}$"""))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
