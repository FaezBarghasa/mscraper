package com.example.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DownloadManager
import com.example.db.AppDatabase
import com.example.model.DownloadJob
import kotlinx.coroutines.flow.StateFlow

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val downloadManager = DownloadManager(
        trackDao = db.trackDao(),
        scope = viewModelScope,
        context = application
    )

    val tasks: StateFlow<List<DownloadJob>> = downloadManager.jobs

    fun startDownload(title: String, artist: String, imageUrl: String, videoUrl: String, audioUrl: String, format: String) {
        val url = if (audioUrl.isNotBlank()) audioUrl else videoUrl
        val bitrate = "320"
        downloadManager.startDownload(url = url, targetFormat = format, targetBitrate = bitrate)
    }

    fun removeTask(jobId: String) {
        downloadManager.cancelDownload(jobId)
    }
}
