package com.keeftalk.chat.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Immutable
data class ChatTheme(
    val id: String,
    val name: String,
    val msgMe: Color,
    val msgThem: Color,
    val useGlassmorphism: Boolean = false,
    val topBarAlpha: Float = 1f,
    val inputAlpha: Float = 1f,
    val textColor: Color? = null,
    val accentColor: Color? = null,
    val background: @Composable () -> Unit = { 
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) 
    }
)

val LocalChatTheme = staticCompositionLocalOf { DefaultChatTheme }

val DefaultChatTheme = ChatTheme(
    id = "default",
    name = "Default",
    msgMe = Primary,
    msgThem = MsgThem
)

val RosaTheme = ChatTheme(
    id = "rosa",
    name = "Rosa",
    msgMe = Color(0xFFFFB6C1),
    msgThem = Color(0x33FFFFFF),
    useGlassmorphism = true,
    topBarAlpha = 0.15f,
    inputAlpha = 0.15f,
    textColor = Color.White,
    accentColor = Color(0xFFFF6B9D),
    background = { RosaBackground() }
)

val AlphaTheme = ChatTheme(
    id = "alpha",
    name = "Alpha",
    msgMe = Color(0x1FCAA84C),
    msgThem = Color(0x0DFFFFFF),
    useGlassmorphism = true,
    topBarAlpha = 0.9f,
    inputAlpha = 0.06f,
    textColor = Color(0xFFD0D0D8),
    accentColor = Color(0xFFC9A84C),
    background = { AlphaBackground() }
)

object ChatThemes {
    val all = listOf(DefaultChatTheme, RosaTheme, AlphaTheme)
    fun getById(id: String?) = all.find { it.id == id } ?: DefaultChatTheme
}

@Composable
fun RosaBackground() {
    // To be implemented in another file for cleanliness or here
    com.keeftalk.chat.ui.components.RosaThemeBackground()
}

@Composable
fun AlphaBackground() {
    com.keeftalk.chat.ui.components.AlphaThemeBackground()
}
