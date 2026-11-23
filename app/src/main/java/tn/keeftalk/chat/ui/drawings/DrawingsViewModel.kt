package tn.keeftalk.chat.ui.drawings

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.domain.model.CanvasSettings
import tn.keeftalk.chat.domain.model.Drawing
import tn.keeftalk.chat.domain.model.DrawingExportFormat
import tn.keeftalk.chat.domain.repository.DrawingRepository
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class DrawingsSortOption {
    MODIFIED_DESC,
    MODIFIED_ASC,
    TITLE_ASC,
    TITLE_DESC
}

sealed class DrawingsUiState {
    data object Loading : DrawingsUiState()
    data class Success(val drawings: List<Drawing>) : DrawingsUiState()
    data class Error(val message: String) : DrawingsUiState()
}

class DrawingsViewModel(
    private val drawingRepository: DrawingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(DrawingsSortOption.MODIFIED_DESC)
    val sortOption: StateFlow<DrawingsSortOption> = _sortOption.asStateFlow()

    val uiState: StateFlow<DrawingsUiState> = combine(
        drawingRepository.getAllDrawingsFlow(),
        _searchQuery,
        _sortOption
    ) { list, query, sort ->
        var filtered = if (query.isBlank()) {
            list
        } else {
            list.filter { it.title.contains(query, ignoreCase = true) }
        }

        filtered = when (sort) {
            DrawingsSortOption.MODIFIED_DESC -> filtered.sortedByDescending { it.updatedAt }
            DrawingsSortOption.MODIFIED_ASC -> filtered.sortedBy { it.updatedAt }
            DrawingsSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            DrawingsSortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
        }

        DrawingsUiState.Success(filtered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DrawingsUiState.Loading
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: DrawingsSortOption) {
        _sortOption.value = option
    }

    suspend fun createNewDrawing(
        title: String = "Untitled Drawing",
        settings: CanvasSettings = CanvasSettings()
    ): Drawing {
        val newDrawing = Drawing(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Untitled Drawing" },
            canvasWidth = settings.width,
            canvasHeight = settings.height,
            backgroundColorHex = if (settings.isTransparent) "#00000000" else settings.backgroundColorHex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return drawingRepository.saveDrawing(newDrawing)
    }

    fun duplicateDrawing(id: String) {
        viewModelScope.launch {
            drawingRepository.duplicateDrawing(id)
        }
    }

    fun deleteDrawing(id: String) {
        viewModelScope.launch {
            drawingRepository.deleteDrawing(id)
        }
    }

    suspend fun saveDrawing(drawing: Drawing, thumbnailBitmap: Bitmap? = null): Drawing {
        return drawingRepository.saveDrawing(drawing, thumbnailBitmap)
    }

    fun exportDrawing(
        drawing: Drawing,
        format: DrawingExportFormat,
        context: Context,
        uri: Uri,
        bitmapProvider: suspend (Drawing) -> Bitmap?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    drawingRepository.exportDrawing(drawing, format, out, bitmapProvider)
                }
            }
        }
    }

    suspend fun importImageToLocalStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val imagesDir = File(context.filesDir, "imported_images").apply { if (!exists()) mkdirs() }
            val file = File(imagesDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        }.getOrNull()
    }
}
