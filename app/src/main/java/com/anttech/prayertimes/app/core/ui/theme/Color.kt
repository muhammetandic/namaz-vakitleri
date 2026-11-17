package com.anttech.prayertimes.core.ui.theme

import androidx.compose.ui.graphics.Color

// Warm orange brand palette
val BrandOrange = Color(0xFFF97316)
val BrandOrangeBright = Color(0xFFFB8B48)
val BrandOrangeSoft = Color(0xFF4A2617)
val BrandOrangeDeep = Color(0xFFFFDCC8)

/** Shared hero gradient: bright orange fading into deep burnt orange. */
val BrandHeroGradient = listOf(Color(0xFFF97B31), Color(0xFFC2410C))

val WarmSand = Color(0xFFE7B6A0)
val WarmSandSoft = Color(0xFF3A261B)
val DeepTeal = Color(0xFF7FD6D8)
val DeepTealSoft = Color(0xFF123B3C)

// Dark warm-brown surfaces (app default theme)
val BackgroundDark = Color(0xFF161110)
val OnBackgroundDark = Color(0xFFF4EAE4)
val SurfaceDark = Color(0xFF1B1512)
val SurfaceVariantDark = Color(0xFF2A211C)
val OnSurfaceVariantDark = Color(0xFFC9B4A6)
val OutlineDark = Color(0xFF9A8175)
val OutlineVariantDark = Color(0xFF372B24)
val SurfaceContainerDark = Color(0xFF211A16)
val SurfaceContainerHighDark = Color(0xFF2A221D)

// Light surfaces kept for previews and legacy screens
val BackgroundLight = Color(0xFFFFF8F5)
val OnBackgroundLight = Color(0xFF2B1A12)
val SurfaceLight = Color(0xFFFFF8F5)
val SurfaceVariantLight = Color(0xFFF5DED2)
val OnSurfaceVariantLight = Color(0xFF55433A)
val OutlineLight = Color(0xFF8C7166)
val OutlineVariantLight = Color(0xFFE0C0B2)

// Brand accents kept for existing screens
val KerahaWarning = Color(0xFFFFC46B)
val ActivePrayerGlow = Color(0xFFF97316)

// Legacy aliases kept so older screens still compile after the orange rebrand
val WarmGoldLight = Color(0xFFB4690E)
val WarmGoldDark = Color(0xFFF0A15A)
val EmeraldGreenLight = BrandOrange
val EmeraldGreenDark = BrandOrangeBright
