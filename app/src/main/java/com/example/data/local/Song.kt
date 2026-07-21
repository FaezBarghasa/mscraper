package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val url: String,
    val filePath: String,
    val duration: String,
    val imageUrl: String,
    val isDownloaded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
