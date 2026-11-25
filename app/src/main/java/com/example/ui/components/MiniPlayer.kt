package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel

/**
 * Q4 2026 Frosted Glass Floating MiniPlayer
 * Features:
 * - Ultra-clean 60.dp height with 16.dp rounded corners
 * - Ambient artwork color tint over dark obsidian glass
 * - Horizontal swipe-to-skip with haptic feedback
 * - Interactive favorite & play/pause buttons
 * - Slim hairline progress bar indicator at bottom
 */
@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onNavigateToNowPlaying: () -> Unit,
    isVisible: Boolean
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val isFavorite = remember(currentTrack, favoriteTracks) {
        currentTrack?.let { track -> favoriteTracks.any { it.id == track.id } } ?: false
    }
    val haptic = LocalHapticFeedback.current

    val ambientPalette = rememberAmbientPalette(currentTrack?.imageUrl)
    val accentColor = ambientPalette.dominant

    AnimatedVisibility(
        visible = isVisible && currentTrack != null,
        enter = slideInVertically(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
            initialOffsetY = { it }
        ),
        exit = slideOutVertically(
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
            targetOffsetY = { it }
        )
    ) {
        currentTrack?.let { track ->
            var swipeOffset by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = accentColor.copy(alpha = 0.35f),
                        spotColor = accentColor.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SurfaceElevated.copy(alpha = 0.95f),
                                ObsidianBlack.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                accentColor.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onNavigateToNowPlaying() }
                    .pointerInput(track.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset < -60f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.nextTrack()
                                } else if (swipeOffset > 60f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.previousTrack()
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = { swipeOffset = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                swipeOffset += dragAmount
                            }
                        )
                    }
            ) {
                // Main Row Content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Cover Thumbnail
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = track.imageUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track Metadata
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Action: Favorite Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleFavorite(track)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) ElectricRed else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Action: Play / Pause Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.togglePlayback()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.20f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Action: Next Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.nextTrack()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Sleek bottom hairline playback progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
            }
        }
    }
}
