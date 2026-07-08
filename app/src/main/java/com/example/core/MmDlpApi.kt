package com.example.core

import com.example.model.SearchSource
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MmDlpApi {
    suspend fun search(query: String, source: SearchSource): List<Track>
    suspend fun exportPlaylistJson(playlistName: String, tracks: List<Track>): String
    suspend fun exportPlaylistXml(playlistName: String, tracks: List<Track>): String
    suspend fun importPlaylistJson(json: String): Pair<String, List<Track>>
    fun setNetworkConfig(enableQuic: Boolean)
}

class MmDlpApiImpl : MmDlpApi {
    private val ffiApi = com.example.core.ffi.MmDlpApi()

    override suspend fun search(query: String, source: SearchSource): List<Track> = withContext(Dispatchers.IO) {
        val ffiSource = when (source) {
            SearchSource.YOUTUBE -> com.example.core.ffi.AudioSource.YOU_TUBE_MUSIC
            SearchSource.SOUNDCLOUD -> com.example.core.ffi.AudioSource.SOUND_CLOUD
            SearchSource.SPOTIFY -> com.example.core.ffi.AudioSource.SPOTIFY
        }
        val results = ffiApi.search(query, ffiSource)
        results.map { ffiTrack ->
            Track(
                id = ffiTrack.trackId,
                title = ffiTrack.title,
                artist = ffiTrack.artist,
                duration = "3:00",
                imageUrl = ffiTrack.albumArtUrl ?: "",
                filePath = "",
                genre = source.displayName
            )
        }
    }

    override suspend fun exportPlaylistJson(playlistName: String, tracks: List<Track>): String = withContext(Dispatchers.IO) {
        val ffiTracks = tracks.map { track ->
            com.example.core.ffi.Track(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = null,
                sourceUrl = "",
                duration = 180uL
            )
        }
        val ffiPlaylist = com.example.core.ffi.Playlist(
            id = java.util.UUID.randomUUID().toString(),
            name = playlistName,
            description = null,
            tracks = ffiTracks,
            source = com.example.core.ffi.AudioSource.YOU_TUBE_MUSIC
        )
        ffiApi.exportPlaylistJson(ffiPlaylist)
    }

    override suspend fun exportPlaylistXml(playlistName: String, tracks: List<Track>): String = withContext(Dispatchers.IO) {
        val ffiTracks = tracks.map { track ->
            com.example.core.ffi.Track(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = null,
                sourceUrl = "",
                duration = 180uL
            )
        }
        val ffiPlaylist = com.example.core.ffi.Playlist(
            id = java.util.UUID.randomUUID().toString(),
            name = playlistName,
            description = null,
            tracks = ffiTracks,
            source = com.example.core.ffi.AudioSource.YOU_TUBE_MUSIC
        )
        ffiApi.exportPlaylistXml(ffiPlaylist)
    }

    override suspend fun importPlaylistJson(json: String): Pair<String, List<Track>> = withContext(Dispatchers.IO) {
        val ffiPlaylist = ffiApi.importPlaylistJson(json)
        val tracks = ffiPlaylist.tracks.map { ffiTrack ->
            Track(
                id = ffiTrack.id,
                title = ffiTrack.title,
                artist = ffiTrack.artist,
                duration = "${ffiTrack.duration / 60uL}:${(ffiTrack.duration % 60uL).toString().padStart(2, '0')}",
                imageUrl = "",
                filePath = ffiTrack.sourceUrl,
                genre = ffiPlaylist.source.name
            )
        }
        ffiPlaylist.name to tracks
    }

    override fun setNetworkConfig(enableQuic: Boolean) {
        ffiApi.setNetworkConfig(enableQuic)
    }
}
