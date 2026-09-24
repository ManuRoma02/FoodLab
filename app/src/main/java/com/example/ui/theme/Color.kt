package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
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

// 2. MATRIX: Phosphor terminal green & dark digital rain
val MatrixColorScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color(0xFF002208),
    primaryContainer = Color(0xFF004D14),
    onPrimaryContainer = Color(0xFF66FF8E),
    secondary = Color(0xFF00C832),
    onSecondary = Color(0xFF002208),
    secondaryContainer = Color(0xFF00380D),
    onSecondaryContainer = Color(0xFF88FFA9),
    tertiary = Color(0xFF39FF14),
    onTertiary = Color(0xFF002208),
    tertiaryContainer = Color(0xFF0D4405),
    onTertiaryContainer = Color(0xFFA5FFA0),
    error = Color(0xFFFF5252),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF030A04),
    onBackground = Color(0xFFD6FFDC),
    surface = Color(0xFF081209),
    onSurface = Color(0xFFD6FFDC),
    surfaceVariant = Color(0xFF1B381F),
    onSurfaceVariant = Color(0xFF75B37D),
    surfaceContainerLow = Color(0xFF0B170C),
    surfaceContainer = Color(0xFF0F1E10),
    surfaceContainerHigh = Color(0xFF162A18),
    surfaceContainerHighest = Color(0xFF1F3821),
    outline = Color(0xFF2A6832),
    outlineVariant = Color(0xFF163E1B)
)

// 3. GAMEBOY: Classic 90s DMG handheld monochrome olive green & dark slate
val GameboyColorScheme = darkColorScheme(
    primary = Color(0xFF8BAC0F),
    onPrimary = Color(0xFF0F380F),
    primaryContainer = Color(0xFF306230),
    onPrimaryContainer = Color(0xFF9BBC0F),
    secondary = Color(0xFFAB47BC), // DMG A/B button purple
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF4A148C),
    onSecondaryContainer = Color(0xFFE1BEE7),
    tertiary = Color(0xFF9BBC0F),
    onTertiary = Color(0xFF0F380F),
    tertiaryContainer = Color(0xFF1B401B),
    onTertiaryContainer = Color(0xFFCADF75),
    error = Color(0xFFEF5350),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111417),
    onBackground = Color(0xFFE4EBD4),
    surface = Color(0xFF171B20),
    onSurface = Color(0xFFE4EBD4),
    surfaceVariant = Color(0xFF353D45),
    onSurfaceVariant = Color(0xFFA0ABA0),
    surfaceContainerLow = Color(0xFF1D2228),
    surfaceContainer = Color(0xFF232A31),
    surfaceContainerHigh = Color(0xFF2B333B),
    surfaceContainerHighest = Color(0xFF353E47),
    outline = Color(0xFF56626D),
    outlineVariant = Color(0xFF303942)
)

// 4. CYBERPUNK: Night City electric yellow, hot magenta & neon cyan
val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFFCEE0A),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF6B6500),
    onPrimaryContainer = Color(0xFFFFF780),
    secondary = Color(0xFF00F0FF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF004D54),
    onSecondaryContainer = Color(0xFF80F7FF),
    tertiary = Color(0xFFFF003C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF7A001D),
    onTertiaryContainer = Color(0xFFFF8A9E),
    error = Color(0xFFFF1744),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF070913),
    onBackground = Color(0xFFF0F3FF),
    surface = Color(0xFF0C101F),
    onSurface = Color(0xFFF0F3FF),
    surfaceVariant = Color(0xFF253052),
    onSurfaceVariant = Color(0xFFA2AEC9),
    surfaceContainerLow = Color(0xFF11172A),
    surfaceContainer = Color(0xFF161D35),
    surfaceContainerHigh = Color(0xFF1E2745),
    surfaceContainerHighest = Color(0xFF29355C),
    outline = Color(0xFF4C5D8B),
    outlineVariant = Color(0xFF222B47)
)

// 5. AMBER_CRT: Vintage mainframe amber glowing phosphor CRT
val AmberCrtColorScheme = darkColorScheme(
    primary = Color(0xFFFFB000),
    onPrimary = Color(0xFF331D00),
    primaryContainer = Color(0xFF6E4000),
    onPrimaryContainer = Color(0xFFFFDC8F),
    secondary = Color(0xFFFF8500),
    onSecondary = Color(0xFF301300),
    secondaryContainer = Color(0xFF612600),
    onSecondaryContainer = Color(0xFFFFBE85),
    tertiary = Color(0xFFFFCC00),
    onTertiary = Color(0xFF332600),
    tertiaryContainer = Color(0xFF594500),
    onTertiaryContainer = Color(0xFFFFEC99),
    error = Color(0xFFFF5252),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B0802),
    onBackground = Color(0xFFFFEED4),
    surface = Color(0xFF130E05),
    onSurface = Color(0xFFFFEED4),
    surfaceVariant = Color(0xFF3B2E18),
    onSurfaceVariant = Color(0xFFC4AD89),
    surfaceContainerLow = Color(0xFF191307),
    surfaceContainer = Color(0xFF20190B),
    surfaceContainerHigh = Color(0xFF2A2110),
    surfaceContainerHighest = Color(0xFF382D18),
    outline = Color(0xFF6E562F),
    outlineVariant = Color(0xFF3B2D17)
)

fun colorSchemeForSkin(skin: ThemeSkin): ColorScheme {
    return when (skin) {
        ThemeSkin.RETRO_DARK -> RetroDarkColorScheme
        ThemeSkin.MATRIX -> MatrixColorScheme
        ThemeSkin.GAMEBOY -> GameboyColorScheme
        ThemeSkin.CYBERPUNK -> CyberpunkColorScheme
        ThemeSkin.AMBER_CRT -> AmberCrtColorScheme
    }
}
