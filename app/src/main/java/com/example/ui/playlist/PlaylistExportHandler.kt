package com.example.ui.playlist

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.core.MmDlpApi
import com.example.core.MmDlpApiImpl
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class PlaylistExportHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val api: MmDlpApi = MmDlpApiImpl(),
    private val onExportResult: (Boolean, String?) -> Unit
) {
    private var pendingExportData: Pair<String, List<Track>>? = null

    fun onResult(uri: Uri?) {
        if (uri == null) {
            onExportResult(false, "Export cancelled")
            return
        }
        val data = pendingExportData ?: return
        
        scope.launch {
            try {
                val isXml = context.contentResolver.getType(uri) == "text/xml" || uri.path?.endsWith(".xml") == true
                val content = if (isXml) {
                    api.exportPlaylistXml(data.first, data.second)
                } else {
                    api.exportPlaylistJson(data.first, data.second)
                }
                
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                            writer.write(content)
                        }
                    }
                }
                onExportResult(true, "Playlist exported successfully")
            } catch (e: Exception) {
                onExportResult(false, "Export failed: ${e.message}")
            } finally {
                pendingExportData = null
            }
        }
    }

    fun startExport(playlistName: String, tracks: List<Track>, launcher: ActivityResultLauncher<String>) {
        pendingExportData = playlistName to tracks
        val fileName = if (launcher.contract is ActivityResultContracts.CreateDocument && 
            (launcher.contract as ActivityResultContracts.CreateDocument).toString().contains("xml")) {
             "${playlistName.replace(" ", "_")}.xml"
        } else {
             "${playlistName.replace(" ", "_")}.json"
        }
        // Actually the extension is usually handled by the system based on mime type passed to CreateDocument
        launcher.launch(playlistName.replace(" ", "_"))
    }
}
