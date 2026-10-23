package tn.keeftalk.chat.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.AuthRepository
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.domain.repository.NoteRepository
import tn.keeftalk.chat.ui.screens.editor.SpanType
import tn.keeftalk.chat.ui.screens.editor.StyleToggle
import tn.keeftalk.chat.ui.screens.editor.components.ToolbarAction
import tn.keeftalk.chat.ui.screens.editor.toRichDoc
import tn.keeftalk.chat.util.NotesLogger
import tn.keeftalk.chat.util.NotesUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Blocks + a revision counter in ONE state object so the UI can never observe "new blocks, old revision".
 * [revision] changes only for EXTERNAL text changes (undo/redo, formatting, mention insert, split, remote adopt);
 * ordinary typing never bumps it. Text editors reset themselves from the model only when it changes.
 */
data class EditorState(val blocks: List<NoteBlock> = emptyList(), val revision: Int = 0)

data class FocusRequest(val blockId: String, val atEnd: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private companion object {
        const val SAVE_DEBOUNCE_MS = 800L
        const val HISTORY_IDLE_MS = 600L
        const val HISTORY_LIMIT = 100
        const val SAVE_RETRY_MS = 5_000L
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Not tied to viewModelScope: the final save must survive the ViewModel being cleared. */
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveMutex = Mutex()

    // ------------------------------------------------------------------ state

    private val _noteId = MutableStateFlow<String?>(null)

    val note: StateFlow<Note?> = _noteId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getNote(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    /** True when the stored content could not be decoded. Editing is disabled so we never overwrite it. */
    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _savingState = MutableStateFlow<SavingState>(SavingState.Idle)
    val savingState = _savingState.asStateFlow()

    private val _activeBlockId = MutableStateFlow<String?>(null)
    val activeBlockId: StateFlow<String?> = _activeBlockId.asStateFlow()
    private var selection: TextRange = TextRange.Zero

    private val _pendingStyles = MutableStateFlow<List<StyleToggle>>(emptyList())
    val pendingStyles: StateFlow<List<StyleToggle>> = _pendingStyles.asStateFlow()

    private val _focusRequest = MutableStateFlow<FocusRequest?>(null)
    val focusRequest: StateFlow<FocusRequest?> = _focusRequest.asStateFlow()

    private val _isToolbarPopupVisible = MutableStateFlow(false)
    val isToolbarPopupVisible = _isToolbarPopupVisible.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // ------------------------------------------------------------------ undo / redo

    private val history = ArrayDeque<List<NoteBlock>>()
    private val redoStack = ArrayDeque<List<NoteBlock>>()
    private var historyJob: Job? = null

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    // ------------------------------------------------------------------ mentions

    /** null = no mention in progress, "" = bare '@' (show contacts), otherwise the search text. */
    private val _mentionQuery = MutableStateFlow<String?>(null)
    val mentionQuery: StateFlow<String?> = _mentionQuery.asStateFlow()

    val contacts = chatRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mentionSuggestions: StateFlow<List<User>> = _mentionQuery
        .debounce { q -> if (q.isNullOrEmpty()) 0L else 250L }
        .flatMapLatest { q ->
            when {
                q == null -> flowOf(emptyList<User>())
                q.isEmpty() -> contacts
                else -> flow { emit(searchUsers(q)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun searchUsers(query: String): List<User> =
        chatRepository.searchUsers(query).getOrDefault(emptyList()).map { profile ->
            User(
                id = profile.id,
                name = profile.fullName ?: profile.username,
                username = profile.username,
                avatarUrl = profile.avatarUrl,
                isActive = true,
                lastSeen = 0L,
                isContact = false
            )
        }

    // ------------------------------------------------------------------ persistence bookkeeping

    private var saveJob: Job? = null
    private var loadedNoteId: String? = null
    private var isTitleManual = false

    @Volatile private var dirty = false
    @Volatile private var editVersion = 0L
    @Volatile private var lastSavedAt = 0L

    init {
        viewModelScope.launch {
            note.filterNotNull().collect { onNoteEmitted(it) }
        }
    }

    fun loadNote(id: String) {
        if (_noteId.value == id) return
        if (dirty) {
            persistScope.launch { flushNow() }
        }
        loadedNoteId = null
        dirty = false
        _loadError.value = false
        _activeBlockId.value = null
        _pendingStyles.value = emptyList()
        _editorState.value = EditorState()
        _noteId.value = id
    }

    // ------------------------------------------------------------------ load / remote changes

    private fun onNoteEmitted(n: Note) {
        if (n.id != _noteId.value) return
        if (loadedNoteId != n.id) {
            initialLoad(n)
            return
        }
        // Later emissions: our own save echoing back, or a remote edit (shared notes).
        if (dirty || _loadError.value) return          // local edits win until saved
        if (n.updatedAt <= lastSavedAt) return          // our own echo / older data
        val decoded = decodeBlocks(n.content) ?: return
        val incoming = normalizeBlocks(decoded)
        lastSavedAt = n.updatedAt
        if (incoming == _editorState.value.blocks) return
        NotesLogger.d("EDITOR", "Adopting remote changes", noteId = n.id)
        _editorState.update { EditorState(incoming, it.revision + 1) }
        applyTitleFrom(n, decoded)
        resetHistoryTo(incoming)
    }

    private fun initialLoad(n: Note) {
        NotesLogger.d("EDITOR", "Loading note content", noteId = n.id)
        loadedNoteId = n.id
        lastSavedAt = n.updatedAt
        dirty = false
        val decoded = decodeBlocks(n.content)
        if (decoded == null) {
            // Do NOT fall back to an empty doc: the next autosave would overwrite the user's real content.
            _loadError.value = true
            _editorState.value = EditorState(listOf(NoteBlock.Text()), 0)
            _title.value = n.title
            return
        }
        _loadError.value = false
        val blocks = normalizeBlocks(decoded)
        _editorState.value = EditorState(blocks, 0)
        applyTitleFrom(n, decoded)
        resetHistoryTo(blocks)
    }

    private fun decodeBlocks(content: String): List<NoteBlock>? {
        if (content.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<NoteBlock>>(content)
        } catch (e: Exception) {
            NotesLogger.e("EDITOR", "Failed to decode note content", throwable = e)
            null
        }
    }

    private fun applyTitleFrom(n: Note, decoded: List<NoteBlock>) {
        val firstLine = NotesUtils.getFirstLineOfNote(decoded)
        isTitleManual = n.title.isNotBlank() && n.title != "Untitled" && n.title != firstLine
        _title.value = n.title
    }

    /** Invariant: never empty, and the last block is always a Text block so the user can keep typing. */
    private fun normalizeBlocks(list: List<NoteBlock>): List<NoteBlock> {
        if (list.isEmpty()) return listOf(NoteBlock.Text())
        return if (list.last() is NoteBlock.Text) list else list + NoteBlock.Text()
    }

    // ------------------------------------------------------------------ block mutations

    private fun mutateBlock(
        blockId: String,
        bumpRevision: Boolean = false,
        transform: (NoteBlock) -> NoteBlock
    ): Boolean {
        var changed = false
        _editorState.update { st ->
            val idx = st.blocks.indexOfFirst { it.id == blockId }
            if (idx < 0) return@update st
            val old = st.blocks[idx]
            val new = transform(old)
            if (new == old) return@update st
            changed = true
            val list = st.blocks.toMutableList()
            list[idx] = new
            EditorState(list, if (bumpRevision) st.revision + 1 else st.revision)
        }
        return changed
    }

    private inline fun <reified T : NoteBlock> editBlock(blockId: String, crossinline f: (T) -> T) {
        if (_loadError.value) return
        if (mutateBlock(blockId) { b -> if (b is T) f(b) else b }) afterEdit(coalesce = true)
    }

    /** Called by the text editor on every keystroke with the new persisted-format parts. */
    fun onTextChanged(blockId: String, parts: List<RichTextPart>) =
        editBlock<NoteBlock.Text>(blockId) { it.copy(richText = parts) }

    fun setCaption(blockId: String, caption: String) =
        editBlock<NoteBlock.Image>(blockId) { it.copy(caption = caption) }

    fun setTableRows(blockId: String, rows: List<TableRow>) =
        editBlock<NoteBlock.Table>(blockId) { it.copy(rows = rows) }

    fun setChecklistItems(blockId: String, items: List<ChecklistItem>) =
        editBlock<NoteBlock.Checklist>(blockId) { it.copy(items = items) }

    fun setCode(blockId: String, code: String) =
        editBlock<NoteBlock.Code>(blockId) { it.copy(code = code) }

    fun setLatex(blockId: String, latex: String) =
        editBlock<NoteBlock.Math>(blockId) { it.copy(latex = latex) }

    /** NORMAL / H1 / H2 / QUOTE for the active text block. */
    fun setTextStyle(style: String) {
        if (_loadError.value) return
        val id = _activeBlockId.value ?: return
        if (mutateBlock(id) { (it as? NoteBlock.Text)?.copy(style = style) ?: it }) afterEdit(coalesce = false)
    }

    /**
     * Inserts [block] at the caret: splits the active text block if the caret is inside it, inserts after
     * the active block otherwise, and guarantees an editable text block after non-text blocks.
     */
    fun addBlock(block: NoteBlock) {
        if (_loadError.value) return
        commitHistoryNow()
        var focusId = block.id
        _editorState.update { st ->
            val list = st.blocks.toMutableList()
            var revision = st.revision
            val activeIdx = list.indexOfFirst { it.id == _activeBlockId.value }
            var insertAt = if (activeIdx >= 0) activeIdx + 1 else list.size
            val active = list.getOrNull(activeIdx) as? NoteBlock.Text
            if (active != null) {
                val doc = active.richText.toRichDoc()
                val caret = selection.max.coerceIn(0, doc.text.length)
                if (doc.text.isNotEmpty()) {
                    if (caret == 0) {
                        insertAt = activeIdx
                    } else if (caret < doc.text.length) {
                        list[activeIdx] = active.copy(richText = doc.slice(0, caret).toParts())
                        list.add(
                            activeIdx + 1,
                            NoteBlock.Text(
                                style = if (active.style == "QUOTE") "QUOTE" else "NORMAL",
                                alignment = active.alignment,
                                richText = doc.slice(caret, doc.text.length).toParts()
                            )
                        )
                        revision++ // the original block's editor must reset to the shortened text
                        insertAt = activeIdx + 1
                    }
                }
            }
            list.add(insertAt, block)
            if (block !is NoteBlock.Text && list.getOrNull(insertAt + 1) !is NoteBlock.Text) {
                list.add(insertAt + 1, NoteBlock.Text())
            }
            focusId = when (block) {
                is NoteBlock.Code, is NoteBlock.Math, is NoteBlock.Checklist -> block.id
                else -> list.getOrNull(insertAt + 1)?.id ?: block.id
            }
            EditorState(normalizeBlocks(list), revision)
        }
        selection = TextRange.Zero
        _activeBlockId.value = focusId
        _focusRequest.value = FocusRequest(focusId)
        afterEdit(coalesce = false)
    }

    fun removeBlock(blockId: String) {
        if (_loadError.value) return
        commitHistoryNow()
        _editorState.update { st ->
            EditorState(normalizeBlocks(st.blocks.filterNot { it.id == blockId }), st.revision)
        }
        if (_activeBlockId.value == blockId) _activeBlockId.value = null
        afterEdit(coalesce = false)
    }

    // ------------------------------------------------------------------ selection / focus / toolbar

    fun setActiveBlock(blockId: String) {
        _activeBlockId.value = blockId
    }

    fun onTextSelection(blockId: String, range: TextRange) {
        _activeBlockId.value = blockId
        if (range != selection && _pendingStyles.value.isNotEmpty()) _pendingStyles.value = emptyList()
        selection = range
    }

    fun onPendingStylesConsumed() {
        _pendingStyles.value = emptyList()
    }

    fun consumeFocusRequest() {
        _focusRequest.value = null
    }

    /** Tap on the empty area below the content. */
    fun focusEnd() {
        val last = _editorState.value.blocks.lastOrNull() ?: return
        _focusRequest.value = FocusRequest(last.id, atEnd = true)
    }

    fun setToolbarPopupVisible(visible: Boolean) {
        _isToolbarPopupVisible.value = visible
    }

    // ------------------------------------------------------------------ formatting

    private fun ToolbarAction.toSpan(): Pair<String, String?>? = when (this) {
        is ToolbarAction.Bold -> SpanType.BOLD to null
        is ToolbarAction.Italic -> SpanType.ITALIC to null
        is ToolbarAction.Underline -> SpanType.UNDERLINE to null
        is ToolbarAction.Strike -> SpanType.STRIKE to null
        is ToolbarAction.Color -> SpanType.COLOR to hex
        else -> null
    }

    fun applyFormatting(action: ToolbarAction) {
        if (_loadError.value) return
        val id = _activeBlockId.value ?: return
        val block = _editorState.value.blocks.firstOrNull { it.id == id } as? NoteBlock.Text ?: return

        // Alignment is block-level: works with or without a selection.
        val alignment = when (action) {
            is ToolbarAction.AlignLeft -> "LEFT"
            is ToolbarAction.AlignCenter -> "CENTER"
            is ToolbarAction.AlignRight -> "RIGHT"
            else -> null
        }
        if (alignment != null) {
            if (mutateBlock(id) { (it as? NoteBlock.Text)?.copy(alignment = alignment) ?: it }) {
                afterEdit(coalesce = false)
            }
            return
        }

        val (type, value) = action.toSpan() ?: return
        val doc = block.richText.toRichDoc()
        val start = selection.min.coerceIn(0, doc.text.length)
        val end = selection.max.coerceIn(0, doc.text.length)

        if (end > start) {
            val newDoc = doc.toggle(type, value, start, end)
            val parts = newDoc.toParts()
            if (mutateBlock(id, bumpRevision = true) { (it as? NoteBlock.Text)?.copy(richText = parts) ?: it }) {
                afterEdit(coalesce = false)
            }
        } else {
            // Collapsed caret: toggle the "typing style" for the next characters.
            val pending = _pendingStyles.value.firstOrNull { it.type == type }
            val activeNow = if (pending != null) {
                pending.enabled && (type !in SpanType.SINGLE_VALUED || pending.value == value)
            } else {
                doc.isActiveAt(start, type, value)
            }
            _pendingStyles.value = _pendingStyles.value.filterNot { it.type == type } +
                    StyleToggle(type, value, enabled = !activeNow)
        }
    }

    // ------------------------------------------------------------------ mentions

    fun setMentionQuery(query: String?) {
        if (_mentionQuery.value != query) _mentionQuery.value = query
    }

    fun dismissMentions() {
        _mentionQuery.value = null
    }

    /** Replaces the "@query" before the caret with a styled mention. */
    fun insertMention(user: User) {
        if (_loadError.value) return
        val id = _activeBlockId.value ?: return
        val block = _editorState.value.blocks.firstOrNull { it.id == id } as? NoteBlock.Text ?: return
        val doc = block.richText.toRichDoc()
        val caret = selection.max.coerceIn(0, doc.text.length)
        val at = if (caret > 0) doc.text.lastIndexOf('@', caret - 1) else -1
        if (at < 0) {
            _mentionQuery.value = null
            return
        }
        val label = "@${user.name}"
        val newDoc = doc.replaceRange(
            start = at,
            end = caret,
            insert = "$label ",
            styleLength = label.length,
            styles = listOf(
                SpanMetadata(SpanType.MENTION, user.id),
                SpanMetadata(SpanType.COLOR, "#2563EB"),
                SpanMetadata(SpanType.BOLD)
            )
        )
        val parts = newDoc.toParts()
        selection = TextRange(at + label.length + 1)
        _mentionQuery.value = null
        if (mutateBlock(id, bumpRevision = true) { (it as? NoteBlock.Text)?.copy(richText = parts) ?: it }) {
            afterEdit(coalesce = false)
        }
    }

    // ------------------------------------------------------------------ title

    fun updateTitle(newTitle: String) {
        if (_loadError.value || newTitle == _title.value) return
        _title.value = newTitle
        isTitleManual = newTitle.isNotBlank() // blank => back to auto-title on the next body edit
        markDirty()
    }

    private fun updateAutoTitle() {
        if (isTitleManual) return
        val firstLine = NotesUtils.getFirstLineOfNote(_editorState.value.blocks)
        if (firstLine.isNotBlank() && firstLine != _title.value) _title.value = firstLine
    }

    // ------------------------------------------------------------------ undo / redo

    fun undo() {
        if (_loadError.value) return
        commitHistoryNow() // flush in-flight typing so it is undone as one step (fixes the old race)
        if (history.size <= 1) return
        redoStack.addLast(history.removeLast())
        restore(history.last())
    }

    fun redo() {
        if (_loadError.value) return
        val next = redoStack.removeLastOrNull() ?: return
        history.addLast(next)
        restore(next)
    }

    private fun restore(blocks: List<NoteBlock>) {
        _editorState.update { EditorState(blocks, it.revision + 1) }
        val active = _activeBlockId.value
        if (active != null && blocks.none { it.id == active }) _activeBlockId.value = null
        _pendingStyles.value = emptyList()
        _canUndo.value = history.size > 1
        _canRedo.value = redoStack.isNotEmpty()
        updateAutoTitle()
        markDirty()
    }

    private fun pushHistory() {
        val state = _editorState.value.blocks
        if (history.lastOrNull() == state) return
        history.addLast(state)
        while (history.size > HISTORY_LIMIT) history.removeFirst()
        _canUndo.value = history.size > 1
    }

    private fun commitHistoryNow() {
        historyJob?.cancel()
        historyJob = null
        pushHistory()
    }

    private fun resetHistoryTo(blocks: List<NoteBlock>) {
        historyJob?.cancel()
        history.clear()
        redoStack.clear()
        history.addLast(blocks)
        _canUndo.value = false
        _canRedo.value = false
    }

    /** Single choke point after any user edit. */
    private fun afterEdit(coalesce: Boolean) {
        redoStack.clear()
        _canRedo.value = false
        updateAutoTitle()
        if (coalesce) {
            historyJob?.cancel()
            historyJob = viewModelScope.launch {
                delay(HISTORY_IDLE_MS.milliseconds)
                pushHistory()
            }
        } else {
            commitHistoryNow()
        }
        markDirty()
    }

    // ------------------------------------------------------------------ saving

    private fun markDirty() {
        if (_loadError.value) return
        dirty = true
        editVersion++
        _savingState.value = SavingState.Saving
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS.milliseconds)
            // The write itself runs in persistScope: leaving the screen must not cancel it half-way.
            persistScope.launch { persist() }.join()
            if (!dirty) {
                _savingState.value = SavingState.Saved
                delay(1000.milliseconds)
                if (!dirty) _savingState.value = SavingState.Idle
            }
        }
    }

    /** Call from ON_STOP / back press. Saves immediately, bypassing the debounce. */
    suspend fun flushNow() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!dirty) return@withContext
        saveJob?.cancel()
        persistScope.launch { persist() }.join()
        if (!dirty) _savingState.value = SavingState.Idle
    }

    private suspend fun persist() {
        saveMutex.withLock { persistLocked() }
    }

    private suspend fun persistLocked() {
        if (!dirty || _loadError.value) return
        val base = note.value ?: return
        val versionAtStart = editVersion
        val blocks = _editorState.value.blocks
        val titleSnapshot = _title.value
        try {
            val contentJson = json.encodeToString(blocks)
            val now = System.currentTimeMillis()
            lastSavedAt = now // set BEFORE the write so the DB echo is recognised as ours
            repository.saveNote(base.copy(title = titleSnapshot, content = contentJson, updatedAt = now))
            if (editVersion == versionAtStart) dirty = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // An uncaught exception in viewModelScope.launch used to crash the whole app.
            NotesLogger.e("EDITOR", "Save failed, will retry", throwable = e)
            _messages.tryEmit("Couldn't save the note. Retrying…")
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        viewModelScope.launch {
            delay(SAVE_RETRY_MS.milliseconds)
            if (dirty) markDirty()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; persistScope is not.
        if (dirty) persistScope.launch { persist() }
    }

    // ------------------------------------------------------------------ media

    private val mediaUrlCache = ConcurrentHashMap<String, String>()

    fun uploadImage(uri: String) {
        if (_loadError.value) return
        viewModelScope.launch {
            try {
                val fileId = repository.uploadMedia(uri)
                addBlock(NoteBlock.Image(mediaId = fileId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NotesLogger.e("EDITOR", "Media upload failed", throwable = e)
                _messages.tryEmit("Image upload failed")
            }
        }
    }

    /** Cached. The old getMediaUrl() built a brand-new flow + download on EVERY recomposition. */
    suspend fun resolveMediaUrl(mediaId: String): String? {
        mediaUrlCache[mediaId]?.let { return it }
        return try {
            repository.downloadToCache(mediaId)?.also { mediaUrlCache[mediaId] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NotesLogger.e("EDITOR", "Media download failed", throwable = e)
            null
        }
    }

    // ------------------------------------------------------------------ note actions

    fun togglePin() {
        val currentNote = note.value ?: return
        viewModelScope.launch { repository.pinNote(currentNote.id, !currentNote.pinned) }
    }

    fun toggleArchive() {
        val currentNote = note.value ?: return
        viewModelScope.launch { repository.archiveNote(currentNote.id, !currentNote.archived) }
    }
}