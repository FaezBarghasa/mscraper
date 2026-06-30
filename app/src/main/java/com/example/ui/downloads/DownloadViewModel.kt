package com.example.ui.downloads

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.storage.PublicStorageManager
import com.example.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.mmdlp.*
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

    private val engine = MmDlpEngine()
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun startDownload(title: String, artist: String, imageUrl: String, videoUrl: String, audioUrl: String, format: String) {
        val taskId = UUID.randomUUID().toString()
        val tempFile = File(getApplication<Application>().cacheDir, "${taskId}.${format.lowercase()}")
        
        val newTask = DownloadTask(
            id = taskId,
            title = title,
            artist = artist,
            url = videoUrl,
            imageUrl = imageUrl
        )
        
        _tasks.update { it + newTask }

        viewModelScope.launch {
            try {
                engine.downloadAndMux(
                    videoUrl = videoUrl,
                    audioUrl = audioUrl,
                    outputPath = tempFile.absolutePath,
                    callback = object : DownloadProgressCallback {
                        override fun onProgress(progress: FfmpegProgress) {
                            // Extract progress from ffmpeg time or other fields
                            // For simplicity, we'll use a placeholder or calculate based on time if available
                            _tasks.update { list ->
                                list.map { 
                                    if (it.id == taskId) it.copy(progress = 0.5f) else it 
                                }
                            }
                        }

                        override fun onComplete() {
                            val publicUri = PublicStorageManager.moveToPublicDownloads(
                                context = getApplication(),
                                tempFilePath = tempFile.absolutePath,
                                fileName = "${title}.${format.lowercase()}",
                                mimeType = when(format.lowercase()) {
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
                        }

                        override fun onError(error: EngineException) {
                            _tasks.update { list ->
                                list.map { 
                                    if (it.id == taskId) it.copy(
                                        status = DownloadStatus.ERROR, 
                                        error = error.message 
                                    ) else it 
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _tasks.update { list ->
                    list.map { 
                        if (it.id == taskId) it.copy(
                            status = DownloadStatus.ERROR, 
                            error = e.localizedMessage 
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
