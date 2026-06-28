package com.example.model

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, CONVERTING, COMPLETED, CANCELLED, ERROR
}

data class DownloadJob(
    val id: String,
    val url: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
    val status: DownloadStatus,
    val progress: Float, // 0f to 1f
    val downloadSpeed: String, // e.g. "4.2 MB/s"
    val format: String, // MP3, FLAC, etc.
    val error: String? = null
)
