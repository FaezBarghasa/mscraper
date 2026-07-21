package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.bridge.DownloadEvent
import com.example.bridge.MmDlpBridge
import com.example.data.local.AppDatabase
import com.example.data.local.Song
import com.example.data.storage.MediaStoreManager
import com.example.util.CacheHelper
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.*

class DownloadWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val bridge = MmDlpBridge()
    private val mediaStoreManager = MediaStoreManager(context)
    private val db = AppDatabase.getDatabase(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_URL = "url"
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 1001

        fun enqueue(context: Context, url: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(KEY_URL to url))
                .setConstraints(constraints)
                .addTag("download_$url")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_$url",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        
        setForeground(createForegroundInfo("Preparing download..."))

        return try {
            val metadata = bridge.extractMetadata(url)
            val tempFile = CacheHelper.getTempFile(applicationContext, "mp3")
            
            // For now, let's assume we use the first available audio format or similar
            // In a real app, we'd pick the best one.
            val audioUrl = metadata.formats.firstOrNull { it.acodec != null && it.vcodec == null }?.url ?: url
            val videoUrl = "" // No video for now as we want audio extract

            var success = false
            bridge.downloadAsFlow(videoUrl, audioUrl, tempFile.absolutePath).collect { event ->
                when (event) {
                    is DownloadEvent.Progress -> {
                        val progressText = "Downloading: ${event.progress.time ?: ""}"
                        notificationManager.notify(NOTIFICATION_ID, createNotification(progressText))
                    }
                    is DownloadEvent.Complete -> {
                        success = true
                    }
                    is DownloadEvent.Error -> {
                        throw event.exception
                    }
                }
            }

            if (success) {
                val fileName = "${metadata.title}.mp3"
                val uri = mediaStoreManager.createPendingAudioUri(fileName, "audio/mpeg")
                if (uri != null) {
                    mediaStoreManager.writeCacheFileToUri(tempFile, uri)
                    mediaStoreManager.finalizeFile(uri)
                    
                    val song = Song(
                        id = metadata.id,
                        title = metadata.title,
                        artist = metadata.uploader ?: "Unknown",
                        url = url,
                        filePath = uri.toString(),
                        duration = metadata.duration?.toString() ?: "0",
                        imageUrl = "", // We might want to download thumbnail too
                        isDownloaded = true
                    )
                    db.songDao().insert(song)
                    tempFile.delete()
                    Result.success()
                } else {
                    Result.failure()
                }
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = createNotification(progress)
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotification(progress: String) = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setContentTitle("M-scraper Download")
        .setContentText(progress)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .build()
}
