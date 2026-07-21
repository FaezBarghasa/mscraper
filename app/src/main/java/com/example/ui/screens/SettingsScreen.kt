package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.settings.SettingsRepositoryImpl

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepositoryImpl(context) }
    val enableQuic by repository.enableQuic.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable QUIC/HTTP3")
            Switch(
                checked = enableQuic,
                onCheckedChange = { 
                    // repository.setEnableQuic(it) // Need scope.launch
                }
            )
        }
    }
}
