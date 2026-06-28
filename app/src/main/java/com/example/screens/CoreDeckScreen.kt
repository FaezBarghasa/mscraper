package com.example.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.model.DownloadStatus
import com.example.model.Track
import com.example.model.TrackEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.QueuePosition
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreDeckScreen(navController: NavController, viewModel: MusicViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()

    var activeTab by remember { mutableStateOf("DOWNLOADER") }
    val tabs = listOf("DOWNLOADER", "SCANNER", "SMART PLAYLISTS", "UTILITIES")

    val progressState by viewModel.progressManager.overlayState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "SYSTEM CORE OPERATIONS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DeepVoid
                    )
                )
            },
            containerColor = DeepVoid
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DeepVoid)
            ) {
                // Horizontal tabs selector
                ScrollableTabRow(
                    selectedTabIndex = tabs.indexOf(activeTab),
                    containerColor = DeepVoid,
                    contentColor = accentColor,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(activeTab)]),
                            color = accentColor
                        )
                    }
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = {
                                Text(
                                    tab,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == tab) Color.White else TextGray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeTab) {
                    "DOWNLOADER" -> DownloaderDeckTab(viewModel, accentColor, secColor)
                    "SCANNER" -> ScannerDeckTab(viewModel, accentColor, secColor)
                    "SMART PLAYLISTS" -> SmartPlaylistsTab(viewModel, navController, accentColor, secColor)
                    "UTILITIES" -> UtilitiesTab(viewModel, accentColor, secColor)
                }
            }
        }

        if (progressState.isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier
                                .size(48.dp)
                                .background(accentColor.copy(alpha = 0.1f), CircleShape)
                                .padding(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = progressState.mainMessage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = progressState.subMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        LinearProgressIndicator(
                            progress = progressState.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "ACTIVE OPERATIONS: ${progressState.activeTaskCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloaderDeckTab(viewModel: MusicViewModel, accentColor: Color, secColor: Color) {
    val jobs by viewModel.downloadManager.jobs.collectAsState()
    var inputUrl by remember { mutableStateOf("https://music.apple.com/us/album/midnight-city/457421833?i=457421838") }
    var targetFormat by remember { mutableStateOf("MP3") }
    var targetBitrate by remember { mutableStateOf("320kbps") }

    val formats = listOf("MP3", "FLAC", "AAC", "OGG", "WAV")
    val bitrates = listOf("128kbps", "256kbps", "320kbps", "Lossless")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "EXTRACT SIGNAL STREAM",
                        style = MaterialTheme.typography.titleSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Paste media track URLs from supported digital nodes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        placeholder = { Text("https://music.apple.com/...", color = TextGray) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("TARGET CONVERSION FORMAT", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        formats.forEach { fmt ->
                            val isSelected = targetFormat == fmt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) accentColor else HoloBg)
                                    .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { targetFormat = fmt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(fmt, color = if (isSelected) DeepVoid else Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("SAMPLE RATE / BITRATE LEVEL", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bitrates.forEach { rate ->
                            val isSelected = targetBitrate == rate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) secColor else HoloBg)
                                    .border(1.dp, if (isSelected) secColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { targetBitrate = rate }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(rate, color = if (isSelected) DeepVoid else Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (inputUrl.isNotBlank()) {
                                viewModel.downloadManager.startDownload(inputUrl, targetFormat, targetBitrate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Download", tint = DeepVoid)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIATE SYSTEM MULTI-DOWNLOAD", color = DeepVoid, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DOWNLOAD METRICS QUEUE (${jobs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (jobs.any { it.status == DownloadStatus.COMPLETED }) {
                    Text(
                        "CLEAR DONE",
                        modifier = Modifier.clickable { viewModel.downloadManager.clearCompleted() },
                        color = secColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (jobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NO ACTIVE CORES ASSIGNED.", style = MaterialTheme.typography.labelMedium, color = TextGray)
                }
            }
        } else {
            items(jobs) { job ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HoloBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (job.status) {
                                        DownloadStatus.COMPLETED -> Icons.Filled.CheckCircle
                                        DownloadStatus.ERROR -> Icons.Filled.Error
                                        DownloadStatus.CONVERTING -> Icons.Filled.Transform
                                        else -> Icons.Filled.DownloadForOffline
                                    },
                                    contentDescription = null,
                                    tint = when (job.status) {
                                        DownloadStatus.COMPLETED -> PrimaryGreen
                                        DownloadStatus.ERROR -> Color.Red
                                        DownloadStatus.CONVERTING -> secColor
                                        else -> accentColor
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${job.artist} • ${job.format} • ${job.downloadSpeed}", style = MaterialTheme.typography.labelSmall, color = TextGray)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Controls (Pause / Resume / Cancel)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (job.status == DownloadStatus.DOWNLOADING) {
                                    IconButton(onClick = { viewModel.downloadManager.pauseDownload(job.id) }) {
                                        Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = Color.White)
                                    }
                                } else if (job.status == DownloadStatus.PAUSED) {
                                    IconButton(onClick = { viewModel.downloadManager.resumeDownload(job.id) }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = accentColor)
                                    }
                                }

                                if (job.status != DownloadStatus.COMPLETED && job.status != DownloadStatus.CANCELLED) {
                                    IconButton(onClick = { viewModel.downloadManager.cancelDownload(job.id) }) {
                                        Icon(Icons.Filled.Cancel, contentDescription = "Cancel", tint = TextGray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress slider
                        LinearProgressIndicator(
                            progress = job.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = when (job.status) {
                                DownloadStatus.COMPLETED -> PrimaryGreen
                                DownloadStatus.CONVERTING -> secColor
                                else -> accentColor
                            },
                            trackColor = HoloBg
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ScannerDeckTab(viewModel: MusicViewModel, accentColor: Color, secColor: Color) {
    val context = LocalContext.current
    val isScanning by viewModel.libraryScanner.isScanning.collectAsState()
    val scanProgress by viewModel.libraryScanner.scanProgress.collectAsState()
    val scannedFileCount by viewModel.libraryScanner.scannedFileCount.collectAsState()

    val localTracks by viewModel.localTracks.collectAsState()
    val scope = rememberCoroutineScope()

    var showDuplicatesDeck by remember { mutableStateOf(false) }
    var duplicateGroups by remember { mutableStateOf<List<com.example.core.DuplicateGroup>>(emptyList()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SYSTEM DIRECTORY INDEXER",
                        style = MaterialTheme.typography.titleSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Scan local storage folders to locate and catalog offline audio tags.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = scanProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = accentColor,
                                trackColor = HoloBg
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "SCANNED DIRECTORIES: $scannedFileCount CHANNELS...",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val added = viewModel.libraryScanner.scanDirectory("/storage/emulated/0/Music/Crysta")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "SCAN COMPLETED! cataloged $added NEW FILES.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Storage, contentDescription = "Scan", tint = DeepVoid)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RUN SYSTEM SCAN", color = DeepVoid, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DUPLICATE DETECTOR & RESOLUTION",
                        style = MaterialTheme.typography.titleSmall,
                        color = secColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Scan memory blocks for duplicate signals and clean local blocks.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val dups = viewModel.duplicateManager.detectDuplicates(localTracks, false)
                            duplicateGroups = dups
                            showDuplicatesDeck = true
                            Toast.makeText(context, "LOCATED ${dups.size} DUPLICATE GROUPS.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = secColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = "Duplicates", tint = DeepVoid)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DETECT SYSTEM DUPLICATES", color = DeepVoid, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showDuplicatesDeck) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DUPLICATE GROUPS (${duplicateGroups.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showDuplicatesDeck = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextGray)
                    }
                }
            }

            if (duplicateGroups.isEmpty()) {
                item {
                    Text("NO DUPLICATES ENCOUNTERED IN THE CURRENT STREAM.", style = MaterialTheme.typography.labelMedium, color = TextGray)
                }
            } else {
                items(duplicateGroups) { group ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(group.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(group.artist, style = MaterialTheme.typography.labelSmall, color = secColor)

                            Spacer(modifier = Modifier.height(8.dp))

                            group.tracks.forEach { tr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${tr.format} • ${tr.bitrate}kbps • File: ${tr.filePath.takeLast(24)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGray
                                    )
                                    Text(
                                        "Added: ${tr.id.substringAfter("scanned_", "Scanned")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.duplicateManager.resolveDuplicates(group, "HIGHEST_QUALITY")
                                            duplicateGroups = viewModel.duplicateManager.detectDuplicates(localTracks, false)
                                            Toast.makeText(context, "RESOLVED BY HIGHEST BITRATE QUALITY", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("KEEP QUALITY", color = DeepVoid, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.duplicateManager.resolveDuplicates(group, "OLDEST")
                                            duplicateGroups = viewModel.duplicateManager.detectDuplicates(localTracks, false)
                                            Toast.makeText(context, "RESOLVED BY OLDEST RECORD", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HoloBg),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text("KEEP OLDEST", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SmartPlaylistsTab(viewModel: MusicViewModel, navController: NavController, accentColor: Color, secColor: Color) {
    val localTracks by viewModel.localTracks.collectAsState()

    // Smart rules evaluation on the fly (Phase 2, Task 2.3)
    val recentlyAdded = localTracks.filter { System.currentTimeMillis() - it.dateAdded < 7 * 24 * 60 * 60 * 1000 }
    val topPlayed = localTracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
    val highFidelity = localTracks.filter { it.bitrate >= 320 || it.format == "FLAC" }

    val smartPlaylists = listOf(
        SmartPlaylistData("Recently Added", "Signals cataloged over the last 7 days", recentlyAdded),
        SmartPlaylistData("Top Scanned Streams", "Most cataloged playbacks in core logs", topPlayed),
        SmartPlaylistData("Lossless & Hi-Fi", "High bitrate audio signals (>= 320kbps)", highFidelity)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SMART PLAYLIST DECK",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Dynamic smart lists evaluated on-the-fly by active metadata logs.",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }

        items(smartPlaylists) { pl ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pl.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(pl.description, style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("${pl.tracks.size} TRACKS", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pl.tracks.isEmpty()) {
                        Text("No records matched rules criteria in local database.", style = MaterialTheme.typography.labelSmall, color = TextGray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pl.tracks.take(3).forEach { tr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.playTrack(viewModel.run { tr.toUiTrack() })
                                            navController.navigate("now_playing")
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = secColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tr.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                                        Text("${tr.artist} • ${tr.format} ${tr.bitrate}kbps", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                    }
                                }
                            }
                            
                            if (pl.tracks.size > 3) {
                                Text(
                                    "And ${pl.tracks.size - 3} more channels...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun UtilitiesTab(viewModel: MusicViewModel, accentColor: Color, secColor: Color) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localTracks by viewModel.localTracks.collectAsState()

    val isSyncing by viewModel.backupSyncService.isSyncing.collectAsState()
    val syncStatus by viewModel.backupSyncService.syncStatus.collectAsState()
    val syncProgress by viewModel.backupSyncService.syncProgress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "UTILITY CORE SECTOR",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Export playlists, configure backups, and synchronize files with cloud decoders.",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }

        // 1. Sharing, Open In External, Export M3U/PLS (Phase 4)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PLAYLIST SIGNAL EXPORT", style = MaterialTheme.typography.titleSmall, color = accentColor, fontWeight = FontWeight.Bold)
                    Text("Build local tracks index into standard playlist decoders.", style = MaterialTheme.typography.labelSmall, color = TextGray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (localTracks.isEmpty()) {
                                    Toast.makeText(context, "NO LOCAL TRACKS TO EXPORT", Toast.LENGTH_SHORT).show()
                                } else {
                                    val m3u = viewModel.sharingService.exportToM3u("LocalPlaylist", localTracks)
                                    Toast.makeText(context, "M3U EXPORT CREATED:\n${m3u.take(120)}...", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("EXPORT M3U", color = DeepVoid, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (localTracks.isEmpty()) {
                                    Toast.makeText(context, "NO LOCAL TRACKS TO EXPORT", Toast.LENGTH_SHORT).show()
                                } else {
                                    val pls = viewModel.sharingService.exportToPls("LocalPlaylist", localTracks)
                                    Toast.makeText(context, "PLS EXPORT CREATED:\n${pls.take(120)}...", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoloBg),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text("EXPORT PLS", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (localTracks.isEmpty()) {
                                    Toast.makeText(context, "NO LOCAL TRACKS TO EXPORT", Toast.LENGTH_SHORT).show()
                                } else {
                                    val json = viewModel.sharingService.exportToJson("LocalPlaylist", localTracks)
                                    Toast.makeText(context, "JSON EXPORT CREATED:\n${json.take(120)}...", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoloBg),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text("EXPORT JSON", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (localTracks.isEmpty()) {
                                    Toast.makeText(context, "NO LOCAL TRACKS TO EXPORT", Toast.LENGTH_SHORT).show()
                                } else {
                                    val xml = viewModel.sharingService.exportToXml("LocalPlaylist", localTracks)
                                    Toast.makeText(context, "XML EXPORT CREATED:\n${xml.take(120)}...", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoloBg),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text("EXPORT XML", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Local snapshot backup (Phase 6, Task 6.1)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LOCAL SNAPSHOT BACKUP", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Create encrypted snapshots of cataloged database and current theme profiles.", style = MaterialTheme.typography.labelSmall, color = TextGray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val backup = viewModel.backupSyncService.createLocalBackup()
                            Toast.makeText(context, backup, Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HoloBg),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CREATE LOCAL SNAPSHOT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Encrypted Cloud Sync (Phase 6, Task 6.2)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ENCRYPTED CLOUD SYNCHRONIZATION", style = MaterialTheme.typography.titleSmall, color = secColor, fontWeight = FontWeight.Bold)
                    Text("Synchronize local media tags with remote secure cloud relays.", style = MaterialTheme.typography.labelSmall, color = TextGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSyncing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                progress = syncProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = secColor,
                                trackColor = HoloBg
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                syncStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = secColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("G-DRIVE", "DROPBOX", "ONEDRIVE").forEach { provider ->
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val ok = viewModel.backupSyncService.syncWithCloud(provider)
                                            if (ok) {
                                                Toast.makeText(context, "$provider SYNC COMPLETED SUCCESSFULLY.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = secColor),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(provider, color = DeepVoid, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. SurrealDB Cloud Node Sync (MANDATORY per USER INTENT "use surrealdb")
        item {
            var endpoint by remember { mutableStateOf(viewModel.backupSyncService.getSurrealEndpoint()) }
            var namespace by remember { mutableStateOf(viewModel.backupSyncService.getSurrealNamespace()) }
            var database by remember { mutableStateOf(viewModel.backupSyncService.getSurrealDatabase()) }
            var username by remember { mutableStateOf(viewModel.backupSyncService.getSurrealUsername()) }
            var password by remember { mutableStateOf(viewModel.backupSyncService.getSurrealPassword()) }
            var isPassVisible by remember { mutableStateOf(false) }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SURREALDB CYBER NODE INTEGRATION",
                            style = MaterialTheme.typography.titleSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF66))
                        )
                    }
                    Text(
                        "Directly synchronize playlists, settings, and metadata snapshots to a secure multi-model SurrealDB cluster.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Database Endpoint URL", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = TextGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        "💡 Emulator: use 'http://10.0.2.2:8000' to sync with your desktop SurrealDB instance.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = namespace,
                            onValueChange = { namespace = it },
                            label = { Text("Namespace", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = database,
                            onValueChange = { database = it },
                            label = { Text("Database", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("User", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Pass", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = TextGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            visualTransformation = if (isPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPassVisible = !isPassVisible }) {
                                    Icon(
                                        imageVector = if (isPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextGray
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = viewModel.backupSyncService.testSurrealConnection(
                                        endpoint, namespace, database, username, password
                                    )
                                    res.onSuccess { msg ->
                                        Toast.makeText(context, "🟢 CONNECTION SUCCESSFUL:\n$msg", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "🔴 CONNECTION FAILED:\n${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoloBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TEST", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val res = viewModel.backupSyncService.syncToSurreal(
                                        endpoint, namespace, database, username, password, viewModel
                                    )
                                    res.onSuccess { msg ->
                                        Toast.makeText(context, "🟢 SYNC EXPORT SUCCESSFUL:\n$msg", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "🔴 SYNC EXPORT FAILED:\n${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = DeepVoid, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SYNC OUT", color = DeepVoid, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val res = viewModel.backupSyncService.restoreFromSurreal(
                                        endpoint, namespace, database, username, password, viewModel
                                    )
                                    res.onSuccess { msg ->
                                        Toast.makeText(context, "🟢 RESTORE SUCCESSFUL:\n$msg", Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "🔴 RESTORE FAILED:\n${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = secColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = DeepVoid, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PULL IN", color = DeepVoid, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

data class SmartPlaylistData(
    val name: String,
    val description: String,
    val tracks: List<TrackEntity>
)
