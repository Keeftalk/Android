package com.keeftalk.chat.ui.screens.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.round
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.keeftalk.chat.domain.model.MediaAlbum
import com.keeftalk.chat.domain.model.MediaItem
import com.keeftalk.chat.ui.screens.picker.components.MediaItemThumbnail

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MediaPickerScreen(
    viewModel: MediaPickerViewModel,
    onBack: () -> Unit,
    onComplete: (List<MediaItem>) -> Unit,
) {
    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.loadAlbums()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    if (!permissionState.allPermissionsGranted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Media permissions are required to browse the gallery.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    val pagingItems = viewModel.mediaItems.collectAsLazyPagingItems()
    val albums by viewModel.albums.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    
    var showAlbumPicker by remember { mutableStateOf(value = false) }
    val gridState = rememberLazyGridState()
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var currentDragIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { if (albums.isNotEmpty()) showAlbumPicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedAlbum?.name ?: "Recent", style = MaterialTheme.typography.titleLarge)
                        if (albums.isNotEmpty()) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        Button(
                            onClick = { onComplete(selectedItems.toList()) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("Next (${selectedItems.size})")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedItems.isNotEmpty()) {
                SelectedMediaBar(
                    items = selectedItems.toList(),
                    onRemove = { viewModel.removeItem(it) },
                    onReorder = { from, to -> viewModel.reorderItems(from, to) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val item = gridState.layoutInfo.visibleItemsInfo.find {
                                it.size.toIntRect().contains(offset.round() - it.offset)
                            }
                            if (item != null) {
                                dragStartIndex = item.index
                                currentDragIndex = item.index
                        val mediaItem = pagingItems[item.index]
                        mediaItem?.let { viewModel.toggleSelection(it) }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val item = gridState.layoutInfo.visibleItemsInfo.find {
                                it.size.toIntRect().contains(change.position.round() - it.offset)
                            }
                            if (item != null && item.index != currentDragIndex) {
                                currentDragIndex = item.index
                                val start = minOf(dragStartIndex, currentDragIndex)
                                val end = maxOf(dragStartIndex, currentDragIndex)
                                val rangeItems = mutableListOf<MediaItem>()
                                for (i in start..end) {
                                    pagingItems[i]?.let { rangeItems.add(it) }
                                }
                                viewModel.selectRange(rangeItems)
                            }
                        },
                        onDragEnd = {
                            dragStartIndex = -1
                            currentDragIndex = -1
                        },
                        onDragCancel = {
                            dragStartIndex = -1
                            currentDragIndex = -1
                        }
                    )
                }
        ) {
            if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No media found in this album.", color = Color.Gray)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(pagingItems.itemCount) { index ->
                    val item = pagingItems[index]
                    if (item != null) {
                        val isSelected = selectedItems.contains(item)
                        val selectionIndex = if (isSelected) {
                            selectedItems.indexOf(item)
                        } else {
                            -1
                        }
                        
                        MediaItemThumbnail(
                            item = item,
                            isSelected = isSelected,
                            selectionIndex = selectionIndex,
                            isSelectionModeActive = selectedItems.isNotEmpty(),
                            onThumbnailClick = { 
                                if (selectedItems.isNotEmpty()) {
                                    viewModel.toggleSelection(item)
                                } else {
                                    /* Preview? */
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(item) },
                            onToggleSelection = { viewModel.toggleSelection(item) }
                        )
                    }
                }
            }

            if (showAlbumPicker) {
                AlbumPickerDialog(
                    albums = albums,
                    onAlbumSelected = {
                        viewModel.selectAlbum(it)
                        showAlbumPicker = false
                    },
                    onDismiss = { showAlbumPicker = false }
                )
            }
        }
    }
}

@Composable
fun AlbumPickerDialog(
    albums: List<MediaAlbum>,
    onAlbumSelected: (MediaAlbum?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Album") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Recent") },
                    modifier = Modifier.clickable { onAlbumSelected(null) }
                )
                albums.forEach { album ->
                    if (album.id != "ALL") {
                        ListItem(
                            headlineContent = { Text(album.name) },
                            supportingContent = { Text("${album.itemCount} items") },
                            modifier = Modifier.clickable { onAlbumSelected(album) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
