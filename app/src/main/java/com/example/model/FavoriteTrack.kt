package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val imageUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
