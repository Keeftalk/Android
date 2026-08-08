package com.keeftalk.chat.ui.emoji

import androidx.compose.runtime.Immutable

@Immutable
data class Emoji(
    val unicode: String = "",
    val name: String = "",
    val keywords: List<String> = emptyList(),
    val category: EmojiCategory = EmojiCategory.SMILEYS,
    val skinToneSupport: Boolean = false,
    val variants: List<String> = emptyList()
)

enum class EmojiCategory(val displayName: String, val icon: String) {
    RECENT("Recent", "🕒"),
    FREQUENT("Frequent", "⭐"),
    SMILEYS("Smileys & Emotion", "😀"),
    PEOPLE("People & Body", "👋"),
    ANIMALS("Animals & Nature", "🐶"),
    FOOD("Food & Drink", "🍎"),
    TRAVEL("Travel & Places", "🚗"),
    ACTIVITIES("Activities", "⚽"),
    OBJECTS("Objects", "💡"),
    SYMBOLS("Symbols", "🔣"),
    FLAGS("Flags", "🏁")
}
