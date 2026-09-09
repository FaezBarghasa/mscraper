package com.example.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.Track
import com.example.model.DownloadStatus
import com.example.ui.theme.DeepVoid
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    navController: NavController,
    downloadViewModel: DownloadViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel()
) {
    val tasks by downloadViewModel.tasks.collectAsState()
    val accentColor by musicViewModel.themePrimary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepVoid)
            )
        },
        containerColor = DeepVoid
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No downloads yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    DownloadTaskItem(task, accentColor, 
                        onDelete = { downloadViewModel.removeTask(task.id) },
                        onPlay = { 
                            task.fileUri?.let { uri ->
                                musicViewModel.playTrack(
                                    Track(
                                        id = task.id,
                                        title = task.title,
                                        artist = task.artist,
                                        duration = "--:--",
                                        imageUrl = task.imageUrl,
                                        filePath = uri.toString()
                                    )
                                )
                                navController.navigate("now_playing")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask, 
    accentColor: Color,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(task.artist, color = accentColor, style = MaterialTheme.typography.labelSmall)
                }
                
                if (task.status == DownloadStatus.COMPLETED) {
                    Row {
                        IconButton(onClick = onPlay) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = accentColor)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                    }
                } else if (task.status == DownloadStatus.ERROR) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }

            if (task.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }

            if (task.error != null) {
                Text(task.error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatPickerDialog(
    onDismiss: () -> Unit,
    onFormatSelected: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DeepVoid) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Select Format", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            listOf("MP3", "FLAC", "WAV").forEach { format ->
                TextButton(
                    onClick = { onFormatSelected(format); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(format, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
