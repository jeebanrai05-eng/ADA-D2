package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AdaHudColorScheme = darkColorScheme(
    primary = HudCyan,
    onPrimary = HudObsidian,
    primaryContainer = HudPanelElevated,
    onPrimaryContainer = HudIceWhite,
    secondary = HudSkyBlue,
    onSecondary = HudObsidian,
    secondaryContainer = HudPanelSurface,
    onSecondaryContainer = HudMutedText,
    tertiary = HudAmber,
    onTertiary = HudObsidian,
    background = HudObsidian,
    onBackground = HudIceWhite,
    surface = HudDeepNavy,
    onSurface = HudIceWhite,
    surfaceVariant = HudPanelSurface,
    onSurfaceVariant = HudMutedText,
    error = HudCrimson,
    onError = Color.White,
    outline = HudCyanDim
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AdaHudColorScheme,
        typography = Typography,
        content = content
    )
}
