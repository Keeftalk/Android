package com.keeftalk.chat.ui.vault

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.keeftalk.chat.domain.model.VaultItem
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.components.*
import com.keeftalk.chat.util.BiometricAuthManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    searchQuery: String = "",
    onSettingsClick: () -> Unit = {},
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<com.keeftalk.chat.ui.components.FabActionType>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(searchQuery) {
        viewModel.onSearchQueryChanged(searchQuery)
    }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { VaultTab.entries.size }
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<VaultItem?>(null) }
    var selectedItemForInfo by remember { mutableStateOf<VaultItem?>(null) }
    var selectedItemForRename by remember { mutableStateOf<VaultItem?>(null) }
    var selectedItemForSharing by remember { mutableStateOf<VaultItem?>(null) }
    var selectedItemForViewing by remember { mutableStateOf<VaultItem?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showStorageAnalyzer by remember { mutableStateOf(false) }
    var isSouvenirBannerDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fabActionFlow?.collect { action ->
            if (action == com.keeftalk.chat.ui.components.FabActionType.UPLOAD_FILE || 
                action == com.keeftalk.chat.ui.components.FabActionType.NEW_FOLDER) {
                showAddSheet = true
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
        } else if (uiState.isVaultLocked) {
            VaultLockedView(uiState, viewModel, context)
        } else {
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

            LaunchedEffect(uiState.selectedTab) {
                pagerState.animateScrollToPage(uiState.selectedTab.ordinal)
            }

            LaunchedEffect(pagerState.currentPage) {
                viewModel.onTabSelected(VaultTab.entries[pagerState.currentPage])
            }

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    VaultBottomNavigation(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.onTabSelected(it) }
                    )
                }
            ) { innerPadding ->
                if (showAddSheet) {
                    VaultAddBottomSheet(
                        onDismiss = { showAddSheet = false },
                        onOptionClick = { option ->
                            showAddSheet = false
                            when (option) {
                                "UPLOAD" -> filePickerLauncher.launch("*/*")
                                "PHOTOS" -> photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                "FOLDER" -> showNewFolderDialog = true
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
                                VaultAction.CUT -> viewModel.cutToClipboard(listOf(item.id))
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

                if (showNewFolderDialog) {
                    VaultInputDialog(
                        title = "New Folder",
                        onDismiss = { showNewFolderDialog = false },
                        onConfirm = { name ->
                            viewModel.createFolder(name)
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
                        onDismiss = { showStorageAnalyzer = false }
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) { page ->
                        val tab = VaultTab.entries[page]
                        VaultTabPage(
                            tab = tab,
                            uiState = uiState,
                            viewModel = viewModel,
                            isSouvenirBannerDismissed = isSouvenirBannerDismissed,
                            onDismissSouvenir = { isSouvenirBannerDismissed = true },
                            onAnalyzeStorage = { showStorageAnalyzer = true },
                            onImportPhotos = { 
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            onItemClick = { item -> 
                                if (item.metadata["isFolder"] == "true") {
                                    viewModel.navigateToFolder(com.keeftalk.chat.domain.model.VaultFolder(item.id, item.title))
                                } else {
                                    selectedItemForViewing = item 
                                }
                            },
                            onItemMoreClick = { item -> selectedItemForActions = item },
                            onToggleViewMode = { viewModel.toggleViewMode() },
                            onShowSort = { showSortSheet = true }
                        )
                    }
                }
            }
        }
    }
}

suspend fun openFile(context: android.content.Context, item: VaultItem, viewModel: VaultViewModel) {
    val file = viewModel.getDecryptedFile(item) ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.file?.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open file with"))
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
    isSouvenirBannerDismissed: Boolean,
    onDismissSouvenir: () -> Unit,
    onAnalyzeStorage: () -> Unit,
    onImportPhotos: () -> Unit,
    onItemClick: (VaultItem) -> Unit,
    onItemMoreClick: (VaultItem) -> Unit,
    onToggleViewMode: () -> Unit,
    onShowSort: () -> Unit
) {
    val filteredItems = remember(uiState.items, uiState.trashItems, tab, uiState.searchQuery, uiState.selectedCategory, uiState.sortOrder, uiState.currentParentId) {
        var baseItems = when {
            uiState.searchQuery.isNotEmpty() -> {
                // Global Search across all non-deleted items
                uiState.items
            }
            tab == VaultTab.HOME -> uiState.items
            tab == VaultTab.FOLDERS -> uiState.items + uiState.folders.map { 
                VaultItem(id = it.id, userId = "", title = it.name, folderId = it.parentId, metadata = mapOf("isFolder" to "true")) 
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

        // Apply Category Filter (only if not in specific tabs that imply category)
        if (uiState.selectedCategory != null && tab != VaultTab.DOCUMENTS) {
            baseItems = baseItems.filter { it.file?.fileType == uiState.selectedCategory }
        }

        // Apply Search
        if (uiState.searchQuery.isNotEmpty()) {
            baseItems = baseItems.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
        }

        // Apply Sorting
        baseItems = when (uiState.sortOrder) {
            VaultSortOrder.NAME_ASC -> baseItems.sortedBy { it.title.lowercase() }
            VaultSortOrder.NAME_DESC -> baseItems.sortedByDescending { it.title.lowercase() }
            VaultSortOrder.DATE_ASC -> baseItems.sortedBy { it.createdAt }
            VaultSortOrder.DATE_DESC -> baseItems.sortedByDescending { it.createdAt }
            VaultSortOrder.SIZE_ASC -> baseItems.sortedBy { it.file?.fileSize ?: 0L }
            VaultSortOrder.SIZE_DESC -> baseItems.sortedByDescending { it.file?.fileSize ?: 0L }
        }

        if (tab == VaultTab.HOME && uiState.selectedCategory == null && uiState.searchQuery.isEmpty()) {
            baseItems.take(10)
        } else {
            baseItems
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (tab == VaultTab.FOLDERS && uiState.navigationStack.isNotEmpty()) {
            item {
                Surface(
                    onClick = { viewModel.navigateBack() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 0.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.navigationStack.last().name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (tab == VaultTab.HOME) {
            if (!isSouvenirBannerDismissed) {
                item {
                    CloudImportBanner(
                        onImportClick = onImportPhotos,
                        onDismiss = onDismissSouvenir
                    )
                }
            }
            item {
                VaultStorageCard(
                    info = uiState.storageInfo,
                    onAnalyzeClick = onAnalyzeStorage
                )
            }
            item {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CATEGORIES", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VaultCategoryChip(
                            "Photos", 
                            Icons.Default.Image, 
                            Color(0xFF3B82F6), 
                            uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.IMAGE
                        ) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.IMAGE) }
                        
                        VaultCategoryChip(
                            "Videos", 
                            Icons.Default.VideoLibrary, 
                            Color(0xFFEF4444), 
                            uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.VIDEO
                        ) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.VIDEO) }
                        
                        VaultCategoryChip(
                            "Docs", 
                            Icons.Default.Description, 
                            Color(0xFFF59E0B), 
                            uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.DOCUMENT
                        ) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.DOCUMENT) }
                        
                        VaultCategoryChip(
                            "Audio", 
                            Icons.Default.MusicNote, 
                            Color(0xFF10B981), 
                            uiState.selectedCategory == com.keeftalk.chat.domain.model.FileType.AUDIO
                        ) { viewModel.onCategorySelected(com.keeftalk.chat.domain.model.FileType.AUDIO) }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT FILES", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.clipboardItems.isNotEmpty()) {
                            IconButton(onClick = { viewModel.pasteItems() }) {
                                Icon(Icons.Default.ContentPaste, "Paste", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Surface(
                            onClick = onShowSort,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            onClick = onToggleViewMode,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.viewMode == VaultViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("See All", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { })
                    }
                }
            }
        }

        if (tab == VaultTab.FOLDERS) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FILES & FOLDERS", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.clipboardItems.isNotEmpty()) {
                            IconButton(onClick = { viewModel.pasteItems() }) {
                                Icon(Icons.Default.ContentPaste, "Paste", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Surface(
                            onClick = onShowSort,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            onClick = onToggleViewMode,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.viewMode == VaultViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                VaultEmptyState(tab)
            }
        } else {
            item {
                AnimatedContent(
                    targetState = uiState.viewMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                        fadeOut(animationSpec = tween(90))
                    }, label = "ViewMode"
                ) { viewMode ->
                    if (viewMode == VaultViewMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .heightIn(max = 2000.dp)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            userScrollEnabled = false
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                VaultItemGrid(item, onClick = { onItemClick(item) })
                            }
                        }
                    } else {
                        Column {
                            filteredItems.forEach { item ->
                                VaultItemList(item, onClick = { onItemClick(item) }, onMoreClick = { onItemMoreClick(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultEmptyState(tab: VaultTab) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getIconForTab(tab),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (tab == VaultTab.TRASH) "Trash is empty" else "No files found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (tab == VaultTab.TRASH) "Deleted items will appear here for 30 days." else "Files you secure in ${tab.name.lowercase()} will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

fun getIconForTab(tab: VaultTab): ImageVector {
    return when (tab) {
        VaultTab.HOME -> Icons.Default.Dashboard
        VaultTab.FOLDERS -> Icons.Default.Folder
        VaultTab.CHATS -> Icons.AutoMirrored.Filled.Chat
        VaultTab.NOTES -> Icons.AutoMirrored.Filled.StickyNote2
        VaultTab.AGENDA -> Icons.Default.Event
        VaultTab.DOCUMENTS -> Icons.Default.Description
        VaultTab.FAVORITES -> Icons.Default.Star
        VaultTab.SHARED -> Icons.Default.Share
        VaultTab.TRASH -> Icons.Default.Delete
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAddBottomSheet(
    onDismiss: () -> Unit,
    onOptionClick: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Secure Import",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            VaultAddOptionItem(
                title = "Upload File",
                subtitle = "End-to-end encrypted storage",
                icon = Icons.Default.CloudUpload,
                gradient = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))),
                onClick = { onOptionClick("UPLOAD") }
            )
            VaultAddOptionItem(
                title = "Import from Photos",
                subtitle = "Securely import your souvenirs",
                icon = Icons.Default.PhotoLibrary,
                gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF3B82F6))),
                onClick = { onOptionClick("PHOTOS") }
            )
            VaultAddOptionItem(
                title = "New Folder",
                subtitle = "Organize with zero-knowledge",
                icon = Icons.Default.CreateNewFolder,
                gradient = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))),
                onClick = { onOptionClick("FOLDER") }
            )
            VaultAddOptionItem(
                title = "Secure Note",
                subtitle = "Encrypted markdown support",
                icon = Icons.Default.StickyNote2,
                gradient = Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB7185))),
                onClick = { onOptionClick("NOTE") }
            )
        }
    }
}

@Composable
fun VaultAddOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        }
    }
}
