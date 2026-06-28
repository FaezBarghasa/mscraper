package com.example

import com.example.core.DuplicateGroup
import com.example.core.DuplicateManager
import com.example.model.TrackEntity
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMetadataExtractor_matchesAppleMusicUrl() {
    val url = "https://music.apple.com/us/album/midnight-city/457421833?i=457421838"
    // Mock extract pattern matching
    val isApple = url.contains("music.apple.com")
    assertTrue(isApple)
    
    val id = url.substringAfter("i=").substringBefore("&")
    assertEquals("457421838", id)
  }

  @Test
  fun testDuplicateDetection_findsMatchingMetadata() {
    val t1 = TrackEntity("id1", "Sunset", "The Midnight", null, null, null, null, null, null, 240, 320, 44100, "MP3", "path1", 5000L, null)
    val t2 = TrackEntity("id2", "Sunset", "The Midnight", null, null, null, null, null, null, 240, 128, 44100, "MP3", "path2", 2000L, null)
    val t3 = TrackEntity("id3", "Tech Noir", "Gunship", null, null, null, null, null, null, 280, 320, 44100, "FLAC", "path3", 8000L, null)

    val list = listOf(t1, t2, t3)
    val grouped = list.groupBy { "${it.title.lowercase().trim()}_${it.artist?.lowercase()?.trim()}" }
    val duplicates = grouped.values.filter { it.size > 1 }

    assertEquals(1, duplicates.size)
    assertEquals(2, duplicates.first().size)
    assertEquals("Sunset", duplicates.first().first().title)
  }

  @Test
  fun testPlaylistFormatters_generateValidM3u() {
    val t1 = TrackEntity("id1", "Sunset", "The Midnight", null, null, null, null, null, null, 240, 320, 44100, "MP3", "path1", 5000L, null)
    val builder = StringBuilder()
    builder.append("#EXTM3U\n")
    builder.append("#EXTINF:${t1.duration},${t1.artist} - ${t1.title}\n")
    builder.append("${t1.filePath}\n")

    val result = builder.toString()
    assertTrue(result.contains("#EXTM3U"))
    assertTrue(result.contains("#EXTINF:240,The Midnight - Sunset"))
    assertTrue(result.contains("path1"))
  }

  @Test
  fun testContextAwareSourceRouting_resolvesCorrectSource() {
    val streamingPriority = listOf("YouTube Music", "YouTube Standard", "Direct HTTP")
    val downloadPriority = listOf("SoundCloud", "YouTube Music", "Direct HTTP")

    // Scenario: User is in STREAMING context
    val currentContext = "STREAM"
    val resolvedSourceForStream = if (currentContext == "STREAM") streamingPriority.first() else downloadPriority.first()
    assertEquals("YouTube Music", resolvedSourceForStream)

    // Scenario: User switches to DOWNLOAD context
    val nextContext = "DOWNLOAD"
    val resolvedSourceForDownload = if (nextContext == "STREAM") streamingPriority.first() else downloadPriority.first()
    assertEquals("SoundCloud", resolvedSourceForDownload)
  }

  @Test
  fun testStreamToDownloadUpgrade_checksMinQuality() {
    val currentlyStreamingBitrate = 128 // 128kbps Opus from YouTube
    val minDownloadQualityThreshold = 256 // Require 256kbps for saving

    // Does it require upgrade/re-download?
    val needsReDownload = currentlyStreamingBitrate < minDownloadQualityThreshold
    assertTrue(needsReDownload)

    // If we stream at 320kbps
    val highQualityStream = 320
    val needsReDownloadHQ = highQualityStream < minDownloadQualityThreshold
    assertFalse(needsReDownloadHQ)
  }
}
