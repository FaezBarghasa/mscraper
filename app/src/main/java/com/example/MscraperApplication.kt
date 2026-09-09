package com.example

import android.app.Application
import uniffi.mmdlp.uniffiEnsureInitialized

class MscraperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        uniffiEnsureInitialized()
    }
}
