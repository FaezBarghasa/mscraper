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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.minDimension
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val sleepSecondsLeft by viewModel.sleepTimerSecondsLeft.collectAsState()
    var showSleepTimerDialog by remember { mutableStateOf(false) }

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

    val context = LocalContext.current
    val accentColorStr by viewModel.cyberAccentColor.collectAsState()
    
    LaunchedEffect(track.imageUrl, accentColorStr) {
        if (accentColorStr == "DYNAMIC" && track.imageUrl.isNotBlank()) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(track.imageUrl)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    withContext(Dispatchers.Default) {
                        val palette = Palette.from(bitmap).generate()
                        val primary = palette.getVibrantColor(0xFF00E6FF.toInt())
                        val secondary = palette.getMutedColor(0xFF7B00FF.toInt())
                        viewModel.updateThemeOverrides(Color(primary), Color(secondary))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (accentColorStr != "DYNAMIC") {
            viewModel.updateThemeOverrides(null, null)
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            currentMinutes = sleepMinutes,
            onSelectMinutes = { viewModel.setSleepTimer(it) }
        )
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(DeepVoid)
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
            val queue by viewModel.queue.collectAsState()
            
            val initialPage = remember(queue, track.id) {
                val idx = queue.indexOfFirst { it.id == track.id }
                if (idx != -1) idx else 0
            }

            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { if (queue.isEmpty()) 1 else queue.size }
            )

            // Keep pager in sync with track:
            LaunchedEffect(track.id) {
                val targetIdx = queue.indexOfFirst { it.id == track.id }
                if (targetIdx != -1 && pagerState.currentPage != targetIdx) {
                    pagerState.animateScrollToPage(targetIdx)
                }
            }

            // Sync pager scroll to skip tracks:
            LaunchedEffect(pagerState.currentPage) {
                if (queue.isNotEmpty() && pagerState.currentPage in queue.indices) {
                    val trackToPlay = queue[pagerState.currentPage]
                    if (trackToPlay.id != track.id) {
                        viewModel.playTrack(trackToPlay)
                    }
                }
            }

            NowPlayingTopBar(
                navController = navController,
                track = track,
                accentColor = accentColor,
                isDownloadActive = isDownloadToggleActive,
                onToggleMode = { viewModel.toggleStreamDownloadMode() },
                sleepMinutes = sleepMinutes,
                sleepSecondsLeft = sleepSecondsLeft,
                onSleepTimerClick = { showSleepTimerDialog = true }
            )
            Spacer(modifier = Modifier.weight(1f))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) { page ->
                val trackForPage = if (queue.isEmpty()) track else queue[page]
                RotatingRecord(
                    isPlaying = isPlaying && (trackForPage.id == track.id), 
                    imageUrl = trackForPage.imageUrl, 
                    accentColor = accentColor, 
                    secColor = secColor
                )
            }

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
    onToggleMode: () -> Unit,
    sleepMinutes: Int,
    sleepSecondsLeft: Int,
    onSleepTimerClick: () -> Unit
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
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sleep Timer Button
            IconButton(
                onClick = onSleepTimerClick,
                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = "Sleep Timer",
                    tint = if (sleepMinutes > 0) accentColor else Color.White
                )
            }
            if (sleepSecondsLeft > 0) {
                val formattedTime = String.format("%02d:%02d", sleepSecondsLeft / 60, sleepSecondsLeft % 60)
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
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
}

@Composable
fun RotatingRecord(isPlaying: Boolean, imageUrl: String, accentColor: Color, secColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 12000 else 0, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val activeScale = if (isPlaying) pulseScale else 1.0f

    Box(
        modifier = Modifier
            .size(300.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Neon Glow Halo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = activeScale * 1.1f
                    scaleY = activeScale * 1.1f
                    alpha = if (isPlaying) 0.3f else 0.1f
                }
                .background(Brush.radialGradient(listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)), CircleShape)
        )

        // Outer pulsing neon accent glow ring
        Box(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    scaleX = activeScale
                    scaleY = activeScale
                    alpha = if (isPlaying) 0.8f else 0.3f
                }
                .border(2.dp, Brush.sweepGradient(listOf(accentColor, secColor, accentColor)), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(DeepVoid)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .rotate(if (isPlaying) rotation else 0f),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.Crossfade(
                targetState = imageUrl,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            ) { targetUrl ->
                AsyncImage(
                    model = targetUrl,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.9f)
                )
            }
            // Vinyl texture lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 1..5) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = (size.minDimension / 2) * (i / 6f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                    )
                }
            }
            // Center hole
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(DeepVoid)
                    .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accentColor))
            }
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
    val totalSecs = try {
        val parts = durationStr.split(":")
        val mins = parts[0].toInt()
        val secs = parts[1].toInt()
        mins * 60 + secs
    } catch (e: Exception) {
        243
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing)
    )

    val elapsedSecs = (totalSecs * animatedProgress).toInt()
    val elapsedStr = String.format("%d:%02d", elapsedSecs / 60, elapsedSecs % 60)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(secColor.copy(alpha = 0.7f), accentColor, Color.White)
                        )
                    )
                    .shadow(4.dp, RoundedCornerShape(4.dp), ambientColor = accentColor, spotColor = accentColor)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        WaveformDisplay(progress = animatedProgress, accentColor = accentColor)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                elapsedStr, 
                style = MaterialTheme.typography.labelSmall, 
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                durationStr, 
                style = MaterialTheme.typography.labelSmall, 
                color = TextGray
            )
        }
    }
}

@Composable
fun WaveformDisplay(progress: Float, accentColor: Color) {
    val waveformData = remember { List(60) { Random.nextFloat() * 0.8f + 0.2f } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformData.forEachIndexed { index, amp ->
            val isPlayed = (index.toFloat() / waveformData.size) <= progress
            val alpha = if (isPlayed) 1f else 0.2f
            val color = if (isPlayed) accentColor else Color.White
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(amp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = alpha))
                    .then(
                        if (isPlayed) Modifier.shadow(2.dp, RoundedCornerShape(2.dp), ambientColor = accentColor, spotColor = accentColor) else Modifier
                    )
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
                .shadow(12.dp, CircleShape, ambientColor = accentColor, spotColor = accentColor)
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

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    currentMinutes: Int,
    onSelectMinutes: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", color = Color.White) },
        containerColor = DeepVoid,
        text = {
            Column {
                listOf(0, 15, 30, 45, 60, 90).forEach { mins ->
                    val label = if (mins == 0) "Turn Off" else "${mins} minutes"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMinutes(mins)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentMinutes == mins),
                            onClick = {
                                onSelectMinutes(mins)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = TextGray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
