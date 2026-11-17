package com.anttech.prayertimes.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrangeLightScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeSoft,
    onPrimaryContainer = BrandOrangeDeep,
    inversePrimary = Color(0xFFFFB68F),
    secondary = WarmSand,
    onSecondary = Color.White,
    secondaryContainer = WarmSandSoft,
    onSecondaryContainer = Color(0xFF351000),
    tertiary = DeepTeal,
    onTertiary = Color.White,
    tertiaryContainer = DeepTealSoft,
    onTertiaryContainer = Color(0xFF002020),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = BrandOrange,
    inverseSurface = Color(0xFF3B2A22),
    inverseOnSurface = Color(0xFFFFEDE5),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Color.Black
)

/**
 * Dark warm-brown scheme: the app's default look (orange hero on deep brown).
 */
private val OrangeDarkScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeSoft,
    onPrimaryContainer = BrandOrangeDeep,
    inversePrimary = BrandOrange,
    secondary = WarmSand,
    onSecondary = Color(0xFF3A1D10),
    secondaryContainer = WarmSandSoft,
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = DeepTeal,
    onTertiary = Color(0xFF003738),
    tertiaryContainer = DeepTealSoft,
    onTertiaryContainer = Color(0xFFB2E5E6),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = BrandOrange,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceVariantDark,
    inverseSurface = Color(0xFFF4EAE4),
    inverseOnSurface = Color(0xFF2B1A12),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Color.Black
)

/**
 * The app ships in dark mode: the orange-on-brown design is the brand look,
 * independent of the system setting.
 */
@Composable
fun NamazVakitleriTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) OrangeDarkScheme else OrangeLightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
