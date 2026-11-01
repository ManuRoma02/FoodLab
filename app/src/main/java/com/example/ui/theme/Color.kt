package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.util.ThemeSkin

// --- Apple System Accents ---
val AppleBlue = Color(0xFF0A84FF)
val AppleGreen = Color(0xFF30D158)
val AppleOrange = Color(0xFFFF9F0A)
val AppleRed = Color(0xFFFF453A)
val ApplePurple = Color(0xFFBF5AF2)
val AppleYellow = Color(0xFFFFD60A)
val AppleTeal = Color(0xFF64D2FF)
val ApplePink = Color(0xFFFF375F)
val AppleGray = Color(0xFF8E8E93)
val AppleGray2 = Color(0xFF636366)
val AppleGray3 = Color(0xFF48484A)
val AppleGray4 = Color(0xFF3A3A3C)
val AppleGray5 = Color(0xFF2C2C2E)
val AppleGray6 = Color(0xFF1C1C1E)

// Specular highlight border gradient
val SpecularBorderGradient = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.Transparent
    )
)

fun specularBorderGradient(alphaTop: Float = 0.20f, alphaBottom: Float = 0.0f): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = alphaTop),
            Color.White.copy(alpha = alphaBottom)
        )
    )
}

// --- Cupertino Dark Palette ---
val Background = Color(0xFF000000) // Pure OLED Dark
val OnBackground = Color(0xFFFFFFFF)
val Surface = Color(0xFF000000)
val OnSurface = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFF141416)
val SurfaceContainer = Color(0xFF1C1C1E) // Inset Grouped Surface
val SurfaceContainerHigh = Color(0xFF2C2C2E)
val SurfaceContainerHighest = Color(0xFF3A3A3C)
val OnSurfaceVariant = Color(0xFF8E8E93)
val Outline = Color(0xFF545458)
val OutlineVariant = Color(0x59545458)

val Primary = AppleBlue
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF0040DD)
val OnPrimaryContainer = Color(0xFFD0E1FD)

val Secondary = AppleGreen
val OnSecondary = Color(0xFF000000)
val SecondaryContainer = Color(0xFF0F5B24)
val OnSecondaryContainer = Color(0xFFB4F3C6)

val Tertiary = AppleOrange
val OnTertiary = Color(0xFF000000)
val TertiaryContainer = Color(0xFF6B3E00)
val OnTertiaryContainer = Color(0xFFFFD8A8)

val Error = AppleRed
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFF8C1D18)
val OnErrorContainer = Color(0xFFFFD4D1)

// Cupertino Dark Color Scheme
val CupertinoDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Outline,
    outlineVariant = OutlineVariant
)

// Default scheme is Cupertino Dark
val RetroDarkColorScheme = CupertinoDarkColorScheme

// 1. ORIGINAL: Pure OLED Black with Orange (calories), Green (protein), Yellow (carbs), White (fat)
val OriginalColorScheme = darkColorScheme(
    primary = Color(0xFFFF9500),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF6B3E00),
    onPrimaryContainer = Color(0xFFFFD8A8),
    secondary = Color(0xFF30D158),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF0F5B24),
    onSecondaryContainer = Color(0xFFB4F3C6),
    tertiary = Color(0xFFFFD60A),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF5A4800),
    onTertiaryContainer = Color(0xFFFFF0A3),
    error = AppleRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFD4D1),
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Outline,
    outlineVariant = OutlineVariant
)

// 3. NEO_BRUTALISM: High-contrast brutalist canvas with stark black borders and saturated punchy accents
val NeoBrutalismColorScheme = lightColorScheme(
    primary = Color(0xFFFF3366), // Saturated Neo Coral / Shocking Pink
    onPrimary = Color(0xFF000000), // Pure Black text
    primaryContainer = Color(0xFFFFE500), // Electric Cyber Yellow
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF00E599), // Electric Acid Mint
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFB3FCE0),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF00D2FF), // Electric Cyan
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF8CEEFF),
    onTertiaryContainer = Color(0xFF000000),
    error = Color(0xFFFF2A2A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF8A80),
    onErrorContainer = Color(0xFF000000),
    background = Color(0xFFFFFDF5), // Off-white brutal canvas
    onBackground = Color(0xFF000000), // Solid pitch black
    surface = Color(0xFFFFFFFF), // Crisp card surface
    onSurface = Color(0xFF000000), // Solid pitch black
    surfaceVariant = Color(0xFFFFF9D2), // Canary yellow tint
    onSurfaceVariant = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFFF7F5ED),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFF4B8),
    surfaceContainerHighest = Color(0xFFFFE500),
    outline = Color(0xFF000000), // Solid stark black outlines
    outlineVariant = Color(0xFF000000)
)

fun themePreviewColor(skin: ThemeSkin): Color {
    return when (skin) {
        ThemeSkin.ORIGINAL -> Color(0xFFFF9500)
        ThemeSkin.RETRO_DARK -> AppleBlue
        ThemeSkin.NEO_BRUTALISM -> Color(0xFFFFE500)
    }
}

fun colorSchemeForSkin(skin: ThemeSkin): ColorScheme {
    return when (skin) {
        ThemeSkin.ORIGINAL -> OriginalColorScheme
        ThemeSkin.RETRO_DARK -> RetroDarkColorScheme
        ThemeSkin.NEO_BRUTALISM -> NeoBrutalismColorScheme
    }
}

data class MacroColors(
    val calories: Color,
    val carbs: Color,
    val protein: Color,
    val fat: Color
)

fun macroColorsForSkin(skin: ThemeSkin): MacroColors {
    return when (skin) {
        ThemeSkin.ORIGINAL -> MacroColors(
            calories = Color(0xFFFF9500),
            carbs = Color(0xFFFFD60A),
            protein = Color(0xFF30D158),
            fat = Color(0xFFFFFFFF)
        )
        ThemeSkin.RETRO_DARK -> MacroColors(
            calories = AppleOrange,
            carbs = AppleYellow,
            protein = AppleGreen,
            fat = Color(0xFF64D2FF)
        )
        ThemeSkin.NEO_BRUTALISM -> MacroColors(
            calories = Color(0xFFFF3366), // Saturated coral
            carbs = Color(0xFFFFB800),    // Electric gold
            protein = Color(0xFF00C853),  // Vibrant acid green
            fat = Color(0xFF00B0FF)       // Electric blue
        )
    }
}

val LocalMacroColors = staticCompositionLocalOf {
    macroColorsForSkin(ThemeSkin.ORIGINAL)
}

val MaterialTheme.macroColors: MacroColors
    @Composable
    get() = LocalMacroColors.current

