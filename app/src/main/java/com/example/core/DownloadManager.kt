package com.example.core

import android.content.Context
import com.example.db.TrackDao
import com.example.model.DownloadJob
import com.example.model.DownloadStatus
import com.example.model.TrackEntity
import com.example.data.storage.PublicStorageManager
import com.example.data.storage.MediaScanner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

class DownloadManager(
    private val trackDao: TrackDao,
    private val scope: CoroutineScope,
    private val context: Context
) {
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()
    private val ffiApi = com.example.core.ffi.MmDlpApi()
    private val engine = uniffi.mmdlp.MmDlpEngine()

    fun startDownload(url: String, targetFormat: String, targetBitrate: String) {
        val jobId = UUID.randomUUID().toString()
        val job = scope.launch(Dispatchers.IO) {
            try {
                val initialJob = DownloadJob(
                    id = jobId,
                    url = url,
                    title = "Fetching Metadata...",
                    artist = "...",
                    imageUrl = "",
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0.1f,
                    downloadSpeed = "Initialising...",
                    format = targetFormat
                )
                _jobs.update { it + initialJob }

                val mediaInfo = engine.extractMetadata(url)
                val title = mediaInfo.title
                val artist = mediaInfo.uploader ?: "Unknown"

                _jobs.update { list ->
                    list.map { j ->
                        if (j.id == jobId) {
                            j.copy(
                                title = title,
                                artist = artist,
                                progress = 0.3f,
                                downloadSpeed = "Downloading..."
                            )
                        } else j
                    }
                }

                val ffiFormat = when (targetFormat.uppercase()) {
                    "FLAC" -> com.example.core.ffi.AudioFormat.FLAC
                    "WAV" -> com.example.core.ffi.AudioFormat.WAV
                    else -> com.example.core.ffi.AudioFormat.MP3
                }

                val tempDir = context.cacheDir.absolutePath
                val tempFilePath = ffiApi.downloadTrack(
                    url = url,
                    quality = com.example.core.ffi.AudioQuality.HIGH,
                    format = ffiFormat,
                    tempDir = tempDir
                )

                _jobs.update { list ->
                    list.map { j -> if (j.id == jobId) j.copy(progress = 0.8f, downloadSpeed = "Saving...") else j }
                }

                val fileName = "${title}.${targetFormat.lowercase()}"
                val mimeType = when (targetFormat.lowercase()) {
                    "flac" -> "audio/flac"
                    "wav" -> "audio/wav"
                    else -> "audio/mpeg"
                }

                val publicUri = PublicStorageManager.moveToPublicDownloads(
                    context = context,
                    tempFilePath = tempFilePath,
                    fileName = fileName,
                    mimeType = mimeType
                )

                val publicPath = "/storage/emulated/0/Download/mscraper/$fileName"
                MediaScanner.scanFile(context, publicPath, mimeType)

                val parsedBitrate = targetBitrate.substringBefore("k").toIntOrNull() ?: 320
                val fileSize = 10 * 1024 * 1024L

                insertTrackIntoDb(
                    jobId = jobId,
                    title = title,
                    artist = artist,
                    imageUrl = "",
                    format = targetFormat,
                    bitrate = parsedBitrate,
                    filePath = publicUri.toString(),
                    fileSize = fileSize
                )

                _jobs.update { list ->
                    list.map { j ->
                        if (j.id == jobId) {
                            j.copy(
                                status = DownloadStatus.COMPLETED,
                                progress = 1.0f,
                                downloadSpeed = "DONE"
                            )
                        } else j
                    }
                }

            } catch (e: Exception) {
                _jobs.update { list ->
                    list.map { j ->
                        if (j.id == jobId || j.url == url) {
                            j.copy(
                                status = DownloadStatus.ERROR,
                                error = e.localizedMessage ?: "Download failed"
                            )
                        } else j
                    }
                }
            }
        }
        activeJobs[url] = job
    }

    private fun insertTrackIntoDb(
        jobId: String,
        title: String,
        artist: String,
        imageUrl: String,
        format: String,
        bitrate: Int,
        filePath: String,
        fileSize: Long
    ) {
        val extTrack = TrackEntity(
            id = jobId,
            title = title,
            artist = artist,
            album = "M-Scraper Downloads",
            albumArtist = artist,
            genre = "Music",
            year = 2026,
            trackNumber = 1,
            discNumber = 1,
            duration = 180,
            bitrate = bitrate,
            sampleRate = 44100,
            format = format,
            filePath = filePath,
            fileSize = fileSize,
            artworkPath = imageUrl,
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
        startDownload(job.url, job.format, "320")
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
