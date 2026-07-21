package com.example.util

import android.content.Context
import java.io.File
import java.util.UUID

object CacheHelper {
    fun getTempFile(context: Context, extension: String): File {
        val cacheDir = File(context.cacheDir, "mscraper_temp")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "${UUID.randomUUID()}.$extension")
    }

    fun clearTempFiles(context: Context) {
        val cacheDir = File(context.cacheDir, "mscraper_temp")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}
