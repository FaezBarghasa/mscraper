package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006D77),
    secondary = Color(0xFF9E1946),
    tertiary = Color(0xFF2D6A4F),
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0B0E14),
    onSurface = Color(0xFF0B0E14)
)

@Composable
fun MscraperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
