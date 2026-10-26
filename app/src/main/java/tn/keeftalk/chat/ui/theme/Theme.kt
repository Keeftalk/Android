package tn.keeftalk.chat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import tn.keeftalk.chat.extension.model.ThemePack
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.PeriodOfDay
import tn.keeftalk.chat.ui.theme.keef.KeefThemeTokens
import tn.keeftalk.chat.ui.theme.keef.LocalEnvironmentalState
import tn.keeftalk.chat.ui.theme.keef.LocalKeefThemeTokens

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
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
val LocalChatSettings = staticCompositionLocalOf { tn.keeftalk.chat.domain.model.UserChatSettings("") }

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
    primary = Primary,
    onPrimary = Color.White,
    background = DarkBackgroundBody,
    onBackground = DarkTextPrimary,
    surface = DarkBackgroundSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBackgroundSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderColor
)

@Composable
fun KeeftalkTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    environmentalState: EnvironmentalState = EnvironmentalState.default(),
    dynamicColor: Boolean = false,
    fontSize: Int = 16,
    fontScale: Float = 1.0f,
    uiScale: Float = 1.0f,
    isBold: Boolean = false,
    cardBlurPercent: Int = 20,
    buttonBlurPercent: Int = 20,
    surfaceBlurPercent: Int = 20,
    cardOpacityPercent: Int = 60,
    buttonOpacityPercent: Int = 50,
    surfaceOpacityPercent: Int = 70,
    borderOpacityPercent: Int = 50,
    cornerRadiusDp: Int = 22,
    dialogBlurPercent: Int = 20,
    dialogOpacityPercent: Int = 80,
    fabBlurPercent: Int = 20,
    fabOpacityPercent: Int = 60,
    containerBlurPercent: Int = 20,
    containerOpacityPercent: Int = 60,
    chatSettings: tn.keeftalk.chat.domain.model.UserChatSettings = tn.keeftalk.chat.domain.model.UserChatSettings(""),
    isSkeleton: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemDark
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    val context = LocalContext.current
    val keefTokens = remember(
        environmentalState,
        cardBlurPercent,
        buttonBlurPercent,
        surfaceBlurPercent,
        cardOpacityPercent,
        buttonOpacityPercent,
        surfaceOpacityPercent,
        borderOpacityPercent,
        cornerRadiusDp,
        dialogBlurPercent,
        dialogOpacityPercent,
        fabBlurPercent,
        fabOpacityPercent,
        containerBlurPercent,
        containerOpacityPercent
    ) {
        KeefThemeTokens.forEnvironment(
            env = environmentalState,
            cardBlurPercent = cardBlurPercent,
            buttonBlurPercent = buttonBlurPercent,
            surfaceBlurPercent = surfaceBlurPercent,
            cardOpacityPercent = cardOpacityPercent,
            buttonOpacityPercent = buttonOpacityPercent,
            surfaceOpacityPercent = surfaceOpacityPercent,
            borderOpacityPercent = borderOpacityPercent,
            cornerRadiusDp = cornerRadiusDp,
            dialogBlurPercent = dialogBlurPercent,
            dialogOpacityPercent = dialogOpacityPercent,
            fabBlurPercent = fabBlurPercent,
            fabOpacityPercent = fabOpacityPercent,
            containerBlurPercent = containerBlurPercent,
            containerOpacityPercent = containerOpacityPercent
        )
    }

    val customThemeState = remember(context) {
        try {
            tn.keeftalk.chat.di.AppModule.provideExtensionHost(context).registry.themeRegistry.activeCustomTheme
        } catch (_: Exception) {
            null
        }
    }
    val customTheme = customThemeState?.collectAsState()?.value

    val colorScheme = if (customTheme != null) {
        val tc = if (isDark) customTheme.darkColors else customTheme.lightColors
        val parseColor: (String, Color) -> Color = { hex, fallback ->
            try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { fallback }
        }
        if (isDark) {
            darkColorScheme(
                primary = parseColor(tc.primaryHex, Primary),
                onPrimary = parseColor(tc.onPrimaryHex, Color.White),
                background = parseColor(tc.backgroundBodyHex, DarkBackgroundBody),
                onBackground = parseColor(tc.onBackgroundHex, DarkTextPrimary),
                surface = parseColor(tc.surfaceHex, DarkBackgroundSurface),
                onSurface = parseColor(tc.onSurfaceHex, DarkTextPrimary),
                surfaceVariant = parseColor(tc.surfaceVariantHex, DarkBackgroundSurfaceAlt),
                onSurfaceVariant = parseColor(tc.onSurfaceVariantHex, DarkTextSecondary)
            )
        } else {
            lightColorScheme(
                primary = parseColor(tc.primaryHex, Primary),
                onPrimary = parseColor(tc.onPrimaryHex, Color.White),
                background = parseColor(tc.backgroundBodyHex, BackgroundBody),
                onBackground = parseColor(tc.onBackgroundHex, TextPrimary),
                surface = parseColor(tc.surfaceHex, BackgroundSurface),
                onSurface = parseColor(tc.onSurfaceHex, TextPrimary),
                surfaceVariant = parseColor(tc.surfaceVariantHex, BackgroundSurfaceAlt),
                onSurfaceVariant = parseColor(tc.onSurfaceVariantHex, TextSecondary)
            )
        }
    } else if (isSkeleton) {
        if (isDark) DarkColorScheme else LightColorScheme
    } else {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            appTheme == AppTheme.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
            appTheme == AppTheme.LIGHT -> LightColorScheme
            appTheme == AppTheme.DARK -> DarkColorScheme
            else -> if (isDark) DarkColorScheme else LightColorScheme
        }
    }

    val activeKeefTokens = remember(keefTokens, customTheme, isDark) {
        if (customTheme != null) {
            val tc = if (isDark) customTheme.darkColors else customTheme.lightColors
            val parseColor: (String, Color) -> Color = { hex, fallback ->
                try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { fallback }
            }
            val accentCol = parseColor(tc.accentHex, parseColor(tc.primaryHex, Primary))
            val cardCol = parseColor(tc.cardHex, parseColor(tc.surfaceHex, DarkBackgroundSurface))
            val softCol = parseColor(tc.surfaceVariantHex, DarkBackgroundSurfaceAlt)
            val inkCol = parseColor(tc.onBackgroundHex, DarkTextPrimary)
            keefTokens.copy(
                accent = accentCol,
                card = cardCol,
                soft = softCol,
                ink = inkCol
            )
        } else {
            keefTokens
        }
    }

    val chatExtra = remember(appTheme, isSystemDark, activeKeefTokens, customTheme) {
        if (customTheme != null) {
            val tc = if (isDark) customTheme.darkColors else customTheme.lightColors
            val parseColor: (String, Color) -> Color = { hex, fallback ->
                try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { fallback }
            }
            ChatThemeExtra(
                msgMe = parseColor(tc.msgMeHex, parseColor(tc.primaryHex, Primary)),
                msgThem = parseColor(tc.msgThemHex, parseColor(tc.surfaceVariantHex, DarkBackgroundSurfaceAlt)),
                useGlassmorphism = true
            )
        } else {
            when (appTheme) {
                AppTheme.SYSTEM -> if (isSystemDark) ChatThemeExtra(Primary, DarkMsgThem) else ChatThemeExtra(MsgMe, MsgThem)
                AppTheme.LIGHT -> ChatThemeExtra(MsgMe, MsgThem)
                AppTheme.DARK -> ChatThemeExtra(Primary, DarkMsgThem)
            }
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
        getTypographyForTheme(fontSize, fontScale, isBold)
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
        LocalKeefThemeTokens provides activeKeefTokens,
        LocalEnvironmentalState provides environmentalState,
        androidx.compose.ui.platform.LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
