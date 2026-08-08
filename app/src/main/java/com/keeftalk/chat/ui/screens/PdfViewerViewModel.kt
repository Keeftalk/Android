package com.keeftalk.chat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class PdfViewerViewModel(
    application: Application,
    private val repository: ChatRepository,
    private val messageId: String
) : AndroidViewModel(application) {

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message.asStateFlow()

    private val _localFile = MutableStateFlow<File?>(null)
    val localFile: StateFlow<File?> = _localFile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMessage()
    }

    private fun loadMessage() {
        viewModelScope.launch {
            repository.getMessage(messageId).collectLatest { msg ->
                if (msg != null) {
                    val currentMsg = _message.value
                    _message.value = msg
                    
                    // Only trigger ensureLocal if we don't have a local file yet
                    // or if it's the first time loading the message
                    if (_localFile.value == null || (currentMsg?.localFilePath != msg.localFilePath)) {
                        ensureLocal(msg)
                    }
                } else {
                    _isLoading.value = false
                    _error.value = "Message not found"
                }
            }
        }
    }

    private fun ensureLocal(msg: Message) {
        viewModelScope.launch {
            repository.ensureMediaLocal(msg)
                .onSuccess { file ->
                    _localFile.value = file
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to load PDF"
                    _isLoading.value = false
                }
        }
    }

    fun toggleLock() {
        val msg = _message.value ?: return
        viewModelScope.launch {
            repository.toggleMediaLock(msg.id, !msg.mediaLocked)
            // The getMessages flow should update _message automatically
        }
    }

    fun download() {
        val msg = _message.value ?: return
        viewModelScope.launch {
            repository.downloadMedia(msg).onSuccess { result ->
                val toastMsg = if (result.alreadyExisted) {
                    "Pdf already saved in Keeftalk/files"
                } else {
                    "Pdf downloaded to Keeftalk/files"
                }
                android.widget.Toast.makeText(getApplication(), toastMsg, android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                android.widget.Toast.makeText(getApplication(), e.message ?: "Failed to download", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
