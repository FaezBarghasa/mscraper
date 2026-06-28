package com.example.model

data class TrackEntity(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Int, // in seconds
    val bitrate: Int, // in kbps (e.g. 320)
    val sampleRate: Int, // in Hz (e.g. 44100)
    val format: String, // MP3, FLAC, WAV, etc.
    val filePath: String,
    val fileSize: Long,
    val artworkPath: String?,
    val playCount: Int = 0,
    val lastPlayed: Long? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val rating: Float = 0f,
    val customTags: String? = null // Stored as raw string/JSON metadata
)
