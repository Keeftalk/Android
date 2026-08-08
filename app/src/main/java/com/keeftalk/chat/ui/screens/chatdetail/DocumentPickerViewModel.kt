package com.keeftalk.chat.ui.screens.chatdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.DocumentFolder
import com.keeftalk.chat.domain.model.DocumentModel
import com.keeftalk.chat.domain.model.DocumentType
import com.keeftalk.chat.domain.repository.DocumentRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.core.net.toUri

class DocumentPickerViewModel(
    private val repository: DocumentRepository,
    private val chatRepository: ChatRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _recentDocuments = MutableStateFlow<List<DocumentModel>>(emptyList())
    val recentDocuments: StateFlow<List<DocumentModel>> = _recentDocuments.asStateFlow()

    private val _vaultDocuments = MutableStateFlow<List<DocumentModel>>(emptyList())
    val vaultDocuments: StateFlow<List<DocumentModel>> = _vaultDocuments.asStateFlow()

    private val _sharedDocuments = MutableStateFlow<List<DocumentModel>>(emptyList())
    val sharedDocuments: StateFlow<List<DocumentModel>> = _sharedDocuments.asStateFlow()

    private val _selectedDocuments = MutableStateFlow<Set<DocumentModel>>(emptySet())
    val selectedDocuments: StateFlow<Set<DocumentModel>> = _selectedDocuments.asStateFlow()

    private val _folders = MutableStateFlow<List<DocumentFolder>>(emptyList())
    val folders: StateFlow<List<DocumentFolder>> = _folders.asStateFlow()

    private val _selectedFolder = MutableStateFlow<DocumentFolder?>(null)
    val selectedFolder: StateFlow<DocumentFolder?> = _selectedFolder.asStateFlow()

    private val _isShowingFolders = MutableStateFlow(false)
    val isShowingFolders: StateFlow<Boolean> = _isShowingFolders.asStateFlow()

    private val _activeTab = MutableStateFlow(DocumentPickerTab.RECENT)
    val activeTab: StateFlow<DocumentPickerTab> = _activeTab.asStateFlow()

    private val _isVaultEmpty = MutableStateFlow(false)
    val isVaultEmpty: StateFlow<Boolean> = _isVaultEmpty.asStateFlow()

    private val _isFullAccessUnlocked = MutableStateFlow(false)
    val isFullAccessUnlocked: StateFlow<Boolean> = _isFullAccessUnlocked.asStateFlow()

    init {
        _isFullAccessUnlocked.value = repository.isFullAccessUnlocked()
        refreshAll()
    }

    fun refreshAll() {
        _isFullAccessUnlocked.value = repository.isFullAccessUnlocked()
        loadRecentDocuments()
        loadVaultDocuments()
        loadSharedDocuments()
        loadFolders()
    }

    fun onFullAccessGranted(uri: Uri) {
        repository.setFullAccessUri(uri)
        _isFullAccessUnlocked.value = true
        refreshAll()
    }

    private fun loadRecentDocuments() {
        viewModelScope.launch {
            repository.getRecentDocuments().collect { docs ->
                _recentDocuments.value = docs
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            repository.getFolders().collect { folderList ->
                _folders.value = folderList
            }
        }
    }

    fun selectFolder(folder: DocumentFolder?) {
        _selectedFolder.value = folder
        _isShowingFolders.value = false
        if (folder != null && folder.id != "ALL_RECENT") {
            viewModelScope.launch {
                repository.getDocumentsInFolder(folder.id).collect { docs ->
                    _recentDocuments.value = docs
                }
            }
        } else {
            _selectedFolder.value = null // Reset to null for "Recent" view
            loadRecentDocuments()
        }
    }

    fun toggleFolderList() {
        _isShowingFolders.value = !_isShowingFolders.value
    }

    fun toggleSelection(document: DocumentModel) {
        _selectedDocuments.update { current ->
            if (current.contains(document)) {
                current - document
            } else {
                current + document
            }
        }
    }

    fun clearSelection() {
        _selectedDocuments.value = emptySet()
    }

    private fun loadVaultDocuments() {
        viewModelScope.launch {
            vaultRepository.getItems(null).collect { items ->
                _isVaultEmpty.value = items.isEmpty()
                _vaultDocuments.value = items.filter { 
                    it.file?.fileType == com.keeftalk.chat.domain.model.FileType.DOCUMENT 
                }.map { 
                    DocumentModel(
                        id = it.id.hashCode().toLong(),
                        uri = (it.file?.storagePath ?: "").toUri(),
                        name = it.title,
                        size = "", 
                        mimeType = it.file?.mimeType ?: "",
                        date = "", 
                        type = DocumentType.OTHER 
                    )
                }
            }
        }
    }

    private fun loadSharedDocuments() {
        viewModelScope.launch {
            repository.getSharedDocuments().collect { docs ->
                _sharedDocuments.value = docs
            }
        }
    }

    fun setTab(tab: DocumentPickerTab) {
        if (tab == DocumentPickerTab.RECENT) {
            _isShowingFolders.value = true // Always show folders when Recent is clicked
            loadFolders()
        } else {
            _isShowingFolders.value = false
        }
        _activeTab.value = tab
    }
}

enum class DocumentPickerTab {
    RECENT, VAULT, SHARED
}
