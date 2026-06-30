package com.example.core

import android.content.Context
import android.util.Base64
import com.example.model.PlaylistEntity
import com.example.model.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class SurrealDbService(context: Context) {

    private val prefs = context.getSharedPreferences("surreal_sync_prefs", Context.MODE_PRIVATE)

    // Robust Connection Pool configuration to manage multiple asynchronous database connections efficiently
    private val connectionPool = ConnectionPool(10, 5, TimeUnit.MINUTES)

    private val client = OkHttpClient.Builder()
        .connectionPool(connectionPool)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Configuration Accessors
    fun getEndpoint(): String = prefs.getString("endpoint", "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080"
    fun getNamespace(): String = prefs.getString("namespace", "m-scraper") ?: "m-scraper"
    fun getDatabase(): String = prefs.getString("database", "music") ?: "music"
    fun getUsername(): String = prefs.getString("username", "root") ?: "root"
    fun getPassword(): String = prefs.getString("password", "root") ?: "root"

    /**
     * Initializes SurrealDB database schema asynchronously upon application launch.
     * Sets up table definitions for tracks, playlists, contains relation, and settings,
     * then executes database version migrations inside a single transaction to maintain structural integrity.
     */
    suspend fun initializeDatabase(): Result<String> {
        val querySetup = """
            -- Setup schema definitions for music library
            DEFINE TABLE db_meta SCHEMALESS;
            DEFINE TABLE track SCHEMALESS;
            DEFINE TABLE playlist SCHEMALESS;
            DEFINE TABLE contains SCHEMALESS;
            DEFINE TABLE settings SCHEMALESS;
            
            -- Ensure basic indices exist for fast retrieval
            DEFINE INDEX trackId ON TABLE track COLUMNS id UNIQUE;
            DEFINE INDEX playlistId ON TABLE playlist COLUMNS id UNIQUE;
        """.trimIndent()

        val setupResult = runQuery(querySetup)
        if (setupResult.isFailure) return setupResult

        return handleMigrations()
    }

    /**
     * Helper to perform incremental migrations on top of SurrealDB schema versioning
     */
    private suspend fun handleMigrations(): Result<String> {
        val checkVerQuery = "SELECT value FROM db_meta:schema_version;"
        val res = runQuery(checkVerQuery)
        var currentVersion = 0
        res.onSuccess { body ->
            try {
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val resultObj = arr.getJSONObject(0)
                    val status = resultObj.optString("status")
                    if (status == "OK") {
                        val resultArr = resultObj.optJSONArray("result")
                        if (resultArr != null && resultArr.length() > 0) {
                            currentVersion = resultArr.getJSONObject(0).optInt("value", 0)
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep version as 0 if DB or structure is brand new
            }
        }

        val targetVersion = 2
        val migrationQueries = mutableListOf<String>()

        if (currentVersion < 1) {
            migrationQueries.add("""
                -- Migration V1: Add system configurations snapshot
                INSERT INTO settings:system {
                    id: 'system',
                    initializedAt: time::now(),
                    theme: 'CYAN',
                    crossfadeEnabled: true,
                    crossfadeDuration: 3.0
                } ON DUPLICATE KEY UPDATE initializedAt = time::now();
            """.trimIndent())
        }

        if (currentVersion < 2) {
            migrationQueries.add("""
                -- Migration V2: Setup rich analytics schema definitions on track table
                DEFINE FIELD playCount ON TABLE track TYPE option<int>;
                DEFINE FIELD lastPlayed ON TABLE track TYPE option<datetime>;
            """.trimIndent())
        }

        if (migrationQueries.isNotEmpty()) {
            val fullMigrationScript = StringBuilder().apply {
                append("BEGIN TRANSACTION;\n")
                migrationQueries.forEach { append(it).append("\n") }
                append("UPDATE db_meta:schema_version SET value = $targetVersion;\n")
                append("COMMIT TRANSACTION;")
            }.toString()

            val migrationResult = runQuery(fullMigrationScript)
            if (migrationResult.isFailure) return migrationResult
        }

        return Result.success("Schema version $targetVersion successfully set up.")
    }

    /**
     * Run SurrealQL script asynchronously on Dispatchers.IO
     */
    suspend fun runQuery(query: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = getEndpoint()
            val cleanUrl = if (url.endsWith("/sql")) url else if (url.endsWith("/")) "${url}sql" else "$url/sql"
            val auth = Base64.encodeToString("${getUsername()}:${getPassword()}".toByteArray(), Base64.NO_WRAP)
            val mediaType = "text/plain".toMediaType()
            val body = query.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(cleanUrl)
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Basic $auth")
                .addHeader("NS", getNamespace())
                .addHeader("DB", getDatabase())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("SurrealDB Node Error ${response.code}: $errorBody"))
                } else {
                    val bodyStr = response.body?.string() ?: ""
                    Result.success(bodyStr)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Index a single track using multi-model content features
     */
    suspend fun indexTrack(track: TrackEntity): Result<String> {
        val escapedId = track.id.escapeSurreal()
        val query = """
            UPDATE track:`$escapedId` CONTENT {
                id: '$escapedId',
                title: '${track.title.escapeSurreal()}',
                artist: '${(track.artist ?: "").escapeSurreal()}',
                album: '${(track.album ?: "").escapeSurreal()}',
                duration: ${track.duration},
                filePath: '${track.filePath.escapeSurreal()}',
                fileSize: ${track.fileSize},
                bitrate: ${track.bitrate},
                format: '${track.format.escapeSurreal()}',
                dateAdded: ${track.dateAdded},
                indexedAt: time::now()
            };
        """.trimIndent()
        return runQuery(query)
    }

    /**
     * Groups multiple track metadata updates into a single asynchronous SurrealDB transaction
     * block to minimize HTTP request overhead, thread locking, and maximize IO throughput.
     */
    suspend fun indexTracksBatch(tracks: List<TrackEntity>): Result<String> {
        if (tracks.isEmpty()) return Result.success("[]")

        val queryBuilder = StringBuilder()
        queryBuilder.append("BEGIN TRANSACTION;\n")
        tracks.forEach { track ->
            val escapedId = track.id.escapeSurreal()
            queryBuilder.append("""
                UPDATE track:`$escapedId` CONTENT {
                    id: '$escapedId',
                    title: '${track.title.escapeSurreal()}',
                    artist: '${(track.artist ?: "").escapeSurreal()}',
                    album: '${(track.album ?: "").escapeSurreal()}',
                    duration: ${track.duration},
                    filePath: '${track.filePath.escapeSurreal()}',
                    fileSize: ${track.fileSize},
                    bitrate: ${track.bitrate},
                    format: '${track.format.escapeSurreal()}',
                    dateAdded: ${track.dateAdded},
                    indexedAt: time::now()
                };
            """.trimIndent()).append("\n")
        }
        queryBuilder.append("COMMIT TRANSACTION;")

        return runQuery(queryBuilder.toString())
    }

    /**
     * Index a playlist and establish dynamic graph relationships with tracks.
     * Uses SurrealDB's RELATE statement: RELATE playlist:<id> -> contains -> track:<trackId>
     */
    suspend fun indexPlaylist(playlist: PlaylistEntity, trackIds: List<String>): Result<String> {
        val escapedPlaylistId = playlist.id.escapeSurreal()
        val queryBuilder = StringBuilder()

        // 1. Create or update the playlist node
        queryBuilder.append("""
            UPDATE playlist:`$escapedPlaylistId` CONTENT {
                id: '$escapedPlaylistId',
                name: '${playlist.name.escapeSurreal()}',
                description: '${(playlist.description ?: "").escapeSurreal()}',
                isSmart: ${playlist.isSmart},
                dateCreated: ${playlist.dateCreated},
                indexedAt: time::now()
            };
        """.trimIndent()).append("\n")

        // 2. Clear old relationships for this playlist
        queryBuilder.append("DELETE contains WHERE out = playlist:`$escapedPlaylistId`;\n")

        // 3. Establish relational links between playlist and its tracks
        trackIds.forEach { trackId ->
            val escapedTrackId = trackId.escapeSurreal()
            queryBuilder.append("RELATE playlist:`$escapedPlaylistId` -> contains -> track:`$escapedTrackId` SET timestamp = time::now();\n")
        }

        return runQuery(queryBuilder.toString())
    }

    /**
     * Utility string escape helper
     */
    private fun String.escapeSurreal(): String {
        return this.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
