package com.keeftalk.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keeftalk.chat.domain.model.AppCustomization
import com.keeftalk.chat.domain.model.FabAction
import com.keeftalk.chat.domain.model.KeeftalkModule
import com.keeftalk.chat.domain.repository.AppCustomizationRepository
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
        if (current.bottomNavTabs.size >= 5) return // Limit to 5 tabs
        
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

    fun toggleIsEnabled() {
        val current = customization.value
        updateCustomization(current.copy(isEnabled = !current.isEnabled))
    }

    fun toggleSyncEnabled() {
        val current = customization.value
        updateCustomization(current.copy(syncEnabled = !current.syncEnabled))
    }

    fun manualSync() {
        viewModelScope.launch {
            // This would trigger a manual fetch in Repo
            // For now, Repo automatically observes, but we could add a force fetch
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
                    FabAction.ACCESSIBILITY
                ),
                fabSizeMultiplier = 0.8f,
                secondaryFabSizeMultiplier = 0.8f,
                chatSpacingMultiplier = 0.5f
            ))
        }
    }
}
