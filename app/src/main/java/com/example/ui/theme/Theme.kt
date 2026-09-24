package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GamingDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BackgroundDark,
    primaryContainer = CardBackgroundElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonGreen,
    onSecondary = BackgroundDark,
    secondaryContainer = CardBackgroundElevated,
    onSecondaryContainer = NeonGreen,
    tertiary = NeonOrange,
    onTertiary = BackgroundDark,
    tertiaryContainer = CardBackgroundElevated,
    onTertiaryContainer = NeonOrange,
    error = NeonRed,
    onError = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun MemeMicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Gaming utilities are best suited with immersive high-contrast dark theme
    val colorScheme = GamingDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BackgroundDark.toArgb()
                window.navigationBarColor = BackgroundDark.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
