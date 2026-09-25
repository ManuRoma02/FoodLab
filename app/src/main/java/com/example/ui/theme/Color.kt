package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

// 2. TITANIUM: Apple Style Pro Titanium with electric cyan/blue and graphite
val TitaniumColorScheme = darkColorScheme(
    primary = Color(0xFF2997FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00458E),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = AppleGreen,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF0F5B24),
    onSecondaryContainer = Color(0xFFB4F3C6),
    tertiary = Color(0xFFE5A93C),
    onTertiary = Color(0xFF2E1C00),
    tertiaryContainer = Color(0xFF5A3B00),
    onTertiaryContainer = Color(0xFFFFE0A3),
    error = AppleRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFD4D1),
    background = Color(0xFF121316),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF181A1E),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2A2E36),
    onSurfaceVariant = Color(0xFFA1A1A6),
    surfaceContainerLow = Color(0xFF15171A),
    surfaceContainer = Color(0xFF1E2025),
    surfaceContainerHigh = Color(0xFF262930),
    surfaceContainerHighest = Color(0xFF30343C),
    outline = Color(0xFF484C55),
    outlineVariant = Color(0xFF2A2D34)
)

// 3. NORD: Nordic Frost - Polar Night, Frost Ice Blue & Snow Storm
val NordColorScheme = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF242933),
    primaryContainer = Color(0xFF3B5663),
    onPrimaryContainer = Color(0xFFD8EEF4),
    secondary = Color(0xFFA3BE8C),
    onSecondary = Color(0xFF242933),
    secondaryContainer = Color(0xFF3E5434),
    onSecondaryContainer = Color(0xFFE2F0D9),
    tertiary = Color(0xFFEBCB8B),
    onTertiary = Color(0xFF2E2413),
    tertiaryContainer = Color(0xFF5E4F28),
    onTertiaryContainer = Color(0xFFFFF0D4),
    error = Color(0xFFBF616A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF5C292E),
    onErrorContainer = Color(0xFFFFD8DC),
    background = Color(0xFF242933),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF434C5E),
    onSurfaceVariant = Color(0xFFD8DEE9),
    surfaceContainerLow = Color(0xFF292E3A),
    surfaceContainer = Color(0xFF3B4252),
    surfaceContainerHigh = Color(0xFF434C5E),
    surfaceContainerHighest = Color(0xFF4C566A),
    outline = Color(0xFF616E85),
    outlineVariant = Color(0xFF3B4252)
)

// 4. CATPPUCCIN: Deep Mauve / Catppuccin Mocha velvety pastel dark
val CatppuccinColorScheme = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF181825),
    primaryContainer = Color(0xFF553B82),
    onPrimaryContainer = Color(0xFFF1E4FD),
    secondary = Color(0xFFA6E3A1),
    onSecondary = Color(0xFF181825),
    secondaryContainer = Color(0xFF325430),
    onSecondaryContainer = Color(0xFFDCF8DA),
    tertiary = Color(0xFFFAB387),
    onTertiary = Color(0xFF2E190E),
    tertiaryContainer = Color(0xFF633A20),
    onTertiaryContainer = Color(0xFFFFE3D1),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF181825),
    errorContainer = Color(0xFF6B2939),
    onErrorContainer = Color(0xFFFFD9E2),
    background = Color(0xFF181825),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    surfaceContainerLow = Color(0xFF1B1B2A),
    surfaceContainer = Color(0xFF252538),
    surfaceContainerHigh = Color(0xFF313244),
    surfaceContainerHighest = Color(0xFF45475A),
    outline = Color(0xFF585B70),
    outlineVariant = Color(0xFF313244)
)

// 5. WARM_EMBER: Cozy roast espresso & warm terracotta ember
val WarmEmberColorScheme = darkColorScheme(
    primary = Color(0xFFF08C54),
    onPrimary = Color(0xFF2D1600),
    primaryContainer = Color(0xFF6E3612),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFF7CB88B),
    onSecondary = Color(0xFF122817),
    secondaryContainer = Color(0xFF2A5034),
    onSecondaryContainer = Color(0xFFCEECD6),
    tertiary = Color(0xFFE6BA67),
    onTertiary = Color(0xFF2E2000),
    tertiaryContainer = Color(0xFF5E4717),
    onTertiaryContainer = Color(0xFFFFEBBF),
    error = Color(0xFFE55959),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF6B2222),
    onErrorContainer = Color(0xFFFFD7D7),
    background = Color(0xFF161311),
    onBackground = Color(0xFFF5EBE1),
    surface = Color(0xFF1F1A17),
    onSurface = Color(0xFFF5EBE1),
    surfaceVariant = Color(0xFF332B25),
    onSurfaceVariant = Color(0xFFC4B6AA),
    surfaceContainerLow = Color(0xFF1A1613),
    surfaceContainer = Color(0xFF26201C),
    surfaceContainerHigh = Color(0xFF322B25),
    surfaceContainerHighest = Color(0xFF3F3730),
    outline = Color(0xFF5E5248),
    outlineVariant = Color(0xFF332B25)
)

fun themePreviewColor(skin: ThemeSkin): Color {
    return when (skin) {
        ThemeSkin.ORIGINAL -> Color(0xFFFF9500)
        ThemeSkin.RETRO_DARK -> AppleBlue
        ThemeSkin.TITANIUM -> Color(0xFF2997FF)
        ThemeSkin.NORD -> Color(0xFF88C0D0)
        ThemeSkin.CATPPUCCIN -> Color(0xFFCBA6F7)
        ThemeSkin.WARM_EMBER -> Color(0xFFF08C54)
    }
}

fun colorSchemeForSkin(skin: ThemeSkin): ColorScheme {
    return when (skin) {
        ThemeSkin.ORIGINAL -> OriginalColorScheme
        ThemeSkin.RETRO_DARK -> RetroDarkColorScheme
        ThemeSkin.TITANIUM -> TitaniumColorScheme
        ThemeSkin.NORD -> NordColorScheme
        ThemeSkin.CATPPUCCIN -> CatppuccinColorScheme
        ThemeSkin.WARM_EMBER -> WarmEmberColorScheme
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
        ThemeSkin.TITANIUM -> MacroColors(
            calories = Color(0xFF2997FF),
            carbs = Color(0xFFE5A93C),
            protein = AppleGreen,
            fat = Color(0xFFA1A1A6)
        )
        ThemeSkin.NORD -> MacroColors(
            calories = Color(0xFF88C0D0),
            carbs = Color(0xFFEBCB8B),
            protein = Color(0xFFA3BE8C),
            fat = Color(0xFFD8DEE9)
        )
        ThemeSkin.CATPPUCCIN -> MacroColors(
            calories = Color(0xFFCBA6F7),
            carbs = Color(0xFFFAB387),
            protein = Color(0xFFA6E3A1),
            fat = Color(0xFFCDD6F4)
        )
        ThemeSkin.WARM_EMBER -> MacroColors(
            calories = Color(0xFFF08C54),
            carbs = Color(0xFFE6BA67),
            protein = Color(0xFF7CB88B),
            fat = Color(0xFFF5EBE1)
        )
    }
}

val LocalMacroColors = staticCompositionLocalOf {
    macroColorsForSkin(ThemeSkin.ORIGINAL)
}

val MaterialTheme.macroColors: MacroColors
    @Composable
    get() = LocalMacroColors.current

