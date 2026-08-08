package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.AppCustomization
import com.keeftalk.chat.domain.model.FabAction
import com.keeftalk.chat.domain.model.KeeftalkModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAppCustomizationDto(
    @SerialName("user_id") val userId: String,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("sync_enabled") val syncEnabled: Boolean = true,
    @SerialName("enabled_modules") val enabledModules: List<String>,
    @SerialName("bottom_nav_tabs") val bottomNavTabs: List<String>,
    @SerialName("enabled_fab_actions") val enabledFabActions: List<String>,
    @SerialName("fab_size_multiplier") val fabSizeMultiplier: Float = 0.8f,
    @SerialName("secondary_fab_size_multiplier") val secondaryFabSizeMultiplier: Float = 0.8f,
    @SerialName("chat_spacing_multiplier") val chatSpacingMultiplier: Float = 0.5f
) {
    fun toDomain() = AppCustomization(
        userId = userId,
        isEnabled = isEnabled,
        syncEnabled = syncEnabled,
        enabledModules = enabledModules.mapNotNull { 
            runCatching { KeeftalkModule.valueOf(it) }.getOrNull() 
        }.toSet(),
        bottomNavTabs = bottomNavTabs.mapNotNull { 
            runCatching { KeeftalkModule.valueOf(it) }.getOrNull() 
        },
        enabledFabActions = enabledFabActions.mapNotNull { 
            runCatching { FabAction.valueOf(it) }.getOrNull() 
        }.toSet(),
        fabSizeMultiplier = fabSizeMultiplier,
        secondaryFabSizeMultiplier = secondaryFabSizeMultiplier,
        chatSpacingMultiplier = chatSpacingMultiplier
    )

    companion object {
        fun fromDomain(domain: AppCustomization) = UserAppCustomizationDto(
            userId = domain.userId,
            isEnabled = domain.isEnabled,
            syncEnabled = domain.syncEnabled,
            enabledModules = domain.enabledModules.map { it.name },
            bottomNavTabs = domain.bottomNavTabs.map { it.name },
            enabledFabActions = domain.enabledFabActions.map { it.name },
            fabSizeMultiplier = domain.fabSizeMultiplier,
            secondaryFabSizeMultiplier = domain.secondaryFabSizeMultiplier,
            chatSpacingMultiplier = domain.chatSpacingMultiplier
        )
    }
}
