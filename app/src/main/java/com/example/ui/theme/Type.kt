package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Exclusive SFPRODISPLAYBOLD.OTF font family across the entire application
val SfProDisplayBold: FontFamily = FontFamily(
    Font(R.font.sf_pro_display_bold, FontWeight.Thin),
    Font(R.font.sf_pro_display_bold, FontWeight.ExtraLight),
    Font(R.font.sf_pro_display_bold, FontWeight.Light),
    Font(R.font.sf_pro_display_bold, FontWeight.Normal),
    Font(R.font.sf_pro_display_bold, FontWeight.Medium),
    Font(R.font.sf_pro_display_bold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
    Font(R.font.sf_pro_display_bold, FontWeight.ExtraBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Black)
)

val PlusJakartaSans: FontFamily = SfProDisplayBold
val SpaceGrotesk: FontFamily = SfProDisplayBold

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimaryWhite
    ),
    displayMedium = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimaryWhite
    ),
    displaySmall = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = TextPrimaryWhite
    ),
    headlineLarge = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimaryWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimaryWhite
    ),
    headlineSmall = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimaryWhite
    ),
    titleLarge = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimaryWhite
    ),
    titleMedium = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimaryWhite
    ),
    titleSmall = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimaryWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimaryWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = TextSecondaryGrey
    ),
    bodySmall = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondaryGrey
    ),
    labelLarge = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = TextPrimaryWhite
    ),
    labelMedium = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
        color = TextSecondaryGrey
    ),
    labelSmall = TextStyle(
        fontFamily = SfProDisplayBold,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondaryGrey
    )
)

val Typography = AppTypography
