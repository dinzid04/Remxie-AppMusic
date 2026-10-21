package com.example.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val ObsidianCarbon = Color(0xFF070709)
val ElevatedSurface = Color(0xFF121216)
val GlassCardSurface = Color(0xFF1A1A20)
val SubtleSurface = Color(0xFF23232B)
val ElectricCrimson = Color(0xFFFF4D6D)
val VibrantViolet = Color(0xFF8B5CF6)
val SpotifyGreen = Color(0xFF1ED760)
val YouTubeMusicRed = Color(0xFFFF0033)
val AmberGold = Color(0xFFF59E0B)
val CyanGlow = Color(0xFF06B6D4)
val TextPrimary = TextPrimaryWhite
val TextSecondary = TextSecondaryGrey
val TextMuted = Color(0xFF6B6B76)
val HairlineBorder = Color(0xFF262630)

private val BitChordDarkColorScheme = darkColorScheme(
    primary = ElectricCrimson,
    onPrimary = TextPrimaryWhite,
    secondary = TextPrimaryWhite,
    onSecondary = OledBlack,
    background = OledBlack,
    onBackground = TextPrimaryWhite,
    surface = SurfaceDark,
    onSurface = TextPrimaryWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondaryGrey,
    outline = HairlineBorder
)

@Composable
fun BitChordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BitChordDarkColorScheme,
        typography = AppTypography,
        content = {
            androidx.compose.material3.ProvideTextStyle(
                value = androidx.compose.ui.text.TextStyle(
                    fontFamily = SfProDisplayBold,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                content = content
            )
        }
    )
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    BitChordTheme(content = content)
}

@Composable
fun MusicPlatformTheme(content: @Composable () -> Unit) {
    BitChordTheme(content = content)
}

@Composable
fun rememberDominantAccentColor(
    imageUrl: String?,
    defaultColor: Color = Color(0xFFE94057)
): Color = rememberDynamicImageColor(imageUrl = imageUrl, fallbackColor = defaultColor)

@Composable
fun rememberDynamicImageColor(
    imageUrl: String?,
    fallbackColor: Color = Color(0xFF3E3832)
): Color {
    val context = LocalContext.current
    var extractedColor by remember(imageUrl) { mutableStateOf(fallbackColor) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            extractedColor = fallbackColor
            return@LaunchedEffect
        }
        val color = withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(64, 64)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        sampleVibrantColor(bitmap, fallbackColor)
                    } else fallbackColor
                } else fallbackColor
            } catch (_: Exception) {
                fallbackColor
            }
        }
        extractedColor = color
    }
    return extractedColor
}

fun sampleVibrantColor(bitmap: Bitmap, fallback: Color): Color {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var weightSum = 0L
    val width = bitmap.width
    val height = bitmap.height
    val stepX = (width / 12).coerceAtLeast(1)
    val stepY = (height / 12).coerceAtLeast(1)
    val hsl = FloatArray(3)

    for (x in 0 until width step stepX) {
        for (y in 0 until height step stepY) {
            val pixel = bitmap.getPixel(x, y)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            ColorUtils.RGBToHSL(r, g, b, hsl)
            val saturation = hsl[1]
            val lightness = hsl[2]
            if (lightness in 0.15f..0.85f) {
                val weight = ((saturation * 10f) + 1f).toLong()
                rSum += r * weight
                gSum += g * weight
                bSum += b * weight
                weightSum += weight
            }
        }
    }

    if (weightSum == 0L) return fallback
    val avgR = (rSum / weightSum).toInt().coerceIn(0, 255)
    val avgG = (gSum / weightSum).toInt().coerceIn(0, 255)
    val avgB = (bSum / weightSum).toInt().coerceIn(0, 255)
    ColorUtils.RGBToHSL(avgR, avgG, avgB, hsl)
    hsl[1] = hsl[1].coerceAtLeast(0.50f)
    hsl[2] = hsl[2].coerceIn(0.35f, 0.56f)
    val boostedInt = ColorUtils.HSLToColor(hsl)
    return Color(boostedInt)
}
