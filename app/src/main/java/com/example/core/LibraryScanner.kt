package com.example.core

import android.content.Context
import android.media.MediaMetadataRetriever
import com.example.db.TrackDao
import com.example.model.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class LibraryScanner(
    private val trackDao: TrackDao,
    private val context: Context? = null
) {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scannedFileCount = MutableStateFlow(0)
    val scannedFileCount: StateFlow<Int> = _scannedFileCount.asStateFlow()

    private val audioExtensions = setOf("mp3", "flac", "wav", "m4a", "ogg", "aac", "opus")

    /**
     * Recursively scan directory on disk and index discovered audio files into Room database.
     */
    suspend fun scanDirectory(virtualPath: String): Int = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f
        _scannedFileCount.value = 0

        val targetDir = File(virtualPath)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            _isScanning.value = false
            _scanProgress.value = 1f
            return@withContext 0
        }

        val audioFiles = mutableListOf<File>()
        targetDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension.lowercase() in audioExtensions) {
                audioFiles.add(file)
            }
        }

        val totalFiles = audioFiles.size
        if (totalFiles == 0) {
            _isScanning.value = false
            _scanProgress.value = 1f
            return@withContext 0
        }

        var newlyAdded = 0
        val retriever = MediaMetadataRetriever()

        try {
            audioFiles.forEachIndexed { index, file ->
                val finalId = "local_${file.absolutePath.hashCode()}_${file.length()}"
                val existing = trackDao.getTrackById(finalId)

                if (existing == null) {
                    var title = file.nameWithoutExtension
                    var artist = "Unknown Artist"
                    var album = "Local Storage"
                    var durationSecs = 0
                    var bitrate = 320
                    var year: Int? = null
                    var genre: String? = null

                    try {
                        retriever.setDataSource(file.absolutePath)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                            if (it.isNotBlank()) title = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                            if (it.isNotBlank()) artist = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                            if (it.isNotBlank()) album = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                            durationSecs = (it / 1000).toInt()
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let {
                            bitrate = it / 1000
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let {
                            year = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.let {
                            if (it.isNotBlank()) genre = it
                        }
                    } catch (_: Exception) {
                        // Fall back to filename defaults on metadata read error
                    }

                    val ext = TrackEntity(
                        id = finalId,
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = artist,
                        genre = genre,
                        year = year,
                        trackNumber = null,
                        discNumber = null,
                        duration = durationSecs,
                        bitrate = bitrate,
                        sampleRate = 44100,
                        format = file.extension.uppercase(),
                        filePath = file.absolutePath,
                        fileSize = file.length(),
                        artworkPath = null,
                        dateAdded = System.currentTimeMillis()
                    )

                    trackDao.insertTrack(ext)
                    newlyAdded++
                }

                _scannedFileCount.value = index + 1
                _scanProgress.value = (index + 1).toFloat() / totalFiles
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
            _isScanning.value = false
            _scanProgress.value = 1.0f
        }

        newlyAdded
    }
}
