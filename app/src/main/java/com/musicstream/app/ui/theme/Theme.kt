package com.musicstream.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

// 0. Custom Extended Colors for App-Specific UI
data class MusicStreamColors(
    val navBarBackground: Color,
    val navBarActive: Color,
    val navBarInactive: Color,
    val seekBarActive: Color,
    val seekBarInactive: Color,
    val favoriteActive: Color,
    val favoriteInactive: Color,
    val signOutButton: Color,
    val featuredGradientStart: Color,
    val featuredGradientEnd: Color
)

private val DarkAppColors = MusicStreamColors(
    navBarBackground = DarkBackground,
    navBarActive = PalettePrimaryRed, // Maps to BDC3C7
    navBarInactive = TextTertiaryDark,
    seekBarActive = PalettePrimaryRed,
    seekBarInactive = DarkElevated,
    favoriteActive = FavoriteRed,
    favoriteInactive = TextTertiaryDark,
    signOutButton = FavoriteRed,
    featuredGradientStart = PalettePrimaryRed,
    featuredGradientEnd = PaletteDarkBlue
)

private val LightAppColors = MusicStreamColors(
    navBarBackground = LightSurface,
    navBarActive = PalettePrimaryRed,
    navBarInactive = InactiveGrey,
    seekBarActive = PalettePrimaryRed,
    seekBarInactive = LightElevated,
    favoriteActive = FavoriteRed,
    favoriteInactive = InactiveGrey,
    signOutButton = FavoriteRed,
    featuredGradientStart = PalettePrimaryRed,
    featuredGradientEnd = PaletteDarkBlue
)

val LocalMusicStreamColors = staticCompositionLocalOf { DarkAppColors }

object MusicStreamTheme {
    val colors: MusicStreamColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMusicStreamColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

// 1. Dark Mode Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = PalettePrimaryRed,
    onPrimary = TextPrimaryDark,
    secondary = PaletteDarkBlue,
    onSecondary = TextPrimaryDark,
    tertiary = FavoriteRed,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = PaletteGrey
)

// 2. Light Mode Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = PalettePrimaryRed,
    onPrimary = Color.White,
    secondary = PaletteDarkBlue,
    onSecondary = Color.White,
    tertiary = FavoriteRed,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = InactiveGrey
)

@Composable
fun MusicStreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    // Automatically setting status bar colors based on theme choice
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = appColors.navBarBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography // Ensure this matches your custom Typography object in Type.kt
    ) {
        CompositionLocalProvider(
            LocalMusicStreamColors provides appColors,
            LocalTextStyle provides Typography.bodyLarge
        ) {
            ProvideTextStyle(value = Typography.bodyLarge) {
                content()
            }
        }
    }
}