package com.attendance.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.foundation.isSystemInDarkTheme

val LocalIsDarkMode = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = PrimaryGreenDark,
    secondary = PrimaryGreen,
    onSecondary = Color.White,
    secondaryContainer = Color.White,
    onSecondaryContainer = PrimaryGreenDark,
    tertiary = LateOrange,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color.White,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = Color.White,
    error = AbsentRed,
    onError = Color.White,
    outline = DividerColor,
    outlineVariant = DividerColor
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen, // Original Primary Green
    onPrimary = Color.White,
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = Color.White,
    secondary = PrimaryGreen,
    onSecondary = Color.White,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = PrimaryGreen,
    tertiary = LateOrange,
    onTertiary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = Color.Transparent,
    error = AbsentRed,
    onError = Color.White,
    outline = DividerColorDark,
    outlineVariant = DividerColorDark
)

@Composable
fun AttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            
            // Allow content to be drawn under system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    CompositionLocalProvider(LocalIsDarkMode provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background
            ) {
                content()
            }
        }
    }
}
