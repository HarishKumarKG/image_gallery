package com.harish.gallery.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Neutral98,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue20,
    secondary = Teal40,
    tertiary = Coral40,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Coral20,
    background = Neutral98,
    surface = Neutral98,
    surfaceContainer = Neutral94,
    onSurface = Neutral10,
    onSurfaceVariant = NeutralVariant30
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    tertiary = Coral80,
    tertiaryContainer = Coral40,
    onTertiaryContainer = Coral90,
    background = Neutral6,
    surface = Neutral6,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    onSurface = Neutral90,
    onSurfaceVariant = NeutralVariant80
)

/** True while the dark theme is showing (for the few places that draw their own colours). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * The app's look. On Android 12+ it follows your wallpaper colours (Material You);
 * on older phones it uses the blue/coral brand colours.
 */
@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colors, typography = GalleryTypography, content = content)
    }
}
