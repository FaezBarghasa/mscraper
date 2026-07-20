package com.example.data.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Wraps [MediaScannerConnection.scanFile] as both a callback (for legacy callers)
 * and a suspend function (for coroutine callers).
 */
object MediaScanner {

    /**
     * Synchronous callback variant — retains backward compatibility.
     */
    fun scan(context: Context, path: String, mimeType: String, onComplete: (Uri?) -> Unit = {}) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            arrayOf(mimeType),
        ) { _, uri -> onComplete(uri) }
    }

    /**
     * Suspend variant — awaits the scan callback before returning.
     * Safe to call from any coroutine context; switches to [Dispatchers.IO] internally.
     *
     * @return The scanned content URI, or null if the scanner did not produce one.
     */
    suspend fun scanFile(
        context: Context,
        filePath: String,
        mimeType: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                mimeType?.let { arrayOf(it) },
            ) { _, uri ->
                continuation.resume(uri?.toString())
            }
        }
    }
}
