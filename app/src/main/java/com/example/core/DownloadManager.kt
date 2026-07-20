package com.example.core

import com.example.db.TrackDao
import com.example.model.DownloadJob
import com.example.model.DownloadStatus
import com.example.model.TrackEntity
import com.example.network.MediaApiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DownloadManager(
    private val trackDao: TrackDao,
    private val scope: CoroutineScope
) {
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val activeJobs = mutableMapOf<String, Job>()
    private val nativeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val okHttpClient = OkHttpClient()

    fun startDownload(url: String, targetFormat: String, targetBitrate: String) {
        val job = nativeScope.launch {
            try {
                var actualUrl = url
                if (actualUrl.startsWith("ytmsearch1:") || actualUrl.startsWith("scsearch1:") || actualUrl.startsWith("spsearch1:")) {
                    val query = actualUrl.substringAfter(":")
                    val results = MediaApiManager.searchMusic(query)
                    if (results.isNotEmpty()) {
                        actualUrl = results.first().url
                    } else {
                        throw Exception("No results found for query: $query")
                    }
                }
                val videoId = if (actualUrl.contains("v=")) actualUrl.substringAfter("v=").substringBefore("&") else actualUrl

                
                val streams = MediaApiManager.pipedApi.getStreams(videoId)
                val audioUrl = MediaApiManager.getStreamUrl(videoId)
                    ?: throw Exception("No suitable audio stream found")

                val jobId = UUID.randomUUID().toString()
                val newJob = DownloadJob(
                    id = jobId,
                    url = url,
                    title = streams.title,
                    artist = streams.uploader,
                    imageUrl = streams.thumbnailUrl,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0f,
                    downloadSpeed = "Starting...",
                    format = targetFormat
                )
                _jobs.update { it + newJob }
                
                val outputPath = "/storage/emulated/0/Music/M-scraper/${newJob.title}.${targetFormat.lowercase()}"
                
                downloadFile(audioUrl, outputPath, jobId, newJob, targetBitrate)
            } catch (e: Exception) {
                _jobs.update { list ->
                    list.map { j -> if (j.url == url) j.copy(status = DownloadStatus.ERROR, error = e.localizedMessage) else j }
                }
            }
        }
        activeJobs[url] = job
    }

    private suspend fun downloadFile(
        downloadUrl: String, 
        outputPath: String, 
        jobId: String, 
        job: DownloadJob, 
        targetBitrate: String
    ) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Failed to download file: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            
            val dir = File("/storage/emulated/0/Music/M-scraper/")
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(outputPath)
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)

            var bytesCopied: Long = 0
            val buffer = ByteArray(8 * 1024)
            var bytes = inputStream.read(buffer)
            var lastUpdate = System.currentTimeMillis()
            
            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                bytesCopied += bytes
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) {
                    val progress = if (contentLength > 0) bytesCopied.toFloat() / contentLength else 0f
                    _jobs.update { list ->
                        list.map { j ->
                            if (j.id == jobId) {
                                j.copy(progress = progress, downloadSpeed = "Downloading...")
                            } else j
                        }
                    }
                    lastUpdate = now
                }
                bytes = inputStream.read(buffer)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            insertTrackIntoDb(jobId, job, targetBitrate, outputPath, contentLength)
            _jobs.update { list ->
                list.map { if (it.id == jobId) it.copy(status = DownloadStatus.COMPLETED, progress = 1f, downloadSpeed = "DONE") else it }
            }
        } catch (e: Exception) {
            _jobs.update { list ->
                list.map { if (it.id == jobId) it.copy(status = DownloadStatus.ERROR, error = e.localizedMessage) else it }
            }
        }
    }

    private fun insertTrackIntoDb(jobId: String, job: DownloadJob, bitrate: String, outputPath: String, fileSize: Long) {
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
            duration = 0,
            bitrate = parsedBitrate,
            sampleRate = 44100,
            format = job.format,
            filePath = outputPath,
            fileSize = fileSize,
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
