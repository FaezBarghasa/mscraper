package com.example.data.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri

object MediaScanner {
    fun scan(context: Context, path: String, mimeType: String, onComplete: (Uri?) -> Unit = {}) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            arrayOf(mimeType)
        ) { _, uri ->
            onComplete(uri)
        }
    }
}
