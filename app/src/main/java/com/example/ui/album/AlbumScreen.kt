package com.example.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class AlbumSummary(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val imageUrl: String,
    val sampleTrack: Track
)

@Composable
fun AlbumScreen(viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val library by viewModel.synchronizedLibrary.collectAsState()

    val albums: List<AlbumSummary> = library
        .groupBy { it.title.substringBefore(" - ") }
        .map { (albumTitle, tracks) ->
            val firstTrack = tracks.first()
            AlbumSummary(
                title = if (tracks.size > 1) albumTitle else firstTrack.title,
                artist = firstTrack.artist,
                trackCount = tracks.size,
                imageUrl = tracks.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl ?: "",
                sampleTrack = firstTrack
            )
        }
        .sortedByDescending { it.trackCount }

    if (albums.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepVoid),
            contentAlignment = Alignment.Center
        ) {
            Text("No albums found in library", color = TextGray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(DeepVoid)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(albums, key = { it.title + it.artist }) { album ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playTrack(album.sampleTrack) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (album.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = album.imageUrl,
                                contentDescription = album.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color.White.copy(alpha = 0.05f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = album.title.take(1).uppercase(),
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            album.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${album.trackCount} ${if (album.trackCount == 1) "track" else "tracks"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                            IconButton(
                                onClick = { viewModel.playTrack(album.sampleTrack) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play Album", tint = CyberCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}
