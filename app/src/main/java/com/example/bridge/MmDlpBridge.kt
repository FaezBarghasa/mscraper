package com.example.bridge

import com.example.core.ffi.AudioFormat
import com.example.core.ffi.AudioQuality
import com.example.core.ffi.MmDlpApi
import com.example.core.ffi.TrackMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

sealed class DownloadEvent {
    data class Progress(val percent: Float) : DownloadEvent()
    object Complete : DownloadEvent()
    data class Error(val exception: Throwable) : DownloadEvent()
}

class MmDlpBridge(
    private val api: MmDlpApi = MmDlpApi()
) {
    fun extractMetadata(url: String): TrackMetadata {
        return api.extractMetadata(url)
    }

    fun downloadAsFlow(url: String, outputPath: String): Flow<DownloadEvent> = flow {
        emit(DownloadEvent.Progress(0.1f))
        try {
            val parentDir = File(outputPath).parent ?: ""
            val downloadedPath = api.downloadTrack(
                url = url,
                quality = AudioQuality.HIGH,
                format = AudioFormat.MP3,
                tempDir = parentDir
            )
            val downloadedFile = File(downloadedPath)
            val targetFile = File(outputPath)
            if (downloadedFile.absolutePath != targetFile.absolutePath && downloadedFile.exists()) {
                downloadedFile.renameTo(targetFile)
            }
            emit(DownloadEvent.Progress(1.0f))
            emit(DownloadEvent.Complete)
        } catch (e: Throwable) {
            emit(DownloadEvent.Error(e))
        }
    }
}
