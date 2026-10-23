package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
data class ThemeColors(
    val primaryHex: String = "#0A84FF",
    val onPrimaryHex: String = "#FFFFFF",
    val backgroundBodyHex: String = "#000000",
    val onBackgroundHex: String = "#FFFFFF",
    val surfaceHex: String = "#1C1C1E",
    val onSurfaceHex: String = "#FFFFFF",
    val surfaceVariantHex: String = "#2C2C2E",
    val onSurfaceVariantHex: String = "#8E8E93",
    val accentHex: String = "#30D158",
    val cardHex: String = "#1C1C1E",
    val msgMeHex: String = "#0A84FF",
    val msgThemHex: String = "#2C2C2E"
)

@Serializable
data class ThemePack(
    val id: String,
    val name: String,
    val isDark: Boolean = true,
    val lightColors: ThemeColors = ThemeColors(),
    val darkColors: ThemeColors = ThemeColors(),
    val cornerRadiusDp: Int = 22,
    val cardBlurPercent: Int = 20,
    val cardOpacityPercent: Int = 60,
    val backgroundImageRelativePath: String? = null,
    val isReaderThemeOnly: Boolean = false
)
