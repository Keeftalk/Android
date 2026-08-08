package com.keeftalk.chat.ui.screens.picker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.keeftalk.chat.domain.model.MediaAlbum
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.domain.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediaPickerViewModel(
    application: Application,
    private val repository: MediaRepository,
) : AndroidViewModel(application) {

    private val _selectedAlbum = MutableStateFlow<MediaAlbum?>(null)
    val selectedAlbum = _selectedAlbum.asStateFlow()

    private val _albums = MutableStateFlow<List<MediaAlbum>>(emptyList())
    val albums = _albums.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItems: Flow<PagingData<MediaItem>> = _selectedAlbum.flatMapLatest { album ->
        repository.getMediaPagingData(album?.id)
    }.cachedIn(viewModelScope)

    private val _selectedItems = MutableStateFlow<LinkedHashSet<MediaItem>>(linkedSetOf())
    val selectedItems = _selectedItems.asStateFlow()

    private val _maxSelectionCount = MutableStateFlow(30)
    val maxSelectionCount = _maxSelectionCount.asStateFlow()

    fun loadAlbums() {
        viewModelScope.launch {
            repository.getAlbums().collect {
                _albums.value = it
            }
        }
    }

    fun selectAlbum(album: MediaAlbum?) {
        _selectedAlbum.value = album
    }

    fun toggleSelection(item: MediaItem) {
        val current = _selectedItems.value
        val newSet = LinkedHashSet(current)
        if (newSet.contains(item)) {
            newSet.remove(item)
        } else {
            if (newSet.size < _maxSelectionCount.value) {
                newSet.add(item)
            }
        }
        _selectedItems.value = newSet
    }

    fun removeItem(item: MediaItem) {
        val current = _selectedItems.value
        val newSet = LinkedHashSet(current)
        newSet.remove(item)
        _selectedItems.value = newSet
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        val currentList = _selectedItems.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _selectedItems.value = LinkedHashSet(currentList)
        }
    }

    fun selectRange(items: List<MediaItem>) {
        val current = _selectedItems.value
        val newSet = LinkedHashSet(current)
        items.forEach { item ->
            if (newSet.size < _maxSelectionCount.value) {
                newSet.add(item)
            }
        }
        _selectedItems.value = newSet
    }
}
