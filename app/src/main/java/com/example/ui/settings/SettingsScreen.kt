package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.core.MmDlpApiImpl
import com.example.data.settings.SettingsRepository
import com.example.data.settings.ThemeMode
import com.example.ui.theme.DeepVoid
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    musicViewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    repository = SettingsRepository(context),
                    api = MmDlpApiImpl()
                ) as T
            }
        }
    )
    val enableQuic by settingsViewModel.enableQuic.collectAsState()
    val defaultFormat by settingsViewModel.defaultDownloadFormat.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val accentColor by musicViewModel.themePrimary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepVoid)
            )
        },
        containerColor = DeepVoid
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCategory("Network", accentColor)
                SettingsToggle(
                    title = "Enable QUIC / HTTP3",
                    subtitle = "Improves streaming speed but may be incompatible with some networks.",
                    checked = enableQuic,
                    onCheckedChange = { settingsViewModel.updateEnableQuic(it) },
                    accentColor = accentColor,
                    testTag = "quic_switch"
                )
            }

            item {
                SettingsCategory("Storage", accentColor)
                SettingsDropdown(
                    title = "Default Download Format",
                    currentValue = defaultFormat,
                    options = listOf("MP3", "FLAC", "WAV"),
                    onOptionSelected = { settingsViewModel.updateDefaultDownloadFormat(it) },
                    accentColor = accentColor
                )
            }

            item {
                SettingsCategory("Audio DSP", accentColor)
                val spatialAudio by musicViewModel.spatialAudio.collectAsState()
                SettingsToggle(
                    title = "Holographic Spatial Audio",
                    subtitle = "Virtualize immersive 3D sound field",
                    checked = spatialAudio,
                    onCheckedChange = { musicViewModel.toggleSpatialAudio() },
                    accentColor = accentColor
                )
                
                val equalizerEnabled by musicViewModel.equalizerEnabled.collectAsState()
                SettingsToggle(
                    title = "DSP Equalizer",
                    subtitle = "Enable multi-band audio processing",
                    checked = equalizerEnabled,
                    onCheckedChange = { musicViewModel.setEqualizerEnabled(it) },
                    accentColor = accentColor
                )
            }

            item {
                SettingsCategory("Appearance", accentColor)
                val accentColorStr by musicViewModel.cyberAccentColor.collectAsState()
                SettingsDropdown(
                    title = "Neon Accent Sync",
                    currentValue = accentColorStr,
                    options = listOf("NEON TOKYO", "DATA GLITCH", "ACID RAIN", "CYAN", "DYNAMIC"),
                    onOptionSelected = { musicViewModel.setCyberAccentColor(it) },
                    accentColor = accentColor
                )

                SettingsDropdown(
                    title = "Theme Mode",
                    currentValue = themeMode.name,
                    options = ThemeMode.entries.map { it.name },
                    onOptionSelected = { settingsViewModel.updateThemeMode(ThemeMode.valueOf(it)) },
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String, accentColor: Color) {
    Text(
        text = title.uppercase(),
        color = accentColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsDropdown(
    title: String,
    currentValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(currentValue, color = accentColor, style = MaterialTheme.typography.bodySmall)
        }
        Box {
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.Gray)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DeepVoid)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
