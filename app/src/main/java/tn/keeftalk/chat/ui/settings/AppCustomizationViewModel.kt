package tn.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tn.keeftalk.chat.domain.model.AppCustomization
import tn.keeftalk.chat.domain.model.FabAction
import tn.keeftalk.chat.domain.model.KeeftalkModule
import tn.keeftalk.chat.domain.repository.AppCustomizationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppCustomizationViewModel(
    private val repository: AppCustomizationRepository
) : ViewModel() {

    val customization = repository.customization.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppCustomization()
    )

    fun toggleModule(module: KeeftalkModule) {
        if (!customization.value.isEnabled) return
        if (AppCustomization.MANDATORY_MODULES.contains(module)) return
        
        val current = customization.value
        val enabled = current.enabledModules.toMutableSet()
        if (enabled.contains(module)) {
            enabled.remove(module)
        } else {
            enabled.add(module)
        }
        
        // Remove from bottom nav if disabled
        val bottomNav = current.bottomNavTabs.toMutableList()
        if (!enabled.contains(module)) {
            bottomNav.remove(module)
        }
        
        updateCustomization(current.copy(enabledModules = enabled, bottomNavTabs = bottomNav))
    }

    fun toggleFabAction(action: FabAction) {
        if (!customization.value.isEnabled) return
        if (AppCustomization.MANDATORY_FAB_ACTIONS.contains(action)) return
        
        val current = customization.value
        val enabled = current.enabledFabActions.toMutableSet()
        if (enabled.contains(action)) {
            enabled.remove(action)
        } else {
            enabled.add(action)
        }
        updateCustomization(current.copy(enabledFabActions = enabled))
    }

    fun reorderBottomNav(from: Int, to: Int) {
        val current = customization.value
        val list = current.bottomNavTabs.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        
        val item = list.removeAt(from)
        list.add(to, item)
        updateCustomization(current.copy(bottomNavTabs = list))
    }

    fun addToBottomNav(module: KeeftalkModule) {
        val current = customization.value
        if (current.bottomNavTabs.contains(module)) return
        if (current.bottomNavTabs.size >= AppCustomization.MAX_BOTTOM_NAV_TABS) return // Limit to 15 tabs
        
        val list = current.bottomNavTabs.toMutableList()
        list.add(module)
        updateCustomization(current.copy(bottomNavTabs = list))
    }

    fun removeFromBottomNav(module: KeeftalkModule) {
        val current = customization.value
        val list = current.bottomNavTabs.toMutableList()
        list.remove(module)
        updateCustomization(current.copy(bottomNavTabs = list))
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun toggleIsEnabled() {
        val current = customization.value
        updateCustomization(current.copy(isEnabled = !current.isEnabled))
    }

    fun toggleMinimalMode() {
        val current = customization.value
        updateCustomization(current.copy(isMinimalMode = !current.isMinimalMode))
    }

    fun toggleSyncEnabled() {
        val current = customization.value
        updateCustomization(current.copy(syncEnabled = !current.syncEnabled))
    }

    fun manualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncCustomization()
            _isSyncing.value = false
        }
    }

    fun updateFabSize(multiplier: Float) {
        val current = customization.value
        updateCustomization(current.copy(fabSizeMultiplier = multiplier))
    }

    fun updateSecondaryFabSize(multiplier: Float) {
        val current = customization.value
        updateCustomization(current.copy(secondaryFabSizeMultiplier = multiplier))
    }

    fun updateChatSpacing(multiplier: Float) {
        val current = customization.value
        updateCustomization(current.copy(chatSpacingMultiplier = multiplier))
    }

    private fun updateCustomization(customization: AppCustomization) {
        viewModelScope.launch {
            repository.updateCustomization(customization)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.updateCustomization(AppCustomization(
                userId = customization.value.userId,
                isEnabled = true,
                syncEnabled = true,
                enabledModules = setOf(
                    KeeftalkModule.CHATS,
                    KeeftalkModule.CALLS,
                    KeeftalkModule.SMS,
                    KeeftalkModule.AGENDA,
                    KeeftalkModule.NOTES,
                    KeeftalkModule.EMAIL,
                    KeeftalkModule.VAULT
                ),
                bottomNavTabs = listOf(
                    KeeftalkModule.CHATS,
                    KeeftalkModule.CALLS
                ),
                enabledFabActions = setOf(
                    FabAction.NEW_CHAT,
                    FabAction.NEW_GROUP,
                    FabAction.QR,
                    FabAction.AI,
                    FabAction.DISPLAY
                ),
                fabSizeMultiplier = 0.8f,
                secondaryFabSizeMultiplier = 0.8f,
                chatSpacingMultiplier = 0.5f
            ))
        }
    }
}
