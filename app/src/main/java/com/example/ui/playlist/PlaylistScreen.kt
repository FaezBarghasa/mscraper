package com.example.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.TextGray
import com.example.viewmodel.MusicViewModel

@androidx.media3.common.util.UnstableApi
@Composable
fun PlaylistScreen(navController: NavController, viewModel: MusicViewModel = viewModel()) {
    val queue by viewModel.queue.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    
    val favoriteTracksEntity by viewModel.favoriteTracks.collectAsState()
    val favoriteTrackIds = favoriteTracksEntity.map { it.id }.toSet()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepVoid,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("NOW PLAYING QUEUE", style = MaterialTheme.typography.headlineSmall, color = accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                
                PlaylistMenu(
                    playlistName = "Active_Queue",
                    tracks = queue,
                    onImportSuccess = { _, tracks ->
                        viewModel.setQueue(tracks)
                    },
                    accentColor = accentColor,
                    snackbarHostState = snackbarHostState
                )

                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (queue.isNotEmpty()) {
                            queue.forEach { track ->
                                val url = track.filePath.takeIf { it.isNotEmpty() } ?: "https://soundcloud.com/dummy/${track.title}"
                                viewModel.downloadManager.startDownload(url, "mp3", "320kbps")
                            }
                            android.widget.Toast.makeText(context, "Downloading Playlist...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Download All", tint = accentColor)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Swipe items left to eject them from the live queue stream.", style = MaterialTheme.typography.labelSmall, color = TextGray)
            Spacer(modifier = Modifier.height(20.dp))
            
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("QUEUE EMPTY. NO CHANNELS ASSIGNED.", style = MaterialTheme.typography.titleMedium, color = TextGray)
                }
            } else {
                com.example.ui.components.TrackList(
                    tracks = queue,
                    onTrackClick = { track ->
                        viewModel.playTrack(track)
                        navController.navigate("now_playing")
                    },
                    onTrackDismiss = { trackToRemove ->
                        val updated = queue.filterNot { it.id == trackToRemove.id }
                        viewModel.setQueue(updated)
                    },
                    onAddToQueue = { track ->
                        viewModel.addToQueue(track)
                    },
                    onQueueNext = { track -> viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT) },
                    favoriteTrackIds = favoriteTrackIds,
                    onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
