package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.util.ThemeSkin

private val DarkColorScheme = darkColorScheme(
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
    outline = Outline,
    outlineVariant = OutlineVariant
)

val LocalThemeSkin = staticCompositionLocalOf { ThemeSkin.ORIGINAL }

val MaterialTheme.isNeoBrutalist: Boolean
    @Composable
    get() = LocalThemeSkin.current == ThemeSkin.NEO_BRUTALISM

val MaterialTheme.isBentoCraft: Boolean
    @Composable
    get() = LocalThemeSkin.current == ThemeSkin.BENTO_CRAFT

val MaterialTheme.isNothingOs: Boolean
    @Composable
    get() = LocalThemeSkin.current == ThemeSkin.NOTHING_OS

val MaterialTheme.themeSkin: ThemeSkin
    @Composable
    get() = LocalThemeSkin.current

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for tactical feel
    dynamicColor: Boolean = false,
    themeSkin: ThemeSkin = ThemeSkin.ORIGINAL,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeForSkin(themeSkin)
    val macroColors = macroColorsForSkin(themeSkin)
    val shapes = when (themeSkin) {
        ThemeSkin.NEO_BRUTALISM -> NeoBrutalShapes
        ThemeSkin.BENTO_CRAFT -> BentoCraftShapes
        ThemeSkin.NOTHING_OS -> NothingShapes
        else -> Shapes
    }
    val typography = when (themeSkin) {
        ThemeSkin.NEO_BRUTALISM -> NeoBrutalTypography
        ThemeSkin.BENTO_CRAFT -> BentoCraftTypography
        ThemeSkin.NOTHING_OS -> NothingTypography
        else -> Typography
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val isLight = colorScheme.background.luminance() > 0.5f
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalMacroColors provides macroColors,
        LocalThemeSkin provides themeSkin
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
