package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.MmDlpApi
import com.example.core.MmDlpApiImpl
import com.example.data.settings.SettingsRepository
import com.example.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val api: MmDlpApi
) : ViewModel() {

    val enableQuic: StateFlow<Boolean> = repository.enableQuic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultDownloadFormat: StateFlow<String> = repository.defaultDownloadFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MP3")

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun updateEnableQuic(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateEnableQuic(enabled)
            api.setNetworkConfig(enabled)
        }
    }

    fun updateDefaultDownloadFormat(format: String) {
        viewModelScope.launch {
            repository.updateDefaultDownloadFormat(format)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }
}
