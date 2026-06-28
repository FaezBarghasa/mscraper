package com.example.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.model.PlaylistEntity
import com.example.model.Track
import com.example.ui.components.TrackList
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.HoloBg
import com.example.ui.theme.HoloBorder
import com.example.ui.theme.TextGray
import com.example.viewmodel.MusicViewModel

@Composable
fun CustomPlaylistScreen(
    navController: NavController,
    viewModel: MusicViewModel
) {
    val playlist by viewModel.selectedPlaylist.collectAsState()
    val tracks by viewModel.selectedPlaylistTracks.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val favoriteTrackIds = favoriteTracks.map { it.id }.toSet()

    val currentPlaylist = playlist ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentPlaylist.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentPlaylist.description ?: "CUSTOM MUSIC ARCHIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Delete playlist button
            IconButton(
                onClick = {
                    viewModel.deletePlaylist(currentPlaylist.id)
                    navController.popBackStack()
                },
                modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions Row (Play All & Shuffle)
        if (tracks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.setQueue(tracks)
                        viewModel.playTrack(tracks.first())
                        navController.navigate("now_playing")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY ALL", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.setQueue(tracks)
                        viewModel.toggleShuffle()
                        viewModel.playTrack(viewModel.queue.value.first())
                        navController.navigate("now_playing")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHUFFLE", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tracks List
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty",
                        tint = TextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "THIS FOLDER IS EMPTY",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add tracks to this playlist by tapping the 'three dots' menu next to any track in Search or Discovery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "Swipe left on any track to eject it from this playlist.",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TrackList(
                tracks = tracks,
                onTrackClick = { track ->
                    viewModel.setQueue(tracks)
                    viewModel.playTrack(track)
                    navController.navigate("now_playing")
                },
                onTrackDismiss = { trackToRemove ->
                    viewModel.removeTrackFromPlaylist(trackToRemove.id, currentPlaylist.id)
                },
                onAddToQueue = { track ->
                    viewModel.addToQueue(track)
                },
                onQueueNext = { track ->
                    viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT)
                },
                favoriteTrackIds = favoriteTrackIds,
                onFavoriteClick = { track ->
                    viewModel.toggleFavorite(track)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SidebarContent(
    viewModel: MusicViewModel,
    navController: NavController,
    onCloseDrawer: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val accentColor by viewModel.themePrimary.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070707))
            .padding(16.dp)
    ) {
        // Sidebar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = "Vaults",
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "VAULT MATRIX",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.5f))
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Create Playlist Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .clickable { showCreateDialog = true }
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CreateNewFolder,
                contentDescription = "New Vault",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "CREATE NEW VAULT",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "PLAYLIST ARCHIVES",
            style = MaterialTheme.typography.labelSmall,
            color = TextGray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Playlists Folders List
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO SECURE VAULTS CREATED",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(playlists.size) { index ->
                    val playlist = playlists[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.01f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.04f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.selectPlaylist(playlist)
                                onCloseDrawer()
                                navController.navigate("custom_playlist")
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!playlist.description.isNullOrEmpty()) {
                                Text(
                                    text = playlist.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                viewModel.deletePlaylist(playlist.id)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Footer version / branding info
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "DECODING PROTOCOL V2.6.2",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Dialog for creation
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                containerColor = Color(0xFF121212),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = {
                    Text("INITIALIZE VAULT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("VAULT NAME") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = accentColor,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedIndicatorColor = accentColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            label = { Text("DESCRIPTION (OPTIONAL)") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = accentColor,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedIndicatorColor = accentColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName, newPlaylistDesc.takeIf { it.isNotBlank() })
                                newPlaylistName = ""
                                newPlaylistDesc = ""
                                showCreateDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("INITIALIZE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("CANCEL", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun PlaylistPickerDialog(
    track: Track,
    playlists: List<PlaylistEntity>,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = "ADD TO VAULT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select a custom folder to store '${track.title.uppercase()}'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                
                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO FOLDERS CONFIGURED",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextGray
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(playlists.size) { index ->
                            val playlist = playlists[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .clickable {
                                        onPlaylistSelected(playlist)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = playlist.name.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onCreateNewPlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE NEW VAULT FOLDER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White)
            }
        }
    )
}
