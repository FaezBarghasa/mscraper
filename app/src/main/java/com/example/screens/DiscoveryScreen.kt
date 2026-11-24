package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.navigation.Screen
import com.example.model.Track
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.components.TrackList

@androidx.media3.common.util.UnstableApi
@Composable
fun DiscoveryScreen(
    navController: NavController,
    viewModel: MusicViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {}
) {
    var selectedGenre by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var trackToEdit by remember { mutableStateOf<Track?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    val playlists by viewModel.playlists.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()

    val synchronizedLibrary by viewModel.synchronizedLibrary.collectAsState()

    // Dynamic trending tracks loaded from InnerTube via MmDlpApi
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var releaseTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoadingTrending = true
        try {
            val ffiApi = com.example.core.ffi.MmDlpApi()
            val tResults = ffiApi.search("Trending Synthwave", com.example.core.ffi.AudioSource.YOU_TUBE_MUSIC)
            if (tResults.isNotEmpty()) {
                trendingTracks = tResults.map { meta ->
                    val durationStr = meta.durationSeconds?.let { s ->
                        val totalSecs = s.toLong()
                        val m = totalSecs / 60
                        val sec = totalSecs % 60
                        String.format(java.util.Locale.US, "%d:%02d", m, sec)
                    } ?: "--:--"

                    Track(
                        id = meta.trackId,
                        title = meta.title,
                        artist = meta.artist,
                        duration = durationStr,
                        imageUrl = meta.albumArtUrl ?: "",
                        filePath = ""
                    )
                }
            }
            val rResults = ffiApi.search("Electronic New Releases", com.example.core.ffi.AudioSource.YOU_TUBE_MUSIC)
            if (rResults.isNotEmpty()) {
                releaseTracks = rResults.map { meta ->
                    val durationStr = meta.durationSeconds?.let { s ->
                        val totalSecs = s.toLong()
                        val m = totalSecs / 60
                        val sec = totalSecs % 60
                        String.format(java.util.Locale.US, "%d:%02d", m, sec)
                    } ?: "--:--"

                    Track(
                        id = meta.trackId,
                        title = meta.title,
                        artist = meta.artist,
                        duration = durationStr,
                        imageUrl = meta.albumArtUrl ?: "",
                        filePath = ""
                    )
                }
            }
        } catch (_: Exception) {
            // Keep fallback below to synchronized library if offline
        } finally {
            isLoadingTrending = false
        }
    }

    val activeTrending = if (trendingTracks.isNotEmpty()) trendingTracks else synchronizedLibrary
    val activeReleases = if (releaseTracks.isNotEmpty()) releaseTracks else synchronizedLibrary.asReversed()
    val activeTrackList = if (activeTrending.isNotEmpty()) activeTrending else synchronizedLibrary

    val filteredTrending = if (selectedGenre == "ALL") activeTrending else activeTrending.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
    val filteredReleases = if (selectedGenre == "ALL") activeReleases else activeReleases.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
    val featuredTrack = activeTrending.firstOrNull() ?: synchronizedLibrary.firstOrNull()

    val filteredTrackListItems = activeTrackList.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(accentColor, onOpenDrawer)
        
        val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
        if (recentlyPlayed.isNotEmpty()) {
            Text(
                text = "RECENTLY PLAYED",
                style = MaterialTheme.typography.titleSmall,
                color = accentColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(recentlyPlayed) { track ->
                    RecentlyPlayedItem(track = track, onClick = {
                        viewModel.playTrack(track)
                        navController.navigate("now_playing")
                    })
                }
            }
        }

        FeaturedSection(featuredTrack, navController, viewModel, accentColor)
        
        Text(
            text = "SUB-GENRES DECODERS",
            style = MaterialTheme.typography.titleSmall,
            color = accentColor.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        GenreFilterBar(selectedGenre = selectedGenre, onGenreSelected = { selectedGenre = it }, accentColor)
        
        // AI Playlist Generator Section (DISABLED)
        // Spacer(modifier = Modifier.height(16.dp))

        TrendingSection(filteredTrending, viewModel, navController)
        NewReleasesSection(filteredReleases, viewModel, navController, accentColor)
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM TRACK DATABASE",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.material3.IconButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    val downloadableTracks = filteredTrackListItems.filter { it.filePath.startsWith("http") || it.id.isNotEmpty() }
                    if (downloadableTracks.isNotEmpty()) {
                        downloadableTracks.forEach { track ->
                            val url = if (track.filePath.startsWith("http")) track.filePath else "https://www.youtube.com/watch?v=${track.id}"
                            viewModel.downloadManager.startDownload(url, "mp3", "320kbps")
                        }
                        android.widget.Toast.makeText(context, "Queued ${downloadableTracks.size} tracks for download", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
            ) {
                androidx.compose.material3.Icon(Icons.Filled.Download, contentDescription = "Download All Tracks", tint = accentColor)
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search by title or artist", color = TextGray) },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = accentColor
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            singleLine = true
        )
        var groupBy by remember { androidx.compose.runtime.mutableStateOf("None") }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Organize Library", color = TextGray, style = MaterialTheme.typography.bodyMedium)
            
            var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }
            Box {
                androidx.compose.material3.TextButton(
                    onClick = { expanded = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text("Group By: $groupBy")
                    androidx.compose.material3.Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                }
                
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DeepVoid)
                ) {
                    listOf("None", "Genre", "Year", "Playback Frequency").forEach { option ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(option, color = if (groupBy == option) accentColor else Color.White) },
                            onClick = {
                                groupBy = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Group the tracks
        val groupedTracks = remember(filteredTrackListItems, groupBy) {
            when (groupBy) {
                "Genre" -> filteredTrackListItems.groupBy { 
                    val hash = Math.abs(it.artist.hashCode()) % 3
                    when(hash) { 0 -> "Synthwave"; 1 -> "Industrial"; else -> "Electro-Dark" } 
                }
                "Year" -> filteredTrackListItems.groupBy { 
                    val hash = Math.abs(it.title.hashCode()) % 5
                    (2018 + hash).toString()
                }
                "Playback Frequency" -> filteredTrackListItems.groupBy { 
                    val hash = Math.abs(it.title.hashCode()) % 3
                    when(hash) { 0 -> "High (Daily)"; 1 -> "Medium (Weekly)"; else -> "Low (Rare)" }
                }
                else -> mapOf("" to filteredTrackListItems)
            }
        }

        if (groupBy == "None") {
            TrackList(
                tracks = filteredTrackListItems,
                onTrackClick = { track ->
                    viewModel.playTrack(track)
                    navController.navigate("now_playing")
                },
                onAddToQueue = { track ->
                    viewModel.addToQueue(track)
                },
                onQueueNext = { track ->
                    viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT)
                },
                favoriteTrackIds = favoriteTracks.map { it.id }.toSet(),
                onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                onEditClick = { track -> trackToEdit = track },
                onAddToPlaylist = { trackToAddToPlaylist = it },
                modifier = Modifier.height(300.dp)
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(300.dp)) {
                groupedTracks.forEach { (groupName, tracksInGroup) ->
                    item {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(tracksInGroup) { track ->
                        com.example.ui.components.TrackListItem(
                            track = track,
                            isFavorite = favoriteTracks.any { it.id == track.id },
                            onClick = {
                                viewModel.playTrack(track)
                                navController.navigate("now_playing")
                            },
                            onAddToQueue = { viewModel.addToQueue(track) },
                            onQueueNext = { viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT) },
                            onFavoriteClick = { viewModel.toggleFavorite(track) },
                            onEditClick = { trackToEdit = track },
                            onAddToPlaylist = { trackToAddToPlaylist = track }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // padding for bottom nav
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
                    viewModel.updateTrackMetadata(trackToEdit!!.id, editTitle.trim(), editArtist.trim())
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
}

@Composable
fun GenreFilterBar(
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    accentColor: Color
) {
    val genres = listOf("ALL", "SYNTHWAVE", "INDUSTRIAL", "ELECTRO-DARK")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            val isSelected = selectedGenre == genre
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) accentColor else HoloBg)
                    .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .clickable { onGenreSelected(genre) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = genre,
                    color = if (isSelected) DeepVoid else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TopBar(accentColor: Color, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HoloBg)
            .border(1.dp, HoloBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu / Vault Folders",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile Avatar",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "GOOD EVENING",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilot_2026",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
            }
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen)
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
            )
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
fun FeaturedSection(
    featuredTrack: Track?,
    navController: NavController,
    viewModel: MusicViewModel,
    accentColor: Color
) {
    if (featuredTrack == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HoloBg)
                .border(1.dp, HoloBorder, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "DECODER STATION",
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AWAITING SIGNAL",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Import local audio files or search online streams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(420.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                viewModel.playTrack(featuredTrack)
                navController.navigate(Screen.NowPlaying.route)
            }
    ) {
        if (featuredTrack.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = featuredTrack.imageUrl,
                contentDescription = "Featured",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HoloBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(96.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, DeepVoid),
                    startY = 0f,
                    endY = 1200f
                ))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = "FEATURED DECODER SIGNAL",
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = featuredTrack.title,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = featuredTrack.artist,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Immerse yourself in authentic high-fidelity soundscapes.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.playTrack(featuredTrack)
                    navController.navigate(Screen.NowPlaying.route)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = DeepVoid)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LISTEN NOW", color = DeepVoid, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
fun TrendingSection(items: List<Track>, viewModel: MusicViewModel, navController: NavController) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TRENDING NOW", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text("VIEW ALL", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen, fontWeight = FontWeight.Bold)
        }
        
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HoloBg)
                    .border(1.dp, HoloBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("NO CHANNELS FOUND", color = TextGray, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable {
                                viewModel.playTrack(item)
                                navController.navigate("now_playing")
                            }
                    ) {
                        if (item.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(HoloBg)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(item.artist, style = MaterialTheme.typography.bodySmall, color = TextGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
fun NewReleasesSection(items: List<Track>, viewModel: MusicViewModel, navController: NavController, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            text = "NEW RELEASES",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HoloBg)
                    .border(1.dp, HoloBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("NO RELEASES IN THIS BAND", color = TextGray, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .width(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HoloBg)
                            .border(1.dp, HoloBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.playTrack(item)
                                navController.navigate("now_playing")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepVoid),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(item.artist, style = MaterialTheme.typography.labelSmall, color = accentColor, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextGray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(item.duration, style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    viewModel.addToQueue(item)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add to queue", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedItem(track: Track, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageUrl = track.imageUrl.takeIf { it.isNotEmpty() } ?: track.filePath
        if (imageUrl.isNotEmpty() && (imageUrl.startsWith("http") || java.io.File(imageUrl).exists())) {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = "Track Art",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HoloBg)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HoloBg)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
