package com.example.core

import android.content.Context
import android.net.Uri
import com.example.db.TrackDao
import com.example.model.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class DuplicateGroup(
    val title: String,
    val artist: String,
    val tracks: List<TrackEntity>
)

class DuplicateManager(
    private val trackDao: TrackDao,
    private val context: Context? = null
) {

    /**
     * Compute SHA-256 hash for local physical files.
     * Falls back to metadata signature if file does not exist locally (e.g. streaming URI).
     */
    private fun computeFileSha256(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile || file.length() == 0L) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun detectDuplicates(allTracks: List<TrackEntity>, useContentHash: Boolean): List<DuplicateGroup> {
        val grouped = if (useContentHash) {
            // Group by actual file SHA-256 hash if available, or exact fileSize + duration + title + artist
            allTracks.groupBy { track ->
                computeFileSha256(track.filePath)
                    ?: "meta_${track.fileSize}_${track.duration}_${track.title.lowercase().trim()}_${track.artist?.lowercase()?.trim()}"
            }
        } else {
            // Group by standard metadata (exact lowercase title, artist, and duration)
            allTracks.groupBy { "${it.title.lowercase().trim()}_${it.artist?.lowercase()?.trim()}_${it.duration}" }
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
                try {
                    if (track.filePath.startsWith("content://") && context != null) {
                        context.contentResolver.delete(Uri.parse(track.filePath), null, null)
                    } else {
                        val f = File(track.filePath)
                        if (f.exists()) {
                            f.delete()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
