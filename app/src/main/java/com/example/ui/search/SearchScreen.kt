package com.example.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.Track
import com.example.ui.components.SourceSelector
import com.example.ui.components.TrackList
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.TokyoBlue
import com.example.ui.downloads.DownloadViewModel
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.QueuePosition
import kotlinx.coroutines.flow.collectLatest

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

    if (showFormatPicker && trackToDownload != null) {
        com.example.ui.downloads.FormatPickerDialog(
            onDismiss = { showFormatPicker = false; trackToDownload = null },
            onFormatSelected = { format ->
                val coreTrack = searchResults.find { it.trackId == trackToDownload!!.id }
                if (coreTrack != null) {
                    downloadViewModel.startDownload(
                        title = coreTrack.title,
                        artist = coreTrack.artist,
                        imageUrl = coreTrack.albumArtUrl ?: "",
                        videoUrl = if (coreTrack.source == com.example.core.ffi.AudioSource.YOUTUBE_MUSIC) {
                            "https://www.youtube.com/watch?v=${coreTrack.trackId}"
                        } else {
                            coreTrack.trackId
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
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                placeholder = { Text("Search tracks...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else {
                val uiTracks = searchResults.map { coreTrack ->
                    Track(
                        id = coreTrack.trackId,
                        title = coreTrack.title,
                        artist = coreTrack.artist,
                        duration = "3:00",
                        imageUrl = coreTrack.albumArtUrl ?: "",
                        genre = selectedSource.displayName
                    )
                }

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
                    onAddToPlaylist = { /* handle playlist */ },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
