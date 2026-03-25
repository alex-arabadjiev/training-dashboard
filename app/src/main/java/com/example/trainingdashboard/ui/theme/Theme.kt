package com.example.trainingdashboard.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KineticDarkColorScheme = darkColorScheme(
    primary = KineticGreen,
    onPrimary = KineticBackground,
    secondary = KineticBlue,
    onSecondary = KineticBackground,
    tertiary = KineticBlueBright,
    onTertiary = KineticBackground,
    background = KineticBackground,
    onBackground = KineticOnBackground,
    surface = KineticSurface,
    onSurface = KineticOnSurface,
    surfaceVariant = KineticSurfaceContainer,
    onSurfaceVariant = KineticOnSurfaceVariant,
    surfaceContainer = KineticSurfaceContainer,
    surfaceContainerHigh = KineticSurfaceContainerHigh,
    surfaceContainerHighest = KineticSurfaceContainerHighest,
    error = KineticError,
    onError = KineticOnError,
    errorContainer = KineticErrorContainer,
    onErrorContainer = KineticOnErrorContainer,
    outline = KineticOutline,
    outlineVariant = KineticOutlineVariant
)

@Composable
fun TrainingDashboardTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = KineticDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KineticBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
