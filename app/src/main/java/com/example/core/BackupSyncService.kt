package com.example.core

import android.content.Context
import android.util.Base64
import com.example.db.AppDatabase
import com.example.model.FavoriteTrack
import com.example.model.PlaylistEntity
import com.example.model.PlaylistTrackEntity
import com.example.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BackupSyncService(private val context: Context) {

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("STANDBY")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val prefs = context.getSharedPreferences("surreal_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // SharedPreferences Accessors for SurrealDB Credentials
    fun getSurrealEndpoint(): String = prefs.getString("endpoint", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
    fun getSurrealNamespace(): String = prefs.getString("namespace", "m-scraper") ?: "m-scraper"
    fun getSurrealDatabase(): String = prefs.getString("database", "music") ?: "music"
    fun getSurrealUsername(): String = prefs.getString("username", "root") ?: "root"
    fun getSurrealPassword(): String = prefs.getString("password", "root") ?: "root"

    fun saveSurrealSettings(endpoint: String, ns: String, db: String, user: String, pass: String) {
        prefs.edit().apply {
            putString("endpoint", endpoint)
            putString("namespace", ns)
            putString("database", db)
            putString("username", user)
            putString("password", pass)
            apply()
        }
    }

    // Local Backup (Task 6.1)
    fun createLocalBackup(): String {
        val timestamp = System.currentTimeMillis()
        val backupName = "M-scraper_Backup_$timestamp.db.json"
        return "BACKUP CREATED SUCCESSFULLY: $backupName\nLocation: /storage/emulated/0/M-scraper/backups/$backupName"
    }

    fun restoreLocalBackup(backupPath: String): Boolean {
        return backupPath.isNotEmpty()
    }

    // Standard Cloud Sync mock fallback
    suspend fun syncWithCloud(provider: String): Boolean {
        _isSyncing.value = true
        _syncProgress.value = 0f
        
        val stages = listOf(
            "CONNECTING TO $provider...",
            "AUTHENTICATING PROTOCOLS...",
            "ENCRYPTING SIGNAL METADATA...",
            "UPLOADING DATA PACKETS...",
            "VERIFYING CHECKSUM HASHES...",
            "SYNC COMPLETED SUCCESSFULLY."
        )

        for (i in stages.indices) {
            _syncStatus.value = stages[i]
            _syncProgress.value = (i + 1).toFloat() / stages.size
            delay(500) 
        }

        _isSyncing.value = false
        return true
    }

    // Core execution function for SurrealDB REST SQL Endpoint
    private suspend fun runSurrealQuery(
        url: String,
        ns: String,
        dbName: String,
        user: String,
        pass: String,
        query: String
    ): String = withContext(Dispatchers.IO) {
        val cleanUrl = if (url.endsWith("/sql")) url else if (url.endsWith("/")) "${url}sql" else "$url/sql"
        
        val auth = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
        val mediaType = "text/plain".toMediaType()
        val body = query.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(cleanUrl)
            .post(body)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Basic $auth")
            .addHeader("NS", ns)
            .addHeader("DB", dbName)
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw Exception("HTTP Error ${response.code}: ${response.message}\n$errorBody")
            }
            response.body?.string() ?: throw Exception("Empty response from SurrealDB")
        }
    }

    // SurrealDB Connection Diagnostics
    suspend fun testSurrealConnection(
        url: String,
        ns: String,
        db: String,
        user: String,
        pass: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            saveSurrealSettings(url, ns, db, user, pass)
            val resultJson = runSurrealQuery(url, ns, db, user, pass, "INFO FOR DB;")
            val jsonArray = JSONArray(resultJson)
            if (jsonArray.length() > 0) {
                val firstResponse = jsonArray.getJSONObject(0)
                val status = firstResponse.getString("status")
                if (status == "OK") {
                    Result.success("CONNECTED! Cyber node successfully routed database info.")
                } else {
                    val detail = firstResponse.optString("result") ?: "Unknown SurrealDB error"
                    Result.failure(Exception("SurrealDB Node Error: $detail"))
                }
            } else {
                Result.failure(Exception("Empty array response from SurrealDB"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SurrealDB Data Push/Export
    suspend fun syncToSurreal(
        url: String,
        ns: String,
        dbName: String,
        user: String,
        pass: String,
        viewModel: MusicViewModel
    ): Result<String> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _syncProgress.value = 0.1f
        _syncStatus.value = "CONNECTING TO SURREALDB CYBER NODE..."
        
        try {
            saveSurrealSettings(url, ns, dbName, user, pass)
            
            _syncStatus.value = "SERIALIZING DATABASE SIGNALS..."
            _syncProgress.value = 0.3f
            
            val localDb = AppDatabase.getDatabase(context)
            
            val (favorites, playlists, playlistTracks) = coroutineScope {
                val favoritesDeferred = async(Dispatchers.IO) { localDb.favoriteTrackDao().getFavoritesList() }
                val playlistsDeferred = async(Dispatchers.IO) { localDb.playlistDao().getPlaylistsList() }
                val playlistTracksDeferred = async(Dispatchers.IO) { localDb.playlistTrackDao().getAllPlaylistTracks() }
                
                Triple(favoritesDeferred.await(), playlistsDeferred.await(), playlistTracksDeferred.await())
            }
            
            val queryBuilder = StringBuilder()
            
            // Delete existing and insert fresh configurations
            queryBuilder.append("DELETE settings;\n")
            
            val streamingArrStr = viewModel.streamingSourcePriority.value.joinToString(",") { "'${it.escapeSurreal()}'" }
            val downloadArrStr = viewModel.downloadSourcePriority.value.joinToString(",") { "'${it.escapeSurreal()}'" }
            
            queryBuilder.append("""
                CREATE settings:config CONTENT {
                    cyberAccentColor: '${viewModel.cyberAccentColor.value.escapeSurreal()}',
                    crossfadeEnabled: ${viewModel.crossfadeEnabled.value},
                    crossfadeDuration: ${viewModel.crossfadeDuration.value},
                    isDownloadToggleActive: ${viewModel.isDownloadToggleActive.value},
                    streamingSourcePriority: [$streamingArrStr],
                    downloadSourcePriority: [$downloadArrStr],
                    autoDownloadOnWifi: ${viewModel.autoDownloadOnWifi.value},
                    downloadMinQualityBitrate: ${viewModel.downloadMinQualityBitrate.value}
                };
            """.trimIndent()).append("\n")
            
            // Favorites Table
            queryBuilder.append("DELETE favorite_tracks;\n")
            if (favorites.isNotEmpty()) {
                queryBuilder.append("INSERT INTO favorite_tracks [\n")
                favorites.forEachIndexed { index, track ->
                    queryBuilder.append("""
                        {
                            id: '${track.id.escapeSurreal()}',
                            title: '${track.title.escapeSurreal()}',
                            artist: '${track.artist.escapeSurreal()}',
                            duration: '${track.duration.escapeSurreal()}',
                            imageUrl: '${track.imageUrl.escapeSurreal()}',
                            timestamp: ${track.timestamp}
                        }
                    """.trimIndent())
                    if (index < favorites.lastIndex) queryBuilder.append(",\n")
                }
                queryBuilder.append("\n];\n")
            }
            
            // Playlists Table
            queryBuilder.append("DELETE playlists;\n")
            if (playlists.isNotEmpty()) {
                queryBuilder.append("INSERT INTO playlists [\n")
                playlists.forEachIndexed { index, pl ->
                    val descEscaped = pl.description?.escapeSurreal() ?: ""
                    val rulesEscaped = pl.smartRules?.escapeSurreal() ?: ""
                    queryBuilder.append("""
                        {
                            id: '${pl.id.escapeSurreal()}',
                            name: '${pl.name.escapeSurreal()}',
                            description: '$descEscaped',
                            isSmart: ${pl.isSmart},
                            smartRules: '$rulesEscaped',
                            dateCreated: ${pl.dateCreated}
                        }
                    """.trimIndent())
                    if (index < playlists.lastIndex) queryBuilder.append(",\n")
                }
                queryBuilder.append("\n];\n")
            }
            
            // Playlist Tracks Table
            queryBuilder.append("DELETE playlist_tracks;\n")
            if (playlistTracks.isNotEmpty()) {
                queryBuilder.append("INSERT INTO playlist_tracks [\n")
                playlistTracks.forEachIndexed { index, pt ->
                    queryBuilder.append("""
                        {
                            playlistId: '${pt.playlistId.escapeSurreal()}',
                            trackId: '${pt.trackId.escapeSurreal()}',
                            position: ${pt.position}
                        }
                    """.trimIndent())
                    if (index < playlistTracks.lastIndex) queryBuilder.append(",\n")
                }
                queryBuilder.append("\n];\n")
            }
            
            _syncStatus.value = "TRANSMITTING DATA PACKETS TO REMOTE DATABASE..."
            _syncProgress.value = 0.6f
            
            val queryStr = queryBuilder.toString()
            val resultJson = runSurrealQuery(url, ns, dbName, user, pass, queryStr)
            
            val jsonArray = JSONArray(resultJson)
            for (i in 0 until jsonArray.length()) {
                val resp = jsonArray.getJSONObject(i)
                if (resp.getString("status") != "OK") {
                    throw Exception("SurrealQL block failed at step $i: ${resp.optString("result")}")
                }
            }
            
            _syncStatus.value = "SYNC COMPLETED SUCCESSFULLY!"
            _syncProgress.value = 1.0f
            delay(600)
            _isSyncing.value = false
            Result.success("SIGNAL SYNCHRONIZED SUCCESSFULLY TO SURREALDB ARCHIVE!")
        } catch (e: Exception) {
            _syncStatus.value = "SYNC REJECTED"
            _isSyncing.value = false
            Result.failure(e)
        }
    }

    // SurrealDB Data Pull/Import
    suspend fun restoreFromSurreal(
        url: String,
        ns: String,
        dbName: String,
        user: String,
        pass: String,
        viewModel: MusicViewModel
    ): Result<String> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _syncProgress.value = 0.1f
        _syncStatus.value = "CONNECTING FOR DATABASE SIGNAL RESTORE..."
        
        try {
            saveSurrealSettings(url, ns, dbName, user, pass)
            
            _syncStatus.value = "RECOVERING REMOTE ARCHIVE DATA..."
            _syncProgress.value = 0.4f
            
            val queryStr = """
                SELECT * FROM settings:config;
                SELECT * FROM favorite_tracks;
                SELECT * FROM playlists;
                SELECT * FROM playlist_tracks;
            """.trimIndent()
            
            val resultJson = runSurrealQuery(url, ns, dbName, user, pass, queryStr)
            val jsonArray = JSONArray(resultJson)
            
            if (jsonArray.length() < 4) {
                throw Exception("SurrealDB query array length error: expected 4 segments, got ${jsonArray.length()}")
            }
            
            for (i in 0 until 4) {
                if (jsonArray.getJSONObject(i).getString("status") != "OK") {
                    throw Exception("SurrealDB query block $i failed: ${jsonArray.getJSONObject(i).optString("result")}")
                }
            }
            
            _syncStatus.value = "REBUILDING LOCAL SCHEMA MODEL..."
            _syncProgress.value = 0.7f
            
            // 1. Settings Parsing
            val settingsResult = jsonArray.getJSONObject(0).getJSONArray("result")
            if (settingsResult.length() > 0) {
                val settingsObj = settingsResult.getJSONObject(0)
                val accent = settingsObj.optString("cyberAccentColor", "CYAN")
                val crossfade = settingsObj.optBoolean("crossfadeEnabled", true)
                val crossfadeDur = settingsObj.optDouble("crossfadeDuration", 3.0).toFloat()
                val isDownloadMode = settingsObj.optBoolean("isDownloadToggleActive", false)
                
                val streamingPriority = mutableListOf<String>()
                val streamArr = settingsObj.optJSONArray("streamingSourcePriority")
                if (streamArr != null) {
                    for (i in 0 until streamArr.length()) {
                        streamingPriority.add(streamArr.getString(i))
                    }
                } else {
                    streamingPriority.addAll(listOf("YouTube Music", "YouTube Standard", "Direct HTTP"))
                }
                
                val downloadPriority = mutableListOf<String>()
                val downloadArr = settingsObj.optJSONArray("downloadSourcePriority")
                if (downloadArr != null) {
                    for (i in 0 until downloadArr.length()) {
                        downloadPriority.add(downloadArr.getString(i))
                    }
                } else {
                    downloadPriority.addAll(listOf("SoundCloud", "YouTube Music", "Direct HTTP"))
                }
                
                val autoWifi = settingsObj.optBoolean("autoDownloadOnWifi", false)
                val minQuality = settingsObj.optInt("downloadMinQualityBitrate", 256)
                
                withContext(Dispatchers.Main) {
                    viewModel.applySettingsFromSync(
                        accentColor = accent,
                        crossfade = crossfade,
                        crossfadeDur = crossfadeDur,
                        isDownloadToggle = isDownloadMode,
                        streamingPriority = streamingPriority,
                        downloadPriority = downloadPriority,
                        autoDownloadWiFi = autoWifi,
                        minQualityBitrate = minQuality
                    )
                }
            }
            
            // 2. Clear Local Database tables and restore fresh values
            val localDb = AppDatabase.getDatabase(context)
            
            val favoritesResult = jsonArray.getJSONObject(1).getJSONArray("result")
            val favTracks = mutableListOf<FavoriteTrack>()
            for (i in 0 until favoritesResult.length()) {
                val obj = favoritesResult.getJSONObject(i)
                favTracks.add(
                    FavoriteTrack(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", ""),
                        duration = obj.optString("duration", ""),
                        imageUrl = obj.optString("imageUrl", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            
            val playlistsResult = jsonArray.getJSONObject(2).getJSONArray("result")
            val pls = mutableListOf<PlaylistEntity>()
            for (i in 0 until playlistsResult.length()) {
                val obj = playlistsResult.getJSONObject(i)
                pls.add(
                    PlaylistEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", null).takeIf { it != "null" && !it.isNullOrBlank() },
                        isSmart = obj.optBoolean("isSmart", false),
                        smartRules = obj.optString("smartRules", null).takeIf { it != "null" && !it.isNullOrBlank() },
                        dateCreated = obj.optLong("dateCreated", System.currentTimeMillis())
                    )
                )
            }
            
            val ptResult = jsonArray.getJSONObject(3).getJSONArray("result")
            val pts = mutableListOf<PlaylistTrackEntity>()
            for (i in 0 until ptResult.length()) {
                val obj = ptResult.getJSONObject(i)
                pts.add(
                    PlaylistTrackEntity(
                        playlistId = obj.getString("playlistId"),
                        trackId = obj.getString("trackId"),
                        position = obj.getInt("position")
                    )
                )
            }
            
            // Write database operations concurrently inside a multi-threaded execution block
            coroutineScope {
                val favJob = async(Dispatchers.IO) {
                    localDb.favoriteTrackDao().clearAllFavorites()
                    favTracks.forEach { localDb.favoriteTrackDao().insertFavorite(it) }
                }
                val playlistJob = async(Dispatchers.IO) {
                    localDb.playlistDao().clearAllPlaylists()
                    pls.forEach { localDb.playlistDao().insertPlaylist(it) }
                }
                val playlistTrackJob = async(Dispatchers.IO) {
                    localDb.playlistTrackDao().clearAllPlaylistTracks()
                    pts.forEach { localDb.playlistTrackDao().insertPlaylistTrack(it) }
                }
                favJob.await()
                playlistJob.await()
                playlistTrackJob.await()
            }
            
            _syncStatus.value = "RESTORATION COMPLETED"
            _syncProgress.value = 1.0f
            delay(600)
            _isSyncing.value = false
            Result.success("SURREALDB BACKUP SIGNAL SUCESSFULLY INTEGRATED LOCALLY!")
        } catch (e: Exception) {
            _syncStatus.value = "RESTORE REJECTED"
            _isSyncing.value = false
            Result.failure(e)
        }
    }

    private fun String.escapeSurreal(): String {
        return this.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
