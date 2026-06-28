package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CyberpunkColorScheme =
  darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    tertiary = PrimaryGreen,
    background = DeepVoid,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = HoloBg,
    onSurfaceVariant = TextGray,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Always dark theme for this aesthetic
  dynamicColor: Boolean = false, // Disable dynamic color to enforce aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = CyberpunkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
