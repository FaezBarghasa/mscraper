package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepositoryImpl(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val ENABLE_QUIC = booleanPreferencesKey("enable_quic")
        private val DOWNLOAD_FORMAT = stringPreferencesKey("download_format")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { 
        ThemeMode.valueOf(it[THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }

    val enableQuic: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_QUIC] ?: true }

    val defaultDownloadFormat: Flow<String> = context.dataStore.data.map { it[DOWNLOAD_FORMAT] ?: "MP3" }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun updateEnableQuic(enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_QUIC] = enabled }
    }

    suspend fun updateDefaultDownloadFormat(format: String) {
        context.dataStore.edit { it[DOWNLOAD_FORMAT] = format }
    }
}
