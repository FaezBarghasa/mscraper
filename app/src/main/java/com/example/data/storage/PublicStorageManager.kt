package com.example.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Moves a file from a private temp path into the public MediaStore Downloads collection.
 *
 * On Android 10+ (API 29+) uses [MediaStore.Downloads] with IS_PENDING write protection.
 * On error the pending MediaStore entry is removed before rethrowing.
 */
object PublicStorageManager {

    /**
     * Copies [tempFilePath] into `Download/mscraper/[fileName]` via MediaStore,
     * then deletes [tempFilePath] on success.
     *
     * @param context       Application context.
     * @param tempFilePath  Absolute path to the file produced by the Rust core.
     * @param fileName      Desired public filename (will be sanitized internally).
     * @param mimeType      MIME type (e.g. "audio/mpeg", "audio/flac").
     * @throws IOException  If the copy or MediaStore insert fails.
     */
    suspend fun moveToPublicDownloads(
        context: Context,
        tempFilePath: String,
        fileName: String,
        mimeType: String,
    ) = withContext(Dispatchers.IO) {
        val safeName = sanitizeFileName(fileName)
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/mscraper")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            @Suppress("DEPRECATION")
            Uri.parse("content://downloads/public_downloads")
        }

        val uri = resolver.insert(collectionUri, values)
            ?: throw IOException("MediaStore.insert returned null for '$safeName'")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempFilePath).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            } ?: throw IOException("Could not open MediaStore output stream for '$safeName'")

            // Mark write complete — makes the file visible to other apps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }

            // Delete the Rust core's temp file
            File(tempFilePath).delete()
        } catch (e: Exception) {
            // Clean up dangling pending entry before re-throwing
            resolver.delete(uri, null, null)
            throw IOException("Failed to move '$safeName' to MediaStore: ${e.message}", e)
        }
    }

    /**
     * Replaces characters that are illegal in file names on most file systems.
     */
    fun sanitizeFileName(raw: String): String =
        raw.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
}
