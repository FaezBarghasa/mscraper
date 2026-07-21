package com.example.data.playlist

import android.content.Context
import android.net.Uri
import com.example.core.MmDlpApi
import com.example.core.MmDlpApiImpl
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class PlaylistRepository(private val context: Context) {
    private val api: MmDlpApi = MmDlpApiImpl()

    suspend fun exportPlaylist(uri: Uri, playlistName: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        val json = api.exportPlaylistJson(playlistName, tracks)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(json)
            }
        }
    }

    suspend fun importPlaylist(uri: Uri): Pair<String, List<Track>> = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: throw Exception("Failed to open input stream")
        
        api.importPlaylistJson(json)
    }
}
