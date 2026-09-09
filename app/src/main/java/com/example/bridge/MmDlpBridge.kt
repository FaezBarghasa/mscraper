package com.example.bridge

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uniffi.mmdlp.*

sealed class DownloadEvent {
    data class Progress(val progress: FfmpegProgress) : DownloadEvent()
    object Complete : DownloadEvent()
    data class Error(val exception: EngineException) : DownloadEvent()
}

class MmDlpBridge {
    private val engine = MmDlpEngine()

    fun extractMetadata(url: String): MediaInfo {
        return engine.extractMetadata(url)
    }

    fun downloadAsFlow(videoUrl: String, audioUrl: String, outputPath: String): Flow<DownloadEvent> = callbackFlow {
        val callback = object : DownloadProgressCallback {
            override fun onProgress(progress: FfmpegProgress) {
                trySend(DownloadEvent.Progress(progress))
            }

            override fun onComplete() {
                trySend(DownloadEvent.Complete)
                close()
            }

            override fun onError(error: EngineException) {
                trySend(DownloadEvent.Error(error))
                close(error)
            }
        }

        try {
            engine.downloadAndMux(videoUrl, audioUrl, outputPath, callback)
        } catch (e: EngineException) {
            trySend(DownloadEvent.Error(e))
            close(e)
        }

        awaitClose {
            // Ideally we'd have a way to cancel the download in MmDlpEngine
        }
    }
}
