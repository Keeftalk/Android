package tn.keeftalk.chat.ui.docs

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.DocRepository

enum class DocCategoryFilter {
    ALL,
    PDF,
    MARKDOWN,
    HTML,
    EPUB_DOCX,
    CODE,
    FAVORITES
}

class DocsViewModel(
    application: Application,
    private val docRepository: DocRepository
) : AndroidViewModel(application) {

    val allDocs: StateFlow<List<Doc>> = docRepository.getAllDocsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val docSettings: StateFlow<DocSettings> = docRepository.getDocSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DocCategoryFilter.ALL)
    val selectedFilter: StateFlow<DocCategoryFilter> = _selectedFilter.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val filteredDocs: StateFlow<List<Doc>> = combine(allDocs, searchQuery, selectedFilter) { docs, query, filter ->
        docs.filter { doc ->
            val matchesQuery = query.isBlank() || doc.title.contains(query, ignoreCase = true) || doc.authorOrSource.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                DocCategoryFilter.ALL -> true
                DocCategoryFilter.PDF -> doc.format == DocFormat.PDF
                DocCategoryFilter.MARKDOWN -> doc.format == DocFormat.MARKDOWN
                DocCategoryFilter.HTML -> doc.format == DocFormat.HTML
                DocCategoryFilter.EPUB_DOCX -> doc.format == DocFormat.EPUB || doc.format == DocFormat.DOCX
                DocCategoryFilter.CODE -> doc.format == DocFormat.CODE
                DocCategoryFilter.FAVORITES -> doc.isFavorite
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageBytes: StateFlow<Long> = allDocs.map { docs ->
        docs.sumOf { it.fileSize }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        syncDocs()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: DocCategoryFilter) {
        _selectedFilter.value = filter
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun importDocFromUri(uri: Uri, customTitle: String? = null) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                docRepository.importDocFromUri(getApplication(), uri, customTitle)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importDocsFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                docRepository.importDocsFromUris(getApplication(), uris)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importFolderFromUri(treeUri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                docRepository.importDocFolderFromUri(getApplication(), treeUri)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun toggleFavorite(doc: Doc) {
        viewModelScope.launch {
            val updated = doc.copy(isFavorite = !doc.isFavorite, updatedAt = System.currentTimeMillis())
            docRepository.saveDoc(updated)
        }
    }

    fun deleteDoc(docId: String) {
        viewModelScope.launch {
            docRepository.deleteDoc(docId)
        }
    }

    fun restoreDoc(docId: String) {
        viewModelScope.launch {
            docRepository.restoreDoc(docId)
        }
    }

    fun syncDocs() {
        viewModelScope.launch {
            docRepository.syncDocsWithCloud()
        }
    }
}
