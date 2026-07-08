package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import com.example.core.MmDlpApiImpl
import com.example.data.settings.SettingsRepository
import com.example.navigation.MainAppScreen
import com.example.ui.components.CRTEffect
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import uniffi.mmdlp.MmDlpEngine

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // JNA configuration for Android
    System.setProperty("jna.nosys", "false")
    System.setProperty("jna.nounpack", "false")
    System.setProperty("jna.boot.library.path", applicationInfo.nativeLibraryDir)

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize settings and network config
    val settingsRepository = com.example.data.settings.SettingsRepositoryImpl(this)
    val api = MmDlpApiImpl()
    
    // Use runBlocking for initial setup before anything else starts
    runBlocking {
        val enableQuic = settingsRepository.enableQuic.first()
        api.setNetworkConfig(enableQuic)
    }

    // Start local Rust Actix backend server in the background
    try {
        val engine = MmDlpEngine()
        engine.startBackendServer(8080.toUShort())
        android.util.Log.d("MainActivity", "Rust Actix server started on port 8080")
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Failed to start Rust Actix server", e)
    }

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = DeepVoid) {
            Box(modifier = Modifier.fillMaxSize()) {
                MainAppScreen()
                CRTEffect()
            }
        }
      }
    }
  }
}
