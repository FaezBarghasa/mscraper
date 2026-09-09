package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan

@Composable
fun LyricsDisplay(
    progress: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = CyberCyan
) {
    val lyrics = listOf(
        "Initializing synaptic bridge...",
        "Receiving high-fidelity audio frequencies...",
        "Neon lights reflecting in the street rain,",
        "Digital heartbeats sync to the deep bass.",
        "We are the runners of the endless cyber grid,",
        "Escaping the system in search of our dreams.",
        "Signal decrypted. Holographic state aligned."
    )

    // Map progress (0f to 1f) to the current line index
    val activeIndex = (progress * lyrics.size).toInt().coerceIn(0, lyrics.size - 1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            // We show the previous line, active line, and next line for a highly realistic and polished AAA experience!
            val prevLine = lyrics.getOrNull(activeIndex - 1) ?: ""
            val currLine = lyrics[activeIndex]
            val nextLine = lyrics.getOrNull(activeIndex + 1) ?: ""

            // Previous Line (Subtle Fade)
            if (prevLine.isNotEmpty()) {
                Text(
                    text = prevLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Current Active Line (Highlight & Glowing/Bold)
            Text(
                text = currLine,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    shadow = Shadow(
                        color = primaryColor,
                        offset = Offset(0f, 0f),
                        blurRadius = 16f
                    )
                ),
                color = Color.White, // White core for the neon glow
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            )

            // Next Line (Subtle Fade)
            if (nextLine.isNotEmpty()) {
                Text(
                    text = nextLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
