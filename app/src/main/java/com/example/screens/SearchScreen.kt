package com.example.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel
import com.example.ui.components.TrackList

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: MusicViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val favoriteTracksEntity by viewModel.favoriteTracks.collectAsState()
    val favoriteTrackIds = favoriteTracksEntity.map { it.id }.toSet()
    val playlists by viewModel.playlists.collectAsState()
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    
    var recentSearches by remember { 
        mutableStateOf(prefs.getString("recent", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()) 
    }
    
    fun addSearch(term: String) {
        if (term.isBlank()) return
        val current = recentSearches.toMutableList()
        current.remove(term)
        current.add(0, term)
        val newRecent = current.take(5)
        recentSearches = newRecent
        prefs.edit().putString("recent", newRecent.joinToString(",")).apply()
        viewModel.updateSearchQuery(term)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .verticalScroll(rememberScrollState())
    ) {
        StatusBar(accentColor, onOpenDrawer)
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onSearch = { addSearch(it) }, 
            accentColor = accentColor, 
            secColor = secColor
        )
        
        if (searchQuery.isNotBlank()) {
            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEARCH RESULTS", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TrackList(
                    tracks = searchResults,
                    onTrackClick = { track ->
                        viewModel.playTrack(track)
                        navController.navigate("now_playing")
                    },
                    onAddToQueue = { track -> viewModel.addToQueue(track) },
                    onQueueNext = { track -> viewModel.addToQueue(track, com.example.viewmodel.QueuePosition.NEXT) },
                    favoriteTrackIds = favoriteTrackIds,
                    onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                    onEditClick = { /* No-op */ },
                    onAddToPlaylist = { trackToAddToPlaylist = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("NO SIGNALS FOUND", color = accentColor, style = MaterialTheme.typography.titleSmall)
                }
            }
        } else {
            if (recentSearches.isNotEmpty()) {
                RecentSearchesList(recentSearches, { search -> 
                    viewModel.updateSearchQuery(search)
                }, accentColor)
            }
            
            DetectedFrequencies(accentColor)
            RecentSignals(viewModel, navController, accentColor, secColor)
        }
        Spacer(modifier = Modifier.height(100.dp))
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

@Composable
fun RecentSearchesList(searches: List<String>, onSearchClick: (String) -> Unit, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Icon(Icons.Filled.History, contentDescription = "History", tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RECENT SEARCHES", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searches.forEach { search ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(HoloBg)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { onSearchClick(search) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(search, color = accentColor, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun StatusBar(accentColor: Color, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu / Vault Folders",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("SYS.ONLINE", color = accentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Text("WIFI BATT", color = accentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: (String) -> Unit, accentColor: Color, secColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(HoloBg)
            .border(1.dp, HoloBorder, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(">", color = secColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).background(Color.Transparent),
                placeholder = { Text("Search for signals...", color = accentColor.copy(alpha = 0.4f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = secColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { 
                    onSearch(query) 
                })
            )
            
            if (query.isNotEmpty()) {
                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = accentColor, modifier = Modifier.clickable { onQueryChange("") })
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor, modifier = Modifier.clickable { onSearch(query) })
        }
    }
}

@Composable
fun DetectedFrequencies(accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
            Spacer(modifier = Modifier.width(8.dp))
            Text("DETECTED FREQUENCIES", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text("Select vibe module", color = accentColor.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FrequencyCard(title = "SYNTHWAVE", color = CyberCyan, imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBDVeWDWfXYC_syPYD1gQBbsimWXpi3koN97lfu_i4NFSGy9M7rF8CmREixbG0yMQMY8aFEEg2wJGAAkaO28xIwO4XgkdMX_fBqDhrEA5_oiDNekcq1-VjSOsF-dYLmERwrn4fsudDNQ6PYWWv4PNBE1YoSBa07Iq1iraSuIeb5fT-sUbcyaMulkrKimc3JC_cK9H6ADQNEC3bShQNx4a9Y5v7XvVJ8zznxZSB6_CJ387ZKH5jK3KwtQdgstQJSZQeupOZtFmsin30I", modifier = Modifier.weight(1f))
            FrequencyCard(title = "CYBERPUNK", color = WarningYellow, imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCUg_ZAkx6oJRccOKeXdrIpBTR41OTFLBWlKtNAqf8r76NNcIGLR-EIy1mbul68wBhLRrR836x8N-Sp-cx7ugKPoy7f58Ig8E_iE76bCmF6jtvCSzV5zGKUEoly5z9CfRA2G4_6GXd4luAHC2uui5ta9hDU627Jnvw9ykTi5y-EIIIsh_TW-Fim16sKTKFwhrYOcjLldZFCilt2Hmxm3QxUYC1hqa61cTXHwp2eBNypu0TnCfR6iUIXKR94-Mffmdtmveaim2TS78Sw", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FrequencyCard(title = "LO-FI", color = PrimaryGreen, imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDwxcpALKMlWp4nog5Kj10_ntarWqSXJ3sygkf20tFeRoisk7YWLjLNxttzuV438uTQ3XWv1tgbwxR-wA-lxzb4nbmKRTTkT0EpYqWRO35i-n88zQKIi-V4XVJ2JqMx85mxbW4nzdpkY8Xdy5mwZs9dumyZdlTzZ-5TCXe18nfEBVh-cSCIpoeIAvX2HOrWbPcXdP38Tw_2OyNS9XbLXA70hv4QrMYeica8ecGZ__6Xv-ASjsH0k6Hp9nnq_MxFeLIL9pyUEGTxyvEG", modifier = Modifier.weight(1f))
            FrequencyCard(title = "DARK TECHNO", color = Color(0xFF9333EA), imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBzHyiMbZ3DrUc4JhhC7d9wW0vw_t-lfV1DtDolywLb9mRYQ4-UOTr49bhgWtylhmQGt-hxIiOyHZEb2-uApQ6f1vNdcuo0l4xPbw0yTuL_ThR0jHGOD9HAmDWQUwur6Ti6eF_ZDIksYeAEyVqCQlx05nxnbfiTFDxgXDfz6GOeBnXOrhhzb6sqjbrDXD8XlfVcuym9RuC9HeE0Ra8oUNfN3jwvO6Ga6myUP5XfMJSlzWMT_-p0M_-XiYmsoqT1AyMMrSFOYkdAtP_G", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FrequencyCard(title: String, color: Color, imageUrl: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(HoloBg)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
        Box(modifier = Modifier.fillMaxSize().background(DeepVoid.copy(alpha = 0.3f)))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.height(4.dp).width(32.dp).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

data class Signal(val id: String, val artist: String, val album: String, val imageUrl: String, val duration: String)

@Composable
fun RecentSignals(viewModel: MusicViewModel, navController: NavController, accentColor: Color, secColor: Color) {
    val signals = listOf(
        Signal("midnight_sig", "The Midnight", "Deep Blue // Days of Thunder", "https://lh3.googleusercontent.com/aida-public/AB6AXuAZ1JDbx91xxV8j1Fn6qoQZl8TP2P9J0dt-u5dY9rUYAdl12Y2uMqG64te-cH-m6ZVRnFZoeZLUv3wVE9VoTKwwxQPMbXwZQV0ui8h7e2QXwlq8WVshA1N2x9s0kAQsfHmrQWiCR1amjpZ_5x3fefHDWCn8uWvrrJqrQzbBhwf-e3O0IMQ00i10zCvjJ5JvCIiEs1QfeW1oa66xeMaDnCZjQRuLG_S-2kdlFAp5p0RdGbDxXEVy4uryg0yrnvUtM2ZoZXxtKLvxsq-B", "4:03"),
        Signal("gunship_sig", "Gunship", "Tech Noir // Dark All Day", "https://lh3.googleusercontent.com/aida-public/AB6AXuDMKNDi5OybO4bNSthBslKCbQWGKFhvNcEAdksG8_3ImWV1Q5nvNgyd-_vaL5GVD6A10I8DNPyxpl9Pw28U8U6toBHR782OGxJ4Ii2j_cWUlUN3FNGdiQkv-7RMa2GMlJqzKmRcLf4Q4iieXC6Js51hlazr0ObcZ0NhEPIsBs4BLnyl8kNX1nmUydMul-_4wjU8q5aBI2XGUY3Mr3J3NBzPV1O8h7pf1E_EaBnZwWvoN447paZSFd3Ry4Za7NRnnkbRGo8I1O9NsbdN", "4:57"),
        Signal("kavinsky_sig", "Kavinsky", "Nightcall // Outrun", "https://lh3.googleusercontent.com/aida-public/AB6AXuBBEtoMVVFUzB83CnpCLLYXTr43Jig9eeoY-9-mPD-JxbAFK34_ATCUc-1yMgJk0CSHXUbMw7wEcGYUHVOgr6x4q10vVLSsJPX9-g3YzexBw7hxISrHeBcsDPd1RRluwrBqF044Dd3ntM2DQz-0U3QO6qC4eaUe10I2jSIpcNeHwxGF6-Yhb-7XDr1Ed-s5--_FD_0a__7ifb4E5BUJopP74LoSpUPlyXIN8koLo3k0LsBqbIagsysNnzGiaUqrjhSYvD28hsmYoqNs", "4:19")
    )
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
            Text("RECENT SIGNALS", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        signals.forEach { signal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HoloBg)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.playTrack(Track(signal.id, signal.album.split(" // ").first(), signal.artist, signal.duration, signal.imageUrl))
                        navController.navigate("now_playing")
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = signal.imageUrl,
                    contentDescription = signal.artist,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(signal.artist, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(signal.album, color = accentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, secColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = secColor)
                }
            }
        }
    }
}
