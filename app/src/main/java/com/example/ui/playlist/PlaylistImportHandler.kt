package com.example.ui.playlist

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.example.core.MmDlpApi
import com.example.core.MmDlpApiImpl
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class PlaylistImportHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val api: MmDlpApi = MmDlpApiImpl(),
    private val onImportResult: (Boolean, String?, Pair<String, List<Track>>?) -> Unit
) {
    fun onResult(uri: Uri?) {
        if (uri == null) {
            onImportResult(false, "Import cancelled", null)
            return
        }

        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
                } ?: throw Exception("Failed to read file")

                val result = api.importPlaylistJson(json)
                onImportResult(true, "Playlist imported successfully", result)
            } catch (e: Exception) {
                onImportResult(false, "Import failed: ${e.message}", null)
            }
        }
    }

    fun startImport(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(arrayOf("application/json", "text/xml"))
    }
}
