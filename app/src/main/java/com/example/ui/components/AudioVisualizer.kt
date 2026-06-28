package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonMagenta

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    style: String = "BARS",
    primaryColor: Color = CyberCyan,
    secondaryColor: Color = NeonMagenta
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Wave movement
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseShift"
    )
    
    // Pulse animation
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val bars = List(7) { index ->
        val target = if (isPlaying) 1f else 0.15f
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 300 + (index * 120),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        when (style) {
            "WAVE" -> {
                val path = Path()
                val points = 60
                val activeAmp = if (isPlaying) h * 0.45f else h * 0.1f
                
                for (i in 0..points) {
                    val x = w * i / points
                    // Calculate y using sine wave with phase shift
                    val angle = (i.toFloat() / points) * 4 * Math.PI.toFloat() + phaseShift
                    val y = (h / 2) + Math.sin(angle.toDouble()).toFloat() * activeAmp
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            "ORBIT" -> {
                val center = Offset(w / 2, h / 2)
                val baseRadius = Math.min(w, h) * 0.35f
                val scale = if (isPlaying) pulseFactor else 1.0f
                
                // Outer circle
                drawCircle(
                    color = primaryColor,
                    radius = baseRadius * scale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Inner circle
                drawCircle(
                    color = secondaryColor,
                    radius = baseRadius * 0.6f * (2f - scale),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                
                // Dynamic rays
                if (isPlaying) {
                    val rays = 8
                    for (i in 0 until rays) {
                        val angle = (i * 2 * Math.PI / rays) + (phaseShift / 2)
                        val startLen = baseRadius * 0.4f
                        val endLen = baseRadius * 1.1f * scale
                        
                        val startX = center.x + (startLen * Math.cos(angle)).toFloat()
                        val startY = center.y + (startLen * Math.sin(angle)).toFloat()
                        
                        val endX = center.x + (endLen * Math.cos(angle)).toFloat()
                        val endY = center.y + (endLen * Math.sin(angle)).toFloat()
                        
                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            "NEON_PULSE" -> {
                val center = Offset(w / 2, h / 2)
                val baseRadius = Math.min(w, h) * 0.22f
                val scale = if (isPlaying) pulseFactor else 1.0f
                
                // Draw a pulsing glowing background halo (purple)
                drawCircle(
                    color = Color(0xFFB026FF).copy(alpha = 0.12f * scale),
                    radius = baseRadius * 2.2f * scale,
                    center = center
                )
                
                // Outer glowing purple ring
                drawCircle(
                    color = Color(0xFFB026FF).copy(alpha = 0.4f),
                    radius = baseRadius * 1.3f * scale,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFB026FF),
                    radius = baseRadius * 1.3f * scale,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                
                // Inner neon green pulsing ring
                drawCircle(
                    color = Color(0xFF00FF41).copy(alpha = 0.25f),
                    radius = baseRadius * (1.8f - scale),
                    center = center,
                    style = Stroke(width = 6.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00FF41),
                    radius = baseRadius * (1.8f - scale),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                
                // Projecting neon green frequency rays
                val raysCount = 20
                for (i in 0 until raysCount) {
                    val angle = (i * 2 * Math.PI / raysCount) + (phaseShift * 0.45)
                    val barHeightFactor = if (isPlaying) {
                        val offsetTime = phaseShift + (i * 0.4f)
                        (Math.sin(offsetTime.toDouble()).toFloat() + 1f) / 2f
                    } else {
                        0.15f
                    }
                    val startLen = baseRadius * 1.3f * scale
                    val maxRayLen = Math.min(w, h) * 0.35f
                    val endLen = startLen + (maxRayLen * barHeightFactor)
                    
                    val startX = center.x + (startLen * Math.cos(angle)).toFloat()
                    val startY = center.y + (startLen * Math.sin(angle)).toFloat()
                    
                    val endX = center.x + (endLen * Math.cos(angle)).toFloat()
                    val endY = center.y + (endLen * Math.sin(angle)).toFloat()
                    
                    // Draw outer ray with glow (purple/neon glow)
                    drawLine(
                        color = Color(0xFFB026FF).copy(alpha = 0.35f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 5.dp.toPx()
                    )
                    // Draw inner core ray (neon green)
                    drawLine(
                        color = Color(0xFF00FF41),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
            else -> { // "BARS"
                val barCount = bars.size
                val barWidth = w / (barCount * 1.8f - 0.8f)
                
                bars.forEachIndexed { index, anim ->
                    val x = index * barWidth * 1.8f
                    val height = h * anim.value
                    val y = h - height
                    
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, height)
                    )
                }
            }
        }
    }
}
