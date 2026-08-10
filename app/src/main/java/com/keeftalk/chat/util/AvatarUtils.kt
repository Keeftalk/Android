package com.keeftalk.chat.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object AvatarUtils {
    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        return name.trim().firstOrNull()?.toString()?.uppercase() ?: "?"
    }

    fun getAvatarColor(seed: String?): Color {
        if (seed.isNullOrBlank()) return Color(0xFF78909C)
        val colors = listOf(
            Color(0xFFFF8A80), // Premium Coral
            Color(0xFFFF80AB), // Premium Pink
            Color(0xFFEA80FC), // Premium Lavender
            Color(0xFFB388FF), // Premium Violet
            Color(0xFF8C9EFF), // Premium Indigo
            Color(0xFF82B1FF), // Premium Sky Blue
            Color(0xFF80D8FF), // Premium Light Blue
            Color(0xFF84FFFF), // Premium Aquamarine
            Color(0xFFA7FFEB), // Premium Mint
            Color(0xFFB9F6CA), // Premium Emerald
            Color(0xFFCCFF90), // Premium Lime
            Color(0xFFF4FF81), // Premium Lemon
            Color(0xFFFFE57F), // Premium Gold
            Color(0xFFFFD180), // Premium Orange
            Color(0xFFFF9E80)  // Premium Deep Orange
        )
        val index = Math.abs(seed.hashCode()) % colors.size
        return colors[index]
    }

    fun getAvatarColorInt(seed: String?): Int {
        return getAvatarColor(seed).toArgb()
    }

    fun getTextColorForBackground(backgroundColor: Color): Color {
        // Use a simple luminance check for better contrast
        val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
        return if (luminance > 0.6) Color(0xFF1A1A1A) else Color.White
    }
}
