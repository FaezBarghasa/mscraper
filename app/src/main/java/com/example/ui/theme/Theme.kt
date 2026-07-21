package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonMagenta,
    tertiary = NeonGreen,
    background = CyberDark,
    surface = CyberGray,
    onPrimary = CyberDark,
    onSecondary = CyberDark,
    onTertiary = CyberDark,
    onBackground = NeonCyan,
    onSurface = NeonCyan
)

@Composable
fun MscraperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MscraperTypography,
        content = content
    )
}
