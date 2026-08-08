package com.keeftalk.chat.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.util.FaceDetectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class MediaReviewViewModel : ViewModel() {

    private val faceDetectionManager = FaceDetectionManager()

    private val _editorStates = MutableStateFlow<Map<Long, EditorModel>>(emptyMap())
    val editorStates = _editorStates.asStateFlow()

    private val _reviewItems = MutableStateFlow<List<EditorModel>>(emptyList())
    val reviewItems = _reviewItems.asStateFlow()

    private val undoStacks = mutableMapOf<Long, Stack<EditorModel>>()
    private val redoStacks = mutableMapOf<Long, Stack<EditorModel>>()

    fun setMediaItems(items: List<MediaItem>) {
        val models = items.map { EditorModel(mediaItem = it) }
        setReviewItems(models)
    }

    fun setDocumentItems(items: List<DocumentModel>) {
        val models = items.map { EditorModel(documentItem = it) }
        setReviewItems(models)
    }

    fun setMixedItems(media: List<MediaItem>, docs: List<DocumentModel>) {
        val models = media.map { EditorModel(mediaItem = it) } + docs.map { EditorModel(documentItem = it) }
        setReviewItems(models)
    }

    fun setReviewItems(models: List<EditorModel>) {
        _reviewItems.value = models
        val currentStates = _editorStates.value.toMutableMap()
        models.forEach { model ->
            val id = model.mediaItem?.id ?: model.documentItem?.id ?: 0L
            if (!currentStates.containsKey(id)) {
                currentStates[id] = model
                undoStacks[id] = Stack()
                redoStacks[id] = Stack()
            }
        }
        _editorStates.value = currentStates
    }

    fun updateCaption(itemId: Long, caption: String) {
        val currentState = _editorStates.value[itemId] ?: return
        val newState = currentState.copy(caption = caption)
        val states = _editorStates.value.toMutableMap()
        states[itemId] = newState
        _editorStates.value = states
    }

    fun updateEditedText(itemId: Long, text: String) {
        val currentState = _editorStates.value[itemId] ?: return
        val newState = currentState.copy(editedText = text)
        val states = _editorStates.value.toMutableMap()
        states[itemId] = newState
        _editorStates.value = states
    }

    private fun pushUndo(itemId: Long, state: EditorModel) {
        val stack = undoStacks.getOrPut(itemId) { Stack() }
        stack.push(state)
        if (stack.size > 50) stack.removeAt(0)
        redoStacks[itemId]?.clear()
    }

    fun executeCommand(itemId: Long, command: EditorCommand) {
        val currentState = _editorStates.value[itemId] ?: return
        pushUndo(itemId, currentState)

        val newState = command.execute(currentState)
        val states = _editorStates.value.toMutableMap()
        states[itemId] = newState
        _editorStates.value = states
    }

    fun undo(itemId: Long) {
        val uStack = undoStacks[itemId]
        if (uStack?.isNotEmpty() == true) {
            val currentState = _editorStates.value[itemId] ?: return
            val rStack = redoStacks.getOrPut(itemId) { Stack() }
            rStack.push(currentState)

            val previousState = uStack.pop()
            val states = _editorStates.value.toMutableMap()
            states[itemId] = previousState
            _editorStates.value = states
        }
    }

    fun redo(itemId: Long) {
        val rStack = redoStacks[itemId]
        if (rStack?.isNotEmpty() == true) {
            val currentState = _editorStates.value[itemId] ?: return
            val uStack = undoStacks.getOrPut(itemId) { Stack() }
            uStack.push(currentState)

            val nextState = rStack.pop()
            val states = _editorStates.value.toMutableMap()
            states[itemId] = nextState
            _editorStates.value = states
        }
    }

    fun detectAndBlurFaces(itemId: Long, context: android.content.Context) {
        val state = _editorStates.value[itemId] ?: return
        val uri = state.mediaItem?.uri ?: state.documentItem?.uri ?: return
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return@launch
                val faces = faceDetectionManager.detectFaces(bitmap)

                val newBlurRegions = faces.map { rect ->
                    // Map absolute bitmap coordinates to normalized coordinates
                    val normalizedX = rect.left.toFloat() / bitmap.width
                    val normalizedY = rect.top.toFloat() / bitmap.height
                    val normalizedWidth = rect.width().toFloat() / bitmap.width
                    val normalizedHeight = rect.height().toFloat() / bitmap.height

                    BlurRegion(
                        path = androidx.compose.ui.graphics.Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(normalizedX, normalizedY, normalizedX + normalizedWidth, normalizedY + normalizedHeight))
                        },
                        isFace = true
                    )
                }
                if (newBlurRegions.isNotEmpty()) {
                    executeCommand(itemId, EditorCommand.AddBlurRegion(newBlurRegions.first())) // TODO: Support multiple
                    // Actually, we should probably add all at once.
                }
            } catch (_: Exception) {
                android.util.Log.e("MEDIA_EDITOR_DEBUG", "Face detection failed")
            }
        }
    }

    fun updateVideoTrim(itemId: Long, start: Long, end: Long) {
        val state = _editorStates.value[itemId] ?: return
        val states = _editorStates.value.toMutableMap()
        states[itemId] = state.copy(videoTrimRange = start..end)
        _editorStates.value = states
    }

    fun addTextElement(itemId: Long, text: String, color: androidx.compose.ui.graphics.Color) {
        val newElement = TextElement(
            id = UUID.randomUUID().toString(),
            text = text,
            color = color,
            fontSize = 50f,
            x = 0.5f,
            y = 0.5f
        )
        executeCommand(itemId, EditorCommand.AddTextElement(newElement))
    }
}
