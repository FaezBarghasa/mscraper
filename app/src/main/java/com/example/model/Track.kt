package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val imageUrl: String,
    val filePath: String = "",
    val genre: String = ""
)
