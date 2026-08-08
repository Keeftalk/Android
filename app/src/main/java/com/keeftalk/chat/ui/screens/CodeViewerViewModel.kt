package com.keeftalk.chat.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CodeViewerViewModel(
    application: Application,
    private val repository: ChatRepository,
    private val messageId: String
) : AndroidViewModel(application) {

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message.asStateFlow()

    private val _codeContent = MutableStateFlow<String?>(null)
    val codeContent: StateFlow<String?> = _codeContent.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMessage()
    }

    private fun loadMessage() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMessage(messageId).collectLatest { msg ->
                if (msg != null) {
                    _message.value = msg
                    ensureLocalAndRead(msg)
                } else {
                    _isLoading.value = false
                    _error.value = "Message not found"
                }
            }
        }
    }

    private fun ensureLocalAndRead(msg: Message) {
        viewModelScope.launch {
            repository.ensureMediaLocal(msg)
                .onSuccess { file ->
                    readFileContent(file)
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to load file"
                    _isLoading.value = false
                }
        }
    }

    private suspend fun readFileContent(file: File) {
        withContext(Dispatchers.IO) {
            try {
                val content = file.readText()
                _codeContent.value = content
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("CodeViewerViewModel", "Error reading file", e)
                _error.value = "Error reading file: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun download() {
        val msg = _message.value ?: return
        viewModelScope.launch {
            repository.downloadMedia(msg).onSuccess { result ->
                val toastMsg = if (result.alreadyExisted) {
                    "File already saved in Keeftalk/files"
                } else {
                    "File downloaded to Keeftalk/files"
                }
                android.widget.Toast.makeText(getApplication(), toastMsg, android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                android.widget.Toast.makeText(getApplication(), e.message ?: "Failed to download", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
