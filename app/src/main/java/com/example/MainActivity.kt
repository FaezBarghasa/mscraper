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
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Initialize settings and network config asynchronously
        val settingsRepository = SettingsRepository(this)
        val api = MmDlpApiImpl()

        lifecycleScope.launch(Dispatchers.IO) {
            val enableQuic = settingsRepository.enableQuic.first()
            api.setNetworkConfig(enableQuic)
        }

        setContent {
            MscraperTheme {
                com.example.navigation.MainAppScreen()
            }
        }
    }
}
