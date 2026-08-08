package com.keeftalk.chat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme {
    SYSTEM,
    DAY,
    PINKY,
    MASCULINE,
    DEFAULT,
    LIGHT,
    DARK,
    AMOLED
}

data class ChatThemeExtra(
    val msgMe: Color,
    val msgThem: Color,
    val useGlassmorphism: Boolean = false,
    val backgroundGradient: Brush? = null
)

val LocalChatThemeExtra = staticCompositionLocalOf { 
    ChatThemeExtra(msgMe = MsgMe, msgThem = MsgThem) 
}
val LocalAppTheme = staticCompositionLocalOf { AppTheme.SYSTEM }
val LocalChatSettings = staticCompositionLocalOf { com.keeftalk.chat.domain.model.UserChatSettings("") }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    background = BackgroundBody,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary, // Dark theme keeps primary usually
    onPrimary = Color.White,
    background = DarkBackgroundBody,
    onBackground = DarkTextPrimary,
    surface = DarkBackgroundSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBackgroundSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderColor
)

private val DayColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = Color.White,
    background = DayBackgroundBody,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val PinkyColorScheme = lightColorScheme(
    primary = PinkyPrimary,
    onPrimary = Color.White,
    background = PinkyBackgroundBody,
    onBackground = PinkyTextPrimary,
    surface = BackgroundSurface,
    onSurface = PinkyTextPrimary,
    surfaceVariant = PinkyBackgroundSurfaceAlt,
    onSurfaceVariant = PinkyTextSecondary,
    outline = PinkyBorderColor
)

private val MasculineColorScheme = darkColorScheme(
    primary = MasculinePrimary,
    onPrimary = Color.White,
    background = MasculineBackgroundBody,
    onBackground = MasculineTextPrimary,
    surface = MasculineBackgroundSurface,
    onSurface = MasculineTextPrimary,
    surfaceVariant = MasculineBackgroundSurfaceAlt,
    onSurfaceVariant = MasculineTextSecondary,
    outline = MasculineBorderColor
)

private val AmoledColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color.Gray,
    outline = Color(0xFF333333)
)

@Composable
fun KeeftalkTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false,
    fontSize: Int = 16,
    fontScale: Float = 1.0f,
    uiScale: Float = 1.0f,
    isBold: Boolean = false,
    chatSettings: com.keeftalk.chat.domain.model.UserChatSettings = com.keeftalk.chat.domain.model.UserChatSettings(""),
    isSkeleton: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.SYSTEM, AppTheme.DEFAULT -> isSystemDark
        AppTheme.MASCULINE, AppTheme.DARK, AppTheme.AMOLED -> true
        else -> false
    }

    val context = LocalContext.current
    val colorScheme = if (isSkeleton) {
        if (isDark) DarkColorScheme else LightColorScheme
    } else {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            appTheme == AppTheme.SYSTEM || appTheme == AppTheme.DEFAULT -> if (isSystemDark) DarkColorScheme else LightColorScheme
            appTheme == AppTheme.DAY -> DayColorScheme
            appTheme == AppTheme.PINKY -> PinkyColorScheme
            appTheme == AppTheme.MASCULINE -> MasculineColorScheme
            appTheme == AppTheme.LIGHT -> LightColorScheme
            appTheme == AppTheme.DARK -> DarkColorScheme
            appTheme == AppTheme.AMOLED -> AmoledColorScheme
            else -> if (isDark) DarkColorScheme else LightColorScheme
        }
    }

    val chatExtra = remember(appTheme, isSystemDark) {
        when (appTheme) {
            AppTheme.SYSTEM, AppTheme.DEFAULT -> if (isSystemDark) ChatThemeExtra(Primary, DarkMsgThem) else ChatThemeExtra(MsgMe, MsgThem)
            AppTheme.DAY -> ChatThemeExtra(DayMsgMe, DayMsgThem)
            AppTheme.PINKY -> ChatThemeExtra(PinkyMsgMe, PinkyMsgThem)
            AppTheme.MASCULINE -> ChatThemeExtra(MasculineMsgMe, MasculineMsgThem, useGlassmorphism = true)
            AppTheme.LIGHT -> ChatThemeExtra(MsgMe, MsgThem)
            AppTheme.DARK -> ChatThemeExtra(Primary, DarkMsgThem)
            AppTheme.AMOLED -> ChatThemeExtra(Primary, Color.DarkGray)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode && !isSkeleton) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    val typography = if (isSkeleton) Typography() else remember(appTheme, fontSize, fontScale, isBold) {
        getTypographyForTheme(appTheme, fontSize, fontScale, isBold)
    }

    val icons = remember(appTheme) {
        IconProvider.getIconsForTheme(appTheme)
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity = remember(currentDensity, uiScale) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density * uiScale,
            fontScale = currentDensity.fontScale
        )
    }

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalAppIcons provides icons,
        LocalChatThemeExtra provides chatExtra,
        LocalChatSettings provides chatSettings,
        androidx.compose.ui.platform.LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
