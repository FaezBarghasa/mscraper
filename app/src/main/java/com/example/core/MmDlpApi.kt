package com.example.core

import com.example.model.SearchSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MmDlpApi {
    suspend fun search(query: String, source: SearchSource): List<Track>
    suspend fun exportPlaylistJson(playlistName: String, tracks: List<com.example.model.Track>): String
    suspend fun exportPlaylistXml(playlistName: String, tracks: List<com.example.model.Track>): String
    suspend fun importPlaylistJson(json: String): Pair<String, List<com.example.model.Track>>
    fun setNetworkConfig(enableQuic: Boolean)
}

class MmDlpApiImpl : MmDlpApi {
    private val ytMusicScraper = YtMusicScraper()

    override suspend fun search(query: String, source: SearchSource): List<Track> = withContext(Dispatchers.IO) {
        return@withContext when (source) {
            SearchSource.YOUTUBE -> ytMusicScraper.searchTracks(query)
            SearchSource.SOUNDCLOUD -> emptyList()
            SearchSource.SPOTIFY -> emptyList()
        }
    }

    override suspend fun exportPlaylistJson(playlistName: String, tracks: List<com.example.model.Track>): String {
        // Logic for exporting to JSON (usually handled by Rust core, but here's a placeholder)
        val sb = StringBuilder()
        sb.append("{\"name\": \"$playlistName\", \"tracks\": [")
        tracks.forEachIndexed { index, track ->
            sb.append("{\"id\": \"${track.id}\", \"title\": \"${track.title}\", \"artist\": \"${track.artist}\"}")
            if (index < tracks.size - 1) sb.append(",")
        }
        sb.append("]}")
        return sb.toString()
    }

    override suspend fun exportPlaylistXml(playlistName: String, tracks: List<com.example.model.Track>): String {
        val sb = StringBuilder()
        sb.append("<playlist name=\"$playlistName\">")
        tracks.forEach { track ->
            sb.append("<track id=\"${track.id}\" title=\"${track.title}\" artist=\"${track.artist}\" />")
        }
        sb.append("</playlist>")
        return sb.toString()
    }

    override suspend fun importPlaylistJson(json: String): Pair<String, List<com.example.model.Track>> {
        // Mocking the result of import
        return "Imported Playlist" to emptyList()
    }

    override fun setNetworkConfig(enableQuic: Boolean) {
        // Pass configuration to Rust core
        // In a real scenario, this might call a UniFFI method on MmDlpEngine or similar
        android.util.Log.d("MmDlpApi", "Network config updated: enableQuic=$enableQuic")
    }
}
