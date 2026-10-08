package tn.keeftalk.chat.ui.theme.keef

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.PeriodOfDay
import tn.keeftalk.chat.domain.model.WeatherConditionType

data class KeefThemeTokens(
    val sky1: Color,
    val sky2: Color,
    val ink: Color,
    val ink2: Color,
    val accent: Color,
    val onAccent: Color,
    val card: Color,
    val cardLine: Color,
    val soft: Color,
    val soft2: Color,
    val ok: Color,
    val bad: Color,
    val warn: Color,
    val veilColor: Color,
    val veilAlpha: Float,
    val cloudColor: Color,
    val cloudShade: Color,
    val cornerRadius: Dp = 22.dp,
    val blurRadius: Dp = 18.dp,
    val cardBlurPercent: Int = 20,
    val buttonBlurPercent: Int = 20,
    val surfaceBlurPercent: Int = 20,
    val cardOpacityPercent: Int = 60,
    val buttonOpacityPercent: Int = 50,
    val surfaceOpacityPercent: Int = 70,
    val borderOpacityPercent: Int = 50,
    val dialogBlurPercent: Int = 20,
    val dialogOpacityPercent: Int = 80,
    val fabBlurPercent: Int = 20,
    val fabOpacityPercent: Int = 60,
    val containerBlurPercent: Int = 20,
    val containerOpacityPercent: Int = 60
) {
    companion object {
        fun forEnvironment(
            env: EnvironmentalState,
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
            containerOpacityPercent: Int = 60
        ): KeefThemeTokens {
            val baseTokens = when (env.period) {
                PeriodOfDay.DAWN -> KeefThemeTokens(
                    sky1 = Color(0xFF3A3F86),
                    sky2 = Color(0xFFFFA882),
                    ink = Color(0xFF3B1F2C),
                    ink2 = Color(0xFF7D5560),
                    accent = Color(0xFFD6336C),
                    onAccent = Color.White,
                    card = Color(0x9EFFF5F0),
                    cardLine = Color(0xB8FFFFFF),
                    soft = Color(0x123B1F2C),
                    soft2 = Color(0x213B1F2C),
                    ok = Color(0xFF168A4A),
                    bad = Color(0xFFD12E2E),
                    warn = Color(0xFF8A4B00),
                    veilColor = Color.Black,
                    veilAlpha = 0f,
                    cloudColor = Color(0xFFFFF0EB),
                    cloudShade = Color(0xFFB87888)
                )

                PeriodOfDay.DAY -> KeefThemeTokens(
                    sky1 = Color(0xFF7CC4F2),
                    sky2 = Color(0xFFE3F4FF),
                    ink = Color(0xFF0E2A3F),
                    ink2 = Color(0xFF47657B),
                    accent = Color(0xFF0A6FE0),
                    onAccent = Color.White,
                    card = Color(0x9EFFFFFF),
                    cardLine = Color(0xB8FFFFFF),
                    soft = Color(0x120E2A3F),
                    soft2 = Color(0x210E2A3F),
                    ok = Color(0xFF168A4A),
                    bad = Color(0xFFD12E2E),
                    warn = Color(0xFF8A4B00),
                    veilColor = Color.Black,
                    veilAlpha = 0f,
                    cloudColor = Color.White,
                    cloudShade = Color(0xFF707A8C)
                )

                PeriodOfDay.DUSK -> KeefThemeTokens(
                    sky1 = Color(0xFF4D3F95),
                    sky2 = Color(0xFFF58A6A),
                    ink = Color.White,
                    ink2 = Color(0xFFF3D9D4),
                    accent = Color(0xFFFFB067),
                    onAccent = Color(0xFF0B1020),
                    card = Color(0x7A281950),
                    cardLine = Color(0x33FFFFFF),
                    soft = Color(0x1AFFFFFF),
                    soft2 = Color(0x29FFFFFF),
                    ok = Color(0xFF5FE39A),
                    bad = Color(0xFFFF8A8A),
                    warn = Color(0xFFFFB067),
                    veilColor = Color.Black,
                    veilAlpha = 0f,
                    cloudColor = Color(0xFFFFE8DC),
                    cloudShade = Color(0xFF6B487A)
                )

                PeriodOfDay.NIGHT -> KeefThemeTokens(
                    sky1 = Color(0xFF070C24),
                    sky2 = Color(0xFF22316A),
                    ink = Color(0xFFEEF2FF),
                    ink2 = Color(0xFFA5B1D8),
                    accent = Color(0xFF8FB4FF),
                    onAccent = Color(0xFF0B1020),
                    card = Color(0x9412183C),
                    cardLine = Color(0x24FFFFFF),
                    soft = Color(0x14FFFFFF),
                    soft2 = Color(0x24FFFFFF),
                    ok = Color(0xFF5FE39A),
                    bad = Color(0xFFFF8A8A),
                    warn = Color(0xFFFFB067),
                    veilColor = Color.Black,
                    veilAlpha = 0f,
                    cloudColor = Color(0xFFB0C4DE),
                    cloudShade = Color(0xFF202A50)
                )
            }

            // Apply atmospheric condition adjustments
            val (vColor, vAlpha) = when (env.weatherCondition) {
                WeatherConditionType.CLEAR -> Color.Black to 0f
                WeatherConditionType.CLOUDY -> Color.Black to 0.12f
                WeatherConditionType.RAIN -> Color(0xFF0A1428) to 0.28f
                WeatherConditionType.STORM -> Color(0xFF020510) to 0.48f
                WeatherConditionType.SNOW -> Color.White to 0.15f
                WeatherConditionType.FOG -> Color(0xFFC0D0E0) to 0.32f
            }

            return baseTokens.copy(
                veilColor = vColor,
                veilAlpha = vAlpha,
                cardBlurPercent = cardBlurPercent,
                buttonBlurPercent = buttonBlurPercent,
                surfaceBlurPercent = surfaceBlurPercent,
                cardOpacityPercent = cardOpacityPercent,
                buttonOpacityPercent = buttonOpacityPercent,
                surfaceOpacityPercent = surfaceOpacityPercent,
                borderOpacityPercent = borderOpacityPercent,
                cornerRadius = cornerRadiusDp.dp,
                dialogBlurPercent = dialogBlurPercent,
                dialogOpacityPercent = dialogOpacityPercent,
                fabBlurPercent = fabBlurPercent,
                fabOpacityPercent = fabOpacityPercent,
                containerBlurPercent = containerBlurPercent,
                containerOpacityPercent = containerOpacityPercent
            )
        }

        fun default(): KeefThemeTokens = forEnvironment(EnvironmentalState.default())
    }
}

val LocalKeefThemeTokens = staticCompositionLocalOf { KeefThemeTokens.default() }
val LocalEnvironmentalState = staticCompositionLocalOf { EnvironmentalState.default() }
