package com.example

import android.app.Application
import com.example.core.ffi.uniffiEnsureInitialized

class MscraperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            uniffiEnsureInitialized()
        } catch (_: Throwable) {
            // Ignore native library loading errors in unit tests / Robolectric environments
        }
    }
}
