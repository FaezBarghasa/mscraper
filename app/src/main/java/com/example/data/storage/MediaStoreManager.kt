package com.example.data.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class MediaStoreManager(private val context: Context) {

    suspend fun createPendingAudioUri(fileName: String, mimeType: String): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Download/mscraper")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        resolver.insert(collection, contentValues)
    }

    suspend fun writeCacheFileToUri(cacheFile: File, uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openOutputStream(uri)?.use { outputStream ->
            FileInputStream(cacheFile).use { inputStream ->
                inputStream.copyTo(outputStream, 64 * 1024)
            }
        } ?: throw IOException("Failed to open output stream for URI: $uri")
    }

    suspend fun finalizeFile(uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            resolver.update(uri, contentValues, null, null)
        }

        // Trigger media scan to ensure it shows up in other apps immediately
        // For Android < 10, we might need the actual file path, which is tricky from Uri.
        // But the requirement says IS_PENDING, which is Q+.
        
        val filePath = getFilePathFromUri(uri)
        if (filePath != null) {
            MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }
}
