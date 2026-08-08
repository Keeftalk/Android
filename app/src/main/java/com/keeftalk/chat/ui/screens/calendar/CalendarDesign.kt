package com.keeftalk.chat.ui.screens.calendar

import androidx.compose.ui.graphics.Color

object CalendarDesign {
    val Background = Color(0xFF000000)
    val SurfaceGradientStart = Color(0xFF0A0A0E)
    val SurfaceGradientEnd = Color(0xFF050508)
    
    val TextGradientStart = Color(0xFFF0F0FF)
    val TextGradientEnd = Color(0xFFA78BFA)
    
    val ButtonBg = Color(0xFFFFFF).copy(alpha = 0.06f)
    val AccentGradientStart = Color(0xFF7C3AED)
    val AccentGradientEnd = Color(0xFFA78BFA)
    
    val BorderColor = Color(0xFFFFFFFF).copy(alpha = 0.04f)
    val TabActiveBg = Color(0xFFA78BFA).copy(alpha = 0.18f)
    val TabActiveText = Color(0xFFC4B5FD)
    
    val DayOtherMonth = Color(0xFF444466)
    val DayTodayBg = Color(0xFFA78BFA).copy(alpha = 0.2f)
    val DayTodayText = Color(0xFFC4B5FD)
    
    val Purple = Color(0xFFA78BFA)
    val Green = Color(0xFF34D399)
    val Pink = Color(0xFFF472B6)
    val Yellow = Color(0xFFFBBF24)
    val Blue = Color(0xFF60A5FA)
    val Orange = Color(0xFFFB923C)
    val Red = Color(0xFFF87171)

    val SurfaceGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(SurfaceGradientStart, SurfaceGradientEnd)
    )
    val TextGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(TextGradientStart, TextGradientEnd)
    )
    val AccentGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(AccentGradientStart, AccentGradientEnd)
    )
    
    val CardBg = Color(0xFFFFFFFF).copy(alpha = 0.03f)
    val CardBorder = Color(0xFFFFFFFF).copy(alpha = 0.04f)
    val DividerColor = Color(0xFFFFFFFF).copy(alpha = 0.04f)
}
