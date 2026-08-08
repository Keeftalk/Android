package com.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.repository.PrivacyRepository
import com.keeftalk.chat.domain.repository.ProfileView
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewHistoryViewModel(
    private val privacyRepository: PrivacyRepository
) : ViewModel() {

    private val _views = MutableStateFlow<List<ProfileView>>(emptyList())
    val views = _views.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var currentOffset = 0
    private val limit = 20

    init {
        loadMore()
    }

    fun loadMore() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            privacyRepository.getProfileViews(limit, currentOffset)
                .collect { newViews ->
                    _views.value = _views.value + newViews
                    currentOffset += limit
                    _isLoading.value = false
                }
        }
    }

    fun refresh() {
        currentOffset = 0
        _views.value = emptyList()
        loadMore()
    }
}
