package com.keeftalk.chat.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object AvatarUtils {
    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val trimmed = name.trim().replace(Regex("\\s+"), " ")
        val parts = trimmed.split(" ")
        return if (parts.size == 1) {
            parts[0].firstOrNull()?.toString()?.uppercase() ?: "?"
        } else {
            val first = parts.firstOrNull()?.firstOrNull()?.toString() ?: ""
            val last = parts.lastOrNull()?.firstOrNull()?.toString() ?: ""
            (first + last).uppercase()
        }
    }

    fun getAvatarColor(seed: String?): Color {
        if (seed.isNullOrBlank()) return Color(0xFF78909C)
        val colors = listOf(
            Color(0xFFEF5350), // Red
            Color(0xFFEC407A), // Pink
            Color(0xFFAB47BC), // Purple
            Color(0xFF7E57C2), // Deep Purple
            Color(0xFF5C6BC0), // Indigo
            Color(0xFF42A5F5), // Blue
            Color(0xFF26C6DA), // Cyan
            Color(0xFF26A69A), // Teal
            Color(0xFF66BB6A), // Green
            Color(0xFFFFA726), // Amber
            Color(0xFFFF7043), // Deep Orange
            Color(0xFF8D6E63), // Brown
            Color(0xFF78909C)  // Blue Grey
        )
        val index = Math.abs(seed.hashCode()) % colors.size
        return colors[index]
    }

    fun getAvatarColorInt(seed: String?): Int {
        return getAvatarColor(seed).toArgb()
    }

    fun getTextColorForBackground(backgroundColor: Color): Color {
        // Simple heuristic for text contrast
        return Color.White // Most of our avatar colors are dark enough for white text
    }
}
