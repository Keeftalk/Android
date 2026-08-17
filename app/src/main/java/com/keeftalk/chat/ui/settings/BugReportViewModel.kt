package com.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.repository.BugReportRepository
import com.keeftalk.chat.util.LogcatHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BugReportViewModel(
    private val repository: BugReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BugReportUiState>(BugReportUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var capturedLogs: String = ""

    fun startReporting() {
        viewModelScope.launch {
            _uiState.value = BugReportUiState.CollectingLogs
            capturedLogs = LogcatHelper.getLogcat()
            _uiState.value = BugReportUiState.Inputting
        }
    }

    fun submitReport(description: String) {
        if (description.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = BugReportUiState.Sending
            repository.submitReport(description, capturedLogs)
                .onSuccess {
                    _uiState.value = BugReportUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = BugReportUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun reset() {
        _uiState.value = BugReportUiState.Idle
        capturedLogs = ""
    }
}

sealed class BugReportUiState {
    object Idle : BugReportUiState()
    object CollectingLogs : BugReportUiState()
    object Inputting : BugReportUiState()
    object Sending : BugReportUiState()
    object Success : BugReportUiState()
    data class Error(val message: String) : BugReportUiState()
}
