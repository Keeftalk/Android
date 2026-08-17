package com.keeftalk.chat.ui.share

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.Chat
import com.keeftalk.chat.domain.model.FileType
import com.keeftalk.chat.domain.model.MessageType
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.model.VaultFolder
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.VaultRepository
import androidx.core.content.pm.ShortcutManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.keeftalk.chat.domain.model.DocumentModel
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.util.ShareIntentUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.UUID

sealed class ShareNavigationEvent {
    data class OpenEditor(
        val chatId: String,
        val mediaItems: List<MediaItem>,
        val documentModels: List<DocumentModel>
    ) : ShareNavigationEvent()
}

data class SharedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String?,
    val fileType: FileType
)

data class ShareUiState(
    val sharedFiles: List<SharedFile> = emptyList(),
    val isVaultFlow: Boolean = false,
    val recentChats: List<Chat> = emptyList(),
    val contacts: List<User> = emptyList(),
    val vaultFolders: List<VaultFolder> = emptyList(),
    val currentVaultFolderId: String? = null,
    val vaultNavigationStack: List<VaultFolder> = emptyList(),
    val isProcessing: Boolean = false,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)

class ShareViewModel(
    application: Application,
    private val chatRepository: ChatRepository,
    private val vaultRepository: VaultRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ShareNavigationEvent>()
    val navigationEvent: SharedFlow<ShareNavigationEvent> = _navigationEvent.asSharedFlow()

    private var currentUris: List<Uri> = emptyList()

    fun handleIntent(intent: Intent, isVaultFlow: Boolean) {
        _uiState.value = _uiState.value.copy(isVaultFlow = isVaultFlow)
        
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) } ?: emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }

        currentUris = uris
        uris.forEach { tryTakePersistablePermission(it) }
        resolveMetadata(uris)

        val directChatId = intent.getStringExtra("chatId") ?: intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
        if (!isVaultFlow && directChatId != null) {
            onChatSelected(directChatId)
        }
        
        if (isVaultFlow) {
            loadVaultFolders()
        } else {
            loadChatsAndContacts()
        }
    }

    private fun tryTakePersistablePermission(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Not supported by all providers
        }
    }

    private fun resolveMetadata(uris: List<Uri>) {
        viewModelScope.launch {
            val sharedFiles = uris.mapNotNull { uri ->
                try {
                    val contentResolver = getApplication<Application>().contentResolver
                    var name = "unknown_${UUID.randomUUID()}"
                    var size = 0L
                    
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                val resolvedName = cursor.getString(nameIndex)
                                if (!resolvedName.isNullOrBlank()) name = resolvedName
                            }
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                        }
                    }
                    
                    val mimeType = contentResolver.getType(uri)
                    val fileType = getFileType(mimeType)
                    
                    SharedFile(uri, name, size, mimeType, fileType)
                } catch (e: Exception) {
                    Log.e("ShareViewModel", "Failed to resolve metadata for $uri", e)
                    null
                }
            }
            _uiState.value = _uiState.value.copy(sharedFiles = sharedFiles)
        }
    }

    private fun loadChatsAndContacts() {
        viewModelScope.launch {
            chatRepository.getChats().collect { chats ->
                _uiState.value = _uiState.value.copy(recentChats = chats.take(10))
            }
        }
        viewModelScope.launch {
            chatRepository.getContacts().collect { contacts ->
                _uiState.value = _uiState.value.copy(contacts = contacts)
            }
        }
    }

    private fun loadVaultFolders() {
        viewModelScope.launch {
            vaultRepository.getFolders(_uiState.value.currentVaultFolderId).collect { folders ->
                _uiState.value = _uiState.value.copy(vaultFolders = folders)
            }
        }
    }

    fun onChatSelected(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val mediaItems = ShareIntentUtils.resolveToMediaItems(context, currentUris)
            val docModels = ShareIntentUtils.resolveToDocumentModels(context, currentUris)
            
            _navigationEvent.emit(ShareNavigationEvent.OpenEditor(chatId, mediaItems, docModels))
        }
    }

    fun removeFile(file: SharedFile) {
        val newList = _uiState.value.sharedFiles.filter { it.uri != file.uri }
        _uiState.value = _uiState.value.copy(sharedFiles = newList)
    }

    fun navigateToFolder(folder: VaultFolder) {
        val newStack = _uiState.value.vaultNavigationStack + folder
        _uiState.value = _uiState.value.copy(
            currentVaultFolderId = folder.id,
            vaultNavigationStack = newStack
        )
        loadVaultFolders()
    }

    fun navigateToBreadcrumb(index: Int) {
        if (index == -1) {
            _uiState.value = _uiState.value.copy(
                currentVaultFolderId = null,
                vaultNavigationStack = emptyList()
            )
        } else {
            val newStack = _uiState.value.vaultNavigationStack.take(index + 1)
            _uiState.value = _uiState.value.copy(
                currentVaultFolderId = newStack.last().id,
                vaultNavigationStack = newStack
            )
        }
        loadVaultFolders()
    }

    fun navigateBack() {
        if (_uiState.value.vaultNavigationStack.isNotEmpty()) {
            val newStack = _uiState.value.vaultNavigationStack.dropLast(1)
            _uiState.value = _uiState.value.copy(
                currentVaultFolderId = newStack.lastOrNull()?.id,
                vaultNavigationStack = newStack
            )
            loadVaultFolders()
        }
    }

    fun sendToChats(chatIds: List<String>) {
        if (_uiState.value.sharedFiles.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(isProcessing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val files = _uiState.value.sharedFiles
                
                // Pre-copy to temp files to ensure they persist after activity closes
                val filesWithLocalPaths = files.map { file ->
                    file to copyToTempFile(context, file.uri, file.name)
                }

                chatIds.forEach { chatId ->
                    filesWithLocalPaths.forEach { (file, tempFile) ->
                        if (tempFile != null) {
                            val messageType = when (file.fileType) {
                                FileType.IMAGE -> MessageType.IMAGE
                                FileType.VIDEO -> MessageType.VIDEO
                                FileType.AUDIO -> MessageType.VOICE
                                FileType.DOCUMENT -> MessageType.PDF
                                else -> MessageType.FILE
                            }
                            chatRepository.sendMessage(
                                chatId = chatId,
                                content = "",
                                type = messageType,
                                filePath = tempFile.absolutePath
                            )
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(isProcessing = false, isFinished = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = e.message)
            }
        }
    }

    fun saveToVault() {
        if (_uiState.value.sharedFiles.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(isProcessing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val folderId = _uiState.value.currentVaultFolderId
                _uiState.value.sharedFiles.forEach { sharedFile ->
                    val tempFile = copyToTempFile(context, sharedFile.uri, sharedFile.name)
                    if (tempFile != null) {
                        vaultRepository.uploadFile(tempFile, folderId) { _, _ -> /* progress */ }
                    }
                }
                _uiState.value = _uiState.value.copy(isProcessing = false, isFinished = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = e.message)
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            vaultRepository.createFolder(name, _uiState.value.currentVaultFolderId)
            loadVaultFolders()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun copyToTempFile(context: android.content.Context, uri: Uri, name: String): File? {
        return try {
            val tempFile = File(context.cacheDir, "share_${UUID.randomUUID()}_$name")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileType(mimeType: String?): FileType {
        val type = mimeType?.lowercase() ?: ""
        return when {
            type.startsWith("image") -> FileType.IMAGE
            type.startsWith("video") -> FileType.VIDEO
            type.startsWith("audio") -> FileType.AUDIO
            type.contains("pdf") || type.contains("msword") || type.contains("officedocument") || type.contains("text/plain") -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }

    class Factory(
        private val application: Application,
        private val chatRepository: ChatRepository,
        private val vaultRepository: VaultRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShareViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ShareViewModel(application, chatRepository, vaultRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
