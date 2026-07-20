package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

interface SettingsRepository {
    val enableQuic: Flow<Boolean>
    val defaultDownloadFormat: Flow<String>
    val themeMode: Flow<ThemeMode>
    suspend fun updateEnableQuic(enabled: Boolean)
    suspend fun updateDefaultDownloadFormat(format: String)
    suspend fun updateThemeMode(mode: ThemeMode)
}

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object PreferencesKeys {
        val ENABLE_QUIC = booleanPreferencesKey("enable_quic")
        val DEFAULT_DOWNLOAD_FORMAT = stringPreferencesKey("default_download_format")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val enableQuic: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ENABLE_QUIC] ?: true
        }

    override val defaultDownloadFormat: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_DOWNLOAD_FORMAT] ?: "MP3"
        }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    override suspend fun updateEnableQuic(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_QUIC] = enabled
        }
    }

    override suspend fun updateDefaultDownloadFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_DOWNLOAD_FORMAT] = format
        }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
}
