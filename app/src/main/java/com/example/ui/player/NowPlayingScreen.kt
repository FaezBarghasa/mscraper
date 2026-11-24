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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextGray
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

    val haptic = LocalHapticFeedback.current
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

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
    val displayProgress = if (isScrubbing) scrubProgress else progress
    val currentPositionMs = (displayProgress * durationSeconds * 1000).toLong()
    val durationMs = durationSeconds * 1000

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 280f),
        label = "ArtScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animated Album Art
        AnimatedContent(
            targetState = currentTrack?.imageUrl,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f)) togetherWith
                    (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.92f))
            },
            label = "AlbumArt"
        ) { imageUrl ->
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(artScale)
                    .shadow(
                        elevation = if (isPlaying) 24.dp else 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = accentColor.copy(alpha = 0.4f),
                        spotColor = accentColor.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(96.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Animated Track Info
        AnimatedContent(
            targetState = currentTrack,
            transitionSpec = {
                fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
            },
            label = "TrackInfo"
        ) { track ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = track?.title ?: "No Signal Detected",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track?.artist ?: "Unknown Origin",
                    color = accentColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Scrubber (Debounced seek: seekToProgress only on drag finish)
        Slider(
            value = displayProgress,
            onValueChange = {
                isScrubbing = true
                scrubProgress = it.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                musicViewModel.seekToProgress(scrubProgress)
                isScrubbing = false
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPositionMs), color = TextGray, style = MaterialTheme.typography.labelMedium)
            Text(formatTime(durationMs), color = TextGray, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    musicViewModel.toggleShuffle()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) accentColor else TextGray
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    musicViewModel.previousTrack()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    musicViewModel.togglePlayback()
                },
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = accentColor,
                contentColor = DeepVoid
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    musicViewModel.nextTrack()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(40.dp))
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    musicViewModel.toggleRepeat()
                },
                modifier = Modifier.size(48.dp)
            ) {
                val icon = when (repeatMode) {
                    com.example.viewmodel.RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != com.example.viewmodel.RepeatMode.NONE) accentColor else TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
