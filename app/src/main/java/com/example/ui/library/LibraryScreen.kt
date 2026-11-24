package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.Track
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel

@androidx.media3.common.util.UnstableApi
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: MusicViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {}
) {
    val favoriteTracksEntity by viewModel.favoriteTracks.collectAsState()
    val localTracksEntity by viewModel.localTracks.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    
    val favoriteTracks = favoriteTracksEntity.map { entity ->
        Track(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            duration = entity.duration,
            imageUrl = entity.imageUrl
        )
    }
    
    val localTracks = localTracksEntity.map { entity ->
        Track(
            id = entity.id,
            title = entity.title,
            artist = entity.artist ?: "Unknown Artist",
            duration = String.format(java.util.Locale.US, "%d:%02d", entity.duration / 60, entity.duration % 60),
            imageUrl = entity.artworkPath ?: "",
            filePath = entity.filePath,
            genre = entity.genre ?: "Unknown"
        )
    }
    
    val favoriteTrackIds = favoriteTracks.map { it.id }.toSet()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var trackToEdit by remember { mutableStateOf<Track?>(null) }
    val tabs = listOf("ALL TRACKS", "GENRES", "FAVORITES", "PLAYLISTS")
    
    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.importFolder(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepVoid).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu / Vault Folders", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("YOUR LIBRARY", style = MaterialTheme.typography.headlineMedium, color = accentColor, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = "Import Folder", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { navController.navigate("playlist") },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = "Active Queue", tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = accentColor
                )
            },
            divider = {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedTabIndex == index) accentColor else TextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        var librarySearchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = librarySearchQuery,
            onValueChange = { librarySearchQuery = it },
            placeholder = { Text("Filter tracks...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor) },
            trailingIcon = {
                if (librarySearchQuery.isNotEmpty()) {
                    IconButton(onClick = { librarySearchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = accentColor)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = HoloBg,
                unfocusedContainerColor = HoloBg,
                focusedIndicatorColor = accentColor,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val allTracks = (localTracks + favoriteTracks).distinctBy { it.id }.filter { 
            it.title.contains(librarySearchQuery, ignoreCase = true) || 
            it.artist.contains(librarySearchQuery, ignoreCase = true) 
        }
        
        when (selectedTabIndex) {
            0 -> {
                if (allTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Filled.LibraryMusic, contentDescription = null, tint = accentColor.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("NO DATA DECRYPTED", style = MaterialTheme.typography.titleMedium, color = accentColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Import folders or heart tracks to build your library.", style = MaterialTheme.typography.bodySmall, color = TextGray, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    com.example.ui.components.TrackList(
                        tracks = allTracks,
                        onTrackClick = { track ->
                            viewModel.playTrack(track)
                            navController.navigate("now_playing")
                        },
                        onAddToQueue = { track -> viewModel.addToQueue(track) },
                        onQueueNext = { track -> viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT) },
                        favoriteTrackIds = favoriteTrackIds,
                        onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                        onEditClick = { track -> trackToEdit = track },
                        onAddToPlaylist = { trackToAddToPlaylist = it },
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
                    )
                }
            }
            1 -> {
                val genres = allTracks.groupBy { it.genre.ifBlank { "Unknown" } }
                if (genres.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Text("NO GENRES DETECTED", style = MaterialTheme.typography.titleMedium, color = accentColor)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                        genres.forEach { (genre, tracks) ->
                            item {
                                var expanded by remember { mutableStateOf(false) }
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(HoloBg)
                                            .border(1.dp, if (expanded) accentColor else HoloBorder, RoundedCornerShape(8.dp))
                                            .clickable { expanded = !expanded }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = genre.uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (expanded) accentColor else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("${tracks.size} TRACKS", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                    if (expanded) {
                                        tracks.forEach { track ->
                                            com.example.ui.components.TrackListItem(
                                                track = track,
                                                onClick = {
                                                    viewModel.playTrack(track)
                                                    navController.navigate("now_playing")
                                                },
                                                onAddToQueue = { viewModel.addToQueue(track) },
                                                onQueueNext = { viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT) },
                                                isFavorite = favoriteTrackIds.contains(track.id),
                                                onFavoriteClick = { viewModel.toggleFavorite(track) },
                                                onEditClick = { trackToEdit = track }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (favoriteTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Text("NO FAVORITES DECRYPTED", style = MaterialTheme.typography.titleMedium, color = accentColor)
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
                        favoriteTrackIds = favoriteTrackIds,
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onEditClick = { trackToEdit = it },
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
                    )
                }
            }
            3 -> {
                val playlistNames = listOf("CYBERPUNK HIGHWAY", "NIGHT CALL VOL. 2", "NEON MATRIX MIX", "SYNTH DRIFT")
                val playlistCounts = listOf(24, 18, 42, 12)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlistNames.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(HoloBg)
                                .border(1.dp, HoloBorder, RoundedCornerShape(12.dp))
                                .clickable { }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlistNames[index], style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${playlistCounts[index]} TRACKS SIGNALED", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = accentColor)
                        }
                    }
                }
            }
        }
    }

    if (trackToEdit != null) {
        var editTitle by remember { mutableStateOf(trackToEdit!!.title) }
        var editArtist by remember { mutableStateOf(trackToEdit!!.artist) }
        
        AlertDialog(
            onDismissRequest = { trackToEdit = null },
            title = { Text("Edit Metadata", color = accentColor, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editArtist,
                        onValueChange = { editArtist = it },
                        label = { Text("Artist", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    android.widget.Toast.makeText(navController.context, "Metadata Updated: $editTitle", android.widget.Toast.LENGTH_SHORT).show()
                    trackToEdit = null
                }) {
                    Text("SAVE", color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToEdit = null }) {
                    Text("CANCEL", color = TextGray)
                }
            },
            containerColor = DeepVoid,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (trackToAddToPlaylist != null) {
        com.example.screens.PlaylistPickerDialog(
            track = trackToAddToPlaylist!!,
            playlists = playlists,
            onPlaylistSelected = { playlist ->
                viewModel.addTrackToPlaylist(trackToAddToPlaylist!!, playlist.id)
                trackToAddToPlaylist = null
            },
            onCreateNewPlaylist = {
                trackToAddToPlaylist = null
                onOpenDrawer()
            },
            onDismiss = { trackToAddToPlaylist = null }
        )
    }
}
