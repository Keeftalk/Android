package com.keeftalk.chat.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.renderers.UriGlideRenderer
import com.keeftalk.chat.ui.screens.editor.signal.renderers.FaceBlurRenderer
import com.keeftalk.chat.util.FaceDetectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class MediaReviewViewModel : ViewModel() {

    private val faceDetectionManager = FaceDetectionManager()

    private val _editorStates = MutableStateFlow<Map<Long, SignalEditorModel>>(emptyMap())
    val editorStates = _editorStates.asStateFlow()

    private val _reviewItems = MutableStateFlow<List<ReviewItem>>(emptyList())
    val reviewItems = _reviewItems.asStateFlow()

    private val _captions = MutableStateFlow<Map<Long, String>>(emptyMap())
    val captions = _captions.asStateFlow()

    private val _videoTrimRanges = MutableStateFlow<Map<Long, LongRange>>(emptyMap())
    val videoTrimRanges = _videoTrimRanges.asStateFlow()

    sealed class ReviewItem {
        data class Media(val item: MediaItem) : ReviewItem()
        data class Doc(val item: DocumentModel) : ReviewItem()
        
        val id: Long get() = when(this) {
            is Media -> item.id
            is Doc -> item.id
        }
        
        val uri: Uri get() = when(this) {
            is Media -> item.uri
            is Doc -> item.uri
        }
        
        val displayName: String get() = when(this) {
            is Media -> item.displayName
            is Doc -> item.name
        }
    }

    fun setMediaItems(items: List<MediaItem>) {
        setMixedItems(items, emptyList())
    }

    fun setMixedItems(media: List<MediaItem>, docs: List<DocumentModel>) {
        val items = media.map { ReviewItem.Media(it) } + docs.map { ReviewItem.Doc(it) }
        _reviewItems.value = items
        
        val currentStates = _editorStates.value.toMutableMap()
        items.forEach { item ->
            if (!currentStates.containsKey(item.id)) {
                val model = SignalEditorModel(Color.BLACK)
                // Only setup UriGlideRenderer for images or things Glide can handle
                val isImage = item is ReviewItem.Media && item.item.isImage
                if (isImage) {
                    val renderer = UriGlideRenderer(item.uri, decryptable = false, maxWidth = 2048, maxHeight = 2048)
                    val element = EditorElement(renderer)
                    element.flags.setSelectable(false).persist()
                    model.addElementWithoutPushUndo(element)
                }
                currentStates[item.id] = model
            }
        }
        _editorStates.value = currentStates
    }

    fun updateCaption(itemId: Long, caption: String) {
        val newCaptions = _captions.value.toMutableMap()
        newCaptions[itemId] = caption
        _captions.value = newCaptions
    }

    fun undo(itemId: Long) {
        _editorStates.value[itemId]?.undo()
    }

    fun redo(itemId: Long) {
        _editorStates.value[itemId]?.redo()
    }

    fun updateVideoTrim(itemId: Long, start: Long, end: Long) {
        val newRanges = _videoTrimRanges.value.toMutableMap()
        newRanges[itemId] = start..end
        _videoTrimRanges.value = newRanges
    }

    fun detectAndBlurFaces(itemId: Long, context: android.content.Context) {
        val model = _editorStates.value[itemId] ?: return
        val mainImage = model.getMainImage() ?: return
        val renderer = mainImage.renderer as? UriGlideRenderer ?: return
        val bitmap = renderer.getBitmap() ?: return

        viewModelScope.launch {
            try {
                val faces = faceDetectionManager.detectFaces(bitmap)
                if (faces.isNotEmpty()) {
                    model.pushUndoPoint()
                    faces.forEach { rect ->
                        val faceRenderer = FaceBlurRenderer()
                        val faceElement = EditorElement(faceRenderer, -1) // Z_MASK
                        
                        // Map rect to normalized coordinates in FULL_BOUNDS (-1000..1000)
                        val normalizedRect = RectF(
                            (rect.left.toFloat() / bitmap.width * 2000) - 1000,
                            (rect.top.toFloat() / bitmap.height * 2000) - 1000,
                            (rect.right.toFloat() / bitmap.width * 2000) - 1000,
                            (rect.bottom.toFloat() / bitmap.height * 2000) - 1000
                        )
                        faceElement.localMatrix.setRectToRect(com.keeftalk.chat.ui.screens.editor.signal.Bounds.FULL_BOUNDS, normalizedRect, android.graphics.Matrix.ScaleToFit.FILL)
                        
                        model.addElementWithoutPushUndo(faceElement)
                    }
                    // Trigger refresh
                    _editorStates.value = _editorStates.value.toMutableMap().apply { this[itemId] = model }
                }
            } catch (e: Exception) {
                android.util.Log.e("MEDIA_EDITOR_DEBUG", "Face detection failed", e)
            }
        }
    }
}
