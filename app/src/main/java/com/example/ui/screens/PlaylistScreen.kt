package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.playlist.PlaylistRepository
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen() {
    val context = LocalContext.current
    val repository = remember { PlaylistRepository(context) }
    val scope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                repository.exportPlaylist(it, "My Playlist", emptyList()) // tracks should come from state
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val (name, tracks) = repository.importPlaylist(it)
                // handle imported tracks
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Playlists",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { exportLauncher.launch("playlist.json") }) {
                Text("Export JSON")
            }
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text("Import JSON")
            }
        }
    }
}
