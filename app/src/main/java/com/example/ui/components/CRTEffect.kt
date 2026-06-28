package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.MusicViewModel
import androidx.compose.animation.core.*
import kotlin.random.Random

@Composable
fun CRTEffect(modifier: Modifier = Modifier, viewModel: MusicViewModel = viewModel()) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    // Animation for moving scanlines when playing
    val infiniteTransition = rememberInfiniteTransition()
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val glitchTrigger by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 1f
        val space = 6f
        val count = (size.height / space).toInt()

        // Scanlines
        val offset = if (isPlaying) scanlineOffset else 0f
        for (i in 0 until count) {
            val y = i * space + offset
            if (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
        }

        // Cyberpunk CSS Glitch effect (RGB Split / Displacement) when active playback
        if (isPlaying && glitchTrigger > 0.95f) { // Random rare glitch
            val glitchHeight = Random.nextFloat() * 20f + 10f
            val glitchY = Random.nextFloat() * size.height
            val shift = Random.nextFloat() * 10f - 5f // X offset
            
            // Cyan shift
            drawRect(
                color = Color.Cyan.copy(alpha = 0.15f),
                topLeft = Offset(shift, glitchY),
                size = androidx.compose.ui.geometry.Size(size.width, glitchHeight)
            )
            // Magenta shift
            drawRect(
                color = Color.Magenta.copy(alpha = 0.15f),
                topLeft = Offset(-shift, glitchY + 5f),
                size = androidx.compose.ui.geometry.Size(size.width, glitchHeight)
            )
        }
    }
}
