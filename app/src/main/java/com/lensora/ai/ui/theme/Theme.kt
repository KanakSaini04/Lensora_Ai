package com.lensora.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ObsidianRoyalColorScheme = darkColorScheme(
    primary = RoyalBlue,
    onPrimary = ColorWhite,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = ColorWhite,
    secondary = ColorOffWhite,
    onSecondary = PitchBlackBackground,
    background = PitchBlackBackground,
    onBackground = ColorWhite,
    surface = CharcoalSurface,
    onSurface = ColorWhite,
    surfaceVariant = CharcoalSurface,
    onSurfaceVariant = ColorOffWhite,
    outline = ColorDarkBorder,
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun Lensora_AiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = ObsidianRoyalColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}