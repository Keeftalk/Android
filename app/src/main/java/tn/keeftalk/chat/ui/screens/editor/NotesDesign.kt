package tn.keeftalk.chat.ui.screens.editor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

object NotesDesign {
    val BrandColor: Color @Composable get() = MaterialTheme.colorScheme.primary
    val BrandGradient: List<Color> @Composable get() = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    )
    
    val PinColor = Color(0xFFF59E0B)
    val SuccessColor = Color(0xFF22C55E)
    val InfoColor = Color(0xFF2563EB)
    val DangerColor = Color(0xFFDC2626)

    @Composable
    fun colors(): NotesPalette {
        val scheme = MaterialTheme.colorScheme
        val dark = isSystemInDarkTheme()
        return NotesPalette(
            bg = scheme.background,
            surface = scheme.surface,
            sidebar = scheme.surfaceVariant,
            border = scheme.outline,
            text = scheme.onBackground,
            textSecondary = scheme.onSurfaceVariant,
            textMuted = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            isDark = dark
        )
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

val LocalNotesPalette = compositionLocalOf {
    NotesPalette(
        bg = Color.White,
        surface = Color.White,
        sidebar = Color.LightGray,
        border = Color.Gray,
        text = Color.Black,
        textSecondary = Color.DarkGray,
        textMuted = Color.Gray,
        isDark = false
    )
}
