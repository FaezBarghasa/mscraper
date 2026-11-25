package com.example.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.SearchSource
import com.example.model.Track
import com.example.ui.components.SourceSelector
import com.example.ui.components.TrackList
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.TextGray
import com.example.ui.theme.TokyoBlue
import com.example.ui.downloads.DownloadViewModel
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.QueuePosition
import kotlinx.coroutines.flow.collectLatest

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
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
    val accentColor by musicViewModel.themePrimary.collectAsState()
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
            onCreateNewPlaylist = {
                trackToAddToPlaylist = null
            },
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
        topBar = {
            TopAppBar(
                title = {
                    SourceSelector(
                        selectedSource = selectedSource,
                        onSourceChanged = { searchViewModel.updateSelectedSource(it) },
                        accentColor = accentColor
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("downloads") }) {
                        Icon(Icons.Default.Download, contentDescription = "Downloads", tint = accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepVoid,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DeepVoid
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchViewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search tracks...", color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = TextGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = accentColor.copy(alpha = 0.35f), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Search YouTube Music or SoundCloud" else "No tracks found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Type a track, artist, or genre above." else "Check your network connection or try another query.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            } else {
                val uiTracks = searchResults

                TrackList(
                    tracks = uiTracks,
                    onTrackClick = { track ->
                        musicViewModel.playTrack(track)
                        navController.navigate("now_playing")
                    },
                    onAddToQueue = { musicViewModel.addToQueue(it) },
                    onQueueNext = { musicViewModel.addToQueue(it, QueuePosition.NEXT) },
                    favoriteTrackIds = favoriteTrackIds,
                    onFavoriteClick = { musicViewModel.toggleFavorite(it) },
                    onAddToPlaylist = { trackToAddToPlaylist = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
