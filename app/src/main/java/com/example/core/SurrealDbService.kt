package com.example.core

import android.content.Context
import com.example.db.SurrealDatabase
import com.example.model.PlaylistEntity
import com.example.model.TrackEntity
import com.surrealdb.driver.Surreal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SurrealDbService(context: Context) {

    private val db = SurrealDatabase.getInstance(context)
    private val driver: Surreal get() = db.getDriver()

    /**
     * Initializes SurrealDB database schema asynchronously upon application launch.
     */
    suspend fun initializeDatabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.connect().getOrThrow()

            // Setup schema definitions for music library using v3 SDK query
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

            driver.query(querySetup, emptyMap(), Any::class.java)
            handleMigrations().getOrThrow()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleMigrations(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = try {
                val results = driver.query("SELECT value FROM db_meta:schema_version;", emptyMap(), Map::class.java)
                // v3 SDK returns results in a way we need to parse
                // This is a simplified extraction logic
                val firstResult = results.firstOrNull() as? Map<*, *>
                (firstResult?.get("value") as? Number)?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }

            val targetVersion = 2
            if (currentVersion < targetVersion) {
                if (currentVersion < 1) {
                    driver.query("""
                        INSERT INTO settings:system {
                            id: 'system',
                            initializedAt: time::now(),
                            theme: 'CYAN',
                            crossfadeEnabled: true,
                            crossfadeDuration: 3.0
                        } ON DUPLICATE KEY UPDATE initializedAt = time::now();
                    """.trimIndent(), emptyMap(), Any::class.java)
                }

                if (currentVersion < 2) {
                    driver.query("""
                        DEFINE FIELD playCount ON TABLE track TYPE option<int>;
                        DEFINE FIELD lastPlayed ON TABLE track TYPE option<datetime>;
                    """.trimIndent(), emptyMap(), Any::class.java)
                }

                driver.query("UPDATE db_meta:schema_version SET value = $targetVersion;", emptyMap(), Any::class.java)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Index a single track using SurrealDB v3 SDK
     */
    suspend fun indexTrack(track: TrackEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            driver.upsert("track:${track.id}", track)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Groups multiple track metadata updates into a single transaction block.
     */
    suspend fun indexTracksBatch(tracks: List<TrackEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // In v3+, we can use query with parameters for batch operations or just upsert in a loop if transaction is not exposed simply
            // For now, let's use a transaction block via SurrealQL
            if (tracks.isEmpty()) return@withContext Result.success(Unit)

            val queryBuilder = StringBuilder()
            queryBuilder.append("BEGIN TRANSACTION;\n")
            tracks.forEach { track ->
                queryBuilder.append("UPSERT track:${track.id} CONTENT \$track_${track.id.replace("-", "_")};\n")
            }
            queryBuilder.append("COMMIT TRANSACTION;")

            val params = tracks.associate { "track_${it.id.replace("-", "_")}" to it }
            driver.query(queryBuilder.toString(), params, Any::class.java)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Index a playlist and establish dynamic graph relationships with tracks.
     */
    suspend fun indexPlaylist(playlist: PlaylistEntity, trackIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            driver.upsert("playlist:${playlist.id}", playlist)

            // Relational links
            val query = """
                DELETE contains WHERE out = playlist:${playlist.id};
                FOR ${'$'}trackId IN ${'$'}trackIds {
                    RELATE playlist:${playlist.id} -> contains -> track:${'$'}trackId SET timestamp = time::now();
                };
            """.trimIndent()
            
            driver.query(query, mapOf("trackIds" to trackIds), Any::class.java)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
