package com.example.core

import com.example.db.TrackDao
import com.example.model.TrackEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class DuplicateGroup(
    val title: String,
    val artist: String,
    val tracks: List<TrackEntity>
)

class DuplicateManager(private val trackDao: TrackDao) {

    fun detectDuplicates(allTracks: List<TrackEntity>, useContentHash: Boolean): List<DuplicateGroup> {
        val grouped = if (useContentHash) {
            // Group by simulated content hash (same file size and artist)
            allTracks.groupBy { "${it.fileSize}_${it.artist}" }
        } else {
            // Group by standard metadata (same lowercase title and artist)
            allTracks.groupBy { "${it.title.lowercase().trim()}_${it.artist?.lowercase()?.trim()}" }
        }

        return grouped.values
            .filter { it.size > 1 }
            .map { list ->
                val first = list.first()
                DuplicateGroup(
                    title = first.title,
                    artist = first.artist ?: "Unknown Artist",
                    tracks = list
                )
            }
    }

    suspend fun resolveDuplicates(group: DuplicateGroup, strategy: String) = withContext(Dispatchers.IO) {
        if (group.tracks.size <= 1) return@withContext
        
        val trackToKeep = when (strategy) {
            "OLDEST" -> group.tracks.minByOrNull { it.dateAdded }
            "NEWEST" -> group.tracks.maxByOrNull { it.dateAdded }
            "HIGHEST_QUALITY" -> group.tracks.maxByOrNull { it.bitrate }
            else -> group.tracks.firstOrNull()
        } ?: return@withContext

        group.tracks.forEach { track ->
            if (track.id != trackToKeep.id) {
                trackDao.deleteTrackById(track.id)
            }
        }
    }
}
