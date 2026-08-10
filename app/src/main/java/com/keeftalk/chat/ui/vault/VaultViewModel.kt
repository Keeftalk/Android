package com.keeftalk.chat.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.VaultRepository
import com.keeftalk.chat.domain.repository.SecurityRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.util.CloudImportCoordinator
import com.keeftalk.chat.data.cloud.GooglePhotosService
import com.keeftalk.chat.data.cloud.GoogleDriveService
import com.keeftalk.chat.data.cloud.DropboxService
import com.keeftalk.chat.data.cloud.DropboxItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.minutes

data class VaultUiState(
    val items: List<VaultItem> = emptyList(),
    val trashItems: List<VaultItem> = emptyList(),
    val folders: List<VaultFolder> = emptyList(),
    val tags: List<VaultTag> = emptyList(),
    val favoriteItems: List<VaultItem> = emptyList(),
    val recentChats: List<Chat> = emptyList(),
    val contacts: List<User> = emptyList(),
    val storageInfo: VaultStorageInfo? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val privacyScore: Int = 85,
    val currentParentId: String? = null,
    val navigationStack: List<VaultFolder> = emptyList(),
    val clipboardItems: List<String> = emptyList(),
    val clipboardAction: ClipboardAction? = null,
    val searchQuery: String = "",
    val selectedCategory: FileType? = null,
    val sortOrder: VaultSortOrder = VaultSortOrder.DATE_DESC,
    val viewMode: VaultViewMode = VaultViewMode.GRID,
    val selectedTab: VaultTab = VaultTab.HOME,
    val isVaultLocked: Boolean = true,
    val securitySettings: UserSecuritySettings? = null,
    val recentGroups: List<RecentGroup> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val storageTips: List<VaultStorageTip> = emptyList(),
    val importState: CloudImportCoordinator.ImportState = CloudImportCoordinator.ImportState.Idle,
    val dropboxItems: List<DropboxItem> = emptyList(),
    val dropboxPath: String = "",
    val dropboxSelectedIds: Set<String> = emptySet(),
    val isDropboxLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class VaultViewMode { GRID, LIST }
enum class VaultTab { HOME, FOLDERS, CHATS, NOTES, AGENDA, DOCUMENTS, FAVORITES, SHARED, TRASH }
enum class VaultSortOrder { NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC }
enum class ClipboardAction { COPY, CUT }

sealed class VaultEvent {
    data class ShowToast(val message: String) : VaultEvent()
    data class OpenPhotosPicker(val sessionId: String, val pickerUri: String) : VaultEvent()
    object OpenDropboxBrowser : VaultEvent()
}

data class RecentGroup(
    val dateLabel: String,
    val items: List<VaultItem>
)

class VaultViewModel(
    private val repository: VaultRepository,
    private val securityRepository: SecurityRepository,
    private val chatRepository: ChatRepository,
    private val cloudImportCoordinator: CloudImportCoordinator,
    private val googlePhotosService: GooglePhotosService,
    private val googleDriveService: GoogleDriveService,
    private val dropboxService: DropboxService
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VaultEvent>()
    val events: SharedFlow<VaultEvent> = _events.asSharedFlow()

    private val _currentParentId = MutableStateFlow<String?>(null)
    private var photosAccessToken: String? = null
    private var photosSessionId: String? = null
    private var dropboxAccessToken: String? = null
    private var dropboxCursor: String? = null

    init {
        observeData()
        observeSecuritySettings()
        observeChatsAndContacts()
        observeImportState()
        observeCloudImportResults()
        startPeriodicSync()
    }

    private fun observeImportState() {
        viewModelScope.launch {
            cloudImportCoordinator.importState.collect { state ->
                _uiState.update { it.copy(importState = state) }
            }
        }
    }

    private fun observeCloudImportResults() {
        viewModelScope.launch {
            com.keeftalk.chat.di.AppModule.cloudImportResultFlow.collect { results ->
                if (results.contains("DROPBOX_AUTH_SUCCESS")) {
                    val credential = com.dropbox.core.android.Auth.getDbxCredential()
                    if (credential != null) {
                        startDropboxImport(credential.accessToken)
                        _events.emit(VaultEvent.OpenDropboxBrowser)
                    }
                }
            }
        }
    }

    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                delay(5.minutes)
                if (!uiState.value.isVaultLocked) {
                    repository.sync()
                }
            }
        }
    }

    private fun observeChatsAndContacts() {
        viewModelScope.launch {
            chatRepository.getChats().collect { chats ->
                _uiState.update { it.copy(recentChats = chats) }
            }
        }
        viewModelScope.launch {
            chatRepository.getContacts().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
    }

    private fun observeSecuritySettings() {
        viewModelScope.launch {
            securityRepository.securitySettings.collect { settings ->
                val score = calculatePrivacyScore(settings)
                _uiState.update { it.copy(
                    securitySettings = settings,
                    isVaultLocked = settings?.appLockEnabled ?: false,
                    privacyScore = score
                ) }
            }
        }
    }

    private fun calculatePrivacyScore(settings: UserSecuritySettings?): Int {
        if (settings == null) return 50
        var score = 60
        if (settings.appLockEnabled) score += 15
        if (settings.biometricUnlockEnabled) score += 15
        if (settings.twoFactorEnabled) score += 10
        return score.coerceAtMost(100)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeData() {
        _uiState.update { it.copy(isLoading = true, isSyncing = true) }
        
        viewModelScope.launch {
            repository.sync()

            // Main Explorer Flow (Current Folder context)
            _currentParentId.flatMapLatest { parentId ->
                combine(
                    repository.getItems(parentId),
                    repository.getFolders(parentId),
                    repository.getTags(),
                    repository.getFavoriteItems(),
                    repository.getTrashItems()
                ) { items, folders, tags, favorites, trash ->
                    _uiState.update { it.copy(
                        items = items,
                        folders = folders,
                        tags = tags,
                        favoriteItems = favorites,
                        trashItems = trash,
                        currentParentId = parentId,
                        isLoading = false,
                        isSyncing = false
                    ) }
                }
            }.collect()
        }

        // Home discovery flow: Global, reactively filtered and sorted
        viewModelScope.launch {
            combine(
                repository.getAllItems(),
                _uiState.map { it.selectedCategory }.distinctUntilChanged(),
                _uiState.map { it.sortOrder }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged()
            ) { allItems, category, order, query ->
                var filtered = allItems
                
                // 1. Filter by category
                if (category != null) {
                    filtered = filtered.filter { it.file?.fileType == category }
                }
                
                // 2. Filter by search query
                if (query.isNotEmpty()) {
                    filtered = filtered.filter { it.title.contains(query, ignoreCase = true) }
                }
                
                // 3. Sort items
                filtered = when (order) {
                    VaultSortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
                    VaultSortOrder.NAME_DESC -> filtered.sortedByDescending { it.title.lowercase() }
                    VaultSortOrder.DATE_ASC -> filtered.sortedBy { it.createdAt }
                    VaultSortOrder.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
                    VaultSortOrder.SIZE_ASC -> filtered.sortedBy { it.file?.fileSize ?: 0L }
                    VaultSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.file?.fileSize ?: 0L }
                }
                
                // 4. Group by date
                filtered.groupBy { formatGroupDate(it.createdAt) }
                    .map { (date, groupItems) -> RecentGroup(date, groupItems) }
            }.collect { groups ->
                _uiState.update { it.copy(recentGroups = groups) }
            }
        }

        viewModelScope.launch {
            combine(
                repository.getStorageInfo(),
                repository.getAllItems(),
                _uiState.map { it.trashItems }.distinctUntilChanged()
            ) { info, allItems, trashItems ->
                _uiState.update { it.copy(
                    storageInfo = info,
                    storageTips = generateStorageTips(info, allItems, trashItems)
                ) }
            }.collect()
        }
    }

    private fun generateStorageTips(
        info: VaultStorageInfo,
        allItems: List<VaultItem>,
        trashItems: List<VaultItem>
    ): List<VaultStorageTip> {
        val tips = mutableListOf<VaultStorageTip>()

        // 1. Trash Cleanup
        if (info.trashBytesUsed > 100 * 1024 * 1024) { // > 100MB
            tips.add(VaultStorageTip(
                id = "trash_cleanup",
                title = "Clear Trash",
                description = "You can free up ${formatSize(info.trashBytesUsed)} by permanently deleting items in your trash.",
                actionLabel = "Review",
                action = StorageTipAction.NAVIGATE_TRASH,
                severity = TipSeverity.WARNING
            ))
        }

        // 2. Large Files
        val largeFiles = allItems.filter { (it.file?.fileSize ?: 0L) > 50 * 1024 * 1024 }
        if (largeFiles.isNotEmpty()) {
            tips.add(VaultStorageTip(
                id = "large_files",
                title = "Manage Large Files",
                description = "You have ${largeFiles.size} files larger than 50MB. Review them to save cloud space.",
                actionLabel = "Review",
                action = StorageTipAction.FILTER_LARGE_FILES,
                severity = TipSeverity.INFO
            ))
        }

        // 3. Cache Bloat
        if (info.localCacheBytesUsed > 500 * 1024 * 1024) { // > 500MB
            tips.add(VaultStorageTip(
                id = "cache_bloat",
                title = "Optimize Local Storage",
                description = "Keeftalk is using ${formatSize(info.localCacheBytesUsed)} for local cache. You can clear this to free up device space.",
                actionLabel = "Clear Cache",
                action = StorageTipAction.CLEAR_CACHE,
                severity = TipSeverity.INFO
            ))
        }

        // 4. Storage Pressure
        val usageRatio = info.cloudBytesUsed.toFloat() / info.cloudBytesLimit.toFloat()
        if (usageRatio > 0.9f) {
            tips.add(VaultStorageTip(
                id = "storage_full",
                title = "Storage Almost Full",
                description = "You've used ${"%.1f".format(usageRatio * 100)}% of your 5GB cloud storage. Consider upgrading your plan.",
                actionLabel = "Upgrade",
                action = StorageTipAction.UPGRADE_PLAN,
                severity = TipSeverity.CRITICAL
            ))
        }

        return tips
    }

    private fun formatSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            else -> "%.0f KB".format(kb)
        }
    }

    fun onReviewTip(tip: VaultStorageTip) {
        when (tip.action) {
            StorageTipAction.NAVIGATE_TRASH -> onTabSelected(VaultTab.TRASH)
            StorageTipAction.FILTER_LARGE_FILES -> {
                onTabSelected(VaultTab.HOME)
                // We could implement a specific filter mode here if needed
                // For now, let's just search for large files? or just go Home
            }
            StorageTipAction.CLEAR_CACHE -> {
                viewModelScope.launch {
                    repository.clearLocalCache()
                    _events.emit(VaultEvent.ShowToast("Local cache cleared"))
                }
            }
            StorageTipAction.UPGRADE_PLAN -> { /* Open subscription screen */ }
        }
    }

    fun onTabSelected(tab: VaultTab) {
        _uiState.update { it.copy(
            selectedTab = tab,
            viewMode = if (tab == VaultTab.FOLDERS) VaultViewMode.LIST else it.viewMode
        ) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            repository.sync()
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: FileType?) {
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }
    }

    fun onSortOrderChanged(order: VaultSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun navigateToFolder(folder: VaultFolder) {
        val newStack = _uiState.value.navigationStack + folder
        _uiState.update { it.copy(navigationStack = newStack) }
        _currentParentId.value = folder.id
    }

    fun navigateToBreadcrumb(index: Int) {
        if (index < 0) { // Root
            _uiState.update { it.copy(navigationStack = emptyList()) }
            _currentParentId.value = null
        } else {
            val newStack = _uiState.value.navigationStack.take(index + 1)
            val folder = newStack.last()
            _uiState.update { it.copy(navigationStack = newStack) }
            _currentParentId.value = folder.id
        }
    }

    fun navigateBack() {
        val stack = _uiState.value.navigationStack
        if (stack.isNotEmpty()) {
            val newStack = stack.dropLast(1)
            val newParentId = newStack.lastOrNull()?.id
            _uiState.update { it.copy(navigationStack = newStack) }
            _currentParentId.value = newParentId
        }
    }

    fun copyToClipboard(itemIds: List<String>) {
        _uiState.update { it.copy(clipboardItems = itemIds, clipboardAction = ClipboardAction.COPY) }
    }

    fun cutToClipboard(itemIds: List<String>) {
        _uiState.update { it.copy(clipboardItems = itemIds, clipboardAction = ClipboardAction.CUT) }
    }

    fun pasteItems() {
        val targetId = _currentParentId.value
        val itemIds = _uiState.value.clipboardItems
        val action = _uiState.value.clipboardAction ?: return

        viewModelScope.launch {
            itemIds.forEach { itemId ->
                if (action == ClipboardAction.CUT) {
                    repository.moveItem(itemId, targetId)
                } else {
                    // Logic for copying would go here
                    repository.moveItem(itemId, targetId) // Simple move for now
                }
            }
            _uiState.update { it.copy(clipboardItems = emptyList(), clipboardAction = null) }
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(viewMode = if (it.viewMode == VaultViewMode.GRID) VaultViewMode.LIST else VaultViewMode.GRID) }
    }

    fun uploadFile(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            repository.uploadFile(file, _currentParentId.value) { progress ->
                // Update progress in UI if needed
            }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    fun importFromDrive(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            cloudImportCoordinator.importFromUris(uris, _currentParentId.value)
        }
    }

    fun onDriveFilesSelected(fileIds: List<String>, accessToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val parentId = _currentParentId.value
            
            // 1. Fetch metadata for all files
            val metadataList = fileIds.mapNotNull { id ->
                googleDriveService.getFileMetadata(id, accessToken).getOrNull()
            }
            
            if (metadataList.isNotEmpty()) {
                // 2. Coordinate batch download -> encrypt -> upload
                cloudImportCoordinator.importFromDrive(metadataList, googleDriveService, accessToken, parentId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(VaultEvent.ShowToast("No files selected or access denied"))
            }
        }
    }

    fun startGooglePhotosImport(accessToken: String) {
        this.photosAccessToken = accessToken
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            googlePhotosService.createSession(accessToken)
                .onSuccess { response ->
                    this@VaultViewModel.photosSessionId = response.id
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(VaultEvent.OpenPhotosPicker(response.id, response.pickerUri))
                }
                .onFailure { error ->
                    val message = if (error.message?.contains("403") == true) {
                        "Google Photos Picker API is not enabled for this project. Please check Google Cloud Console."
                    } else {
                        "Failed to start Photos import: ${error.message}"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    fun checkPhotosSession() {
        val sessionId = photosSessionId
        val accessToken = photosAccessToken
        if (sessionId == null || accessToken == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            googlePhotosService.getSession(sessionId, accessToken)
                .onSuccess { response ->
                    if (response.mediaItemsSet) {
                        importPhotosFromSession(sessionId, accessToken)
                        photosSessionId = null // Done with this session
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to check session: ${error.message}") }
                }
        }
    }

    fun startDropboxImport(accessToken: String) {
        this.dropboxAccessToken = accessToken
        _uiState.update { it.copy(dropboxPath = "", dropboxSelectedIds = emptySet()) }
        loadDropboxFolder("")
    }

    fun loadDropboxFolder(path: String, loadMore: Boolean = false) {
        val token = dropboxAccessToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDropboxLoading = true, dropboxPath = if (loadMore) it.dropboxPath else path) }
            val cursor = if (loadMore) dropboxCursor else null
            dropboxService.listFolder(token, path, cursor)
                .onSuccess { (newItems, nextCursor) ->
                    _uiState.update { state ->
                        val updatedItems = if (loadMore) state.dropboxItems + newItems else newItems
                        state.copy(isDropboxLoading = false, dropboxItems = updatedItems)
                    }
                    dropboxCursor = nextCursor
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isDropboxLoading = false, errorMessage = "Failed to load Dropbox folder: ${error.message}") }
                }
        }
    }

    fun toggleDropboxSelection(item: DropboxItem) {
        _uiState.update { state ->
            val current = state.dropboxSelectedIds
            val next = if (current.contains(item.id)) current - item.id else current + item.id
            state.copy(dropboxSelectedIds = next)
        }
    }

    fun importSelectedDropboxFiles() {
        val token = dropboxAccessToken ?: return
        val selectedIds = uiState.value.dropboxSelectedIds
        val selectedItems = uiState.value.dropboxItems.filter { selectedIds.contains(it.id) }
        
        if (selectedItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(dropboxSelectedIds = emptySet()) } // Clear for next time
            cloudImportCoordinator.importFromDropbox(selectedItems, dropboxService, token, _currentParentId.value)
        }
    }

    fun navigateBackDropbox() {
        val currentPath = uiState.value.dropboxPath
        if (currentPath.isEmpty() || currentPath == "/") {
            // Already at root or invalid state
            return
        }
        val parts = currentPath.split("/").filter { it.isNotEmpty() }
        val parentPath = if (parts.size <= 1) "" else "/" + parts.dropLast(1).joinToString("/")
        loadDropboxFolder(parentPath)
    }

    private fun importPhotosFromSession(sessionId: String, accessToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            googlePhotosService.listMediaItems(sessionId, accessToken)
                .onSuccess { items ->
                    if (items.isNotEmpty()) {
                        // Pass items to coordinator for batch download/encrypt/upload
                        // For now, I'll implement a download method in coordinator or handle it here
                        importPhotosList(items)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.emit(VaultEvent.ShowToast("No photos selected"))
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to list items: ${error.message}") }
                }
        }
    }

    private fun importPhotosList(items: List<com.keeftalk.chat.data.cloud.MediaItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = false) }
            cloudImportCoordinator.importFromPhotos(items, googlePhotosService, _currentParentId.value)
        }
    }

    fun downloadFile(item: VaultItem) {
        viewModelScope.launch {
            repository.downloadFile(item)
                .onSuccess { 
                    _events.emit(VaultEvent.ShowToast("File saved to Downloads"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Download failed: ${error.message}") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(itemId)
        }
    }

    fun renameItem(itemId: String, newName: String) {
        viewModelScope.launch {
            repository.renameItem(itemId, newName)
        }
    }

    fun deleteItem(itemId: String, permanent: Boolean = false) {
        viewModelScope.launch {
            repository.deleteItem(itemId, permanent)
        }
    }

    fun restoreItem(itemId: String) {
        viewModelScope.launch {
            repository.restoreItem(itemId)
        }
    }

    fun createFolder(name: String, color: Int? = null) {
        viewModelScope.launch {
            repository.createFolder(name, _currentParentId.value, color)
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            repository.renameFolder(folderId, newName)
        }
    }

    fun updateFolderColor(folderId: String, color: Int) {
        viewModelScope.launch {
            val folder = repository.getFolder(folderId) ?: return@launch
            repository.updateFolder(folder.copy(color = color))
        }
    }

    fun moveItem(itemId: String, targetFolderId: String?) {
        viewModelScope.launch {
            repository.moveItem(itemId, targetFolderId)
        }
    }

    fun moveItemsToFolder(itemIds: List<String>, targetFolderId: String?) {
        viewModelScope.launch {
            itemIds.forEach { itemId ->
                repository.moveItem(itemId, targetFolderId)
            }
            clearSelection()
        }
    }

    fun moveFolder(folderId: String, targetFolderId: String?) {
        viewModelScope.launch {
            repository.moveFolder(folderId, targetFolderId)
        }
    }

    fun deleteFolder(folderId: String, permanent: Boolean = false) {
        viewModelScope.launch {
            repository.deleteFolder(folderId, permanent)
        }
    }

    private fun formatGroupDate(timestamp: Long): String {
        val now = java.util.Calendar.getInstance()
        val date = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return when {
            isSameDay(now, date) -> "Today"
            isYesterday(now, date) -> "Yesterday"
            else -> java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(timestamp)
        }
    }

    private fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: java.util.Calendar, date: java.util.Calendar): Boolean {
        val yesterday = java.util.Calendar.getInstance().apply { 
            timeInMillis = now.timeInMillis
            add(java.util.Calendar.DAY_OF_YEAR, -1) 
        }
        return isSameDay(yesterday, date)
    }

    fun toggleSelection(itemId: String) {
        val current = _uiState.value.selectedItemIds
        _uiState.update { it.copy(
            selectedItemIds = if (current.contains(itemId)) current - itemId else current + itemId
        ) }
    }

    fun toggleGroupSelection(group: RecentGroup) {
        val current = _uiState.value.selectedItemIds
        val groupIds = group.items.map { it.id }.toSet()
        val allSelected = groupIds.all { current.contains(it) }
        
        _uiState.update { it.copy(
            selectedItemIds = if (allSelected) current - groupIds else current + groupIds
        ) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItemIds = emptySet()) }
    }

    fun unlockVault() {
        _uiState.update { it.copy(isVaultLocked = false) }
    }

    fun sendItemToChats(itemId: String, chatIds: List<String>) {
        viewModelScope.launch {
            chatIds.forEach { chatId ->
                chatRepository.shareVaultFileToChat(itemId, chatId)
            }
        }
    }

    suspend fun getDecryptedFile(item: VaultItem): File? {
        return repository.getDecryptedFile(item).getOrNull()
    }
}
