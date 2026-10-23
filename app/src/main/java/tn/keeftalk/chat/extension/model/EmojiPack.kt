package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomEmojiItem(
    val code: String,
    val name: String,
    val relativePath: String, // e.g. "assets/emoji/party.webp"
    val category: String = "CUSTOM",
    val keywords: List<String> = emptyList()
)

@Serializable
data class EmojiPack(
    val id: String,
    val title: String,
    val author: String = "Community Creator",
    val isStickerPack: Boolean = false,
    val items: List<CustomEmojiItem> = emptyList()
)
