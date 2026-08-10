package com.keeftalk.chat.ui.vault

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.keeftalk.chat.domain.model.VaultItem
import com.keeftalk.chat.domain.model.VaultFolder
import com.keeftalk.chat.ui.components.*
import com.keeftalk.chat.util.BiometricAuthManager
import com.keeftalk.chat.util.CloudImportCoordinator
import androidx.activity.result.IntentSenderRequest
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.keeftalk.chat.di.AppModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.net.Uri
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    searchQuery: String = "",
    @Suppress("UNUSED_PARAMETER") onSettingsClick: () -> Unit = {},
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<FabActionType>? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    
    BackHandler(enabled = true) {
        if (uiState.navigationStack.isNotEmpty()) {
            viewModel.navigateBack()
        } else {
            onBack()
        }
    }
    
    LaunchedEffect(searchQuery) {
        viewModel.onSearchQueryChanged(searchQuery)
    }

    // Check Photos selection when returning to app
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkPhotosSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { VaultTab.entries.size }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDropboxBrowser by remember { mutableStateOf(false) }

    val gisAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val authResult = Identity.getAuthorizationClient(context)
                    .getAuthorizationResultFromIntent(result.data)
                
                val accessToken = authResult.accessToken
                val params = authResult.tokenResponseParams
                val pickedFileIds = params?.getString("picked_file_ids")

                if (accessToken != null) {
                    if (!pickedFileIds.isNullOrEmpty()) {
                        // Drive Picker Flow
                        val fileIds = pickedFileIds.split(",")
                        viewModel.onDriveFilesSelected(fileIds, accessToken)
                    } else if (authResult.grantedScopes.contains("https://www.googleapis.com/auth/photospicker.mediaitems.readonly")) {
                        // Photos Auth Flow
                        viewModel.startGooglePhotosImport(accessToken)
                    }
                }
            } catch (e: ApiException) {
                Log.e("VaultScreen", "GIS Auth Failed", e)
                android.widget.Toast.makeText(context, "Authentication failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VaultEvent.ShowToast -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is VaultEvent.OpenPhotosPicker -> {
                    val intent = CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, Uri.parse(event.pickerUri))
                }
                is VaultEvent.OpenDropboxBrowser -> {
                    showDropboxBrowser = true
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    var selectedItemForActions by remember { mutableStateOf<VaultItem?>(null) }
    var selectedFolderForActions by remember { mutableStateOf<VaultFolder?>(null) }
    var selectedItemForInfo by remember { mutableStateOf<VaultItem?>(null) }
    var selectedItemForRename by remember { mutableStateOf<VaultItem?>(null) }
    var selectedFolderForRename by remember { mutableStateOf<VaultFolder?>(null) }
    var selectedItemForSharing by remember { mutableStateOf<VaultItem?>(null) }
    var selectedItemForViewing by remember { mutableStateOf<VaultItem?>(null) }
    var movingItem by remember { mutableStateOf<VaultItem?>(null) }
    var movingFolder by remember { mutableStateOf<VaultFolder?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showStorageAnalyzer by remember { mutableStateOf(false) }
    var showCloudImportMenu by remember { mutableStateOf(false) }
    
    // Drag and Drop state
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    
    var movePickerParentId by remember { mutableStateOf<String?>(null) }
    var movePickerStack by remember { mutableStateOf<List<VaultFolder>>(emptyList()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = com.keeftalk.chat.util.StorageUtils.getFileFromUri(context, it)
            if (file != null) {
                viewModel.uploadFile(file)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            val file = com.keeftalk.chat.util.StorageUtils.getFileFromUri(context, uri)
            if (file != null) {
                viewModel.uploadFile(file)
            }
        }
    }

    val drivePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromDrive(uris)
        }
    }

    LaunchedEffect(Unit) {
        fabActionFlow?.collect { action ->
            when (action) {
                FabActionType.UPLOAD_FILE -> filePickerLauncher.launch("*/*")
                FabActionType.NEW_FOLDER -> showNewFolderDialog = true
                FabActionType.CLOUD_IMPORT -> showCloudImportMenu = true
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (selectedItemForViewing != null) {
            VaultMediaViewer(
                item = selectedItemForViewing!!,
                viewModel = viewModel,
                onBack = { selectedItemForViewing = null }
            )
        } else if (showDropboxBrowser) {
            DropboxBrowserScreen(
                currentPath = uiState.dropboxPath,
                items = uiState.dropboxItems,
                selectedIds = uiState.dropboxSelectedIds,
                isLoading = uiState.isDropboxLoading,
                onBack = {
                    if (uiState.dropboxPath.isEmpty() || uiState.dropboxPath == "/") {
                        showDropboxBrowser = false
                    } else {
                        viewModel.navigateBackDropbox()
                    }
                },
                onPathSelected = { viewModel.loadDropboxFolder(it) },
                onToggleSelection = { viewModel.toggleDropboxSelection(it) },
                onLoadMore = { viewModel.loadDropboxFolder(uiState.dropboxPath, loadMore = true) },
                onImport = {
                    showDropboxBrowser = false
                    viewModel.importSelectedDropboxFiles()
                },
                onRefresh = { viewModel.loadDropboxFolder(uiState.dropboxPath) }
            )
        } else if (uiState.isVaultLocked) {
            VaultLockedView(uiState, viewModel, context)
        } else {
            LaunchedEffect(uiState.selectedTab) {
                pagerState.animateScrollToPage(uiState.selectedTab.ordinal)
            }

            LaunchedEffect(pagerState.currentPage) {
                viewModel.onTabSelected(VaultTab.entries[pagerState.currentPage])
            }

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.navigationBars,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    VaultBottomNavigation(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.onTabSelected(it) }
                    )
                }
            ) { innerPadding ->
                if (showCloudImportMenu) {
                    VaultCloudImportBottomSheet(
                        onDismiss = { showCloudImportMenu = false },
                        onOptionSelected = { option ->
                            showCloudImportMenu = false
                            when (option) {
                                "Dropbox" -> {
                                    val cloudAuthManager = AppModule.provideCloudAuthManager(context)
                                    cloudAuthManager.startDropboxAuthFlow(context as FragmentActivity)
                                }
                                "iCloud" -> { /* iCloud API logic */ }
                                "OneDrive" -> { /* OneDrive API logic */ }
                                "Google Drive" -> {
                                    val cloudAuthManager = AppModule.provideCloudAuthManager(context)
                                    cloudAuthManager.startGoogleDrivePickerFlow(
                                        activity = context as FragmentActivity,
                                        onResolutionRequired = { pendingIntent ->
                                            gisAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                                        },
                                        onAlreadyAuthorized = { accessToken ->
                                            // Handle already authorized case by trying to trigger picker manually
                                            // or just notify user to retry. Usually setPrompt(CONSENT) avoids this.
                                        },
                                        onError = { /* Handle error */ }
                                    )
                                }
                                "Google Photos" -> {
                                    val cloudAuthManager = AppModule.provideCloudAuthManager(context)
                                    cloudAuthManager.startGooglePhotosAuthFlow(
                                        activity = context as FragmentActivity,
                                        onResolutionRequired = { pendingIntent ->
                                            gisAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                                        },
                                        onAlreadyAuthorized = { accessToken ->
                                            viewModel.startGooglePhotosImport(accessToken)
                                        },
                                        onError = { /* Handle error */ }
                                    )
                                }
                                "Device Files" -> {
                                    drivePickerLauncher.launch(arrayOf("*/*"))
                                }
                            }
                        }
                    )
                }

                if (selectedItemForActions != null) {
                    VaultItemActionsBottomSheet(
                        item = selectedItemForActions!!,
                        onDismiss = { selectedItemForActions = null },
                        onAction = { action ->
                            val item = selectedItemForActions!!
                            selectedItemForActions = null
                            when (action) {
                                VaultAction.OPEN -> selectedItemForViewing = item
                                VaultAction.SEND_INTERNAL -> selectedItemForSharing = item
                                VaultAction.COPY -> viewModel.copyToClipboard(listOf(item.id))
                                VaultAction.CUT -> movingItem = item
                                VaultAction.DOWNLOAD -> viewModel.downloadFile(item)
                                VaultAction.TOGGLE_FAVORITE -> viewModel.toggleFavorite(item.id)
                                VaultAction.SHARE -> scope.launch { shareFile(context, item, viewModel) }
                                VaultAction.MOVE_TO_TRASH -> viewModel.deleteItem(item.id, permanent = false)
                                VaultAction.RENAME -> selectedItemForRename = item
                                VaultAction.INFO -> selectedItemForInfo = item
                                VaultAction.RESTORE -> viewModel.restoreItem(item.id)
                                VaultAction.DELETE_PERMANENT -> viewModel.deleteItem(item.id, permanent = true)
                            }
                        }
                    )
                }

                if (selectedFolderForActions != null) {
                    VaultFolderActionsBottomSheet(
                        folder = selectedFolderForActions!!,
                        onDismiss = { selectedFolderForActions = null },
                        onAction = { action ->
                            val folder = selectedFolderForActions!!
                            selectedFolderForActions = null
                            when (action) {
                                VaultAction.OPEN -> viewModel.navigateToFolder(folder)
                                VaultAction.RENAME -> selectedFolderForRename = folder
                                VaultAction.CUT -> movingFolder = folder
                                VaultAction.MOVE_TO_TRASH -> viewModel.deleteFolder(folder.id, permanent = false)
                                else -> {}
                            }
                        }
                    )
                }

                if (movingItem != null || movingFolder != null) {
                    VaultMovePicker(
                        currentFolders = uiState.folders.filter { it.parentId == movePickerParentId },
                        path = movePickerStack,
                        onFolderSelected = { targetId ->
                            if (movingItem != null) viewModel.moveItem(movingItem!!.id, targetId)
                            if (movingFolder != null) viewModel.moveFolder(movingFolder!!.id, targetId)
                            movingItem = null
                            movingFolder = null
                            movePickerStack = emptyList()
                            movePickerParentId = null
                        },
                        onNavigateInto = { folder ->
                            movePickerStack += folder
                            movePickerParentId = folder.id
                        },
                        onNavigateBack = {
                            if (movePickerStack.isNotEmpty()) {
                                movePickerStack = movePickerStack.dropLast(1)
                                movePickerParentId = movePickerStack.lastOrNull()?.id
                            }
                        },
                        onDismiss = {
                            movingItem = null
                            movingFolder = null
                            movePickerStack = emptyList()
                            movePickerParentId = null
                        }
                    )
                }

                if (selectedItemForSharing != null) {
                    ChatPickerDialog(
                        chats = uiState.recentChats,
                        contacts = uiState.contacts,
                        onDismiss = { selectedItemForSharing = null },
                        onChatsSelected = { chatIds ->
                            viewModel.sendItemToChats(selectedItemForSharing!!.id, chatIds)
                            selectedItemForSharing = null
                        }
                    )
                }

                if (selectedItemForRename != null) {
                    VaultInputDialog(
                        title = "Rename File",
                        initialValue = selectedItemForRename!!.title,
                        onDismiss = { selectedItemForRename = null },
                        onConfirm = { newName ->
                            viewModel.renameItem(selectedItemForRename!!.id, newName)
                            selectedItemForRename = null
                        }
                    )
                }

                if (selectedFolderForRename != null) {
                    VaultEditFolderDialog(
                        folder = selectedFolderForRename!!,
                        onDismiss = { selectedFolderForRename = null },
                        onConfirm = { newName, newColor ->
                            viewModel.renameFolder(selectedFolderForRename!!.id, newName)
                            viewModel.updateFolderColor(selectedFolderForRename!!.id, newColor.toArgb())
                            selectedFolderForRename = null
                        }
                    )
                }

                if (showNewFolderDialog) {
                    VaultNewFolderDialog(
                        onDismiss = { showNewFolderDialog = false },
                        onConfirm = { name, color ->
                            viewModel.createFolder(name, color.toArgb())
                            showNewFolderDialog = false
                        }
                    )
                }

                if (selectedItemForInfo != null) {
                    VaultFileInfoDialog(
                        item = selectedItemForInfo!!,
                        onDismiss = { selectedItemForInfo = null }
                    )
                }

                if (showSortSheet) {
                    VaultSortBottomSheet(
                        currentOrder = uiState.sortOrder,
                        onDismiss = { showSortSheet = false },
                        onSortSelected = { order ->
                            viewModel.onSortOrderChanged(order)
                            showSortSheet = false
                        }
                    )
                }

                if (showStorageAnalyzer) {
                    VaultStorageAnalyzer(
                        info = uiState.storageInfo,
                        tips = uiState.storageTips,
                        onReviewTip = { tip ->
                            viewModel.onReviewTip(tip)
                            showStorageAnalyzer = false
                        },
                        onDismiss = { showStorageAnalyzer = false }
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isSyncing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true
                        ) { page ->
                            val tab = VaultTab.entries[page]
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (tab == VaultTab.FOLDERS) {
                                    VaultBreadcrumbs(
                                        path = uiState.navigationStack,
                                        onBreadcrumbClick = { index -> viewModel.navigateToBreadcrumb(index) }
                                    )
                                }
                                
                                VaultTabPage(
                                    tab = tab,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onAnalyzeStorage = { showStorageAnalyzer = true },
                                    onItemClick = { item -> 
                                        if (item.metadata["isFolder"] == "true") {
                                            viewModel.navigateToFolder(VaultFolder(id = item.id, name = item.title))
                                        } else {
                                            selectedItemForViewing = item 
                                        }
                                    },
                                    onItemMoreClick = { item ->
                                        if (item.metadata["isFolder"] == "true") {
                                            selectedFolderForActions = uiState.folders.find { it.id == item.id }
                                        } else {
                                            selectedItemForActions = item
                                        }
                                    },
                                    onToggleViewMode = { viewModel.toggleViewMode() },
                                    onShowSort = { showSortSheet = true },
                                    onUploadClick = { filePickerLauncher.launch("*/*") },
                                    onCreateFolderClick = { showNewFolderDialog = true },
                                    onToggleSelection = { viewModel.toggleSelection(it) },
                                    onToggleGroupSelection = { viewModel.toggleGroupSelection(it) },
                                    onDropIntoFolder = { sourceIds, targetId ->
                                        viewModel.moveItemsToFolder(sourceIds.toList(), targetId)
                                    },
                                    draggedItemId = draggedItemId,
                                    onDraggedItemIdChange = { draggedItemId = it },
                                    dragOffset = dragOffset,
                                    onDragOffsetChange = { dragOffset = it },
                                    dropTargetId = dropTargetId,
                                    onDropTargetIdChange = { dropTargetId = it }
                                )
                            }
                        }
                    }
                    
                    // Unified Selection Toolbar
                    AnimatedVisibility(
                        visible = uiState.selectedItemIds.isNotEmpty(),
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${uiState.selectedItemIds.size} selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = { viewModel.clearSelection() }) { Icon(Icons.Default.Close, "Clear") }
                                    IconButton(onClick = { /* Share selected */ }) { Icon(Icons.Default.Share, "Share") }
                                    IconButton(onClick = { /* Move selected */ }) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, "Move") }
                                    IconButton(onClick = { /* Delete selected */ }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }

        VaultDragOverlay(draggedItemId = draggedItemId, dragOffset = dragOffset, selectedIds = uiState.selectedItemIds)
        
        // Cloud Import Progress Overlay
        val importState = uiState.importState
        if (importState is CloudImportCoordinator.ImportState.Progress) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) {}, // Block interaction
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Importing from Cloud",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { importState.percentage },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Processing ${importState.current} of ${importState.total}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultDragOverlay(
    draggedItemId: String?,
    dragOffset: Offset,
    selectedIds: Set<String>
) {
    if (draggedItemId != null) {
        val count = if (selectedIds.contains(draggedItemId)) selectedIds.size else 1
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }
                .zIndex(200f)
                .size(if (count > 1) 84.dp else 64.dp)
        ) {
            if (count > 1) {
                Box(modifier = Modifier.size(72.dp).offset(8.dp, 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
                Box(modifier = Modifier.size(72.dp).offset(4.dp, 4.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
            }
            
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (count > 1) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    if (count > 1) {
                        Text(
                            text = count.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

suspend fun shareFile(context: android.content.Context, item: VaultItem, viewModel: VaultViewModel) {
    val file = viewModel.getDecryptedFile(item) ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.file?.mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share file via"))
}

@Composable
fun VaultLockedView(uiState: VaultUiState, viewModel: VaultViewModel, context: android.content.Context) {
    val activity = context as FragmentActivity
    val biometricManager = remember { BiometricAuthManager(activity) }

    LaunchedEffect(Unit) {
        biometricManager.showBiometricPrompt(
            activity = activity,
            title = "Vault Locked",
            subtitle = "Authenticate to access your private files",
            allowBiometrics = uiState.securitySettings?.biometricUnlockEnabled ?: true,
            onSuccess = { viewModel.unlockVault() },
            onError = { Log.e("VaultLock", it) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "LockGlow")
                val glow by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    ), label = "glow"
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer { scaleX = glow; scaleY = glow; alpha = 0.2f }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                )
                
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Secured with E2EE",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your sensitive files are end-to-end encrypted and locked behind biometric security.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = {
                    biometricManager.showBiometricPrompt(
                        activity = activity,
                        allowBiometrics = uiState.securitySettings?.biometricUnlockEnabled ?: true,
                        onSuccess = { viewModel.unlockVault() },
                        onError = {}
                    )
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Fingerprint, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock Access", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun VaultTabPage(
    tab: VaultTab,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onAnalyzeStorage: () -> Unit,
    onItemClick: (VaultItem) -> Unit,
    onItemMoreClick: (VaultItem) -> Unit,
    onToggleViewMode: () -> Unit,
    onShowSort: () -> Unit,
    onUploadClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleGroupSelection: (RecentGroup) -> Unit,
    onDropIntoFolder: (Set<String>, String) -> Unit,
    draggedItemId: String?,
    onDraggedItemIdChange: (String?) -> Unit,
    dragOffset: Offset,
    onDragOffsetChange: (Offset) -> Unit,
    dropTargetId: String?,
    onDropTargetIdChange: (String?) -> Unit
) {
    val selectedIds = uiState.selectedItemIds
    val folderBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }

    val filteredItems = remember(uiState.items, uiState.trashItems, uiState.folders, tab, uiState.searchQuery, uiState.selectedCategory, uiState.sortOrder, uiState.currentParentId) {
        var baseItems = when {
            uiState.searchQuery.isNotEmpty() -> uiState.items
            tab == VaultTab.FOLDERS -> {
                val folderItems = uiState.folders.map { 
                    VaultItem(
                        id = it.id, 
                        userId = it.userId, 
                        title = it.name, 
                        folderId = it.parentId, 
                        metadata = mapOf(
                            "isFolder" to "true", 
                            "itemCount" to it.itemCount.toString(),
                            "totalSize" to it.totalSize.toString(),
                            "color" to (it.color?.toString() ?: "")
                        ), 
                        createdAt = it.createdAt
                    ) 
                }
                folderItems + uiState.items
            }
            tab == VaultTab.CHATS -> uiState.items.filter { it.file?.sourceType == com.keeftalk.chat.domain.model.SourceType.CHAT }
            tab == VaultTab.NOTES -> uiState.items.filter { it.file?.sourceType == com.keeftalk.chat.domain.model.SourceType.NOTE }
            tab == VaultTab.AGENDA -> uiState.items.filter { it.file?.sourceType == com.keeftalk.chat.domain.model.SourceType.AGENDA }
            tab == VaultTab.DOCUMENTS -> uiState.items.filter { it.file?.fileType == com.keeftalk.chat.domain.model.FileType.DOCUMENT }
            tab == VaultTab.FAVORITES -> uiState.items.filter { it.favorite }
            tab == VaultTab.SHARED -> uiState.items.filter { it.metadata["isShared"] == "true" }
            tab == VaultTab.TRASH -> uiState.trashItems
            else -> uiState.items
        }

        if (uiState.selectedCategory != null && tab != VaultTab.DOCUMENTS) {
            baseItems = baseItems.filter { it.metadata["isFolder"] == "true" || it.file?.fileType == uiState.selectedCategory }
        }

        if (uiState.searchQuery.isNotEmpty()) {
            baseItems = baseItems.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
        }

        baseItems = baseItems.sortedWith(
            compareBy<VaultItem> { it.metadata["isFolder"] != "true" }.thenBy {
                when (uiState.sortOrder) {
                    VaultSortOrder.NAME_ASC -> it.title.lowercase()
                    VaultSortOrder.NAME_DESC -> ""
                    VaultSortOrder.DATE_ASC -> it.createdAt.toString()
                    VaultSortOrder.DATE_DESC -> ""
                    VaultSortOrder.SIZE_ASC -> (it.file?.fileSize ?: 0L).toString()
                    VaultSortOrder.SIZE_DESC -> ""
                }
            }
        )
        
        if (uiState.sortOrder == VaultSortOrder.NAME_DESC || uiState.sortOrder == VaultSortOrder.DATE_DESC || uiState.sortOrder == VaultSortOrder.SIZE_DESC) {
             val folders = baseItems.filter { it.metadata["isFolder"] == "true" }.reversed()
             val files = baseItems.filter { it.metadata["isFolder"] != "true" }.reversed()
             baseItems = folders + files
        }
        baseItems
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (tab == VaultTab.HOME) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    VaultStorageCard(info = uiState.storageInfo, onAnalyzeClick = onAnalyzeStorage)
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text("CATEGORIES", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VaultCategoryChip("Photos", Icons.Default.Image, Color(0xFF3B82F6), uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.IMAGE) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.IMAGE) }
                            VaultCategoryChip("Videos", Icons.Default.VideoLibrary, Color(0xFFEF4444), uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.VIDEO) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.VIDEO) }
                            VaultCategoryChip("Docs", Icons.Default.Description, Color(0xFFF59E0B), uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.DOCUMENT) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.DOCUMENT) }
                            VaultCategoryChip("Audio", Icons.Default.MusicNote, Color(0xFF10B981), uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.AUDIO) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.AUDIO) }
                            VaultCategoryChip("Others", Icons.Default.MoreHoriz, Color(0xFF94A3B8), uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.OTHER) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.OTHER) }
                        }
                    }
                }
                uiState.recentGroups.forEach { group ->
                    item(span = StaggeredGridItemSpan.FullLine) {
                        RecentSectionHeader(title = group.dateLabel, isSelected = group.items.all { selectedIds.contains(it.id) }, onToggleSelection = { onToggleGroupSelection(group) })
                    }
                    staggeredItems(group.items, key = { it.id }) { item ->
                        VaultItemGrid(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            showMetadata = false,
                            onClick = { if (selectedIds.isNotEmpty()) onToggleSelection(item.id) else onItemClick(item) },
                            onLongClick = { onToggleSelection(item.id) },
                            modifier = Modifier.vaultDragSource(
                                itemId = item.id,
                                onDragStart = { id: String, rootPos: Offset -> 
                                    onDraggedItemIdChange(id)
                                    onDragOffsetChange(rootPos)
                                },
                                onDrag = { delta: Offset -> 
                                    onDragOffsetChange(dragOffset + delta)
                                    onDropTargetIdChange(folderBounds.entries.find { it.value.contains(dragOffset) }?.key)
                                },
                                onDragEnd = { 
                                    val targetId = dropTargetId
                                    val draggedId = draggedItemId
                                    if (targetId != null && draggedId != null) {
                                        val sourceIds = if (selectedIds.contains(draggedId)) selectedIds else setOf(draggedId)
                                        if (!sourceIds.contains(targetId)) {
                                            onDropIntoFolder(sourceIds, targetId)
                                        }
                                    }
                                    onDraggedItemIdChange(null)
                                    onDragOffsetChange(Offset.Zero)
                                    onDropTargetIdChange(null)
                                }
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (tab == VaultTab.FOLDERS) "FILES & FOLDERS" else tab.name, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.clipboardItems.isNotEmpty()) {
                                IconButton(onClick = { viewModel.pasteItems() }) { Icon(Icons.Default.ContentPaste, "Paste", tint = MaterialTheme.colorScheme.primary) }
                            }
                            Surface(onClick = onShowSort, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(onClick = onToggleViewMode, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(imageVector = if (uiState.viewMode == VaultViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView, contentDescription = "Toggle View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
                if (filteredItems.isEmpty()) {
                    item { 
                        if (uiState.currentParentId != null) {
                            EmptyFolderState(
                                onUploadClick = onUploadClick, 
                                onCreateFolderClick = onCreateFolderClick
                            )
                        } else {
                            VaultEmptyState(tab, uiState.selectedCategory)
                        }
                    }
                } else {
                    item {
                        AnimatedContent(targetState = uiState.viewMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "ViewMode") { viewMode ->
                            if (viewMode == VaultViewMode.GRID) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    modifier = Modifier.heightIn(max = 2000.dp).padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    userScrollEnabled = false
                                ) {
                                    gridItems(filteredItems, key = { it.id }) { item ->
                                        val isFolder = item.metadata["isFolder"] == "true"
                                        VaultItemSimpleGrid(
                                            item = item,
                                            isSelected = selectedIds.contains(item.id),
                                            onClick = { if (selectedIds.isNotEmpty()) onToggleSelection(item.id) else onItemClick(item) },
                                            onLongClick = { onToggleSelection(item.id) },
                                            modifier = Modifier
                                                .vaultDragSource(
                                                    itemId = item.id,
                                                    onDragStart = { id: String, rootPos: Offset -> 
                                                        onDraggedItemIdChange(id)
                                                        onDragOffsetChange(rootPos)
                                                    },
                                                    onDrag = { delta: Offset -> 
                                                        onDragOffsetChange(dragOffset + delta)
                                                        onDropTargetIdChange(folderBounds.entries.find { it.value.contains(dragOffset) }?.key)
                                                    },
                                                    onDragEnd = {
                                                    val targetId = dropTargetId
                                                    val draggedId = draggedItemId
                                                    if (targetId != null && draggedId != null) {
                                                        val sourceIds = if (selectedIds.contains(draggedId)) selectedIds else setOf(draggedId)
                                                        if (!sourceIds.contains(targetId)) onDropIntoFolder(sourceIds, targetId)
                                                    }
                                                    onDraggedItemIdChange(null)
                                                    onDragOffsetChange(Offset.Zero)
                                                    onDropTargetIdChange(null)
                                                }
                                                )
                                                .onGloballyPositioned { coords -> if (isFolder) folderBounds[item.id] = coords.boundsInRoot() }
                                        )
                                    }
                                }
                            } else {
                                Column {
                                    filteredItems.forEach { item ->
                                        val isFolder = item.metadata["isFolder"] == "true"
                                        VaultItemList(
                                            item = item,
                                            isSelected = selectedIds.contains(item.id),
                                            onClick = { if (selectedIds.isNotEmpty()) onToggleSelection(item.id) else onItemClick(item) },
                                            onLongClick = { onToggleSelection(item.id) },
                                            onMoreClick = { onItemMoreClick(item) },
                                            modifier = Modifier
                                                .vaultDragSource(
                                                    itemId = item.id,
                                                    onDragStart = { id: String, rootPos: Offset -> 
                                                        onDraggedItemIdChange(id)
                                                        onDragOffsetChange(rootPos)
                                                    },
                                                    onDrag = { delta: Offset -> 
                                                        onDragOffsetChange(dragOffset + delta)
                                                        onDropTargetIdChange(folderBounds.entries.find { it.value.contains(dragOffset) }?.key)
                                                    },
                                                    onDragEnd = {
                                                    val targetId = dropTargetId
                                                    val draggedId = draggedItemId
                                                    if (targetId != null && draggedId != null) {
                                                        val sourceIds = if (selectedIds.contains(draggedId)) selectedIds else setOf(draggedId)
                                                        if (!sourceIds.contains(targetId)) onDropIntoFolder(sourceIds, targetId)
                                                    }
                                                    onDraggedItemIdChange(null)
                                                    onDragOffsetChange(Offset.Zero)
                                                    onDropTargetIdChange(null)
                                                }
                                                )
                                                .onGloballyPositioned { coords -> if (isFolder) folderBounds[item.id] = coords.boundsInRoot() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VaultDragOverlay(draggedItemId = draggedItemId, dragOffset = dragOffset, selectedIds = selectedIds)
    }
}

@Composable
fun Modifier.vaultDragSource(
    itemId: String,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier {
    var globalPosition by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { globalPosition = it.positionInRoot() }
        .pointerInput(itemId) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> onDragStart(itemId, globalPosition + offset) },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            )
        }
}
