package com.keeftalk.chat.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.domain.repository.NoteRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.FileRepository
import com.keeftalk.chat.util.NotesLogger
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class UnifiedNotesViewModel(
    private val repository: NoteRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val fileRepository: FileRepository,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    init {
        NotesLogger.i("VIEWMODEL", "UnifiedNotesViewModel initialized")
    }

    private val _activeContainer = MutableStateFlow("All")
    val activeContainer = _activeContainer.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedNoteId = MutableStateFlow<String?>(null)
    val selectedNoteId = _selectedNoteId.asStateFlow()

    val currentUserId = authRepository.currentUserProfile.map { 
        val id = it?.id ?: ""
        NotesLogger.v("AUTH", "Current User ID updated: $id")
        id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val customContainers = prefs.userPreferencesFlow.map { 
        NotesLogger.v("VIEWMODEL", "Custom containers updated: ${it.customNoteContainers}")
        it.customNoteContainers 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = combine(_activeContainer, _searchQuery) { container, query ->
        NotesLogger.v("VIEWMODEL", "Querying notes for container=$container, query=$query")
        repository.getNotes(container, query)
    }.flatMapLatest { it }
        .onEach { NotesLogger.v("VIEWMODEL", "Emitted ${it.size} notes to UI") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveNotes = repository.getNotes("All", "").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allArchivedNotes = repository.getNotes("Archived", "").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val counts: StateFlow<Map<String, Int>> = combine(allActiveNotes, allArchivedNotes, customContainers) { active, archived, custom ->
        val map = mutableMapOf<String, Int>()
        map["All"] = active.size
        map["Archived"] = archived.size
        map["Shared"] = active.count { it.sharedUsers.isNotEmpty() }
        custom.forEach { c ->
            map[c] = active.count { it.container == c }
        }
        map.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedNote: StateFlow<Note?> = _selectedNoteId.flatMapLatest { id ->
        NotesLogger.d("VIEWMODEL", "Switching selected note to id=$id")
        if (id == null) flowOf(null)
        else repository.getNote(id)
    }.onEach { NotesLogger.v("VIEWMODEL", "Selected note data updated in UI", noteId = it?.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val canEdit = combine(selectedNote, currentUserId) { note, userId ->
        val can = if (note == null) true 
        else if (note.ownerId == userId || userId == "admin") true
        else {
            val share = note.sharedUsers.find { it.user.id == userId }
            share?.access == "write"
        }
        if (note != null) {
            NotesLogger.i("REALTIME", if (can) "Editor unlocked (Read/Write)" else "Editor locked (Read Only)", noteId = note.id)
        }
        can
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val richTextState = RichTextState()

    private val _savingState = MutableStateFlow<SavingState>(SavingState.Idle)
    val savingState = _savingState.asStateFlow()

    // History stack for Undo/Redo
    private val history = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private var isUndoing = false

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _isMediaUploading = MutableStateFlow(false)
    val isMediaUploading = _isMediaUploading.asStateFlow()

    private val _isCreatingNote = MutableStateFlow(false)
    val isCreatingNote = _isCreatingNote.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()
    private var editingLockJob: Job? = null

    private var saveJob: Job? = null

    val contacts = chatRepository.getContacts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _mentionQuery = MutableStateFlow("")
    val mentionQuery = _mentionQuery.asStateFlow()

    val mentionSuggestions = mentionQuery.flatMapLatest { query ->
        NotesLogger.v("MENTION", "Querying mention suggestions for '$query'")
        if (query.isBlank()) {
            contacts
        } else {
            flow {
                val results = chatRepository.searchUsers(query).getOrDefault(emptyList())
                NotesLogger.v("MENTION", "Found ${results.size} users for mention")
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
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showShareModal = MutableStateFlow(false)
    val showShareModal = _showShareModal.asStateFlow()

    private val _showMentionPopup = MutableStateFlow(false)
    val showMentionPopup = _showMentionPopup.asStateFlow()

    private val _isToolbarExpanded = MutableStateFlow(true)
    val isToolbarExpanded = _isToolbarExpanded.asStateFlow()

    private val _shareSearchQuery = MutableStateFlow("")
    val shareSearchQuery = _shareSearchQuery.asStateFlow()

    val shareSuggestions = shareSearchQuery.flatMapLatest { query ->
        if (query.isBlank()) contacts
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

    fun setShareSearchQuery(query: String) {
        _shareSearchQuery.value = query
    }

    fun setShowShareModal(show: Boolean) {
        NotesLogger.d("UI", "setShowShareModal: $show")
        _showShareModal.value = show
        if (!show) _shareSearchQuery.value = ""
    }

    fun setShowMentionPopup(show: Boolean) {
        NotesLogger.v("UI", "setShowMentionPopup: $show")
        _showMentionPopup.value = show
    }

    fun setToolbarExpanded(expanded: Boolean) {
        _isToolbarExpanded.value = expanded
    }

    fun setMentionQuery(query: String) {
        _mentionQuery.value = query
    }

    fun syncNotes() {
        NotesLogger.i("SYNC", "Manual sync requested from UI")
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncNotes()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    suspend fun uploadMedia(uri: String): String? {
        NotesLogger.i("MEDIA_UPLOAD_STARTED", "Starting upload for URI: $uri")
        _isMediaUploading.value = true
        return try {
            val fileId = repository.uploadMedia(uri)
            NotesLogger.i("MEDIA_UPLOAD_SUCCESS", "Upload success! File ID: $fileId")
            
            // Increment reference count for the new file
            fileRepository.incrementReferenceCount(fileId)
            
            fileId
        } catch (e: Exception) {
            NotesLogger.e("MEDIA", "Upload failed critical error", throwable = e)
            null
        } finally {
            _isMediaUploading.value = false
        }
    }

    suspend fun saveFile(file: File): Boolean {
        return try {
            fileRepository.saveFile(file)
            true
        } catch (e: Exception) {
            NotesLogger.e("VIEWMODEL", "Failed to save file", throwable = e)
            false
        }
    }

    suspend fun downloadToCache(fileId: String): String {
        return repository.downloadToCache(fileId)
    }

    fun updateMediaInHtml(id: String, newUrl: String, status: String) {
        val traceId = id.take(8)
        NotesLogger.i("MEDIA_DEBUG", "[traceId=$traceId] STAGE 6.5: Updating placeholder in editor with real URL: $newUrl")
        
        val rawExport = richTextState.toHtml()
        com.keeftalk.chat.util.NotesUtils.checkForCorruption(rawExport, "updateMediaInHtml_entry")
        
        val currentHtml = com.keeftalk.chat.util.NotesUtils.sanitizeHtml(rawExport)
        
        // Match src="keeftalk-media://[type]/$id"
        // Use regex to find any src that contains our ID with the keeftalk-media scheme
        val srcRegex = """src=["']keeftalk-media://([^/]+/)?$id["']""".toRegex()
        
        var newHtml = currentHtml.replace(srcRegex, "src=\"$newUrl\"")
        
        // Also update data-status if it exists, or add it
        val statusPattern = """(data-id="$id"[^>]*data-status=")([^"]*)(")""".toRegex()
        if (newHtml.contains(statusPattern)) {
            newHtml = statusPattern.replace(newHtml) { matchResult ->
                "${matchResult.groupValues[1]}$status${matchResult.groupValues[3]}"
            }
        } else {
            // If data-status doesn't exist, add it after data-id
            newHtml = newHtml.replace("data-id=\"$id\"", "data-id=\"$id\" data-status=\"$status\"")
        }
        
        if (newHtml != currentHtml) {
            NotesLogger.i("MEDIA_DEBUG", "[traceId=$traceId] STAGE 6.6: HTML Replacement successful. Applying to RichTextState.")
            val selection = richTextState.selection
            isUndoing = true
            
            com.keeftalk.chat.util.NotesUtils.checkForCorruption(newHtml, "updateMediaInHtml_final")
            richTextState.setHtml(newHtml)
            
            val newLength = richTextState.annotatedString.length
            if (selection.end <= newLength) {
                richTextState.selection = selection
                NotesLogger.v("EDITOR", "Cursor preserved at ${selection.end}")
            } else {
                richTextState.selection = TextRange(newLength)
            }
            isUndoing = false
            onContentChange()
            NotesLogger.i("MEDIA_DEBUG", "[traceId=$traceId] STAGE 6.7: RichTextState updated. Final length: $newLength")
        } else {
            NotesLogger.w("MEDIA_DEBUG", "[traceId=$traceId] STAGE 6.6: FAILED to find placeholder in HTML to update. Check regex.")
        }
    }

    init {
        NotesLogger.i("SYNC", "Performing initial sync on startup")
        syncNotes()
        
        // Background queue processor
        viewModelScope.launch {
            while (true) {
                delay(30000)
                repository.processSyncQueue()
            }
        }
        
        // Start realtime observation
        viewModelScope.launch {
            repository.observeNotesRealtime().collect {
                NotesLogger.i("REALTIME", "Realtime update received in ViewModel")
            }
        }

        // Periodic sync (every 60 seconds as a safety measure to prevent DB lock)
        viewModelScope.launch {
            while (true) {
                delay(60000)
                NotesLogger.v("SYNC", "Periodic background sync triggered (60s interval)")
                repository.syncNotes()
            }
        }

        viewModelScope.launch {
            selectedNote.collect { note ->
                if (note != null) {
                    if (_isEditing.value) {
                        NotesLogger.i("EDITOR", "Ignored remote update because local editing is active", noteId = note.id)
                        return@collect
                    }
                    
                    val currentHtml = com.keeftalk.chat.util.NotesUtils.sanitizeHtml(richTextState.toHtml())
                    if (note.content != currentHtml) {
                        NotesLogger.i("MEDIA_DEBUG", "STAGE 5: Note loaded from DB/Remote. Length: ${note.content.length}")
                        com.keeftalk.chat.util.NotesUtils.checkForCorruption(note.content, "DB_Load_Raw")
                        
                        val imgTags = """<img[^>]+>""".toRegex().findAll(note.content).toList()
                        NotesLogger.i("MEDIA_DEBUG", "  Found ${imgTags.size} img tags in loaded content")
                        imgTags.forEachIndexed { i, match ->
                            NotesLogger.v("MEDIA_DEBUG", "  IMG[$i]: ${match.value}")
                        }

                        NotesLogger.i("EDITOR", "Applying remote update", noteId = note.id)
                        
                        // Preserve selection if possible
                        val selection = richTextState.selection
                        
                        isUndoing = true // Prevent onContentChange from being triggered by this update
                        val sanitizedInput = com.keeftalk.chat.util.NotesUtils.sanitizeHtml(note.content)
                        com.keeftalk.chat.util.NotesUtils.checkForCorruption(sanitizedInput, "DB_Load_Sanitized")
                        
                        richTextState.setHtml(sanitizedInput)
                        
                        // Attempt to restore selection (clamp to new length)
                        val newLength = richTextState.annotatedString.length
                        if (selection.end <= newLength) {
                            richTextState.selection = selection
                            NotesLogger.v("EDITOR", "Cursor preserved at ${selection.end}", noteId = note.id)
                        } else {
                            richTextState.selection = TextRange(newLength)
                        }
                        isUndoing = false
                        
                        // Update history
                        if (history.isEmpty() || history.last() != note.content) {
                            history.add(note.content)
                            if (history.size > 50) history.removeAt(0)
                            _canUndo.value = history.size > 1
                        }
                    }
                } else {
                    if (richTextState.toHtml().isNotEmpty()) {
                        NotesLogger.v("EDITOR", "Clearing RichTextState (no note selected)")
                        richTextState.setHtml("")
                        history.clear()
                        redoStack.clear()
                    }
                }
            }
        }
    }

    fun setContainer(container: String) {
        NotesLogger.i("UI", "Container changed to $container")
        _activeContainer.value = container
        _selectedNoteId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectNote(id: String?) {
        NotesLogger.i("UI", "Note selected: $id")
        if (id != _selectedNoteId.value) {
            // If leaving a note, ensure we release edit lock
            if (_selectedNoteId.value != null && _isEditing.value) {
                _isEditing.value = false
                NotesLogger.i("EDITOR", "Leaving note, releasing edit lock", noteId = _selectedNoteId.value)
            }

            if (id != null) NotesLogger.i("REALTIME", "User joined note", noteId = id)
            else if (_selectedNoteId.value != null) NotesLogger.i("REALTIME", "User left note", noteId = _selectedNoteId.value)
        }
        _selectedNoteId.value = id
    }

    fun createNote() {
        // Optimization: Use Fast cache for ID to avoid waiting for Flow on UI event
        val fastId = prefs.getUserIdFast() ?: ""
        val userId = if (fastId.isNotEmpty()) fastId else currentUserId.value
        
        NotesLogger.i("UI", "createNote requested by user '$userId'")
        
        viewModelScope.launch {
            _isCreatingNote.value = true
            try {
                // Ensure we have a valid User ID
                val finalUserId = if (userId.isEmpty()) {
                    withTimeoutOrNull(3000) {
                        authRepository.getCurrentSession()?.id
                    } ?: ""
                } else userId

                if (finalUserId.isEmpty()) {
                    NotesLogger.e("UI", "createNote failed: no user ID found")
                    return@launch
                }

                // Create the note object
                val newNoteId = UUID.randomUUID().toString()
                val newNote = Note(
                    id = newNoteId,
                    title = "Untitled",
                    content = "",
                    container = if (activeContainer.value == "Archived" || activeContainer.value == "Shared") "All" else activeContainer.value,
                    ownerId = finalUserId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // Pre-navigate to Edit Screen to make it feel instant
                _selectedNoteId.value = newNoteId

                // Save to DB in background. 
                // We use a separate launch to allow the creating spinner to hide quickly.
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        withTimeout(7000) {
                            repository.saveNote(newNote)
                        }
                        NotesLogger.d("UI", "Successfully saved new note to DB", noteId = newNoteId)
                    } catch (e: IllegalStateException) {
                        NotesLogger.e("UI", "Security context missing while saving new note", noteId = newNoteId, throwable = e)
                        // Trigger a UI event or clear selection if we want to "disappear" the editor
                        // but actually we should probably notify the user.
                    } catch (e: Exception) {
                        NotesLogger.e("UI", "Failed to save new note to DB", noteId = newNoteId, throwable = e)
                    }
                }

            } catch (e: Exception) {
                NotesLogger.e("UI", "createNote critical error", throwable = e)
                // If navigation happened but save failed, we should probably handle it,
                // but pre-navigation is a standard "instant feel" pattern.
            } finally {
                // Non-cancellable cleanup
                withContext(NonCancellable) {
                    _isCreatingNote.value = false
                }
            }
        }
    }

    fun updateTitle(title: String) {
        val currentNote = selectedNote.value ?: return
        if (currentNote.title == title) return
        
        NotesLogger.v("AUTOSAVE", "Title changed to '$title'", noteId = currentNote.id)
        val updatedNote = currentNote.copy(title = title, updatedAt = System.currentTimeMillis())
        viewModelScope.launch { repository.saveNote(updatedNote) }
    }

    fun addAttachment(file: File) {
        val currentNote = selectedNote.value ?: return
        NotesLogger.i("AUTOSAVE", "Adding attachment ${file.id}", noteId = currentNote.id)
        val updatedNote = currentNote.copy(
            attachments = currentNote.attachments + file,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repository.saveNote(updatedNote) }
    }

    fun onContentChange() {
        if (isUndoing) return // Critical: Prevent loop when applying remote updates
        if (!canEdit.value) return

        val currentNote = selectedNote.value ?: return
        
        // Start editing lock
        _isEditing.value = true
        NotesLogger.v("EDITOR", "User started typing / Local edit active", noteId = currentNote.id)
        editingLockJob?.cancel()
        editingLockJob = viewModelScope.launch {
            delay(3000) // 3 second debounce for the editing lock
            _isEditing.value = false
            NotesLogger.v("EDITOR", "Editing lock released", noteId = currentNote.id)
        }

        val rawHtml = richTextState.toHtml()
        com.keeftalk.chat.util.NotesUtils.checkForCorruption(rawHtml, "autoSave_EditorExport")
        
        val newContent = com.keeftalk.chat.util.NotesUtils.sanitizeHtml(rawHtml)
        com.keeftalk.chat.util.NotesUtils.checkForCorruption(newContent, "autoSave_Sanitized")
        
        // Stage 4 Trace: HTML Generation
        val imgTags = """<img[^>]+>""".toRegex().findAll(rawHtml).toList()
        if (imgTags.isNotEmpty()) {
            NotesLogger.i("MEDIA_DEBUG", "STAGE 4: Exporting HTML for Save. Len: ${rawHtml.length}, Img tags: ${imgTags.size}")
            imgTags.forEachIndexed { i, match ->
                 NotesLogger.v("MEDIA_DEBUG", "  EXPORT_IMG[$i]: ${match.value}")
            }
        }
        
        // Only save if there's an actual meaningful change
        if (currentNote.content == newContent) return

        val lastHistory = history.lastOrNull()
        if (lastHistory != newContent) {
            history.add(newContent)
            if (history.size > 50) history.removeAt(0)
            redoStack.clear()
            _canUndo.value = history.size > 1
            _canRedo.value = false
        }

        NotesLogger.v("AUTOSAVE", "Content change detected, scheduling save", noteId = currentNote.id)
        autoSave(currentNote.copy(content = newContent, updatedAt = System.currentTimeMillis()))
    }

    fun undo() {
        if (history.size > 1) {
            isUndoing = true
            val current = history.removeAt(history.size - 1)
            redoStack.add(current)
            val previous = history.last()
            richTextState.setHtml(com.keeftalk.chat.util.NotesUtils.sanitizeHtml(previous))
            _canUndo.value = history.size > 1
            _canRedo.value = true
            isUndoing = false
            
            // Trigger save
            val currentNote = selectedNote.value ?: return
            autoSave(currentNote.copy(content = previous, updatedAt = System.currentTimeMillis()))
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoing = true
            val next = redoStack.removeAt(redoStack.size - 1)
            history.add(next)
            richTextState.setHtml(com.keeftalk.chat.util.NotesUtils.sanitizeHtml(next))
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            isUndoing = false
            
            // Trigger save
            val currentNote = selectedNote.value ?: return
            autoSave(currentNote.copy(content = next, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun autoSave(note: Note) {
        if (!canEdit.value) return
        saveJob?.cancel()
        NotesLogger.v("AUTOSAVE", "Scheduling auto-save in 3000ms (debounced)", noteId = note.id)
        saveJob = viewModelScope.launch {
            _savingState.value = SavingState.Saving
            delay(3000.milliseconds)
            
            NotesLogger.i("AUTOSAVE", "Typing stopped, triggering database save", noteId = note.id)
            repository.saveNote(note)
            
            _savingState.value = SavingState.Saved
            delay(1000.milliseconds)
            _savingState.value = SavingState.Idle
            
            // Lock is released after save completes
            _isEditing.value = false
            NotesLogger.i("EDITOR", "Local edit completed / sync safe", noteId = note.id)
        }
    }

    fun togglePin() {
        val currentNote = selectedNote.value ?: return
        val newPinned = !currentNote.pinned
        NotesLogger.i("UI", "togglePin: $newPinned", noteId = currentNote.id)
        viewModelScope.launch {
            repository.pinNote(currentNote.id, newPinned)
        }
    }

    fun toggleArchive() {
        val currentNote = selectedNote.value ?: return
        val newArchived = !currentNote.archived
        NotesLogger.i("UI", "toggleArchive: $newArchived", noteId = currentNote.id)
        viewModelScope.launch {
            repository.archiveNote(currentNote.id, newArchived)
            if (!newArchived) _selectedNoteId.value = null
        }
    }

    fun deletePermanently() {
        val currentNote = selectedNote.value ?: return
        NotesLogger.i("UI", "deletePermanently", noteId = currentNote.id)
        viewModelScope.launch {
            repository.deleteNote(currentNote.id)
            _selectedNoteId.value = null
        }
    }

    fun addContainer(name: String) {
        NotesLogger.i("UI", "addContainer: $name")
        viewModelScope.launch {
            val current = customContainers.value.toMutableList()
            if (!current.contains(name)) {
                current.add(name)
                prefs.updateCustomNoteContainers(current)
            } else {
                NotesLogger.w("UI", "addContainer failed: '$name' already exists")
            }
        }
    }

    fun moveToContainer(container: String) {
        val currentNote = selectedNote.value ?: return
        NotesLogger.i("UI", "moveToContainer: $container", noteId = currentNote.id)
        viewModelScope.launch {
            repository.saveNote(currentNote.copy(container = container, updatedAt = System.currentTimeMillis()))
        }
    }

    fun changeColor(color: String) {
        val currentNote = selectedNote.value ?: return
        NotesLogger.i("UI", "changeColor: $color", noteId = currentNote.id)
        viewModelScope.launch {
            repository.saveNote(currentNote.copy(color = color, updatedAt = System.currentTimeMillis()))
        }
    }

    fun addShare(userId: String, access: String) {
        val currentNote = selectedNote.value ?: return
        NotesLogger.i("UI", "addShare: targetUser=$userId, access=$access", noteId = currentNote.id)
        viewModelScope.launch {
            repository.addShare(currentNote.id, userId, access)
        }
    }

    fun removeShare(userId: String) {
        val currentNote = selectedNote.value ?: return
        NotesLogger.i("UI", "removeShare: targetUser=$userId", noteId = currentNote.id)
        viewModelScope.launch {
            repository.removeShare(currentNote.id, userId)
        }
    }
}

sealed class SavingState {
    data object Idle : SavingState()
    data object Saving : SavingState()
    data object Saved : SavingState()
}
