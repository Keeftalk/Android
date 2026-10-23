package tn.keeftalk.chat.ui.docs.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.DocRepository
import java.io.File

class DocViewerViewModel(
    val docId: String,
    private val docRepository: DocRepository
) : ViewModel() {

    val docFlow: StateFlow<Doc?> = docRepository.getDocByIdFlow(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val docSettingsFlow: StateFlow<DocSettings> = docRepository.getDocSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocSettings())

    val bookmarksFlow: StateFlow<List<DocBookmark>> = docRepository.getBookmarksForDocFlow(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlightsFlow: StateFlow<List<DocHighlight>> = docRepository.getHighlightsForDocFlow(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _rawContent = MutableStateFlow<String?>(null)
    val rawContent: StateFlow<String?> = _rawContent.asStateFlow()

    private val _epubChapters = MutableStateFlow<List<DocChapter>>(emptyList())
    val epubChapters: StateFlow<List<DocChapter>> = _epubChapters.asStateFlow()

    private val _docxParagraphs = MutableStateFlow<List<String>>(emptyList())
    val docxParagraphs: StateFlow<List<String>> = _docxParagraphs.asStateFlow()

    private val _outlineItems = MutableStateFlow<List<DocOutlineItem>>(emptyList())
    val outlineItems: StateFlow<List<DocOutlineItem>> = _outlineItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDocumentContent()
    }

    private fun loadDocumentContent() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val currentDoc = docRepository.getDocById(docId)
                if (currentDoc == null) {
                    _error.value = "Document not found"
                    _isLoading.value = false
                    return@launch
                }

                val file = File(currentDoc.filePath)
                if (!file.exists()) {
                    _error.value = "Document file missing from local storage"
                    _isLoading.value = false
                    return@launch
                }

                when (currentDoc.format) {
                    DocFormat.MARKDOWN, DocFormat.HTML, DocFormat.TXT, DocFormat.CODE, DocFormat.OTHER -> {
                        val text = file.readText()
                        _rawContent.value = text
                        if (currentDoc.format == DocFormat.MARKDOWN) {
                            _outlineItems.value = DocFormatParsers.extractMarkdownOutline(text)
                        }
                    }
                    DocFormat.EPUB -> {
                        val chapters = DocFormatParsers.extractEpubChapters(file)
                        _epubChapters.value = chapters
                    }
                    DocFormat.DOCX -> {
                        val paragraphs = DocFormatParsers.extractDocxParagraphs(file)
                        _docxParagraphs.value = paragraphs
                    }
                    DocFormat.PDF -> {
                        // Handled natively via PdfRenderer in DocViewerScreen
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to open document: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProgress(pageOrSection: Int, totalPagesOrSections: Int) {
        viewModelScope.launch {
            val currentDoc = docFlow.value ?: return@launch
            val total = maxOf(1, totalPagesOrSections)
            val percent = ((pageOrSection.toFloat() / total) * 100).toInt().coerceIn(0, 100)

            val updated = currentDoc.copy(
                lastReadPage = pageOrSection,
                totalPagesOrSections = total,
                percentProgress = percent,
                updatedAt = System.currentTimeMillis()
            )
            docRepository.saveDoc(updated)
        }
    }

    fun toggleBookmark(pageOrSection: Int, chapterTitle: String = "", excerpt: String = "") {
        viewModelScope.launch {
            val existing = bookmarksFlow.value.find { it.pageOrPosition == pageOrSection }
            if (existing != null) {
                docRepository.deleteBookmark(docId, pageOrSection)
            } else {
                docRepository.saveBookmark(
                    DocBookmark(
                        docId = docId,
                        pageOrPosition = pageOrSection,
                        chapterTitle = chapterTitle,
                        excerpt = excerpt
                    )
                )
            }
        }
    }

    fun addHighlight(pageOrSection: Int, selectedText: String, colorHex: String = "#2ECC71", noteText: String = "") {
        viewModelScope.launch {
            docRepository.saveHighlight(
                DocHighlight(
                    id = "hl_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
                    docId = docId,
                    pageOrPosition = pageOrSection,
                    selectedText = selectedText,
                    colorHex = colorHex,
                    noteText = noteText
                )
            )
        }
    }

    fun updateSettings(settings: DocSettings) {
        viewModelScope.launch {
            docRepository.updateDocSettings(settings)
        }
    }
}
