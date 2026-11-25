package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Q4 2026 Obsidian + Apple Music / Metrolist Design Tokens
// ==========================================

// Core Surfaces (OLED True Obsidian)
val ObsidianBlack = Color(0xFF08080A)       // Base window canvas
val DeepVoid = Color(0xFF0D0E12)            // Secondary background
val SurfaceElevated = Color(0xFF14151B)     // Floating cards & containers
val SurfaceHigh = Color(0xFF1C1D24)         // Hovered / active tiles
val SurfaceGlass = Color(0x1AFFFFFF)        // Frosted glass background (10%)
val GlassWhite = Color(0x14FFFFFF)          // Translucent pill fill (8%)
val HairlineBorder = Color(0x14FFFFFF)      // 8% white hairline border

// Accent Colors (Apple Music Electric Red & Emerald Green & Cyan)
val ElectricRed = Color(0xFFFA2D48)         // Apple Music Signature
val SpotifyEmerald = Color(0xFF1DB954)      // Spotify Emerald
val AccentCyan = Color(0xFF00E5FF)          // Crisp Cyan
val AccentViolet = Color(0xFF8B5CF6)        // Ambient Violet
val AccentAmber = Color(0xFFF59E0B)         // Warm Accent

// Primary / Secondary Brand Tokens
val PrimaryAccent = ElectricRed
val SecondaryAccent = SpotifyEmerald

// Legacy aliases mapped to elegant dark tokens to maintain backward compatibility
val CyberDark = ObsidianBlack
val CyberGray = SurfaceElevated
val CyberCyan = AccentCyan
val NeonCyan = AccentCyan
val NeonMagenta = ElectricRed
val NeonGreen = SpotifyEmerald
val PrimaryGreen = SpotifyEmerald
val NeonYellow = AccentAmber
val WarningYellow = AccentAmber
val HoloBg = SurfaceGlass
val HoloBorder = HairlineBorder
val TokyoBlue = Color(0xFF3B82F6)
val TokyoPink = Color(0xFFEC4899)
val GlitchGreen = Color(0xFF10B981)
val AcidRed = Color(0xFFEF4444)

// Neutral Text Hierarchy
val TextPrimary = Color(0xFFF8FAFC)        // 98% Crisp White
val TextSecondary = Color(0xFF94A3B8)      // 64% Muted Slate
val TextTertiary = Color(0xFF64748B)       // 45% Subdued Slate
val TextGray = TextSecondary
