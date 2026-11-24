package com.example.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.TextGray
import com.example.viewmodel.MusicViewModel

data class ArtistSummary(val name: String, val trackCount: Int, val imageUrl: String, val sampleTrack: Track)

@Composable
fun ArtistScreen(viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val library by viewModel.synchronizedLibrary.collectAsState()

    val artists: List<ArtistSummary> = library
        .filter { it.artist.isNotBlank() }
        .groupBy { it.artist }
        .map { (artistName, tracks) ->
            ArtistSummary(
                name = artistName,
                trackCount = tracks.size,
                imageUrl = tracks.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl ?: "",
                sampleTrack = tracks.first()
            )
        }
        .sortedByDescending { it.trackCount }

    if (artists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepVoid),
            contentAlignment = Alignment.Center
        ) {
            Text("No artists found in library", color = TextGray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepVoid)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists, key = { it.name }) { artist ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playTrack(artist.sampleTrack) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (artist.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = artist.imageUrl,
                                contentDescription = artist.name,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = artist.name.take(1).uppercase(),
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${artist.trackCount} ${if (artist.trackCount == 1) "track" else "tracks"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                        IconButton(onClick = { viewModel.playTrack(artist.sampleTrack) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play Artist", tint = CyberCyan)
                        }
                    }
                }
            }
        }
    }
}
