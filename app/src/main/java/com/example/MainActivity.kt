package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.core.MmDlpApiImpl
import com.example.data.settings.SettingsRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MscraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uniffi.mmdlp.MmDlpEngine

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // JNA configuration for Android
    System.setProperty("jna.nosys", "false")
    System.setProperty("jna.nounpack", "false")
    System.setProperty("jna.boot.library.path", applicationInfo.nativeLibraryDir)

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize settings and network config asynchronously
    val settingsRepository = SettingsRepository(this)
    val api = MmDlpApiImpl()
    
    lifecycleScope.launch(Dispatchers.IO) {
        val enableQuic = settingsRepository.enableQuic.first()
        api.setNetworkConfig(enableQuic)
    }

    // Start local Rust Actix backend server in the background
    try {
        val engine = MmDlpEngine()
        engine.startBackendServer(8080.toUShort())
        android.util.Log.d("MainActivity", "Rust Actix server started on port 8080")
    } catch (e: Throwable) {
        android.util.Log.e("MainActivity", "Failed to start Rust Actix server", e)
    }

    setContent {
      MscraperTheme {
        val navController = rememberNavController()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNavigation(navController = navController)
        }
      }
    }
  }
}
