package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class KeeftalkModule {
    CHATS, // Mandatory
    CALLS,
    SMS,
    AGENDA,
    NOTES,
    EMAIL,
    VAULT,
    WALLET
}

@Serializable
enum class FabAction {
    NEW_CHAT, // Mandatory
    NEW_GROUP, // Mandatory
    QR, // Mandatory
    AI,
    ACCESSIBILITY
}

@Serializable
data class AppCustomization(
    val userId: String = "",
    val isEnabled: Boolean = true,
    val syncEnabled: Boolean = true,
    val enabledModules: Set<KeeftalkModule> = setOf(
        KeeftalkModule.CHATS,
        KeeftalkModule.CALLS,
        KeeftalkModule.SMS,
        KeeftalkModule.AGENDA,
        KeeftalkModule.NOTES,
        KeeftalkModule.EMAIL,
        KeeftalkModule.VAULT
    ),
    val bottomNavTabs: List<KeeftalkModule> = listOf(
        KeeftalkModule.CHATS,
        KeeftalkModule.CALLS
    ),
    val enabledFabActions: Set<FabAction> = setOf(
        FabAction.NEW_CHAT,
        FabAction.NEW_GROUP,
        FabAction.QR,
        FabAction.AI,
        FabAction.ACCESSIBILITY
    ),
    val fabSizeMultiplier: Float = 0.8f,
    val secondaryFabSizeMultiplier: Float = 0.8f,
    val chatSpacingMultiplier: Float = 0.5f
) {
    companion object {
        fun default(userId: String) = AppCustomization(
            userId = userId,
            enabledModules = setOf(
                KeeftalkModule.CHATS,
                KeeftalkModule.CALLS,
                KeeftalkModule.SMS,
                KeeftalkModule.AGENDA,
                KeeftalkModule.NOTES,
                KeeftalkModule.EMAIL,
                KeeftalkModule.VAULT
            )
        )
        
        val MANDATORY_MODULES = setOf(KeeftalkModule.CHATS)
        val MANDATORY_FAB_ACTIONS = setOf(FabAction.NEW_CHAT, FabAction.NEW_GROUP, FabAction.QR)
    }
}

fun KeeftalkModule.toLabel(): String = when(this) {
    KeeftalkModule.CHATS -> "Chats"
    KeeftalkModule.CALLS -> "Calls"
    KeeftalkModule.SMS -> "SMS"
    KeeftalkModule.AGENDA -> "Agenda"
    KeeftalkModule.NOTES -> "Notes"
    KeeftalkModule.EMAIL -> "Email"
    KeeftalkModule.VAULT -> "Vault"
    KeeftalkModule.WALLET -> "Wallet"
}

fun KeeftalkModule.toIcon(icons: com.keeftalk.chat.ui.theme.AppIcons, chatTab: Int = 0): androidx.compose.ui.graphics.vector.ImageVector = when(this) {
    KeeftalkModule.CHATS -> if (chatTab == 0) icons.messageCircle else icons.messageSquare
    KeeftalkModule.CALLS -> icons.phone
    KeeftalkModule.SMS -> icons.messageSquare
    KeeftalkModule.AGENDA -> icons.calendar
    KeeftalkModule.NOTES -> icons.notes
    KeeftalkModule.EMAIL -> icons.email
    KeeftalkModule.VAULT -> icons.lock
    KeeftalkModule.WALLET -> icons.wallet
}

fun KeeftalkModule.toAppScreen(): com.keeftalk.chat.AppScreen = when(this) {
    KeeftalkModule.CHATS -> com.keeftalk.chat.AppScreen.ChatList
    KeeftalkModule.CALLS -> com.keeftalk.chat.AppScreen.ChatList // Handled via bottomTab
    KeeftalkModule.SMS -> com.keeftalk.chat.AppScreen.ChatList // Handled via chatTab
    KeeftalkModule.AGENDA -> com.keeftalk.chat.AppScreen.Calendar
    KeeftalkModule.NOTES -> com.keeftalk.chat.AppScreen.Notes()
    KeeftalkModule.EMAIL -> com.keeftalk.chat.AppScreen.Email
    KeeftalkModule.VAULT -> com.keeftalk.chat.AppScreen.Vault
    KeeftalkModule.WALLET -> com.keeftalk.chat.AppScreen.Wallet
}
