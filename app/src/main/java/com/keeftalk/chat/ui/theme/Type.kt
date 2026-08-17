package com.keeftalk.chat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// We assume the font files are added to res/font
// val Inter = FontFamily(
//     Font(R.font.inter_regular, FontWeight.Normal),
//     Font(R.font.inter_medium, FontWeight.Medium),
//     Font(R.font.inter_semibold, FontWeight.SemiBold),
//     Font(R.font.inter_bold, FontWeight.Bold)
// )
// val Quicksand = FontFamily(
//     Font(R.font.quicksand_regular, FontWeight.Normal),
//     Font(R.font.quicksand_medium, FontWeight.Medium),
//     Font(R.font.quicksand_semibold, FontWeight.SemiBold),
//     Font(R.font.quicksand_bold, FontWeight.Bold)
// )

val Inter = FontFamily.Default

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )
)

fun getTypographyForTheme(
    fontSize: Int = 16,
    fontScale: Float = 1.0f,
    isBold: Boolean = false
): Typography {
    val fontFamily = Inter
    val fontWeightNormal = if (isBold) FontWeight.SemiBold else FontWeight.Normal
    val fontWeightBold = FontWeight.Bold
    
    // Base scale is 16, we adjust relative to 14.sp (bodyLarge default in Typography above)
    val scale = (fontSize.toFloat() / 16f) * fontScale
    
    return Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightNormal, fontSize = (14 * scale).sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightNormal, fontSize = (13 * scale).sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightNormal, fontSize = (12 * scale).sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (10 * scale).sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightNormal, fontSize = (11 * scale).sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (16 * scale).sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (15 * scale).sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = (18 * scale).sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightBold, fontSize = (20 * scale).sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightBold, fontSize = (24 * scale).sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = fontWeightBold, fontSize = (22 * scale).sp)
    )
}
