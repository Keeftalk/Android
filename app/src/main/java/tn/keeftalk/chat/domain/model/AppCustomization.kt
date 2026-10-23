package tn.keeftalk.chat.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Storefront
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
    SECRETS,
    WALLET,
    FINANCES,
    FEED,
    BOOKS,
    DOCS,
    MARKET,
    HABITS,
    BROWSER,
    TODO,
    MAPS,
    MY_DIN
}

@Serializable
enum class FabAction {
    NEW_CHAT, // Mandatory
    NEW_GROUP, // Mandatory
    QR, // Mandatory
    AI,
    DISPLAY
}

@Serializable
data class AppCustomization(
    val userId: String = "",
    val isEnabled: Boolean = true,
    val syncEnabled: Boolean = true,
    val isMinimalMode: Boolean = true,
    val enabledModules: Set<KeeftalkModule> = setOf(
        KeeftalkModule.CHATS,
        KeeftalkModule.CALLS,
        KeeftalkModule.SMS,
        KeeftalkModule.AGENDA,
        KeeftalkModule.NOTES,
        KeeftalkModule.EMAIL,
        KeeftalkModule.VAULT,
        KeeftalkModule.SECRETS,
        KeeftalkModule.BOOKS,
        KeeftalkModule.DOCS,
        KeeftalkModule.MARKET,
        KeeftalkModule.HABITS,
        KeeftalkModule.BROWSER,
        KeeftalkModule.TODO,
        KeeftalkModule.MAPS,
        KeeftalkModule.MY_DIN
    ),
    val bottomNavTabs: List<KeeftalkModule> = listOf(
        KeeftalkModule.CHATS,
        KeeftalkModule.CALLS,
        KeeftalkModule.SMS,
        KeeftalkModule.BOOKS,
        KeeftalkModule.DOCS,
        KeeftalkModule.MARKET,
        KeeftalkModule.VAULT
    ),
    val enabledFabActions: Set<FabAction> = setOf(
        FabAction.NEW_CHAT,
        FabAction.NEW_GROUP,
        FabAction.QR,
        FabAction.AI,
        FabAction.DISPLAY
    ),
    val fabSizeMultiplier: Float = 0.8f,
    val secondaryFabSizeMultiplier: Float = 0.8f,
    val chatSpacingMultiplier: Float = 0.5f
) {
    companion object {
        const val MAX_BOTTOM_NAV_TABS = 15

        fun default(userId: String) = AppCustomization(
            userId = userId,
            enabledModules = setOf(
                KeeftalkModule.CHATS,
                KeeftalkModule.CALLS,
                KeeftalkModule.SMS,
                KeeftalkModule.AGENDA,
                KeeftalkModule.NOTES,
                KeeftalkModule.EMAIL,
                KeeftalkModule.VAULT,
                KeeftalkModule.SECRETS,
                KeeftalkModule.BOOKS,
                KeeftalkModule.DOCS,
                KeeftalkModule.MARKET,
                KeeftalkModule.HABITS,
                KeeftalkModule.BROWSER,
                KeeftalkModule.TODO,
                KeeftalkModule.MAPS,
                KeeftalkModule.MY_DIN
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
    KeeftalkModule.SECRETS -> "Secrets"
    KeeftalkModule.WALLET -> "Wallet"
    KeeftalkModule.FINANCES -> "My Finances"
    KeeftalkModule.FEED -> "Feed"
    KeeftalkModule.BOOKS -> "My Books"
    KeeftalkModule.DOCS -> "My Docs"
    KeeftalkModule.MARKET -> "My Market"
    KeeftalkModule.HABITS -> "My Habits"
    KeeftalkModule.BROWSER -> "My Browser"
    KeeftalkModule.TODO -> "My Todo"
    KeeftalkModule.MAPS -> "My Maps"
    KeeftalkModule.MY_DIN -> "My Din"
}

fun KeeftalkModule.toIcon(icons: tn.keeftalk.chat.ui.theme.AppIcons, chatTab: Int = 0): androidx.compose.ui.graphics.vector.ImageVector = when(this) {
    KeeftalkModule.CHATS -> if (chatTab == 0) icons.messageCircle else icons.messageSquare
    KeeftalkModule.CALLS -> icons.phone
    KeeftalkModule.SMS -> icons.messageSquare
    KeeftalkModule.AGENDA -> icons.calendar
    KeeftalkModule.NOTES -> icons.notes
    KeeftalkModule.EMAIL -> icons.email
    KeeftalkModule.VAULT -> icons.folderLock
    KeeftalkModule.SECRETS -> icons.lock
    KeeftalkModule.WALLET -> icons.wallet
    KeeftalkModule.FINANCES -> icons.finances
    KeeftalkModule.FEED -> icons.feed
    KeeftalkModule.BOOKS -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.MenuBook
    KeeftalkModule.DOCS -> androidx.compose.material.icons.Icons.Default.Description
    KeeftalkModule.MARKET -> androidx.compose.material.icons.Icons.Default.Storefront
    KeeftalkModule.HABITS -> androidx.compose.material.icons.Icons.Default.CheckCircle
    KeeftalkModule.BROWSER -> androidx.compose.material.icons.Icons.Default.Language
    KeeftalkModule.TODO -> tn.keeftalk.chat.ui.theme.LucideIcons.ListTodo
    KeeftalkModule.MAPS -> tn.keeftalk.chat.ui.theme.LucideIcons.MapPinned
    KeeftalkModule.MY_DIN -> androidx.compose.material.icons.Icons.Default.Mosque
}

fun KeeftalkModule.toAppScreen(): tn.keeftalk.chat.AppScreen = when(this) {
    KeeftalkModule.CHATS -> tn.keeftalk.chat.AppScreen.ChatList
    KeeftalkModule.CALLS -> tn.keeftalk.chat.AppScreen.ChatList // Handled via bottomTab
    KeeftalkModule.SMS -> tn.keeftalk.chat.AppScreen.ChatList // Handled via bottomTab
    KeeftalkModule.AGENDA -> tn.keeftalk.chat.AppScreen.Calendar
    KeeftalkModule.NOTES -> tn.keeftalk.chat.AppScreen.Notes()
    KeeftalkModule.EMAIL -> tn.keeftalk.chat.AppScreen.Email
    KeeftalkModule.VAULT -> tn.keeftalk.chat.AppScreen.Vault
    KeeftalkModule.SECRETS -> tn.keeftalk.chat.AppScreen.Secrets
    KeeftalkModule.WALLET -> tn.keeftalk.chat.AppScreen.Wallet
    KeeftalkModule.FINANCES -> tn.keeftalk.chat.AppScreen.Finances
    KeeftalkModule.FEED -> tn.keeftalk.chat.AppScreen.Feed
    KeeftalkModule.BOOKS -> tn.keeftalk.chat.AppScreen.Books
    KeeftalkModule.DOCS -> tn.keeftalk.chat.AppScreen.Docs
    KeeftalkModule.MARKET -> tn.keeftalk.chat.AppScreen.Market
    KeeftalkModule.HABITS -> tn.keeftalk.chat.AppScreen.Habits
    KeeftalkModule.BROWSER -> tn.keeftalk.chat.AppScreen.Browser()
    KeeftalkModule.TODO -> tn.keeftalk.chat.AppScreen.Todo
    KeeftalkModule.MAPS -> tn.keeftalk.chat.AppScreen.MyMaps
    KeeftalkModule.MY_DIN -> tn.keeftalk.chat.AppScreen.MyDin
}
