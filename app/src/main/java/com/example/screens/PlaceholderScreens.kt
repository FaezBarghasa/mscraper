package com.example.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel

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
            duration = String.format("%d:%02d", entity.duration / 60, entity.duration % 60),
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
                    Icon(androidx.compose.material.icons.Icons.Filled.QueueMusic, contentDescription = "Active Queue", tint = Color.White)
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
                focusedContainerColor = com.example.ui.theme.HoloBg,
                unfocusedContainerColor = com.example.ui.theme.HoloBg,
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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Expand",
                                            tint = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(genre.uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("${tracks.size} TRACKS", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                    
                                    if (expanded) {
                                        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
                                            tracks.forEach { track ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { 
                                                            viewModel.playTrack(track)
                                                            navController.navigate("now_playing")
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = track.imageUrl,
                                                        contentDescription = "Album Art",
                                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(track.title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                                        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = TextGray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                val filteredFavorites = favoriteTracks.filter {
                    it.title.contains(librarySearchQuery, ignoreCase = true) || 
                    it.artist.contains(librarySearchQuery, ignoreCase = true) 
                }
                if (filteredFavorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Filled.FavoriteBorder, contentDescription = null, tint = accentColor.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("NO SIGNALS ENCRYPTED", style = MaterialTheme.typography.titleMedium, color = accentColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Heart tracks across the app to store them locally.", style = MaterialTheme.typography.bodySmall, color = TextGray, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    com.example.ui.components.TrackList(
                        tracks = filteredFavorites,
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
            3 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
                ) {
                    items(6) { index ->
                        val playlistNames = listOf("SYS_OVERDRIVE", "CYBER_RUNNER_2026", "NEON_DREAMER", "INDUSTRIAL_DECRYPT", "VAPOR_ATMOSPHERE", "SILENT_GRID")
                        val playlistCounts = listOf(24, 18, 35, 12, 40, 15)
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

// SettingsScreen removed - replaced by com.example.ui.settings.SettingsScreen

@Composable
fun EqualizerScreen(viewModel: MusicViewModel = viewModel()) {
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()
    val eqEnabled by viewModel.equalizerEnabled.collectAsState()
    val eqPreset by viewModel.equalizerPreset.collectAsState()
    val eqBands by viewModel.equalizerBands.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DSP EQUALIZER", style = MaterialTheme.typography.headlineMedium, color = accentColor, fontWeight = FontWeight.Bold)
            Switch(
                checked = eqEnabled,
                onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = HoloBg
                )
            )
        }
        Text("Modify audio output signals across 5 custom neon bands", style = MaterialTheme.typography.labelSmall, color = TextGray)
        
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Interactive Bezier Curve Graph
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val gridAlpha = 0.15f
                val count = 5
                val w = size.width
                val h = size.height
                
                // Draw grid lines
                for (i in 0 until count) {
                    val x = w * i / (count - 1)
                    drawLine(
                        color = Color.White.copy(alpha = gridAlpha),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw zero baseline
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(0f, h / 2),
                    end = Offset(w, h / 2),
                    strokeWidth = 1.dp.toPx()
                )

                if (eqEnabled) {
                    // Map -10 to +10 into Height
                    val points = eqBands.mapIndexed { index, dbValue ->
                        val x = w * index / (count - 1)
                        // -10db is bottom (h), +10db is top (0)
                        val fraction = (dbValue + 10f) / 20f
                        val y = h - (fraction * h)
                        Offset(x, y)
                    }

                    // Draw glowing curve
                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i+1]
                                val controlX = (p0.x + p1.x) / 2
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }
                    }

                    // Fill under curve
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )

                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots
                    points.forEach { pt ->
                        drawCircle(
                            color = secColor,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Presets Selector
        Text("CYBER PRESETS", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf("SYNTHWAVE", "INDUSTRIAL", "CYBER-BASS", "AMBIENT", "BYPASS")
            presets.forEach { preset ->
                val isSelected = eqPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) accentColor else HoloBg)
                        .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .clickable(enabled = eqEnabled) { viewModel.setEqualizerPreset(preset) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset,
                        color = if (isSelected) DeepVoid else if (eqEnabled) Color.White else TextGray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Faders Column
        Text("FREQUENCY SLIDERS", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val frequencies = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
            frequencies.forEachIndexed { i, freq ->
                val bandValue = eqBands.getOrElse(i) { 0f }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${if (bandValue > 0) "+" else ""}${bandValue.toInt()}dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (eqEnabled) accentColor else TextGray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Vertical slider representation (we can use normal slider inside box or customize)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = bandValue,
                            onValueChange = { viewModel.setEqualizerBand(i, it) },
                            valueRange = -10f..10f,
                            enabled = eqEnabled,
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = -90f
                                }
                                .width(140.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = secColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = HoloBg,
                                disabledThumbColor = TextGray,
                                disabledActiveTrackColor = TextGray.copy(alpha = 0.2f)
                            )
                        )
                    }
                    
                    Text(
                        text = freq,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun PlaylistScreen(navController: NavController, viewModel: MusicViewModel = viewModel()) {
    val queue by viewModel.queue.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
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
                
                com.example.ui.playlist.PlaylistMenu(
                    playlistName = "Active_Queue",
                    tracks = queue,
                    onImportSuccess = { name, tracks ->
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

@Composable
fun ArtistScreen(viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val artists = listOf(
        ArtistData("M83", "Electronic / Synthwave", "1,850,291 Listeners", "https://lh3.googleusercontent.com/aida-public/AB6AXuDqdCMr_-ekSmUoVVPk1cHSB1ll6kp_NQLJFI7kiw_IXENnxXFC_RUrW4mkaf1NtHvFEq4dTPYyMhau-Pnf0F7d5n_Ub4CMsZ-JVITcgaL5BMC46mUYM7psWsHbqV4XOuysvL_hJ8tKdcM0MOa9KbMTc_9nm_Nv4A7_UDx4GWYmjzey20ActLZZ11MwtsIVKBmhJihUBJylSOsJHddm1YJPlzPy-6jlVLk8qAnxNibGw58uJqVb9OKdY8FQzSROWIM0emiGU2setFX_"),
        ArtistData("Kavinsky", "Electro House / Retro", "945,821 Listeners", "https://lh3.googleusercontent.com/aida-public/AB6AXuBBEtoMVVFUzB83CnpCLLYXTr43Jig9eeoY-9-mPD-JxbAFK34_ATCUc-1yMgJk0CSHXUbMw7wEcGYUHVOgr6x4q10vVLSsJPX9-g3YzexBw7hxISrHeBcsDPd1RRluwrBqF044Dd3ntM2DQz-0U3QO6qC4eaUe10I2jSIpcNeHwxGF6-Yhb-7XDr1Ed-s5--_FD_0a__7ifb4E5BUJopP74LoSpUPlyXIN8koLo3k0LsBqbIagsysNnzGiaUqrjhSYvD28hsmYoqNs"),
        ArtistData("Laserhawk", "Outrun Synthwave", "341,209 Listeners", "https://lh3.googleusercontent.com/aida-public/AB6AXuCOdm7DaHtn5PpcHx1T7QhMSvxUAdIOF1huUXzT1X3sq-BBsPvwIccsQFiIoa9ggXmhLNLJFvB60ZpEGTUGsTs-0LI7XFTW727qvJg2dZysvoMOEyzkrKPVTf8hsliOUGexo407H8WjXwkcjwP-v3M1Y9yX21hYZOd32SZW7O6eve7bOE0e1ZYCSmnrVCXgu9PH5tXh578R-jMGpDajp-zVuhTwGZ9g4GEvsBV1d4_m3Yyspl3G5NrqmPq22jWFFwIO2PhrWv27shji"),
        ArtistData("Gunship", "Cyberpunk / Synth", "512,301 Listeners", "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("FEATURED ARTISTS", style = MaterialTheme.typography.headlineMedium, color = CyberCyan, fontWeight = FontWeight.Bold)
        Text("Verified audio signals across the grid network", style = MaterialTheme.typography.labelSmall, color = TextGray)
        Spacer(modifier = Modifier.height(20.dp))

        artists.forEach { artist ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = artist.imageUrl,
                        contentDescription = artist.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, NeonMagenta, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(artist.genre, style = MaterialTheme.typography.bodyMedium, color = CyberCyan)
                        Text(artist.listeners, style = MaterialTheme.typography.labelSmall, color = TextGray)
                    }
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.downloadManager.startDownload("https://soundcloud.com/artist/${artist.name}", "mp3", "320kbps")
                        android.widget.Toast.makeText(context, "Downloading all tracks for ${artist.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download All Tracks", tint = CyberCyan)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "View", tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

data class ArtistData(val name: String, val genre: String, val listeners: String, val imageUrl: String)

@Composable
fun AlbumScreen(viewModel: MusicViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val albums = listOf(
        AlbumData("HURRY UP WE'RE DREAMING", "M83", "2011", "https://lh3.googleusercontent.com/aida-public/AB6AXuB09eN9Abqx8XFiF0yRGIA-y-ze1ARziS7SRJmsUyDM-dEFiMMmtjaZvLlPls6eoYtzfSshP5uia61r9QIBjpJe2rzBPb24TFvIHgjgmvzi8R-GOcc73J5ZccqGqS2jfhvBIXKEtxZusjvEVSctlJKrjxUqd0reEqu2cOA7_hMfyxu9jm_8W7XaKgvsi7v4UZh5Qq6HfrEUM8wHJMwj6ZtxO5IxbOyDp97-SKvuehyF9afHmGJdNgmNPCBxhkwV67qqmmsQuG7ivMyb"),
        AlbumData("OUTRUN", "Kavinsky", "2013", "https://lh3.googleusercontent.com/aida-public/AB6AXuBBEtoMVVFUzB83CnpCLLYXTr43Jig9eeoY-9-mPD-JxbAFK34_ATCUc-1yMgJk0CSHXUbMw7wEcGYUHVOgr6x4q10vVLSsJPX9-g3YzexBw7hxISrHeBcsDPd1RRluwrBqF044Dd3ntM2DQz-0U3QO6qC4eaUe10I2jSIpcNeHwxGF6-Yhb-7XDr1Ed-s5--_FD_0a__7ifb4E5BUJopP74LoSpUPlyXIN8koLo3k0LsBqbIagsysNnzGiaUqrjhSYvD28hsmYoqNs"),
        AlbumData("UNIFIED", "Gunship", "2018", "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr"),
        AlbumData("TRANSMISSIONS", "Revenant", "2024", "https://lh3.googleusercontent.com/aida-public/AB6AXuBzHyiMbZ3DrUc4JhhC7d9wW0vw_t-lfV1DtDolywLb9mRYQ4-UOTr49bhgWtylhmQGt-hxIiOyHZEb2-uApQ6f1vNdcuo0l4xPbw0yTuL_ThR0jHGOD9HAmDWQUwur6Ti6eF_ZDIksYeAEyVqCQlx05nxnbfiTFDxgXDfz6GOeBnXOrhhzb6sqjbrDXD8XlfVcuym9RuC9HeE0Ra8oUNfN3jwvO6Ga6myUP5XfMJSlzWMT_-p0M_-XiYmsoqT1AyMMrSFOYkdAtP_G")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums) { album ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = album.imageUrl,
                        contentDescription = album.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(album.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold)
                    Text(album.artist, style = MaterialTheme.typography.bodySmall, color = CyberCyan, maxLines = 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(album.year, style = MaterialTheme.typography.labelSmall, color = TextGray)
                        
                        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.downloadManager.startDownload("https://soundcloud.com/album/${album.title}", "mp3", "320kbps")
                                android.widget.Toast.makeText(context, "Downloading album: ${album.title}", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download Album", tint = CyberCyan)
                        }
                    }
                }
            }
        }
    }
}

data class AlbumData(val title: String, val artist: String, val year: String, val imageUrl: String)

@Composable
fun GenresScreen() {
    val genres = listOf(
        GenreData("SYNTHWAVE", "852k Active Streams", Brush.horizontalGradient(listOf(Color(0xFF00FFFF), Color(0xFFFF00FF)))),
        GenreData("INDUSTRIAL", "610k Active Streams", Brush.horizontalGradient(listOf(Color(0xFFFF00FF), Color(0xFFFF8800)))),
        GenreData("ELECTRO-DARK", "431k Active Streams", Brush.horizontalGradient(listOf(Color(0xFFFF3366), Color(0xFF330066)))),
        GenreData("OUTRUN", "295k Active Streams", Brush.horizontalGradient(listOf(Color(0xFF00FF66), Color(0xFF0099FF)))),
        GenreData("VAPORWAVE", "180k Active Streams", Brush.horizontalGradient(listOf(Color(0xFFFFCCFF), Color(0xFFCCFFFF)))),
        GenreData("CYBER-GOTH", "142k Active Streams", Brush.horizontalGradient(listOf(Color(0xFF660099), Color(0xFF000000))))
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(genres) { genre ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(genre.gradient)
                    .clickable { }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(genre.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(genre.streams, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

data class GenreData(val name: String, val streams: String, val gradient: Brush)
