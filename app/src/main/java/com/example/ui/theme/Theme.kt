package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = ElegantDarkBg,
    primaryContainer = ElegantDarkSurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = ElegantDarkTextSecondary,
    onSecondary = ElegantDarkBg,
    secondaryContainer = ElegantDarkSurfaceHighlight,
    onSecondaryContainer = PureWhite,
    tertiary = AccentGreen,
    onTertiary = ElegantDarkBg,
    background = ElegantDarkBg,
    onBackground = ElegantDarkTextPrimary,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkTextPrimary,
    surfaceVariant = ElegantDarkSurfaceElevated,
    onSurfaceVariant = ElegantDarkTextSecondary,
    outline = ElegantDarkBorder,
    outlineVariant = ElegantDarkSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = TextPrimaryLight,
    onPrimary = PureWhite,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = TextPrimaryLight,
    secondary = TextSecondaryLight,
    onSecondary = PureWhite,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = AccentGreen,
    onTertiary = PureWhite,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = LightBorder
)

@Composable
fun AwayTimeTheme(
    themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true // Dark first by default
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

