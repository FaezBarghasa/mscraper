package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = Color(0xFF00E5FF),
    blurRadius: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape
            )
            .blur(blurRadius),
        content = content
    )
}
