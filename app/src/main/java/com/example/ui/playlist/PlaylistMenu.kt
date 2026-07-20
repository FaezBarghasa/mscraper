package com.example.ui.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.Track
import com.example.ui.theme.HoloBg
import kotlinx.coroutines.launch

@Composable
fun PlaylistMenu(
    playlistName: String,
    tracks: List<Track>,
    onImportSuccess: (String, List<Track>) -> Unit,
    accentColor: Color,
    snackbarHostState: SnackbarHostState
) {
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("json") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportHandler = remember {
        PlaylistExportHandler(
            context = context,
            scope = scope,
            onExportResult = { _, message ->
                isLoading = false
                if (message != null) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    val importHandler = remember {
        PlaylistImportHandler(
            context = context,
            scope = scope,
            onImportResult = { success, message, data ->
                isLoading = false
                if (message != null) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                if (success && data != null) {
                    onImportSuccess(data.first, data.second)
                }
            }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (exportFormat == "json") "application/json" else "text/xml"
        )
    ) { uri ->
        if (uri != null) isLoading = true
        exportHandler.onResult(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) isLoading = true
        importHandler.onResult(uri)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = accentColor,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(HoloBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Export to JSON", color = Color.White) },
                    onClick = {
                        expanded = false
                        exportFormat = "json"
                        exportHandler.startExport(playlistName, tracks, exportLauncher)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export to XML", color = Color.White) },
                    onClick = {
                        expanded = false
                        exportFormat = "xml"
                        // Note: Handler currently only supports JSON in MmDlpApi interface if we follow prompt strictly
                        // but I added exportPlaylistXml to the interface.
                        // I'll update Handler to choose method based on file extension/mime
                        exportHandler.startExport(playlistName, tracks, exportLauncher)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Import from File", color = Color.White) },
                    onClick = {
                        expanded = false
                        importHandler.startImport(importLauncher)
                    }
                )
            }
        }
    }
}
