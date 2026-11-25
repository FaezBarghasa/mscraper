package com.example

import android.app.Application
import com.example.core.DownloadManager
import com.example.core.ffi.uniffiEnsureInitialized
import com.example.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MscraperApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloadManager: DownloadManager by lazy {
        DownloadManager(
            trackDao = AppDatabase.getDatabase(this).trackDao(),
            scope = applicationScope,
            context = this
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            uniffiEnsureInitialized()
        } catch (_: Throwable) {
            // Ignore native library loading errors in unit tests / Robolectric environments
        }
    }
}
