package com.keeftalk.chat.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.NoteRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.util.NotesLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

import com.keeftalk.chat.ui.screens.editor.toSpanStyle
import com.keeftalk.chat.ui.screens.editor.toRichTextParts
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _noteId = MutableStateFlow<String?>(null)
    
    val note: StateFlow<Note?> = _noteId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getNote(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _blocks = MutableStateFlow<List<NoteBlock>>(emptyList())
    val blocks = _blocks.asStateFlow()

    private val _savingState = MutableStateFlow<SavingState>(SavingState.Idle)
    val savingState = _savingState.asStateFlow()

    private val _activeBlockIndex = MutableStateFlow<Int?>(null)
    val activeBlockIndex = _activeBlockIndex.asStateFlow()

    private val _selection = MutableStateFlow(TextRange.Zero)
    val selection = _selection.asStateFlow()

    private val _isToolbarPopupVisible = MutableStateFlow(false)
    val isToolbarPopupVisible = _isToolbarPopupVisible.asStateFlow()

    private val currentUserId = authRepository.currentUserProfile.map { it?.id ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Undo/Redo Stacks
    private val history = mutableListOf<List<NoteBlock>>()
    private val redoStack = mutableListOf<List<NoteBlock>>()
    private var isUndoRedoAction = false

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    // Mentions
    private val _showMentionPopup = MutableStateFlow(false)
    val showMentionPopup = _showMentionPopup.asStateFlow()

    private val _mentionQuery = MutableStateFlow("")
    val mentionQuery = _mentionQuery.asStateFlow()

    val contacts = chatRepository.getContacts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mentionSuggestions = mentionQuery.flatMapLatest { query ->
        if (query.isEmpty()) contacts
        else flow {
            val results = chatRepository.searchUsers(query).getOrDefault(emptyList())
            emit(results.map { profile ->
                User(
                    id = profile.id,
                    name = profile.fullName ?: profile.username,
                    username = profile.username,
                    avatarUrl = profile.avatarUrl,
                    isActive = true,
                    lastSeen = 0L,
                    isContact = false
                )
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var saveJob: Job? = null
    private var historyJob: Job? = null
    private var isInitializing = false

    init {
        viewModelScope.launch {
            note.collect { n ->
                if (n != null && !isInitializing && _blocks.value.isEmpty()) {
                    isInitializing = true
                    try {
                        NotesLogger.d("EDITOR", "Loading note content", noteId = n.id)
                        val decoded = json.decodeFromString<List<NoteBlock>>(n.content)
                        val initialBlocks = if (decoded.isEmpty()) listOf(NoteBlock.Text(style = "NORMAL")) else decoded
                        _blocks.value = initialBlocks
                        pushHistory(initialBlocks)
                    } catch (e: Exception) {
                        NotesLogger.e("EDITOR", "Failed to decode note content, using default", throwable = e)
                        val initialBlocks = listOf(NoteBlock.Text(style = "NORMAL"))
                        _blocks.value = initialBlocks
                        pushHistory(initialBlocks)
                    }
                    isInitializing = false
                }
            }
        }
    }

    fun loadNote(id: String) {
        _noteId.value = id
    }

    fun updateBlock(index: Int, newBlock: NoteBlock) {
        val current = _blocks.value.toMutableList()
        if (index in current.indices) {
            current[index] = newBlock
            _blocks.value = current
            if (!isUndoRedoAction) {
                // Debounce history to avoid snapshotting every keystroke
                historyJob?.cancel()
                historyJob = viewModelScope.launch {
                    delay(500.milliseconds)
                    pushHistory(current)
                    redoStack.clear()
                    _canRedo.value = false
                }
            }
            scheduleSave()
        }
    }

    fun setActiveBlock(index: Int?, selection: TextRange = TextRange.Zero) {
        _activeBlockIndex.value = index
        _selection.value = selection
    }

    fun setToolbarPopupVisible(visible: Boolean) {
        _isToolbarPopupVisible.value = visible
    }

    fun applyFormatting(action: com.keeftalk.chat.ui.screens.editor.components.ToolbarAction) {
        val index = _activeBlockIndex.value ?: return
        val currentBlocks = _blocks.value.toMutableList()
        val block = currentBlocks[index] as? NoteBlock.Text ?: return
        val range = _selection.value
        
        if (range.collapsed) {
            // Block level alignment can still be toggled
            val updated = when (action) {
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.AlignLeft -> block.copy(alignment = "LEFT")
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.AlignCenter -> block.copy(alignment = "CENTER")
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.AlignRight -> block.copy(alignment = "RIGHT")
                else -> block
            }
            if (updated != block) updateBlock(index, updated)
            return
        }

        val originalAnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
            block.richText.forEach { part ->
                withStyle(style = part.toSpanStyle()) {
                    append(part.text)
                }
            }
        }

        val newAnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
            append(originalAnnotatedString)
            val style = when (action) {
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.Bold -> androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.Italic -> androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.Underline -> androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.Strike -> androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                is com.keeftalk.chat.ui.screens.editor.components.ToolbarAction.Color -> androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color(action.hex.toColorInt()))
                else -> null
            }
            if (style != null) {
                addStyle(style, range.start, range.end)
            }
        }

        val newParts = newAnnotatedString.toRichTextParts()
        updateBlock(index, block.copy(richText = newParts))
    }

    fun addBlock(block: NoteBlock, index: Int? = null) {
        val current = _blocks.value.toMutableList()
        if (index != null && index in current.indices) {
            current.add(index + 1, block)
        } else {
            current.add(block)
        }
        _blocks.value = current
        pushHistory(current)
        redoStack.clear()
        _canRedo.value = false
        scheduleSave()
    }

    fun removeBlock(index: Int) {
        val current = _blocks.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            if (current.isEmpty()) current.add(NoteBlock.Text())
            _blocks.value = current
            pushHistory(current)
            redoStack.clear()
            _canRedo.value = false
            scheduleSave()
        }
    }

    fun updateTitle(title: String) {
        val currentNote = note.value ?: return
        if (currentNote.title == title) return
        val updated = currentNote.copy(title = title, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveNote(updated) }
    }

    fun undo() {
        if (history.size > 1) {
            isUndoRedoAction = true
            val current = history.removeAt(history.size - 1)
            redoStack.add(current)
            val previous = history.last()
            _blocks.value = previous
            _canUndo.value = history.size > 1
            _canRedo.value = true
            scheduleSave()
            isUndoRedoAction = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedoAction = true
            val next = redoStack.removeAt(redoStack.size - 1)
            history.add(next)
            _blocks.value = next
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            scheduleSave()
            isUndoRedoAction = false
        }
    }

    private fun pushHistory(state: List<NoteBlock>) {
        if (history.lastOrNull() == state) return
        history.add(state)
        if (history.size > 50) history.removeAt(0)
        _canUndo.value = history.size > 1
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _savingState.value = SavingState.Saving
            delay(2000.milliseconds)
            val currentNote = note.value ?: return@launch
            val contentJson = json.encodeToString(_blocks.value)
            val updated = currentNote.copy(content = contentJson, updatedAt = System.currentTimeMillis())
            repository.saveNote(updated)
            _savingState.value = SavingState.Saved
            delay(1000.milliseconds)
            _savingState.value = SavingState.Idle
        }
    }

    fun uploadImage(uri: String) {
        viewModelScope.launch {
            try {
                val currentNote = note.value ?: return@launch
                val fileId = repository.uploadMedia(uri)
                addBlock(NoteBlock.Image(mediaId = fileId))
            } catch (e: Exception) {
                NotesLogger.e("EDITOR", "Media upload failed", throwable = e)
            }
        }
    }

    fun getMediaUrl(mediaId: String): StateFlow<String?> {
        return flow {
            val url = repository.downloadToCache(mediaId)
            emit(url)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun togglePin() {
        val currentNote = note.value ?: return
        viewModelScope.launch {
            repository.pinNote(currentNote.id, !currentNote.pinned)
        }
    }

    fun toggleArchive() {
        val currentNote = note.value ?: return
        viewModelScope.launch {
            repository.archiveNote(currentNote.id, !currentNote.archived)
        }
    }

    fun setMentionQuery(query: String) {
        _mentionQuery.value = query
        _showMentionPopup.value = query.isNotEmpty()
    }

    fun setShowMentionPopup(show: Boolean) {
        _showMentionPopup.value = show
    }

    fun insertMention(user: User, blockIndex: Int) {
        val currentBlocks = _blocks.value.toMutableList()
        val block = currentBlocks[blockIndex] as? NoteBlock.Text ?: return
        
        // Find the @ trigger in the last rich text part or construct a new one
        // For simplicity, we'll append it to the text for now
        // In a full implementation, we'd find the exact '@' and replace it
        
        val mentionPart = RichTextPart(
            text = "@${user.name} ",
            spans = listOf(
                SpanMetadata("COLOR", "#2563EB"),
                SpanMetadata("BOLD")
            )
        )
        
        val newParts = block.richText.toMutableList()
        // Replace the last part if it was a query or just add
        newParts.add(mentionPart)
        
        updateBlock(blockIndex, block.copy(richText = newParts))
        setShowMentionPopup(false)
    }
}
