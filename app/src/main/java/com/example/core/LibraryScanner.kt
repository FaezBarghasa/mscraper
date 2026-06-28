package com.example.core

import com.example.db.TrackDao
import com.example.model.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class LibraryScanner(
    private val trackDao: TrackDao,
    private val surrealDbService: SurrealDbService
) {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scannedFileCount = MutableStateFlow(0)
    val scannedFileCount: StateFlow<Int> = _scannedFileCount.asStateFlow()

    /**
     * Scan directory in parallel using Kotlin's multi-threaded Dispatchers.IO.
     * Concurrently indexes discovered tracks into local Room database AND SurrealDB.
     */
    suspend fun scanDirectory(virtualPath: String): Int = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scanProgress.value = 0f
        _scannedFileCount.value = 0

        val mockMediaFiles = listOf(
            Pair("Midnight City", "M83"),
            Pair("Nightcall", "Kavinsky"),
            Pair("Tech Noir", "Gunship"),
            Pair("Shadow Fury", "Laserhawk"),
            Pair("Rage Center", "Revenant"),
            Pair("Sunset Coast", "The Midnight"),
            Pair("Chronos", "FM-84"),
            Pair("Dark All Day", "Gunship")
        )

                val newlyAddedCount = AtomicInteger(0)
        val completedCount = AtomicInteger(0)
        val totalFiles = mockMediaFiles.size
        
        // Thread-safe collection for batching SurrealDB indexing writes
        val tracksToBatch = java.util.Collections.synchronizedList(mutableListOf<TrackEntity>())

        // Parallel processing of media files using coroutineScope and async
        coroutineScope {
            val jobs = mockMediaFiles.mapIndexed { index, file ->
                async(Dispatchers.IO) {
                    // Simulate variable processing latency concurrently
                    delay((200..600).random().toLong())

                    val finalId = "scanned_${file.first.lowercase().replace(" ", "_")}"
                    val existing = trackDao.getTrackById(finalId)

                    if (existing == null) {
                        val ext = TrackEntity(
                            id = finalId,
                            title = file.first,
                            artist = file.second,
                            album = "Scanned Archives",
                            albumArtist = file.second,
                            genre = "SYNTHWAVE",
                            year = 2026,
                            trackNumber = index + 1,
                            discNumber = 1,
                            duration = 240 + (index * 12),
                            bitrate = 320,
                            sampleRate = 44100,
                            format = "MP3",
                            filePath = "$virtualPath/${file.first}.mp3",
                            fileSize = 8592100L,
                            artworkPath = "https://lh3.googleusercontent.com/aida-public/AB6AXuBBEtoMVVFUzB83CnpCLLYXTr43Jig9eeoY-9-mPD-JxbAFK34_ATCUc-1yMgJk0CSHXUbMw7wEcGYUHVOgr6x4q10vVLSsJPX9-g3YzexBw7hxISrHeBcsDPd1RRluwrBqF044Dd3ntM2DQz-0U3QO6qC4eaUe10I2jSIpcNeHwxGF6-Yhb-7XDr1Ed-s5--_FD_0a__7ifb4E5BUJopP74LoSpUPlyXIN8koLo3k0LsBqbIagsysNnzGiaUqrjhSYvD28hsmYoqNs",
                            dateAdded = System.currentTimeMillis()
                        )

                        // 1. Thread-safe Local Room DB indexing
                        trackDao.insertTrack(ext)

                        // Add to batch list for bulk SurrealDB ingestion
                        tracksToBatch.add(ext)

                        newlyAddedCount.incrementAndGet()
                    }

                    // Increment progress indicator in thread-safe manner
                    val currentDone = completedCount.incrementAndGet()
                    _scannedFileCount.value = currentDone
                    _scanProgress.value = currentDone.toFloat() / totalFiles
                }
            }
            jobs.awaitAll()
        }

        // 2. Perform batched transactional indexing to SurrealDB
        if (tracksToBatch.isNotEmpty()) {
            surrealDbService.indexTracksBatch(tracksToBatch)
        }

        _isScanning.value = false
        _scanProgress.value = 1.0f
        newlyAddedCount.get()
    }
}
