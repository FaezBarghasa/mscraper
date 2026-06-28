package com.example.model

data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int // Order position in the playlist
)
