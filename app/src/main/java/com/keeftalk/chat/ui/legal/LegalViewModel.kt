package com.keeftalk.chat.ui.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.LegalDocument
import com.keeftalk.chat.domain.repository.LegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LegalUiState {
    object Idle : LegalUiState
    object Loading : LegalUiState
    data class Success(val document: LegalDocument) : LegalUiState
    data class Error(val message: String) : LegalUiState
}

class LegalViewModel(
    private val repository: LegalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LegalUiState>(LegalUiState.Idle)
    val uiState: StateFlow<LegalUiState> = _uiState.asStateFlow()

    fun loadDocument(type: String) {
        viewModelScope.launch {
            _uiState.value = LegalUiState.Loading
            repository.getLegalDocument(type)
                .onSuccess { document ->
                    _uiState.value = LegalUiState.Success(document)
                }
                .onFailure { error ->
                    _uiState.value = LegalUiState.Error(error.message ?: "Failed to load document")
                }
        }
    }
}
