package com.example.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.SearchSource
import com.example.model.Track
import com.example.ui.components.SourceSelector
import com.example.ui.components.TrackList
import com.example.ui.theme.*
import com.example.ui.downloads.DownloadViewModel
import com.example.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.collectLatest

data class BrowseCategory(
    val title: String,
    val query: String,
    val gradient: List<Color>
)

val BROWSE_CATEGORIES = listOf(
    BrowseCategory("Electronic", "Electronic", listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))),
    BrowseCategory("Synthwave", "Synthwave 80s", listOf(Color(0xFFEC4899), Color(0xFFFA2D48))),
    BrowseCategory("Focus & Study", "Lofi beats focus study", listOf(Color(0xFF10B981), Color(0xFF059669))),
    BrowseCategory("Workout", "High energy workout motivation", listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
    BrowseCategory("Ambient & Sleep", "Deep sleep ambient relax", listOf(Color(0xFF6366F1), Color(0xFF1E1B4B))),
    BrowseCategory("Acoustic & Indie", "Acoustic indie live session", listOf(Color(0xFF14B8A6), Color(0xFF0D9488)))
)

/**
 * Q4 2026 Apple Music / Spotify Search Screen
 * Features:
 * - Pill search bar with instant clear & dynamic source toggles
 * - Instant 2-column Browse Categories grid when query is blank
 * - Direct seamless playback & download queueing
 */
@androidx.media3.common.util.UnstableApi
@Composable
fun SearchScreen(
    navController: NavController,
    searchViewModel: SearchViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel(),
    downloadViewModel: DownloadViewModel = viewModel()
) {
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val selectedSource by searchViewModel.selectedSource.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val accentColor = PrimaryAccent
    val favoriteTracks by musicViewModel.favoriteTracks.collectAsState()
    val favoriteTrackIds = remember(favoriteTracks) { favoriteTracks.map { it.id }.toSet() }

    var trackToDownload by remember { mutableStateOf<Track?>(null) }
    var showFormatPicker by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    val playlists by musicViewModel.playlists.collectAsState()

    if (trackToAddToPlaylist != null) {
        com.example.screens.PlaylistPickerDialog(
            track = trackToAddToPlaylist!!,
            playlists = playlists,
            onPlaylistSelected = { playlist ->
                musicViewModel.addTrackToPlaylist(trackToAddToPlaylist!!, playlist.id)
                trackToAddToPlaylist = null
            },
            onCreateNewPlaylist = { trackToAddToPlaylist = null },
            onDismiss = { trackToAddToPlaylist = null }
        )
    }

    if (showFormatPicker && trackToDownload != null) {
        com.example.ui.downloads.FormatPickerDialog(
            onDismiss = { showFormatPicker = false; trackToDownload = null },
            onFormatSelected = { format ->
                val coreTrack = searchResults.find { it.id == trackToDownload!!.id }
                if (coreTrack != null) {
                    downloadViewModel.startDownload(
                        title = coreTrack.title,
                        artist = coreTrack.artist,
                        imageUrl = coreTrack.imageUrl,
                        videoUrl = if (selectedSource == SearchSource.YOUTUBE) {
                            "https://www.youtube.com/watch?v=${coreTrack.id}"
                        } else {
                            coreTrack.id
                        },
                        audioUrl = "",
                        format = format
                    )
                    showFormatPicker = false
                    trackToDownload = null
                    navController.navigate("downloads")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        searchViewModel.errorMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ObsidianBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { navController.navigate("downloads") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Downloads",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pill Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchViewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    "Artists, songs, or genres",
                                    color = TextTertiary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = accentColor
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchViewModel.updateSearchQuery("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SourceSelector(
                    selectedSource = selectedSource,
                    onSourceChanged = { searchViewModel.updateSelectedSource(it) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (searchQuery.isBlank() && searchResults.isEmpty()) {
                // Spotify Browse Moods Grid
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Browse all",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(BROWSE_CATEGORIES) { category ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(category.gradient))
                                    .clickable {
                                        searchViewModel.updateSearchQuery(category.query)
                                    }
                                    .padding(14.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results found",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                TrackList(
                    tracks = searchResults,
                    onTrackClick = { track ->
                        musicViewModel.playTrack(track)
                    },
                    onFavoriteClick = { track ->
                        musicViewModel.toggleFavorite(track)
                    },
                    onAddToPlaylist = { track ->
                        trackToAddToPlaylist = track
                    },
                    favoriteTrackIds = favoriteTrackIds
                )
            }
        }
    }
}
