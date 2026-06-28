package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int // Order position in the playlist
)
