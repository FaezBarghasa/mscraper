package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.TrackEntity

class SharingService(private val context: Context) {

    // Share track via standard Android share Intent (Task 4.1)
    fun shareSingleTrack(track: TrackEntity) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Sharing track: ${track.title} - ${track.artist}")
            putExtra(Intent.EXTRA_TEXT, "Stream this track on Crysta: ${track.title} by ${track.artist} - Location: ${track.filePath}")
            type = "audio/*"
        }
        val shareIntent = Intent.createChooser(sendIntent, "CRYSTA SIGNALS TRANSMISSION").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    // Open track in an external player app (Task 4.2)
    fun openInExternalPlayer(track: TrackEntity) {
        val playIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(track.filePath), "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(playIntent)
        } catch (e: Exception) {
            // Fallback to chooser
            val chooser = Intent.createChooser(playIntent, "SELECT AUDIO DECODER").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // Export playlist to M3U format (Task 4.3)
    fun exportToM3u(playlistName: String, tracks: List<TrackEntity>): String {
        val builder = StringBuilder()
        builder.append("#EXTM3U\n")
        tracks.forEach { track ->
            builder.append("#EXTINF:${track.duration},${track.artist ?: "Unknown"} - ${track.title}\n")
            builder.append("${track.filePath}\n")
        }
        return builder.toString()
    }

    // Export playlist to PLS format (Task 4.3)
    fun exportToPls(playlistName: String, tracks: List<TrackEntity>): String {
        val builder = StringBuilder()
        builder.append("[playlist]\n")
        tracks.forEachIndexed { i, track ->
            val num = i + 1
            builder.append("File$num=${track.filePath}\n")
            builder.append("Title$num=${track.title} - ${track.artist ?: "Unknown"}\n")
            builder.append("Length$num=${track.duration}\n")
        }
        builder.append("NumberOfEntries=${tracks.size}\n")
        builder.append("Version=2\n")
        return builder.toString()
    }

    // Export playlist to JSON format
    fun exportToJson(playlistName: String, tracks: List<TrackEntity>): String {
        val builder = StringBuilder()
        builder.append("{\n")
        builder.append("  \"playlist\": \"$playlistName\",\n")
        builder.append("  \"tracks\": [\n")
        tracks.forEachIndexed { i, track ->
            builder.append("    {\n")
            builder.append("      \"title\": \"${track.title}\",\n")
            builder.append("      \"artist\": \"${track.artist}\",\n")
            builder.append("      \"filePath\": \"${track.filePath}\",\n")
            builder.append("      \"duration\": ${track.duration}\n")
            builder.append("    }${if (i < tracks.size - 1) "," else ""}\n")
        }
        builder.append("  ]\n")
        builder.append("}\n")
        return builder.toString()
    }

    // Export playlist to XML format
    fun exportToXml(playlistName: String, tracks: List<TrackEntity>): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<playlist name=\"$playlistName\">\n")
        tracks.forEach { track ->
            builder.append("  <track>\n")
            builder.append("    <title><![CDATA[${track.title}]]></title>\n")
            builder.append("    <artist><![CDATA[${track.artist}]]></artist>\n")
            builder.append("    <filePath><![CDATA[${track.filePath}]]></filePath>\n")
            builder.append("    <duration>${track.duration}</duration>\n")
            builder.append("  </track>\n")
        }
        builder.append("</playlist>\n")
        return builder.toString()
    }
}
