package com.example.ui.downloads

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ffi.AudioFormat
import com.example.core.ffi.AudioQuality
import com.example.core.ffi.MmDlpApi
import com.example.data.storage.PublicStorageManager
import com.example.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class DownloadTask(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val imageUrl: String = "",
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val error: String? = null,
    val fileUri: Uri? = null
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val api = MmDlpApi()
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun startDownload(title: String, artist: String, imageUrl: String, videoUrl: String, audioUrl: String, format: String) {
        val taskId = UUID.randomUUID().toString()
        val targetFormat = when (format.uppercase()) {
            "FLAC" -> AudioFormat.FLAC
            "WAV" -> AudioFormat.WAV
            else -> AudioFormat.MP3
        }

        val newTask = DownloadTask(
            id = taskId,
            title = title,
            artist = artist,
            url = if (audioUrl.isNotBlank()) audioUrl else videoUrl,
            imageUrl = imageUrl
        )

        _tasks.update { it + newTask }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempDir = getApplication<Application>().cacheDir.absolutePath
                val downloadUrl = if (audioUrl.isNotBlank()) audioUrl else videoUrl
                
                _tasks.update { list ->
                    list.map { if (it.id == taskId) it.copy(progress = 0.3f) else it }
                }

                val downloadedFilePath = api.downloadTrack(
                    url = downloadUrl,
                    quality = AudioQuality.HIGH,
                    format = targetFormat,
                    tempDir = tempDir
                )

                _tasks.update { list ->
                    list.map { if (it.id == taskId) it.copy(progress = 0.8f) else it }
                }

                val ext = format.lowercase()
                val publicUri = PublicStorageManager.moveToPublicDownloads(
                    context = getApplication(),
                    tempFilePath = downloadedFilePath,
                    fileName = "${title}.${ext}",
                    mimeType = when (ext) {
                        "flac" -> "audio/flac"
                        "wav" -> "audio/wav"
                        else -> "audio/mpeg"
                    }
                )

                _tasks.update { list ->
                    list.map {
                        if (it.id == taskId) it.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 1f,
                            fileUri = publicUri
                        ) else it
                    }
                }
            } catch (e: Exception) {
                _tasks.update { list ->
                    list.map {
                        if (it.id == taskId) it.copy(
                            status = DownloadStatus.ERROR,
                            error = e.localizedMessage ?: "Download failed"
                        ) else it
                    }
                }
            }
        }
    }

    fun removeTask(taskId: String) {
        _tasks.update { list -> list.filter { it.id != taskId } }
    }
}
