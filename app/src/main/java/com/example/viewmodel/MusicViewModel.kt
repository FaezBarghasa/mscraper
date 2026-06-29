package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.model.FavoriteTrack
import com.example.model.Track
import com.example.model.TrackEntity
import com.example.model.PlaylistEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.PrimaryGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.cronet.CronetDataSource
import org.chromium.net.CronetEngine

enum class QueuePosition { NOW, NEXT, END }
enum class RepeatMode { NONE, ALL, ONE }

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val favoriteDao = db.favoriteTrackDao()
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val playlistTrackDao = db.playlistTrackDao()

    // Engines & Services (Phases 1, 2, 4, 5, 6)
    val surrealDbService = com.example.core.SurrealDbService(application)
    val downloadManager = com.example.core.DownloadManager(trackDao, viewModelScope)
    val libraryScanner = com.example.core.LibraryScanner(trackDao, surrealDbService)
    val duplicateManager = com.example.core.DuplicateManager(trackDao)
    val sharingService = com.example.core.SharingService(application)
    val backupSyncService = com.example.core.BackupSyncService(application)
    val settingsManager = com.example.core.SettingsManager(application)
    val progressManager = com.example.core.ProgressManager(libraryScanner, downloadManager, viewModelScope)
    // val geminiGenerator = com.example.core.GeminiPlaylistGenerator()

    // Gemini API state (DISABLED)
    private val _isGeneratingPlaylist = MutableStateFlow(false)
    val isGeneratingPlaylist: StateFlow<Boolean> = _isGeneratingPlaylist.asStateFlow()

    private val _generatedPlaylistName = MutableStateFlow<String?>(null)
    val generatedPlaylistName: StateFlow<String?> = _generatedPlaylistName.asStateFlow()

    private val _generatedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val generatedPlaylistTracks: StateFlow<List<Track>> = _generatedPlaylistTracks.asStateFlow()

    fun generatePlaylistWithAI(mood: String) {
        // AI Playlist Generation disabled for now
        /*
        viewModelScope.launch {
            _isGeneratingPlaylist.value = true
            val (name, tracks) = geminiGenerator.generatePlaylist(mood)
            _generatedPlaylistName.value = name
            _generatedPlaylistTracks.value = tracks
            _isGeneratingPlaylist.value = false
        }
        */
    }

    private val cronetEngine: CronetEngine? by lazy {
        try {
            CronetEngine.Builder(application)
                .enableQuic(true)
                .enableHttp2(true)
                .build()
        } catch (e: Exception) { null }
    }

    private val downloadCache: androidx.media3.datasource.cache.SimpleCache by lazy {
        val cacheDir = java.io.File(application.cacheDir, "media_cache")
        val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB
        val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(application)
        androidx.media3.datasource.cache.SimpleCache(cacheDir, evictor, databaseProvider)
    }

    val player: ExoPlayer by lazy {
        val builder = ExoPlayer.Builder(application)
        val upstreamFactory = if (cronetEngine != null) {
            CronetDataSource.Factory(cronetEngine!!, java.util.concurrent.Executors.newSingleThreadExecutor())
        } else {
            androidx.media3.datasource.DefaultDataSource.Factory(application)
        }
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        builder.setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(application)
                .setDataSourceFactory(cacheDataSourceFactory)
        )
        builder.build()
    }

    val mediaSession: androidx.media3.session.MediaSession by lazy {
        androidx.media3.session.MediaSession.Builder(application, player).build()
    }

    // DB Flows
    val localTracks: StateFlow<List<TrackEntity>> = trackDao.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<FavoriteTrack>> = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cloud Streamed Data Simulation
    private val cloudTracks = listOf(
        TrackEntity(id = "cloud_1", title = "Neon Nights", artist = "The Midnight", album = "Endless Summer", albumArtist = "The Midnight", genre = "SYNTHWAVE", year = 2016, trackNumber = 1, discNumber = 1, duration = 255, bitrate = 320, sampleRate = 44100, format = "mp3", filePath = "https://example.com/neon_nights", fileSize = 5000000L, artworkPath = "https://lh3.googleusercontent.com/aida-public/AB6AXuDqdCMr_-ekSmUoVVPk1cHSB1ll6kp_NQLJFI7kiw_IXENnxXFC_RUrW4mkaf1NtHvFEq4dTPYyMhau-Pnf0F7d5n_Ub4CMsZ-JVITcgaL5BMC46mUYM7psWsHbqV4XOuysvL_hJ8tKdcM0MOa9KbMTc_9nm_Nv4A7_UDx4GWYmjzey20ActLZZ11MwtsIVKBmhJihUBJylSOsJHddm1YJPlzPy-6jlVLk8qAnxNibGw58uJqVb9OKdY8FQzSROWIM0emiGU2setFX_", dateAdded = System.currentTimeMillis()),
        TrackEntity(id = "cloud_2", title = "Dark All Day", artist = "Gunship", album = "Dark All Day", albumArtist = "Gunship", genre = "INDUSTRIAL", year = 2018, trackNumber = 1, discNumber = 1, duration = 315, bitrate = 320, sampleRate = 44100, format = "mp3", filePath = "https://example.com/dark_all_day", fileSize = 5500000L, artworkPath = "https://lh3.googleusercontent.com/aida-public/AB6AXuAUpFNWGTcVOrzXC-2iwWBlLbKyORUaJ5NxuJRr9ATSpaLTNN-el0XQwcePmkDNfsV0wRhp3F3U_jeSKjvbnKp67snBvZCVqiV8cqopjO3_vkkwbtbudeZJPlHkiTqYi4p8BQ7kLjV7fEo5km5pIanS6_JCtJ6PlTj9bU1-MTQ8tewixigM5LvZbbX2ic0UNdxGKr8dLPVb4oJ896A0_V8ovoSP6iiRJADMWzouIh5AeGZryV7caIX8uXKQLcvuBda2ssdUaFdW21Qr", dateAdded = System.currentTimeMillis()),
        TrackEntity(id = "cloud_3", title = "Atlas", artist = "FM-84", album = "Atlas", albumArtist = "FM-84", genre = "SYNTHWAVE", year = 2016, trackNumber = 1, discNumber = 1, duration = 292, bitrate = 320, sampleRate = 44100, format = "mp3", filePath = "https://example.com/atlas", fileSize = 5200000L, artworkPath = "https://lh3.googleusercontent.com/aida-public/AB6AXuCKXXN6oN2DLv-Ikct-IdyH9Q8jqH4HacTzCZXkprcFW8rqwypnt0WWJXEmYNEChITfinBPNMmHupitl3I4Ix-Dk3wlLi2eRw3oAx13I4imd_OetKCk5cQ2Namhujky6WwzVzWRRc-OfLX9u-BONfeB1ho_CAM_UrzccWi8wDcLX_kx6Z8jYdkHQmiCVUwz4E5e9ZRqaORa-H6KNSukNIeyKfgscnaqypw8Zzr0kvF8dXIfVW_BPsGxaQNKDvCERbatvK_ekame7bQ7", dateAdded = System.currentTimeMillis()),
        TrackEntity(id = "cloud_4", title = "Cyber Heart", artist = "Laserhawk", album = "Redline", albumArtist = "Laserhawk", genre = "SYNTHWAVE", year = 2010, trackNumber = 1, discNumber = 1, duration = 252, bitrate = 320, sampleRate = 44100, format = "mp3", filePath = "https://example.com/cyber_heart", fileSize = 4900000L, artworkPath = "https://lh3.googleusercontent.com/aida-public/AB6AXuCCnF7asptZ_a4uC50gs_0zs_DWyyq1pt6Zz836XBsIn72VRSpvW9pDAv5__Dr5uWHwB4qJ-DhNb-tKYX8tE2sVgp9bggv12880y9qM1T9SWuHXLQc8NjnV_H8rXen8J6lcG9vCbmhx1jI8grSVcfhyoWkXJHdFQovRj9QGFRcZPtn32jU_31S6BmZ-Gqp-ikmZQBcuAsRjwGS67BmXO-nSS4WbF_8WT9U592ZrsSetPAqVSQN2VMRzY9ohaJDTZhRlMVlA1mHJsZOd", dateAdded = System.currentTimeMillis())
    )

    // Synchronized Library merging local and cloud tracks
    val synchronizedLibrary: StateFlow<List<Track>> = localTracks.map { localList ->
        val mergedMap = mutableMapOf<String, TrackEntity>()
        cloudTracks.forEach { mergedMap[it.id] = it }
        localList.forEach { mergedMap[it.id] = it } // Local overrides cloud if same ID
        mergedMap.values.map { entity ->
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search Feature
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val searchResults: StateFlow<List<Track>> = kotlinx.coroutines.flow.combine(synchronizedLibrary, _searchQuery) { lib, query ->
        if (query.isBlank()) emptyList()
        else lib.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.artist.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    fun importFolder(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val app = getApplication<Application>()
            val contentResolver = app.contentResolver
            val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(app, uri)
            
            documentFile?.listFiles()?.forEach { file ->
                val name = file.name ?: ""
                if (file.isFile && (name.endsWith(".mp3", ignoreCase = true) || 
                    name.endsWith(".flac", ignoreCase = true) ||
                    name.endsWith(".m4a", ignoreCase = true) ||
                    name.endsWith(".wav", ignoreCase = true) ||
                    name.endsWith(".ogg", ignoreCase = true))) {
                    
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(app, file.uri)
                        val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
                        val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLongOrNull() ?: 0L
                        val durationFormatted = String.format("%d:%02d", java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMs), 
                            java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(durationMs) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMs)))
                        
                        val trackEntity = TrackEntity(
                            id = "imported_${java.util.UUID.randomUUID()}",
                            title = title,
                            artist = artist,
                            album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album",
                            albumArtist = artist,
                            genre = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "",
                            year = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0,
                            trackNumber = 1,
                            discNumber = 1,
                            duration = (durationMs / 1000).toInt(),
                            bitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0,
                            sampleRate = 44100,
                            format = "audio",
                            filePath = file.uri.toString(),
                            fileSize = file.length(),
                            artworkPath = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=200",
                            dateAdded = System.currentTimeMillis()
                        )
                        trackDao.insertTrack(trackEntity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            retriever.release()
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    private val sessionPrefs = application.getSharedPreferences("crysta_session_v1", android.content.Context.MODE_PRIVATE)

    private fun saveSession() {
        val editor = sessionPrefs.edit()
        editor.putFloat("volume", _volume.value)
        val track = _currentTrack.value
        if (track != null) {
            editor.putString("track_id", track.id)
            editor.putString("track_title", track.title)
            editor.putString("track_artist", track.artist)
            editor.putString("track_duration", track.duration)
            editor.putString("track_image_url", track.imageUrl)
            editor.putString("track_file_path", track.filePath)
            editor.putString("track_genre", track.genre)
        } else {
            editor.remove("track_id")
        }
        editor.apply()
    }

    private val _currentTrack = MutableStateFlow<Track?>(
        Track(
            id = "1",
            title = "Midnight City",
            artist = "M83",
            duration = "4:03",
            imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB09eN9Abqx8XFiF0yRGIA-y-ze1ARziS7SRJmsUyDM-dEFiMMmtjaZvLlPls6eoYtzfSshP5uia61r9QIBjpJe2rzBPb24TFvIHgjgmvzi8R-GOcc73J5ZccqGqS2jfhvBIXKEtxZusjvEVSctlJKrjxUqd0reEqu2cOA7_hMfyxu9jm_8W7XaKgvsi7v4UZh5Qq6HfrEUM8wHJMwj6ZtxO5IxbOyDp97-SKvuehyF9afHmGJdNgmNPCBxhkwV67qqmmsQuG7ivMyb"
        )
    )
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()
    
    private val _queue = MutableStateFlow<List<Track>>(
        listOf(
            Track("1", "Midnight City", "M83", "4:03", "https://lh3.googleusercontent.com/aida-public/AB6AXuB09eN9Abqx8XFiF0yRGIA-y-ze1ARziS7SRJmsUyDM-dEFiMMmtjaZvLlPls6eoYtzfSshP5uia61r9QIBjpJe2rzBPb24TFvIHgjgmvzi8R-GOcc73J5ZccqGqS2jfhvBIXKEtxZusjvEVSctlJKrjxUqd0reEqu2cOA7_hMfyxu9jm_8W7XaKgvsi7v4UZh5Qq6HfrEUM8wHJMwj6ZtxO5IxbOyDp97-SKvuehyF9afHmGJdNgmNPCBxhkwV67qqmmsQuG7ivMyb"),
            Track("2", "Nightcall", "Kavinsky", "4:19", "https://lh3.googleusercontent.com/aida-public/AB6AXuBBEtoMVVFUzB83CnpCLLYXTr43Jig9eeoY-9-mPD-JxbAFK34_ATCUc-1yMgJk0CSHXUbMw7wEcGYUHVOgr6x4q10vVLSsJPX9-g3YzexBw7hxISrHeBcsDPd1RRluwrBqF044Dd3ntM2DQz-0U3QO6qC4eaUe10I2jSIpcNeHwxGF6-Yhb-7XDr1Ed-s5--_FD_0a__7ifb4E5BUJopP74LoSpUPlyXIN8koLo3k0LsBqbIagsysNnzGiaUqrjhSYvD28hsmYoqNs"),
            Track("3", "Tech Noir", "Gunship", "4:57", "https://lh3.googleusercontent.com/aida-public/AB6AXuDMKNDi5OybO4bNSthBslKCbQWGKFhvNcEAdksG8_3ImWV1Q5nvNgyd-_vaL5GVD6A10I8DNPyxpl9Pw28U8U6toBHR782OGxJ4Ii2j_cWUlUN3FNGdiQkv-7RMa2GMlJqzKmRcLf4Q4iieXC6Js51hlazr0ObcZ0NhEPIsBs4BLnyl8kNX1nmUydMul-_4wjU8q5aBI2XGUY3Mr3J3NBzPV1O8h7pf1E_EaBnZwWvoN447paZSFd3Ry4Za7NRnnkbRGo8I1O9NsbdN")
        )
    )
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Playback Engine Queue Features (Phase 3, Task 3.3)
    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _playbackHistory = MutableStateFlow<List<Track>>(emptyList())
    val playbackHistory: StateFlow<List<Track>> = _playbackHistory.asStateFlow()

    // Core settings states (Phase 5)
    private val _crossfadeEnabled = MutableStateFlow(true)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDuration = MutableStateFlow(3.0f)
    val crossfadeDuration: StateFlow<Float> = _crossfadeDuration.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(true)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("SYNTHWAVE")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _equalizerBands = MutableStateFlow(listOf(4f, 2f, -1f, 3f, 5f))
    val equalizerBands: StateFlow<List<Float>> = _equalizerBands.asStateFlow()

    private val _spatialAudio = MutableStateFlow(false)
    val spatialAudio: StateFlow<Boolean> = _spatialAudio.asStateFlow()

    private val _hapticIntensity = MutableStateFlow("DYNAMIC")
    val hapticIntensity: StateFlow<String> = _hapticIntensity.asStateFlow()

    private val _cyberAccentColor = MutableStateFlow("NEON TOKYO")
    val cyberAccentColor: StateFlow<String> = _cyberAccentColor.asStateFlow()

    private val _visualizerStyle = MutableStateFlow("NEON_PULSE")
    val visualizerStyle: StateFlow<String> = _visualizerStyle.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _audioQuality = MutableStateFlow("STATION (320kbps)")
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()
    
    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow(0)
    val sleepTimerSecondsLeft: StateFlow<Int> = _sleepTimerSecondsLeft.asStateFlow()
    
    private var timerJob: Job? = null

    // Context-Aware Routing & Stream vs. Download Toggle (Phase 2 & 5)
    private val _isDownloadToggleActive = MutableStateFlow(false) // false = STREAM, true = DOWNLOAD
    val isDownloadToggleActive: StateFlow<Boolean> = _isDownloadToggleActive.asStateFlow()

    private val _streamingSourcePriority = MutableStateFlow(listOf("YouTube Music", "YouTube Standard", "Direct HTTP"))
    val streamingSourcePriority: StateFlow<List<String>> = _streamingSourcePriority.asStateFlow()

    private val _downloadSourcePriority = MutableStateFlow(listOf("SoundCloud", "YouTube Music", "Direct HTTP"))
    val downloadSourcePriority: StateFlow<List<String>> = _downloadSourcePriority.asStateFlow()

    private val _defaultTapAction = MutableStateFlow("STREAM") // "STREAM" or "DOWNLOAD" or "PROMPT"
    val defaultTapAction: StateFlow<String> = _defaultTapAction.asStateFlow()

    private val _autoDownloadOnWifi = MutableStateFlow(false)
    val autoDownloadOnWifi: StateFlow<Boolean> = _autoDownloadOnWifi.asStateFlow()

    private val _downloadMinQualityBitrate = MutableStateFlow(256) // minimum acceptable bitrate for download
    val downloadMinQualityBitrate: StateFlow<Int> = _downloadMinQualityBitrate.asStateFlow()

    private val _quicEnabled = MutableStateFlow(true)
    val quicEnabled: StateFlow<Boolean> = _quicEnabled.asStateFlow()

    fun toggleStreamDownloadMode() {
        _isDownloadToggleActive.update { !it }
        saveRoutingSettings()
    }

    fun toggleQuicStream() {
        _quicEnabled.update { !it }
        savePlaybackSettings()
    }

    fun setStreamingSourcePriority(priority: List<String>) {
        _streamingSourcePriority.value = priority
    }

    fun setDownloadSourcePriority(priority: List<String>) {
        _downloadSourcePriority.value = priority
    }

    fun setDefaultTapAction(action: String) {
        _defaultTapAction.value = action
    }

    fun toggleAutoDownloadOnWifi() {
        _autoDownloadOnWifi.update { !it }
        saveDownloadSettings()
    }

    fun setDownloadMinQualityBitrate(bitrate: Int) {
        _downloadMinQualityBitrate.value = bitrate
        saveDownloadSettings()
    }

    fun applySettingsFromSync(
        accentColor: String,
        crossfade: Boolean,
        crossfadeDur: Float,
        isDownloadToggle: Boolean,
        streamingPriority: List<String>,
        downloadPriority: List<String>,
        autoDownloadWiFi: Boolean,
        minQualityBitrate: Int
    ) {
        _cyberAccentColor.value = accentColor
        _crossfadeEnabled.value = crossfade
        _crossfadeDuration.value = crossfadeDur
        _isDownloadToggleActive.value = isDownloadToggle
        _streamingSourcePriority.value = streamingPriority
        _downloadSourcePriority.value = downloadPriority
        _autoDownloadOnWifi.value = autoDownloadWiFi
        _downloadMinQualityBitrate.value = minQualityBitrate
    }

    // Theme support derived values
    val themePrimary: StateFlow<Color> = _cyberAccentColor.map { color ->
        when (color) {
            "NEON TOKYO" -> com.example.ui.theme.TokyoBlue
            "DATA GLITCH" -> com.example.ui.theme.GlitchGreen
            "ACID RAIN" -> com.example.ui.theme.AcidRed
            "CYAN" -> com.example.ui.theme.CyberCyan
            "MAGENTA" -> com.example.ui.theme.NeonMagenta
            "GREEN" -> com.example.ui.theme.PrimaryGreen
            "ORANGE" -> Color(0xFFFF8800)
            else -> com.example.ui.theme.TokyoBlue
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.theme.TokyoBlue)

    val themeSecondary: StateFlow<Color> = _cyberAccentColor.map { color ->
        when (color) {
            "NEON TOKYO" -> com.example.ui.theme.TokyoPink
            "DATA GLITCH" -> Color(0xFF00AA22) // Brighter secondary for visibility instead of black
            "ACID RAIN" -> com.example.ui.theme.AcidPurple
            "CYAN" -> com.example.ui.theme.NeonMagenta
            "MAGENTA" -> com.example.ui.theme.CyberCyan
            "GREEN" -> com.example.ui.theme.CyberCyan
            "ORANGE" -> com.example.ui.theme.NeonMagenta
            else -> com.example.ui.theme.TokyoPink
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.theme.TokyoPink)

    init {
        // Asynchronously initialize SurrealDB database schema upon application launch to ensure integrity
        viewModelScope.launch(Dispatchers.IO) {
            val result = surrealDbService.initializeDatabase()
            result.onSuccess {
                android.util.Log.d("MusicViewModel", "SurrealDB schema initialized successfully.")
            }.onFailure { err ->
                android.util.Log.e("MusicViewModel", "SurrealDB schema initialization failed: ${err.message}")
            }
        }

        // Load persistency settings if available
        val pb = settingsManager.loadPlaybackSettings()
        _crossfadeEnabled.value = pb.crossfadeEnabled
        _crossfadeDuration.value = pb.crossfadeDuration
        _equalizerPreset.value = pb.equalizerPreset
        _spatialAudio.value = pb.spatialAudio
        _hapticIntensity.value = pb.hapticIntensity
        _quicEnabled.value = pb.quicEnabled

        val dl = settingsManager.loadDownloadSettings()
        _autoDownloadOnWifi.value = dl.autoDownloadOnWifi
        _downloadMinQualityBitrate.value = dl.minQualityBitrate

        val vis = settingsManager.loadVisualSettings()
        _cyberAccentColor.value = vis.cyberAccentColor
        _visualizerStyle.value = vis.visualizerStyle

        val rt = settingsManager.loadRoutingSettings()
        _isDownloadToggleActive.value = rt.isDownloadToggleActive

        // Restore volume and track from session
        val savedVolume = sessionPrefs.getFloat("volume", 0.8f)
        _volume.value = savedVolume
        player.volume = savedVolume

        val savedTrackId = sessionPrefs.getString("track_id", null)
        if (savedTrackId != null) {
            val restoredTrack = Track(
                id = savedTrackId,
                title = sessionPrefs.getString("track_title", "") ?: "",
                artist = sessionPrefs.getString("track_artist", "") ?: "",
                duration = sessionPrefs.getString("track_duration", "0:00") ?: "0:00",
                imageUrl = sessionPrefs.getString("track_image_url", "") ?: "",
                filePath = sessionPrefs.getString("track_file_path", "") ?: "",
                genre = sessionPrefs.getString("track_genre", "") ?: ""
            )
            _currentTrack.value = restoredTrack
            // Pre-load restored track in ExoPlayer
            val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(restoredTrack.title)
                .setArtist(restoredTrack.artist)
                .build()
            val mediaItem = androidx.media3.common.MediaItem.Builder()
                .setUri(restoredTrack.filePath)
                .setMediaId(restoredTrack.id)
                .setMediaMetadata(mediaMetadata)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
        }

        // Progress simulator loop running asynchronously on a background thread pool (Dispatchers.Default)
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(100L)
                if (_isPlaying.value) {
                    val track = _currentTrack.value
                    if (track != null) {
                        val totalSecs = try {
                            val parts = track.duration.split(":")
                            val mins = parts[0].toInt()
                            val secs = parts[1].toInt()
                            mins * 60 + secs
                        } catch (e: Exception) {
                            240
                        }
                        val increment = 0.1f / totalSecs
                        _playbackProgress.update { curr ->
                            val next = curr + increment
                            if (next >= 1.0f) {
                                when (_repeatMode.value) {
                                    RepeatMode.ONE -> {
                                        0.0f // repeat same track
                                    }
                                    RepeatMode.ALL -> {
                                        nextTrack()
                                        0.0f
                                    }
                                    RepeatMode.NONE -> {
                                        val currentIdx = _queue.value.indexOfFirst { it.id == track.id }
                                        if (currentIdx != -1 && currentIdx < _queue.value.size - 1) {
                                            nextTrack()
                                            0.0f
                                        } else {
                                            _isPlaying.value = false
                                            1.0f
                                        }
                                    }
                                }
                            } else {
                                next
                            }
                        }
                    }
                }
            }
        }
        
        // Track observation stream running asynchronously on background dispatcher (Dispatchers.Default)
        viewModelScope.launch(Dispatchers.Default) {
            _currentTrack.collect { t ->
                _playbackProgress.value = 0.0f
                if (t != null) {
                    // Save to history (Phase 3, Task 3.3)
                    val currHistory = _playbackHistory.value.toMutableList()
                    currHistory.remove(t)
                    currHistory.add(0, t)
                    _playbackHistory.value = currHistory.take(50)

                    // Increment play count (Phase 2, Task 2.1)
                    withContext(Dispatchers.IO) {
                        trackDao.incrementPlayCount(t.id)
                    }
                }
                saveSession()
            }
        }
    }

    // Playback modifiers
    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    fun playTrack(track: Track) {
        val currentRecent = _recentlyPlayed.value.toMutableList()
        currentRecent.removeAll { it.id == track.id }
        currentRecent.add(0, track)
        if (currentRecent.size > 10) {
            currentRecent.removeLast()
        }
        _recentlyPlayed.value = currentRecent

        if (_crossfadeEnabled.value && _isPlaying.value && _currentTrack.value != null) {
            viewModelScope.launch {
                // Fade out
                val fadeSteps = 10
                val fadeDuration = (_crossfadeDuration.value * 1000).toLong()
                val stepDuration = fadeDuration / fadeSteps
                
                for (i in fadeSteps downTo 1) {
                    player.volume = (i / fadeSteps.toFloat()) * _volume.value
                    delay(stepDuration)
                }
                
                _currentTrack.value = track
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .build()
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri(track.filePath)
                    .setMediaId(track.id)
                    .setMediaMetadata(mediaMetadata)
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                
                // Fade in
                for (i in 1..fadeSteps) {
                    player.volume = (i / fadeSteps.toFloat()) * _volume.value
                    delay(stepDuration)
                }
                player.volume = _volume.value
            }
        } else {
            _currentTrack.value = track
            _isPlaying.value = true
            val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .build()
            val mediaItem = androidx.media3.common.MediaItem.Builder()
                .setUri(track.filePath)
                .setMediaId(track.id)
                .setMediaMetadata(mediaMetadata)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            player.volume = _volume.value
        }
    }
    
    fun togglePlayback() {
        _isPlaying.update { !it }
        if (_isPlaying.value) {
            player.play()
        } else {
            player.pause()
        }
    }

    fun nextTrack() {
        val current = _currentTrack.value
        val q = _queue.value
        if (current != null && q.isNotEmpty()) {
            val currentIndex = q.indexOfFirst { it.id == current.id }
            val nextIndex = if (_shuffleEnabled.value) {
                (0 until q.size).filter { it != currentIndex }.randomOrNull() ?: 0
            } else {
                (currentIndex + 1) % q.size
            }
            playTrack(q[nextIndex])
        }
    }

    fun previousTrack() {
        val current = _currentTrack.value
        val q = _queue.value
        if (current != null && q.isNotEmpty()) {
            val currentIndex = q.indexOfFirst { it.id == current.id }
            val prevIndex = if (currentIndex - 1 < 0) q.size - 1 else currentIndex - 1
            playTrack(q[prevIndex])
        }
    }

    // Advanced Play Queue Modifiers (Phase 3, Task 3.3)
    fun addToQueue(track: Track, position: QueuePosition = QueuePosition.END) {
        val q = _queue.value.toMutableList()
        val current = _currentTrack.value
        
        // Remove existing to avoid duplicates in the live stream
        q.removeAll { it.id == track.id }

        when (position) {
            QueuePosition.NOW -> {
                if (current != null) {
                    val idx = q.indexOfFirst { it.id == current.id }
                    if (idx != -1) {
                        q.add(idx + 1, track)
                    } else {
                        q.add(0, track)
                    }
                } else {
                    q.add(0, track)
                }
                playTrack(track)
            }
            QueuePosition.NEXT -> {
                if (current != null) {
                    val idx = q.indexOfFirst { it.id == current.id }
                    if (idx != -1) {
                        q.add(idx + 1, track)
                    } else {
                        q.add(0, track)
                    }
                } else {
                    q.add(0, track)
                }
            }
            QueuePosition.END -> {
                q.add(track)
            }
        }
        _queue.value = q
    }

    fun toggleShuffle() {
        _shuffleEnabled.update { !it }
        if (_shuffleEnabled.value) {
            val current = _currentTrack.value
            val q = _queue.value.toMutableList()
            if (q.isNotEmpty()) {
                if (current != null) {
                    q.remove(current)
                    q.shuffle()
                    q.add(0, current)
                } else {
                    q.shuffle()
                }
                _queue.value = q
            }
        }
    }

    fun toggleRepeat() {
        _repeatMode.update {
            when (it) {
                RepeatMode.NONE -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.NONE
            }
        }
    }

    fun setQueue(tracks: List<Track>) {
        _queue.value = tracks
    }
    
    fun toggleCrossfade() {
        _crossfadeEnabled.update { !it }
        savePlaybackSettings()
    }

    fun setCrossfadeDuration(duration: Float) {
        _crossfadeDuration.value = duration
        savePlaybackSettings()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        _equalizerBands.value = when (preset) {
            "SYNTHWAVE" -> listOf(4f, 2f, -1f, 3f, 5f)
            "INDUSTRIAL" -> listOf(6f, 3f, -2f, 1f, -1f)
            "CYBER-BASS" -> listOf(8f, 5f, 1f, 0f, -2f)
            "AMBIENT" -> listOf(-2f, -1f, 2f, 4f, 3f)
            "BYPASS" -> listOf(0f, 0f, 0f, 0f, 0f)
            "ROCK" -> listOf(5f, 3f, -1f, 2f, 4f)
            "JAZZ" -> listOf(4f, 2f, 1f, 2f, -1f)
            "CLASSICAL" -> listOf(3f, 2f, 0f, 2f, 3f)
            "BASS BOOST" -> listOf(8f, 6f, 2f, 0f, -2f)
            "TREBLE BOOST" -> listOf(-2f, 0f, 2f, 6f, 8f)
            "VOCAL BOOST" -> listOf(-2f, 2f, 5f, 3f, -1f)
            else -> _equalizerBands.value
        }
        savePlaybackSettings()
    }

    fun setEqualizerBand(index: Int, value: Float) {
        val current = _equalizerBands.value.toMutableList()
        if (index in current.indices) {
            current[index] = value
            _equalizerBands.value = current
            _equalizerPreset.value = "CUSTOM"
            savePlaybackSettings()
        }
    }

    fun toggleSpatialAudio() {
        _spatialAudio.update { !it }
        savePlaybackSettings()
    }

    fun setHapticIntensity(intensity: String) {
        _hapticIntensity.value = intensity
        savePlaybackSettings()
    }

    private fun savePlaybackSettings() {
        val pb = com.example.core.PlaybackSettings(
            crossfadeEnabled = _crossfadeEnabled.value,
            crossfadeDuration = _crossfadeDuration.value,
            equalizerPreset = _equalizerPreset.value,
            spatialAudio = _spatialAudio.value,
            hapticIntensity = _hapticIntensity.value,
            quicEnabled = _quicEnabled.value
        )
        settingsManager.savePlaybackSettings(pb)
    }

    private fun saveDownloadSettings() {
        val dl = com.example.core.DownloadSettings(
            autoDownloadOnWifi = _autoDownloadOnWifi.value,
            minQualityBitrate = _downloadMinQualityBitrate.value
        )
        settingsManager.saveDownloadSettings(dl)
    }

    private fun saveVisualSettings() {
        val vis = com.example.core.VisualSettings(
            cyberAccentColor = _cyberAccentColor.value,
            visualizerStyle = _visualizerStyle.value
        )
        settingsManager.saveVisualSettings(vis)
    }

    private fun saveRoutingSettings() {
        val rt = com.example.core.RoutingSettings(
            isDownloadToggleActive = _isDownloadToggleActive.value
        )
        settingsManager.saveRoutingSettings(rt)
    }

    fun setCyberAccentColor(color: String) {
        _cyberAccentColor.value = color
        saveVisualSettings()
    }

    fun setVisualizerStyle(style: String) {
        _visualizerStyle.value = style
        saveVisualSettings()
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        if (player != null) {
            player.volume = _volume.value
        }
        saveSession()
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
    }

    fun toggleOfflineMode() {
        _offlineMode.update { !it }
    }
    
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        _sleepTimerSecondsLeft.value = minutes * 60
        timerJob?.cancel()
        if (minutes > 0) {
            timerJob = viewModelScope.launch {
                while (_sleepTimerSecondsLeft.value > 0) {
                    delay(1000L)
                    _sleepTimerSecondsLeft.update { it - 1 }
                }
                _isPlaying.value = false
                player.pause()
                _sleepTimerMinutes.value = 0
            }
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoriteTracks.value.any { it.id == track.id }
            if (isFav) {
                favoriteDao.deleteFavoriteById(track.id)
            } else {
                favoriteDao.insertFavorite(
                    FavoriteTrack(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        duration = track.duration,
                        imageUrl = track.imageUrl
                    )
                )
            }
        }
    }

    // Custom Playlist Management
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylist
        .flatMapLatest { playlist ->
            if (playlist == null) {
                flowOf(emptyList())
            } else {
                playlistTrackDao.getTracksForPlaylist(playlist.id).map { entities ->
                    entities.map { entity ->
                        Track(
                            id = entity.id,
                            title = entity.title,
                            artist = entity.artist ?: "Unknown Artist",
                            duration = String.format("%d:%02d", entity.duration / 60, entity.duration % 60),
                            imageUrl = entity.artworkPath ?: "",
                            filePath = entity.filePath
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = "playlist_" + System.currentTimeMillis()
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    id = id,
                    name = name,
                    description = description,
                    isSmart = false
                )
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylistById(playlistId)
            playlistTrackDao.clearPlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
        }
    }

    fun addTrackToPlaylist(track: Track, playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingTrack = trackDao.getTrackById(track.id)
            if (existingTrack == null) {
                trackDao.insertTrack(
                    TrackEntity(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = "Custom",
                        albumArtist = "Unknown",
                        genre = "CUSTOM",
                        year = 2026,
                        trackNumber = 1,
                        discNumber = 1,
                        duration = try {
                            val parts = track.duration.split(":")
                            parts[0].toInt() * 60 + parts[1].toInt()
                        } catch (e: Exception) { 240 },
                        bitrate = 320,
                        sampleRate = 44100,
                        format = "mp3",
                        filePath = track.filePath,
                        fileSize = 1000000L,
                        artworkPath = track.imageUrl,
                        dateAdded = System.currentTimeMillis()
                    )
                )
            }
            val currentTracks = playlistTrackDao.getAllPlaylistTracks().filter { it.playlistId == playlistId }
            val nextPos = (currentTracks.maxOfOrNull { it.position } ?: -1) + 1
            playlistTrackDao.insertPlaylistTrack(
                com.example.model.PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = track.id,
                    position = nextPos
                )
            )
        }
    }

    fun removeTrackFromPlaylist(trackId: String, playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistTrackDao.deletePlaylistTrack(playlistId, trackId)
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    // Convert TrackEntity back to Track UI model helper
    fun TrackEntity.toUiTrack() = Track(
        id = id,
        title = title,
        artist = artist ?: "Unknown",
        duration = "${duration / 60}:${(duration % 60).toString().padStart(2, '0')}",
        imageUrl = artworkPath ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuB09eN9Abqx8XFiF0yRGIA-y-ze1ARziS7SRJmsUyDM-dEFiMMmtjaZvLlPls6eoYtzfSshP5uia61r9QIBjpJe2rzBPb24TFvIHgjgmvzi8R-GOcc73J5ZccqGqS2jfhvBIXKEtxZusjvEVSctlJKrjxUqd0reEqu2cOA7_hMfyxu9jm_8W7XaKgvsi7v4UZh5Qq6HfrEUM8wHJMwj6ZtxO5IxbOyDp97-SKvuehyF9afHmGJdNgmNPCBxhkwV67qqmmsQuG7ivMyb"
    )
}
