package com.example.model

data class FavoriteTrack(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val imageUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
