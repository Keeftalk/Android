package com.keeftalk.chat.ui.screens.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

object NotesDesign {
    val BrandColor = Color(0xFF6C63FF)
    val BrandGradient = listOf(Color(0xFF6C63FF), Color(0xFF8B83FF))
    
    val LightBg = Color(0xFFF0F2F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSidebar = Color(0xFFF8FAFC)
    val LightBorder = Color(0xFFEEF2F6)
    val LightText = Color(0xFF1E293B)
    val LightTextSecondary = Color(0xFF64748B)
    val LightTextMuted = Color(0xFF94A3B8)
    
    val DarkBg = Color(0xFF0A0A0A)
    val DarkSurface = Color(0xFF141414)
    val DarkSidebar = Color(0xFF141414)
    val DarkBorder = Color(0xFF2A2A2A)
    val DarkText = Color(0xFFE8E8E8)
    val DarkTextSecondary = Color(0xFFAAAAAA)
    val DarkTextMuted = Color(0xFF777777)
    
    val PinColor = Color(0xFFF59E0B)
    val SuccessColor = Color(0xFF22C55E)
    val InfoColor = Color(0xFF2563EB)
    val DangerColor = Color(0xFFDC2626)

    @Composable
    fun colors(): NotesPalette {
        val dark = isSystemInDarkTheme()
        return if (dark) {
            NotesPalette(
                bg = DarkBg, surface = DarkSurface, sidebar = DarkSidebar,
                border = DarkBorder, text = DarkText, textSecondary = DarkTextSecondary,
                textMuted = DarkTextMuted, isDark = true
            )
        } else {
            NotesPalette(
                bg = LightBg, surface = LightSurface, sidebar = LightSidebar,
                border = LightBorder, text = LightText, textSecondary = LightTextSecondary,
                textMuted = LightTextMuted, isDark = false
            )
        }
    }
}

data class NotesPalette(
    val bg: Color,
    val surface: Color,
    val sidebar: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean
)
