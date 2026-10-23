package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
data class IconOverrideItem(
    val semanticKey: String, // e.g. "chat", "search", "lock", "phone"
    val relativePath: String  // e.g. "assets/icons/chat.webp"
)

@Serializable
data class IconPack(
    val id: String,
    val name: String,
    val iconOverrides: List<IconOverrideItem> = emptyList()
)
