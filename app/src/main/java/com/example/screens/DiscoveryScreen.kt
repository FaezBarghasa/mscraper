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

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.components.TrackList

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

    val allTrendingItems = listOf(
        TrendingItem("synth_1", "Synthwave Vol. 1", "Various Artists", "https://lh3.googleusercontent.com/aida-public/AB6AXuCOdm7DaHtn5PpcHx1T7QhMSvxUAdIOF1huUXzT1X3sq-BBsPvwIccsQFiIoa9ggXmhLNLJFvB60ZpEGTUGsTs-0LI7XFTW727qvJg2dZysvoMOEyzkrKPVTf8hsliOUGexo407H8WjXwkcjwP-v3M1Y9yX21hYZOd32SZW7O6eve7bOE0e1ZYCSmnrVCXgu9PH5tXh578R-jMGpDajp-zVuhTwGZ9g4GEvsBV1d4_m3Yyspl3G5NrqmPq22jWFFwIO2PhrWv27shji", "SYNTHWAVE", "3:45"),
        TrendingItem("cyber_heart", "Cyber Heart", "Laserhawk", "https://lh3.googleusercontent.com/aida-public/AB6AXuCCnF7asptZ_a4uC50gs_0zs_DWyyq1pt6Zz836XBsIn72VRSpvW9pDAv5__Dr5uWHwB4qJ-DhNb-tKYX8tE2sVgp9bggv12880y9qM1T9SWuHXLQc8NjnV_H8rXen8J6lcG9vCbmhx1jI8grSVcfhyoWkXJHdFQovRj9QGFRcZPtn32jU_31S6BmZ-Gqp-ikmZQBcuAsRjwGS67BmXO-nSS4WbF_8WT9U592ZrsSetPAqVSQN2VMRzY9ohaJDTZhRlMVlA1mHJsZOd", "SYNTHWAVE", "4:12"),
        TrendingItem("industrial_rage", "Industrial Rage", "Revenant", "https://lh3.googleusercontent.com/aida-public/AB6AXuBzHyiMbZ3DrUc4JhhC7d9wW0vw_t-lfV1DtDolywLb9mRYQ4-UOTr49bhgWtylhmQGt-hxIiOyHZEb2-uApQ6f1vNdcuo0l4xPbw0yTuL_ThR0jHGOD9HAmDWQUwur6Ti6eF_ZDIksYeAEyVqCQlx05nxnbfiTFDxgXDfz6GOeBnXOrhhzb6sqjbrDXD8XlfVcuym9RuC9HeE0Ra8oUNfN3jwvO6Ga6myUP5XfMJSlzWMT_-p0M_-XiYmsoqT1AyMMrSFOYkdAtP_G", "INDUSTRIAL", "5:02"),
        TrendingItem("golden_hour", "Golden Hour", "The Midnight", "https://lh3.googleusercontent.com/aida-public/AB6AXuBH4V3cKguN1f112WrqvJWHOfXpOay-QpdOfc5Td5yR-eSa9jgbX3ATdZ9BRmR1FnoA4IQQtAYsHCPH9yLBEvihPTjBnfrE-99fDkYPRpXI9ju28dZHMGqti2KNcFcwgnPWTxd6dbLQ595UYrGPUevHtXqBheJvzvZ4NahBU2u5CWjDyluk7i26eGrwGRRqzCFc-YRRNdFD6U_D4AXElMGY0hny-K79Bg95cs0t3WUZTHinVQPDU7C9wnsJ-DCw4UOX1kPrauzX7aaL", "SYNTHWAVE", "4:33")
    )
    
    val allReleaseItems = listOf(
        ReleaseItem("dark_all_day", "Dark All Day", "Gunship", "2h ago", "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr", "INDUSTRIAL", "5:15"),
        ReleaseItem("atlas", "Atlas", "FM-84", "5h ago", "https://lh3.googleusercontent.com/aida-public/AB6AXuCKXXN6oN2DLv-Ikct-IdyH9Q8jqH4HacTzCZXkprcFW8rqwypnt0WWJXEmYNEChITfinBPNMmHupitl3I4Ix-Dk3wlLi2eRw3oAx13I4imd_OetKCk5cQ2Namhujky6WwzVzWRRc-OfLX9u-BONfeB1ho_CAM_UrzccWi8wDcLX_kx6Z8jYdkHQmiCVUwz4E5e9ZRqaORa-H6KNSukNIeyKfgscnaqypw8Zzr0kvF8dXIfVW_BPsGxaQNKDvCERbatvK_ekame7bQ7", "SYNTHWAVE", "4:52"),
        ReleaseItem("electro_shiver", "Electro Shiver", "Darkstar", "1d ago", "https://lh3.googleusercontent.com/aida-public/AB6AXuDMKNDi5OybO4bNSthBslKCbQWGKFhvNcEAdksG8_3ImWV1Q5nvNgyd-_vaL5GVD6A10I8DNPyxpl9Pw28U8U6toBHR782OGxJ4Ii2j_cWUlUN3FNGdiQkv-7RMa2GMlJqzKmRcLf4Q4iieXC6Js51hlazr0ObcZ0NhEPIsBs4BLnyl8kNX1nmUydMul-_4wjU8q5aBI2XGUY3Mr3J3NBzPV1O8h7pf1E_EaBnZwWvoN447paZSFd3Ry4Za7NRnnkbRGo8I1O9NsbdN", "ELECTRO-DARK", "3:58")
    )
    
    val filteredTrending = if (selectedGenre == "ALL") allTrendingItems else allTrendingItems.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
    val filteredReleases = if (selectedGenre == "ALL") allReleaseItems else allReleaseItems.filter { it.genre.equals(selectedGenre, ignoreCase = true) }

    val trackListItems = listOf(
        Track("t1", "Neon Nights", "The Midnight", "4:15", "https://lh3.googleusercontent.com/aida-public/AB6AXuDqdCMr_-ekSmUoVVPk1cHSB1ll6kp_NQLJFI7kiw_IXENnxXFC_RUrW4mkaf1NtHvFEq4dTPYyMhau-Pnf0F7d5n_Ub4CMsZ-JVITcgaL5BMC46mUYM7psWsHbqV4XOuysvL_hJ8tKdcM0MOa9KbMTc_9nm_Nv4A7_UDx4GWYmjzey20ActLZZ11MwtsIVKBmhJihUBJylSOsJHddm1YJPlzPy-6jlVLk8qAnxNibGw58uJqVb9OKdY8FQzSROWIM0emiGU2setFX_"),
        Track("t2", "Dark All Day", "Gunship", "5:15", "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr"),
        Track("t3", "Atlas", "FM-84", "4:52", "https://lh3.googleusercontent.com/aida-public/AB6AXuCKXXN6oN2DLv-Ikct-IdyH9Q8jqH4HacTzCZXkprcFW8rqwypnt0WWJXEmYNEChITfinBPNMmHupitl3I4Ix-Dk3wlLi2eRw3oAx13I4imd_OetKCk5cQ2Namhujky6WwzVzWRRc-OfLX9u-BONfeB1ho_CAM_UrzccWi8wDcLX_kx6Z8jYdkHQmiCVUwz4E5e9ZRqaORa-H6KNSukNIeyKfgscnaqypw8Zzr0kvF8dXIfVW_BPsGxaQNKDvCERbatvK_ekame7bQ7"),
        Track("t4", "Cyber Heart", "Laserhawk", "4:12", "https://lh3.googleusercontent.com/aida-public/AB6AXuCCnF7asptZ_a4uC50gs_0zs_DWyyq1pt6Zz836XBsIn72VRSpvW9pDAv5__Dr5uWHwB4qJ-DhNb-tKYX8tE2sVgp9bggv12880y9qM1T9SWuHXLQc8NjnV_H8rXen8J6lcG9vCbmhx1jI8grSVcfhyoWkXJHdFQovRj9QGFRcZPtn32jU_31S6BmZ-Gqp-ikmZQBcuAsRjwGS67BmXO-nSS4WbF_8WT9U592ZrsSetPAqVSQN2VMRzY9ohaJDTZhRlMVlA1mHJsZOd")
    )

    val filteredTrackListItems = trackListItems.filter {
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

        FeaturedSection(navController, viewModel, accentColor)
        
        Text(
            text = "SUB-GENRES DECODERS",
            style = MaterialTheme.typography.titleSmall,
            color = accentColor.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        GenreFilterBar(selectedGenre = selectedGenre, onGenreSelected = { selectedGenre = it }, accentColor)
        
        // AI Playlist Generator Section (DISABLED)
        // AiPlaylistGeneratorSection(viewModel, navController, accentColor)
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
                    if (filteredTrackListItems.isNotEmpty()) {
                        filteredTrackListItems.forEach { track ->
                            val url = track.filePath.takeIf { it.isNotEmpty() } ?: "https://soundcloud.com/dummy/${track.title}"
                            viewModel.downloadManager.startDownload(url, "mp3", "320kbps")
                        }
                        android.widget.Toast.makeText(context, "Downloading all tracks...", android.widget.Toast.LENGTH_SHORT).show()
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
                    // Update the track in our view model (for real app we'd update DB)
                    // Currently DiscoveryScreen uses static lists, so this just shows the UI capability
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
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD_tGqga_Z44e72kI5IVmi67kLnjr-6fBxhLjqSve1nifF44dtbOXLo2NpDnlHAHRMfEer8tYJ3p4v2L35ngM4n259ITVXOOQDdiiubBCa4MyCbY7TOj0Nh4ShlhltXA6nIIG9iMGo0JmMrPpFSfPuNqokzlhvjzwl799EqFkWTnx6CIW9AQkKkfhFazXUaOWaOZ0N2tJ_RnfUkEeD_dEKBsEqI_qumX5XsuW5PKKjEuVDOCtvJSE8IT9mFnEidbmDKJYt3C0QZ91Xq",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, accentColor, CircleShape),
                contentScale = ContentScale.Crop
            )
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

@Composable
fun FeaturedSection(navController: NavController, viewModel: MusicViewModel, accentColor: Color) {
    val featuredTrack = Track(
        id = "featured_1",
        title = "Neon Nights",
        artist = "The Midnight",
        duration = "4:15",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDqdCMr_-ekSmUoVVPk1cHSB1ll6kp_NQLJFI7kiw_IXENnxXFC_RUrW4mkaf1NtHvFEq4dTPYyMhau-Pnf0F7d5n_Ub4CMsZ-JVITcgaL5BMC46mUYM7psWsHbqV4XOuysvL_hJ8tKdcM0MOa9KbMTc_9nm_Nv4A7_UDx4GWYmjzey20ActLZZ11MwtsIVKBmhJihUBJylSOsJHddm1YJPlzPy-6jlVLk8qAnxNibGw58uJqVb9OKdY8FQzSROWIM0emiGU2setFX_"
    )
    
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
        AsyncImage(
            model = featuredTrack.imageUrl,
            contentDescription = "Featured",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
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
                text = "NEON NIGHTS",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "The Midnight",
                style = MaterialTheme.typography.titleLarge,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Immerse yourself in the synth-heavy soundscapes of the future.",
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

data class TrendingItem(val id: String, val title: String, val artist: String, val imageUrl: String, val genre: String, val duration: String)

@Composable
fun TrendingSection(items: List<TrendingItem>, viewModel: MusicViewModel, navController: NavController) {
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
                                viewModel.playTrack(Track(item.id, item.title, item.artist, item.duration, item.imageUrl))
                                navController.navigate("now_playing")
                            }
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold)
                        Text(item.artist, style = MaterialTheme.typography.bodySmall, color = TextGray, maxLines = 1)
                    }
                }
            }
        }
    }
}

data class ReleaseItem(val id: String, val title: String, val artist: String, val time: String, val imageUrl: String, val genre: String, val duration: String)

@Composable
fun NewReleasesSection(items: List<ReleaseItem>, viewModel: MusicViewModel, navController: NavController, accentColor: Color) {
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
                                viewModel.playTrack(Track(item.id, item.title, item.artist, item.duration, item.imageUrl))
                                navController.navigate("now_playing")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold)
                            Text(item.artist, style = MaterialTheme.typography.labelSmall, color = accentColor, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextGray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(item.time, style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    val currentQueue = viewModel.queue.value.toMutableList()
                                    if (!currentQueue.any { it.id == item.id }) {
                                        currentQueue.add(Track(item.id, item.title, item.artist, item.duration, item.imageUrl))
                                        viewModel.setQueue(currentQueue)
                                    }
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
fun AiPlaylistGeneratorSection(viewModel: MusicViewModel, navController: androidx.navigation.NavController, accentColor: Color) {
    val isGenerating by viewModel.isGeneratingPlaylist.collectAsState()
    val generatedName by viewModel.generatedPlaylistName.collectAsState()
    val generatedTracks by viewModel.generatedPlaylistTracks.collectAsState()
    
    var moodQuery by remember { androidx.compose.runtime.mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HoloBg)
            .border(1.dp, HoloBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI PLAYLIST GENERATOR", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Describe your mood or activity and let the system construct a neural mix for you.",
            style = MaterialTheme.typography.bodySmall, color = TextGray
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                value = moodQuery,
                onValueChange = { moodQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. late night coding in the neon rain", color = TextGray) },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = accentColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            androidx.compose.material3.Button(
                onClick = {
                    if (moodQuery.isNotBlank()) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.generatePlaylistWithAI(moodQuery)
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("GENERATE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (generatedName != null && generatedTracks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("RESULT: $generatedName", style = MaterialTheme.typography.titleSmall, color = accentColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            androidx.compose.material3.Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    viewModel.setQueue(generatedTracks)
                    viewModel.playTrack(generatedTracks.first())
                    navController.navigate("now_playing")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PLAY GENERATED SEQUENCE (${generatedTracks.size} TRACKS)", color = Color.White)
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
        val imageUrl = track.filePath.takeIf { it.startsWith("http") } ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=200" 
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
