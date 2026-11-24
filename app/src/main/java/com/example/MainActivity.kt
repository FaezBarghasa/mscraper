package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.core.MmDlpApiImpl
import com.example.data.settings.SettingsRepository
import com.example.data.settings.ThemeMode
import com.example.ui.theme.MscraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    setContent {
      val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.DARK)
      val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }
      MscraperTheme(darkTheme = darkTheme) {
        com.example.navigation.MainAppScreen()
      }
    }
  }
}
