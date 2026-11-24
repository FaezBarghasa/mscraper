package com.example.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.Track
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.TextGray
import com.example.viewmodel.MusicViewModel

@androidx.media3.common.util.UnstableApi
@Composable
fun FavoritesScreen(navController: NavController, viewModel: MusicViewModel = viewModel()) {
    val favoriteTracksEntity by viewModel.favoriteTracks.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    var trackToEdit by remember { mutableStateOf<Track?>(null) }
    
    val favoriteTracks = favoriteTracksEntity.map { entity ->
        Track(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            duration = entity.duration,
            imageUrl = entity.imageUrl
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepVoid).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("YOUR FAVORITES", style = MaterialTheme.typography.headlineMedium, color = accentColor, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.Favorite, contentDescription = "Favorites", tint = accentColor)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (favoriteTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No favorites yet.", color = TextGray)
                }
            }
        } else {
            com.example.ui.components.TrackList(
                tracks = favoriteTracks,
                onTrackClick = { track ->
                    viewModel.playTrack(track)
                    navController.navigate("now_playing")
                },
                onAddToQueue = { viewModel.addToQueue(it) },
                onQueueNext = { viewModel.addToQueue(it, com.example.viewmodel.QueuePosition.NEXT) },
                favoriteTrackIds = favoriteTracks.map { it.id }.toSet(),
                onFavoriteClick = { viewModel.toggleFavorite(it) },
                onEditClick = { trackToEdit = it }
            )
        }
    }
}
