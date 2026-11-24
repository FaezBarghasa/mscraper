package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.TrackEntity
import java.io.File

class SharingService(private val context: Context) {

    private fun resolveUri(filePath: String): Uri? {
        return try {
            if (filePath.startsWith("content://") || filePath.startsWith("http://") || filePath.startsWith("https://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // Share track via standard Android share Intent with valid stream URI
    fun shareSingleTrack(track: TrackEntity) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Sharing track: ${track.title} - ${track.artist ?: "Unknown"}")
            putExtra(Intent.EXTRA_TEXT, "Track: ${track.title} by ${track.artist ?: "Unknown"}")
            
            val contentUri = resolveUri(track.filePath)
            if (contentUri != null && !track.filePath.startsWith("http")) {
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "audio/*"
            } else {
                type = "text/plain"
            }
        }
        val shareIntent = Intent.createChooser(sendIntent, "SHARE AUDIO SIGNAL").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    // Open track in an external player app safely
    fun openInExternalPlayer(track: TrackEntity) {
        val contentUri = resolveUri(track.filePath) ?: return
        val playIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(playIntent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(playIntent, "SELECT AUDIO PLAYER").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    private fun saveAndSharePlaylistFile(fileName: String, content: String, mimeType: String): File {
        val playlistDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "playlists")
        if (!playlistDir.exists()) {
            playlistDir.mkdirs()
        }
        val file = File(playlistDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Exported Playlist: $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "EXPORT PLAYLIST FILE").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return file
    }

    // Export playlist to M3U format and save file
    fun exportToM3u(playlistName: String, tracks: List<TrackEntity>): File {
        val builder = StringBuilder()
        builder.append("#EXTM3U\n")
        tracks.forEach { track ->
            builder.append("#EXTINF:${track.duration},${track.artist ?: "Unknown"} - ${track.title}\n")
            builder.append("${track.filePath}\n")
        }
        val safeName = "${playlistName.replace(Regex("""[/\\:*?"<>|]"""), "_")}.m3u"
        return saveAndSharePlaylistFile(safeName, builder.toString(), "audio/x-mpegurl")
    }

    // Export playlist to PLS format and save file
    fun exportToPls(playlistName: String, tracks: List<TrackEntity>): File {
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
        val safeName = "${playlistName.replace(Regex("""[/\\:*?"<>|]"""), "_")}.pls"
        return saveAndSharePlaylistFile(safeName, builder.toString(), "audio/x-scpls")
    }

    // Export playlist to JSON format and save file
    fun exportToJson(playlistName: String, tracks: List<TrackEntity>): File {
        val builder = StringBuilder()
        builder.append("{\n")
        builder.append("  \"playlist\": \"$playlistName\",\n")
        builder.append("  \"tracks\": [\n")
        tracks.forEachIndexed { i, track ->
            builder.append("    {\n")
            builder.append("      \"title\": \"${track.title.replace("\"", "\\\"")}\",\n")
            builder.append("      \"artist\": \"${(track.artist ?: "").replace("\"", "\\\"")}\",\n")
            builder.append("      \"filePath\": \"${track.filePath.replace("\"", "\\\"")}\",\n")
            builder.append("      \"duration\": ${track.duration}\n")
            builder.append("    }${if (i < tracks.size - 1) "," else ""}\n")
        }
        builder.append("  ]\n")
        builder.append("}\n")
        val safeName = "${playlistName.replace(Regex("""[/\\:*?"<>|]"""), "_")}.json"
        return saveAndSharePlaylistFile(safeName, builder.toString(), "application/json")
    }

    // Export playlist to XML format and save file
    fun exportToXml(playlistName: String, tracks: List<TrackEntity>): File {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<playlist name=\"$playlistName\">\n")
        tracks.forEach { track ->
            builder.append("  <track>\n")
            builder.append("    <title><![CDATA[${track.title}]]></title>\n")
            builder.append("    <artist><![CDATA[${track.artist ?: ""}]]></artist>\n")
            builder.append("    <filePath><![CDATA[${track.filePath}]]></filePath>\n")
            builder.append("    <duration>${track.duration}</duration>\n")
            builder.append("  </track>\n")
        }
        builder.append("</playlist>\n")
        val safeName = "${playlistName.replace(Regex("""[/\\:*?"<>|]"""), "_")}.xml"
        return saveAndSharePlaylistFile(safeName, builder.toString(), "application/xml")
    }
}
