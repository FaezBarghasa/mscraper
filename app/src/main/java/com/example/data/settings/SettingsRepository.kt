package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val PREFERRED_SOURCE = stringPreferencesKey("preferred_source")
        private val PREFERRED_FORMAT = stringPreferencesKey("preferred_format")
        private val ENABLE_QUIC = booleanPreferencesKey("enable_quic")
    }

    val preferredSource: Flow<String> = context.dataStore.data.map { it[PREFERRED_SOURCE] ?: "YouTube" }
    val preferredFormat: Flow<String> = context.dataStore.data.map { it[PREFERRED_FORMAT] ?: "MP3" }
    val enableQuic: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_QUIC] ?: true }

    suspend fun setPreferredSource(source: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_SOURCE] = source
        }
    }

    suspend fun setPreferredFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_FORMAT] = format
        }
    }

    suspend fun setEnableQuic(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_QUIC] = enabled
        }
    }
}
