package com.example.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.draw.alpha
import coil.compose.AsyncImage
import kotlin.random.Random
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.model.Track
import com.example.ui.components.AudioVisualizer
import com.example.ui.components.LyricsDisplay
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel

@Composable
fun NowPlayingScreen(navController: NavController, viewModel: MusicViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val favTracks by viewModel.favoriteTracks.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val isShuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    val track = currentTrack ?: Track(
        id = "1",
        title = "Midnight City",
        artist = "M83",
        duration = "4:03",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB09eN9Abqx8XFiF0yRGIA-y-ze1ARziS7SRJmsUyDM-dEFiMMmtjaZvLlPls6eoYtzfSshP5uia61r9QIBjpJe2rzBPb24TFvIHgjgmvzi8R-GOcc73J5ZccqGqS2jfhvBIXKEtxZusjvEVSctlJKrjxUqd0reEqu2cOA7_hMfyxu9jm_8W7XaKgvsi7v4UZh5Qq6HfrEUM8wHJMwj6ZtxO5IxbOyDp97-SKvuehyF9afHmGJdNgmNPCBxhkwV67qqmmsQuG7ivMyb"
    )

    val backgroundUrl = remember(track.title) {
        val hash = kotlin.math.abs(track.title.hashCode()) % 3
        when (hash) {
            0 -> "https://images.unsplash.com/photo-1555680202-c86f0e12f086?q=80&w=1000" // Cyberpunk rain
            1 -> "https://images.unsplash.com/photo-1542831371-29b0f74f9713?q=80&w=1000" // Neon code
            else -> "https://images.unsplash.com/photo-1605806616949-1e87b487bc2a?q=80&w=1000" // Synthwave grid
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(DeepVoid)
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (offsetX > 150f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.previousTrack()
                    } else if (offsetX < -150f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.nextTrack()
                    }
                    offsetX = 0f
                },
                onHorizontalDrag = { change, dragAmount ->
                    offsetX += dragAmount
                }
            )
        }
    ) {
        // Dynamic background
        AsyncImage(
            model = backgroundUrl,
            contentDescription = "Dynamic Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.15f) // Subtle background alpha
        )

        // Background effects
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, DeepVoid),
                    startY = 0f,
                    endY = 1000f
                ))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isDownloadToggleActive by viewModel.isDownloadToggleActive.collectAsState()
            NowPlayingTopBar(
                navController = navController,
                track = track,
                accentColor = accentColor,
                isDownloadActive = isDownloadToggleActive,
                onToggleMode = { viewModel.toggleStreamDownloadMode() }
            )
            Spacer(modifier = Modifier.weight(1f))
            RotatingRecord(isPlaying, track.imageUrl, accentColor, secColor)
            Spacer(modifier = Modifier.weight(1f))
            SongInfo(track, accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons(track, favTracks, viewModel, accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            AudioVisualizer(
                isPlaying = isPlaying,
                modifier = if (visualizerStyle == "ORBIT" || visualizerStyle == "NEON_PULSE") {
                    Modifier.size(110.dp)
                } else {
                    Modifier.height(45.dp).width(140.dp)
                },
                style = visualizerStyle,
                primaryColor = accentColor,
                secondaryColor = secColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            LyricsDisplay(progress = progress, modifier = Modifier.weight(1f), primaryColor = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            ProgressBar(progress = progress, durationStr = track.duration, accentColor = accentColor, secColor = secColor)
            Spacer(modifier = Modifier.height(32.dp))
            
            PlaybackControls(
                isPlaying = isPlaying, 
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                onToggleShuffle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleShuffle()
                },
                onTogglePlay = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.togglePlayback() 
                },
                onSkipNext = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.nextTrack()
                },
                onSkipPrevious = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.previousTrack()
                },
                onToggleRepeat = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleRepeat()
                },
                accentColor = accentColor
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun NowPlayingTopBar(
    navController: NavController,
    track: Track,
    accentColor: Color,
    isDownloadActive: Boolean,
    onToggleMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Back", tint = Color.White)
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggleMode() }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium, color = accentColor, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isDownloadActive) Color(0xFFFF5555) else CyberCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDownloadActive) "📥 Archive Routing" else "🌐 Live Stream",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        
        IconButton(
            onClick = { onToggleMode() },
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = if (isDownloadActive) Icons.Filled.DownloadDone else Icons.Filled.CloudDownload,
                contentDescription = "Toggle Routing Mode",
                tint = if (isDownloadActive) Color(0xFFFF5555) else CyberCyan
            )
        }
    }
}

@Composable
fun RotatingRecord(isPlaying: Boolean, imageUrl: String, accentColor: Color, secColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 10000 else 0, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val activeScale = if (isPlaying) pulseScale else 1.0f

    Box(
        modifier = Modifier
            .size(280.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing neon accent glow ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = activeScale
                    scaleY = activeScale
                    alpha = if (isPlaying) 0.6f else 0.2f
                }
                .border(2.dp, accentColor, CircleShape)
        )
        
        // Inner pulsing secondary glow ring (out-of-sync breathing)
        Box(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    scaleX = if (isPlaying) 2.0f - activeScale else 1.0f
                    scaleY = if (isPlaying) 2.0f - activeScale else 1.0f
                    alpha = if (isPlaying) 0.4f else 0.1f
                }
                .border(1.dp, secColor, CircleShape)
        )
        
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(HoloBg)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .rotate(if (isPlaying) rotation else 0f),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.Crossfade(
                targetState = imageUrl,
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            ) { targetUrl ->
                AsyncImage(
                    model = targetUrl,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Center hole
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)))
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.Black))
        }
    }
}

@Composable
fun SongInfo(track: Track, accentColor: Color) {
    androidx.compose.animation.Crossfade(
        targetState = track,
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    ) { targetTrack ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = targetTrack.title, 
                style = MaterialTheme.typography.titleLarge, 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = targetTrack.artist, 
                style = MaterialTheme.typography.bodyMedium, 
                color = accentColor, 
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActionButtons(track: Track, favTracks: List<com.example.model.FavoriteTrack>, viewModel: MusicViewModel, accentColor: Color) {
    val isLiked = favTracks.any { it.id == track.id }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.toggleFavorite(track) }) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isLiked) accentColor else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(HoloBg)
                .border(1.dp, HoloBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fileQuality = when {
                track.filePath.endsWith(".flac", ignoreCase = true) -> "FLAC"
                track.filePath.endsWith(".wav", ignoreCase = true) -> "WAV 24BIT"
                track.filePath.endsWith(".mp3", ignoreCase = true) -> "320kbps"
                track.filePath.endsWith(".m4a", ignoreCase = true) -> "AAC 256kbps"
                else -> "LOSSLESS 24BIT"
            }
            Icon(Icons.Filled.GraphicEq, contentDescription = "Codec Mode", tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(fileQuality, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
        }
        
        IconButton(onClick = { }) {
            Icon(Icons.Filled.Share, contentDescription = "Share Signal", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun ProgressBar(progress: Float, durationStr: String, accentColor: Color, secColor: Color) {
    // Parse durationStr in form e.g. "4:03"
    val totalSecs = try {
        val parts = durationStr.split(":")
        val mins = parts[0].toInt()
        val secs = parts[1].toInt()
        mins * 60 + secs
    } catch (e: Exception) {
        243
    }

    val animatedProgress by animateFloatAsState(targetValue = progress)

    val elapsedSecs = (totalSecs * animatedProgress).toInt()
    val elapsedStr = String.format("%d:%02d", elapsedSecs / 60, elapsedSecs % 60)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(accentColor, secColor)))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        WaveformDisplay(progress = animatedProgress, accentColor = accentColor)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(elapsedStr, style = MaterialTheme.typography.labelSmall, color = TextGray)
            Text(durationStr, style = MaterialTheme.typography.labelSmall, color = TextGray)
        }
    }
}

@Composable
fun WaveformDisplay(progress: Float, accentColor: Color) {
    // Generate a static pseudo-random waveform pattern for visualization
    val waveformData = remember { List(60) { Random.nextFloat() * 0.9f + 0.1f } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformData.forEachIndexed { index, amp ->
            val isPlayed = (index.toFloat() / waveformData.size) <= progress
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(amp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isPlayed) accentColor else Color.White.copy(alpha = 0.2f))
            )
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: com.example.viewmodel.RepeatMode,
    onToggleShuffle: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffleEnabled) accentColor else TextGray,
                modifier = Modifier.size(24.dp)
            )
        }
        
        IconButton(onClick = onSkipPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
        }
        
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentColor)
                .clickable { onTogglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, 
                contentDescription = if (isPlaying) "Pause" else "Play", 
                tint = DeepVoid, 
                modifier = Modifier.size(40.dp)
            )
        }
        
        IconButton(onClick = onSkipNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(40.dp))
        }
        
        IconButton(onClick = onToggleRepeat) {
            val repeatIcon = if (repeatMode == com.example.viewmodel.RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
            val repeatColor = if (repeatMode != com.example.viewmodel.RepeatMode.NONE) accentColor else TextGray
            Icon(
                repeatIcon, 
                contentDescription = "Repeat", 
                tint = repeatColor, 
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
