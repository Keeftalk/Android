package com.keeftalk.chat.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable
    data object ChatList : Destination

    @Serializable
    data class ChatDetail(val chatId: String) : Destination

    @Serializable
    data object Vault : Destination
}
