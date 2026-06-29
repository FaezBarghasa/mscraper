package com.example.core

import com.example.db.TrackDao
import com.example.model.DownloadJob
import com.example.model.DownloadStatus
import com.example.model.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// Force loading of native library
object NativeLibLoader {
    init {
        try {
            uniffi.mmdlp.uniffiEnsureInitialized()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

object MmDlpEngine {
    init { NativeLibLoader }
    private val engine = uniffi.mmdlp.MmDlpEngine()

    data class MediaInfo(
        val id: String,
        val title: String,
        val description: String?,
        val uploader: String?,
        val duration: UInt?,
        val formats: List<MediaFormat>
    )
    data class MediaFormat(
        val format_id: String,
        val url: String,
        val ext: String,
        val width: UInt?,
        val height: UInt?,
        val vcodec: String?,
        val acodec: String?,
        val filesize: ULong?
    )

    fun extractMetadata(url: String): MediaInfo {
        val meta = engine.extractMetadata(url)
        return MediaInfo(
            id = meta.id,
            title = meta.title,
            description = meta.description,
            uploader = meta.uploader,
            duration = meta.duration,
            formats = meta.formats.map { f ->
                MediaFormat(
                    format_id = f.formatId,
                    url = f.url,
                    ext = f.ext,
                    width = f.width,
                    height = f.height,
                    vcodec = f.vcodec,
                    acodec = f.acodec,
                    filesize = f.filesize
                )
            }
        )
    }

    interface DownloadCallback {
        fun onProgress(progress: uniffi.mmdlp.FfmpegProgress)
        fun onComplete()
        fun onError(error: uniffi.mmdlp.EngineException)
    }

    fun downloadAndMux(
        videoUrl: String,
        audioUrl: String,
        outputPath: String,
        callback: DownloadCallback
    ) {
        val uniffiCallback = object : uniffi.mmdlp.DownloadProgressCallback {
            override fun onProgress(progress: uniffi.mmdlp.FfmpegProgress) {
                callback.onProgress(progress)
            }
            override fun onComplete() {
                callback.onComplete()
            }
            override fun onError(error: uniffi.mmdlp.EngineException) {
                callback.onError(error)
            }
        }
        engine.downloadAndMux(videoUrl, audioUrl, outputPath, uniffiCallback)
    }
}

class DownloadManager(
    private val trackDao: TrackDao,
    private val scope: CoroutineScope
) {
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()
    private val nativeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDownload(url: String, targetFormat: String, targetBitrate: String) {
        nativeScope.launch {
            val meta = try {
                MmDlpEngine.extractMetadata(url)
            } catch (e: Exception) {
                _jobs.update { list ->
                    list.map { j -> if (j.url == url) j.copy(status = DownloadStatus.ERROR, error = e.localizedMessage) else j }
                }
                return@launch
            }
            val jobId = UUID.randomUUID().toString()
            val newJob = DownloadJob(
                id = jobId,
                url = url,
                title = meta.title,
                artist = meta.uploader ?: "Unknown",
                imageUrl = "",
                status = DownloadStatus.DOWNLOADING,
                progress = 0f,
                downloadSpeed = "Starting...",
                format = targetFormat
            )
            _jobs.update { it + newJob }
            val outputPath = "/storage/emulated/0/Music/M-scraper/${newJob.title}.${targetFormat.lowercase()}"
            try {
                MmDlpEngine.downloadAndMux(
                    videoUrl = url,
                    audioUrl = url,
                    outputPath = outputPath,
                    callback = object : MmDlpEngine.DownloadCallback {
                        override fun onProgress(progress: uniffi.mmdlp.FfmpegProgress) {
                            _jobs.update { list ->
                                list.map { j ->
                                    if (j.id == jobId) {
                                        val newProg = (j.progress + 0.1f).coerceAtMost(1f)
                                        j.copy(progress = newProg, downloadSpeed = progress.bitrate ?: "0")
                                    } else j
                                }
                            }
                        }

                        override fun onComplete() {
                            insertTrackIntoDb(jobId, newJob, targetBitrate)
                            _jobs.update { list ->
                                list.map { if (it.id == jobId) it.copy(status = DownloadStatus.COMPLETED, progress = 1f, downloadSpeed = "DONE") else it }
                            }
                        }

                        override fun onError(error: uniffi.mmdlp.EngineException) {
                            _jobs.update { list ->
                                list.map { if (it.id == jobId) it.copy(status = DownloadStatus.ERROR, error = error.toString()) else it }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _jobs.update { list ->
                    list.map { if (it.id == jobId) it.copy(status = DownloadStatus.ERROR, error = e.localizedMessage) else it }
                }
            }
        }
    }

    private fun insertTrackIntoDb(jobId: String, job: DownloadJob, bitrate: String) {
        val parsedBitrate = bitrate.substringBefore("k").toIntOrNull() ?: 320
        val extTrack = TrackEntity(
            id = job.id,
            title = job.title,
            artist = job.artist,
            album = "Grid Downloader",
            albumArtist = job.artist,
            genre = "SYNTHWAVE",
            year = 2026,
            trackNumber = 1,
            discNumber = 1,
            duration = 243,
            bitrate = parsedBitrate,
            sampleRate = 44100,
            format = job.format,
            filePath = "/storage/emulated/0/Music/M-scraper/${job.title}.${job.format.lowercase()}",
            fileSize = 10485760L,
            artworkPath = job.imageUrl,
            dateAdded = System.currentTimeMillis()
        )
        scope.launch(Dispatchers.IO) { trackDao.insertTrack(extTrack) }
    }

    fun pauseDownload(jobId: String) {
        activeJobs[jobId]?.cancel()
        activeJobs.remove(jobId)
        _jobs.update { list ->
            list.map { if (it.id == jobId) it.copy(status = DownloadStatus.PAUSED, downloadSpeed = "PAUSED") else it }
        }
    }

    fun resumeDownload(jobId: String) {
        val job = _jobs.value.find { it.id == jobId } ?: return
        startDownload(job.url, job.format, "${job.format}kbps")
    }

    fun cancelDownload(jobId: String) {
        activeJobs[jobId]?.cancel()
        activeJobs.remove(jobId)
        _jobs.update { list ->
            list.map { if (it.id == jobId) it.copy(status = DownloadStatus.CANCELLED, downloadSpeed = "CANCELLED") else it }
        }
    }

    fun clearCompleted() {
        _jobs.update { list -> list.filterNot { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.CANCELLED } }
    }
}
