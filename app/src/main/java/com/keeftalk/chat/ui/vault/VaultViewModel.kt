package com.keeftalk.chat.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.VaultRepository
import com.keeftalk.chat.domain.repository.SecurityRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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
    val securitySettings: UserSecuritySettings? = null
)

enum class VaultViewMode { GRID, LIST }
enum class VaultTab { HOME, FOLDERS, CHATS, NOTES, AGENDA, DOCUMENTS, FAVORITES, SHARED, TRASH }
enum class VaultSortOrder { NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC }
enum class ClipboardAction { COPY, CUT }

class VaultViewModel(
    private val repository: VaultRepository,
    private val securityRepository: SecurityRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSecuritySettings()
        observeChatsAndContacts()
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

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true, isSyncing = true) }
        
        viewModelScope.launch {
            combine(
                repository.getItems(_uiState.value.currentParentId),
                repository.getFolders(_uiState.value.currentParentId),
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
                    isLoading = false,
                    isSyncing = false
                ) }
            }.collect()
        }

        viewModelScope.launch {
            repository.getStorageInfo().collect { info ->
                _uiState.update { it.copy(storageInfo = info) }
            }
        }
    }

    fun onTabSelected(tab: VaultTab) {
        _uiState.update { it.copy(selectedTab = tab) }
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
        _uiState.update { it.copy(
            currentParentId = folder.id,
            navigationStack = newStack
        ) }
        loadData()
    }

    fun navigateBack() {
        val stack = _uiState.value.navigationStack
        if (stack.isNotEmpty()) {
            val newStack = stack.dropLast(1)
            val newParentId = newStack.lastOrNull()?.id
            _uiState.update { it.copy(
                currentParentId = newParentId,
                navigationStack = newStack
            ) }
            loadData()
        }
    }

    fun copyToClipboard(itemIds: List<String>) {
        _uiState.update { it.copy(clipboardItems = itemIds, clipboardAction = ClipboardAction.COPY) }
    }

    fun cutToClipboard(itemIds: List<String>) {
        _uiState.update { it.copy(clipboardItems = itemIds, clipboardAction = ClipboardAction.CUT) }
    }

    fun pasteItems() {
        val targetId = _uiState.value.currentParentId
        val itemIds = _uiState.value.clipboardItems
        val action = _uiState.value.clipboardAction ?: return

        viewModelScope.launch {
            itemIds.forEach { itemId ->
                if (action == ClipboardAction.CUT) {
                    repository.moveItem(itemId, targetId)
                } else {
                    // Logic for copying would go here (e.g. create new item pointing to same file)
                    repository.moveItem(itemId, targetId) // Simple move for now
                }
            }
            _uiState.update { it.copy(clipboardItems = emptyList(), clipboardAction = null) }
            loadData()
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(viewMode = if (it.viewMode == VaultViewMode.GRID) VaultViewMode.LIST else VaultViewMode.GRID) }
    }

    fun uploadFile(file: File) {
        viewModelScope.launch {
            repository.uploadFile(file, _uiState.value.currentParentId) { progress ->
                // Update progress in UI if needed
            }
        }
    }

    fun downloadFile(item: VaultItem) {
        viewModelScope.launch {
            repository.downloadFile(item)
        }
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

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name, _uiState.value.currentParentId)
        }
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
