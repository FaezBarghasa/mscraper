package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = AccentCyan,
    background = ObsidianBlack,
    surface = SurfaceElevated,
    surfaceVariant = SurfaceHigh,
    onPrimary = ObsidianBlack,
    onSecondary = ObsidianBlack,
    onTertiary = ObsidianBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = SurfaceGlass
)

@Composable
fun MscraperTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
