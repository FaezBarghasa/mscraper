package com.example.core

import android.content.Context

// Settings Data Models (Phase 5, Task 5.1)
data class DownloadSettings(
    val format: String = "MP3",
    val bitrate: String = "320kbps",
    val maxSimultaneous: Int = 3,
    val autoResume: Boolean = true
)

data class PlaybackSettings(
    val crossfadeEnabled: Boolean = true,
    val crossfadeDuration: Float = 3f,
    val equalizerPreset: String = "SYNTHWAVE",
    val spatialAudio: Boolean = false,
    val hapticIntensity: String = "DYNAMIC"
)

data class LibrarySettings(
    val autoScan: Boolean = true,
    val ignoreDuplicates: Boolean = false,
    val scanPaths: String = "/storage/emulated/0/Music/Crysta"
)

data class NetworkSettings(
    val allowMobileData: Boolean = false,
    val cacheSizeMb: Int = 512,
    val autoSync: Boolean = true
)

data class PrivacySettings(
    val sendDiagnostics: Boolean = false,
    val privateSession: Boolean = false,
    val keepHistory: Boolean = true
)

data class AdvancedSettings(
    val bufferMs: Int = 1000,
    val audioFocusGain: Boolean = true
)

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("crysta_settings_v1", Context.MODE_PRIVATE)

    // Save/Load helpers for Settings Categorization (Task 5.2)
    fun saveDownloadSettings(settings: DownloadSettings) {
        prefs.edit().apply {
            putString("dl_format", settings.format)
            putString("dl_bitrate", settings.bitrate)
            putInt("dl_max", settings.maxSimultaneous)
            putBoolean("dl_auto", settings.autoResume)
            apply()
        }
    }

    fun loadDownloadSettings() = DownloadSettings(
        format = prefs.getString("dl_format", "MP3") ?: "MP3",
        bitrate = prefs.getString("dl_bitrate", "320kbps") ?: "320kbps",
        maxSimultaneous = prefs.getInt("dl_max", 3),
        autoResume = prefs.getBoolean("dl_auto", true)
    )

    fun savePlaybackSettings(settings: PlaybackSettings) {
        prefs.edit().apply {
            putBoolean("pb_crossfade", settings.crossfadeEnabled)
            putFloat("pb_cf_dur", settings.crossfadeDuration)
            putString("pb_eq", settings.equalizerPreset)
            putBoolean("pb_spatial", settings.spatialAudio)
            putString("pb_haptic", settings.hapticIntensity)
            apply()
        }
    }

    fun loadPlaybackSettings() = PlaybackSettings(
        crossfadeEnabled = prefs.getBoolean("pb_crossfade", true),
        crossfadeDuration = prefs.getFloat("pb_cf_dur", 3f),
        equalizerPreset = prefs.getString("pb_eq", "SYNTHWAVE") ?: "SYNTHWAVE",
        spatialAudio = prefs.getBoolean("pb_spatial", false),
        hapticIntensity = prefs.getString("pb_haptic", "DYNAMIC") ?: "DYNAMIC"
    )

    fun exportSettingsToJson(): String {
        val dl = loadDownloadSettings()
        val pb = loadPlaybackSettings()
        // Convert to dynamic structured configuration JSON (Task 5.3)
        return """
        {
          "settings_version": 1,
          "download": {
            "format": "${dl.format}",
            "bitrate": "${dl.bitrate}",
            "max_simultaneous": ${dl.maxSimultaneous},
            "auto_resume": ${dl.autoResume}
          },
          "playback": {
            "crossfade_enabled": ${pb.crossfadeEnabled},
            "crossfade_duration": ${pb.crossfadeDuration},
            "equalizer_preset": "${pb.equalizerPreset}",
            "spatial_audio": ${pb.spatialAudio},
            "haptic_intensity": "${pb.hapticIntensity}"
          }
        }
        """.trimIndent()
    }
}
