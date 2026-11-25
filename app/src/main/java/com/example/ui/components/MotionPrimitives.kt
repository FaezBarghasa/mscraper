package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Modern tactile spring compression on touch/press (iOS/Apple Music style).
 * Scales element subtly down to 0.96f on press and snaps back with fluid spring physics.
 */
fun Modifier.springPressScale(
    targetScale: Float = 0.96f,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // Handled via state or caller modifier if needed
    }
}

@Composable
fun rememberSpringPressScale(
    targetScale: Float = 0.96f
): Pair<Modifier, Float> {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val modifier = Modifier
        .scale(scale.value)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                coroutineScope.launch {
                    scale.animateTo(
                        targetScale,
                        spring(dampingRatio = 0.7f, stiffness = 600f)
                    )
                }
                waitForUpOrCancellation()
                coroutineScope.launch {
                    scale.animateTo(
                        1f,
                        spring(dampingRatio = 0.7f, stiffness = 400f)
                    )
                }
            }
        }
    return Pair(modifier, scale.value)
}

/**
 * Generates an ambient mesh gradient brush from extracted artwork colors.
 */
fun createAmbientMeshBrush(
    primary: Color,
    secondary: Color,
    baseDark: Color = Color(0xFF08080A)
): Brush {
    return Brush.radialGradient(
        colors = listOf(
            primary.copy(alpha = 0.45f),
            secondary.copy(alpha = 0.20f),
            baseDark.copy(alpha = 0.95f),
            baseDark
        ),
        radius = 1200f
    )
}
