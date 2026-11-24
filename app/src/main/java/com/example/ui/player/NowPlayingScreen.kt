package com.example.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.DeepVoid
import com.example.viewmodel.MusicViewModel

@androidx.media3.common.util.UnstableApi
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    musicViewModel: MusicViewModel = viewModel()
) {
    val currentTrack by musicViewModel.currentTrack.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val progress by musicViewModel.playbackProgress.collectAsState()
    val isShuffleEnabled by musicViewModel.shuffleEnabled.collectAsState()
    val repeatMode by musicViewModel.repeatMode.collectAsState()
    val accentColor by musicViewModel.themePrimary.collectAsState()

    // Derive approximate position from progress and duration string
    val durationSeconds = remember(currentTrack) {
        val d = currentTrack?.duration ?: "0:00"
        try {
            val parts = d.split(":")
            if (parts.size == 2) parts[0].toLong() * 60 + parts[1].toLong() else 210L
        } catch (_: Exception) {
            210L
        }
    }
    val currentPositionMs = (progress * durationSeconds * 1000).toLong()
    val durationMs = durationSeconds * 1000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Animated Album Art
        AnimatedContent(
            targetState = currentTrack?.imageUrl,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f)) togetherWith 
                (fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.9f))
            },
            label = "AlbumArt"
        ) { imageUrl ->
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Animated Track Info
        AnimatedContent(
            targetState = currentTrack,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "TrackInfo"
        ) { track ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = track?.title ?: "No Signal Detected",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = track?.artist ?: "Unknown Origin",
                    color = accentColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Scrubber
        Slider(
            value = currentPositionMs.toFloat(),
            onValueChange = { 
                val newProgress = (it / durationMs.toFloat()).coerceIn(0f, 1f)
                musicViewModel.seekToProgress(newProgress)
            },
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPositionMs), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(formatTime(durationMs), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { musicViewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) accentColor else Color.Gray
                )
            }
            
            IconButton(onClick = { musicViewModel.previousTrack() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            Surface(
                onClick = { musicViewModel.togglePlayback() },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = accentColor,
                contentColor = DeepVoid
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            IconButton(onClick = { musicViewModel.nextTrack() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            IconButton(onClick = { musicViewModel.toggleRepeat() }) {
                val icon = when (repeatMode) {
                    com.example.viewmodel.RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != com.example.viewmodel.RepeatMode.NONE) accentColor else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
