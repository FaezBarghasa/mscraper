package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Query("SELECT * FROM songs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 ORDER BY createdAt DESC")
    fun getOffline(): Flow<List<Song>>

    @Delete
    suspend fun delete(song: Song)

    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE url = :url LIMIT 1)")
    suspend fun existsByUrl(url: String): Boolean

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): Song?
}
